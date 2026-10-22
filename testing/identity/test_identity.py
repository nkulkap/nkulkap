# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Test d'intégration de l'étape 2.4 : jetons Keycloak (royaume nkulkap) acceptés par Fineract en serveur de
ressources, autorisations portées par les rôles Fineract, double authentification des rôles sensibles.

Exige la pile en mode OIDC : make up && make seed && make identity-provision && make up AUTH=oidc.
Lancement : make identity-test. Utilisateurs FICTIFS : deploy/keycloak/fictif-users.json.

Réseau : délai de 30 s par requête, aucune nouvelle tentative (chaque réponse est observée telle quelle).
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import json
import os
import ssl
import struct
import sys
import time
import unittest
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FICTIF = json.loads((ROOT / "deploy" / "keycloak" / "fictif-users.json").read_text(encoding="utf-8"))
USERS = {u["role"]: u for u in FICTIF["users"]}
KEYCLOAK = os.environ.get("KEYCLOAK_URL", "http://localhost:8180")
FINERACT = os.environ.get("FINERACT_URL", "https://localhost:8443/fineract-provider/api/v1")
TOKEN_URL = f"{KEYCLOAK}/realms/nkulkap/protocol/openid-connect/token"
TIMEOUT_S = 30
# Écart entre deux échecs d'un même utilisateur, au-delà du seuil « connexion rapide » de Keycloak (1 s).
BRUTE_FORCE_GAP_S = 1.5

# Certificat autosigné de la pile locale.
TLS = ssl.create_default_context()
TLS.check_hostname = False
TLS.verify_mode = ssl.CERT_NONE


def totp(secret: str, at: float | None = None) -> str:
    """Code TOTP (RFC 6238, HMAC-SHA1, 6 chiffres, 30 s) pour un secret brut, tel que Keycloak le stocke."""
    counter = struct.pack(">Q", int(at if at is not None else time.time()) // 30)
    digest = hmac.new(secret.encode(), counter, hashlib.sha1).digest()
    offset = digest[-1] & 0x0F
    return f"{(struct.unpack('>I', digest[offset:offset + 4])[0] & 0x7FFFFFFF) % 1_000_000:06d}"


def http(method: str, url: str, body: bytes | None = None, headers: dict[str, str] | None = None) -> tuple[int, object]:
    req = urllib.request.Request(url, data=body, method=method, headers=headers or {})
    try:
        with urllib.request.urlopen(req, timeout=TIMEOUT_S, context=TLS if url.startswith("https") else None) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        raw = e.read()
        try:
            return e.code, json.loads(raw) if raw else None
        except ValueError:
            return e.code, raw.decode(errors="replace")


def token(role: str, otp: str | None = None) -> tuple[int, object]:
    u = USERS[role]
    form = {"grant_type": "password", "client_id": "nkulkap-tests", "username": u["username"], "password": u["password"],
            "scope": "openid"}
    if otp is not None:
        form["totp"] = otp
    return http("POST", TOKEN_URL, urllib.parse.urlencode(form).encode(),
                {"Content-Type": "application/x-www-form-urlencoded"})


def access_token(role: str) -> str:
    otp = totp(USERS[role]["totpSecret"]) if "totpSecret" in USERS[role] else None
    status, body = token(role, otp)
    if status != 200:
        raise AssertionError(f"jeton refusé pour {role} : {status} {body}")
    return body["access_token"]


def fineract(method: str, path: str, bearer: str | None = None, body: dict | None = None,
             extra: dict[str, str] | None = None) -> tuple[int, object]:
    headers = {"Accept": "application/json", "Fineract-Platform-TenantId": FICTIF["tenant"]}
    if bearer:
        headers["Authorization"] = f"Bearer {bearer}"
    if body is not None:
        headers["Content-Type"] = "application/json"
    headers.update(extra or {})
    return http(method, FINERACT + path, json.dumps(body).encode() if body is not None else None, headers)


def error_code(body: object) -> str:
    return body.get("userMessageGlobalisationCode", "") if isinstance(body, dict) else ""


class IdentityTest(unittest.TestCase):
    def test_collecteur_gets_token_and_calls_ping(self) -> None:
        status, body = fineract("GET", "/nkulkap/ping", access_token("collecteur"))
        self.assertEqual(200, status, body)
        self.assertEqual({"status": "ok", "module": "nkulkap-common"}, body)

    def test_collecteur_is_refused_on_accountant_resources(self) -> None:
        bearer = access_token("collecteur")
        status, body = fineract("GET", "/journalentries?limit=1", bearer)
        self.assertEqual(403, status, body)
        self.assertEqual("error.msg.not.authorized", error_code(body), body)
        entry = {"officeId": 1, "transactionDate": "2026-01-03", "currencyCode": "XAF", "dateFormat": "yyyy-MM-dd",
                 "locale": "fr", "credits": [{"glAccountId": 1, "amount": 100}], "debits": [{"glAccountId": 2, "amount": 100}]}
        status, body = fineract("POST", "/journalentries", bearer, entry)
        self.assertEqual(403, status, body)

    def test_collecteur_keeps_its_own_permissions(self) -> None:
        status, body = fineract("GET", "/clients?limit=1", access_token("collecteur"))
        self.assertEqual(200, status, body)

    def test_accountant_needs_second_factor(self) -> None:
        # Succès d'abord : il remet à zéro le compteur d'échecs de la protection anti-force brute.
        status, body = fineract("GET", "/journalentries?limit=1", access_token("comptable"))
        self.assertEqual(200, status, body)
        status, body = token("comptable")
        self.assertEqual(400, status)
        self.assertEqual("invalid_grant", body.get("error"), body)
        # Deux échecs à moins d'une seconde déclenchent le verrouillage « connexion rapide » de Keycloak.
        time.sleep(BRUTE_FORCE_GAP_S)
        status, body = token("comptable", "000000")
        self.assertEqual(400, status, body)

    def test_every_sensitive_role_needs_second_factor(self) -> None:
        for role in ("conformite", "administrateur"):
            with self.subTest(role=role):
                self.assertEqual(400, token(role)[0], f"{role} obtient un jeton sans code TOTP")
                access_token(role)  # avec le code : accepté, et le compteur d'échecs revient à zéro
        for role in ("collecteur", "caissier", "chef_agence", "auditeur"):
            with self.subTest(role=role):
                self.assertEqual(200, token(role)[0], f"{role} refusé sans raison")

    def test_auditor_reads_but_cannot_write(self) -> None:
        bearer = access_token("auditeur")
        self.assertEqual(200, fineract("GET", "/journalentries?limit=1", bearer)[0])
        status, body = fineract("POST", "/clients", bearer, {"officeId": 1, "legalFormId": 1, "firstname": "Sonde",
                                                           "lastname": "FICTIF", "active": False})
        self.assertEqual(403, status, body)

    def test_forged_and_missing_tokens_are_refused(self) -> None:
        genuine = access_token("collecteur").split(".")
        claims = json.loads(base64.urlsafe_b64decode(genuine[1] + "=" * (-len(genuine[1]) % 4)))
        claims["preferred_username"] = USERS["administrateur"]["username"]
        forged_payload = base64.urlsafe_b64encode(json.dumps(claims).encode()).rstrip(b"=").decode()
        forged = ".".join([genuine[0], forged_payload, genuine[2]])
        self.assertEqual(401, fineract("GET", "/nkulkap/ping", forged)[0], "signature non vérifiée")
        self.assertEqual(401, fineract("GET", "/nkulkap/ping")[0], "requête anonyme acceptée")

    def test_basic_auth_is_no_longer_accepted(self) -> None:
        # Identifiants FICTIFS de l'image de développement Fineract (ADR-018 : effet du mode OIDC).
        basic = base64.b64encode(b"mifos:password").decode()
        status, _ = fineract("GET", "/nkulkap/ping", extra={"Authorization": f"Basic {basic}"})
        self.assertEqual(401, status)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    unittest.main(verbosity=2)
