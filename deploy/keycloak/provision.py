# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Provisionne l'identité NKULKAP de la pile de développement (étape 2.4, ADR-010, ADR-018).

1. Keycloak (API d'administration) : royaume `nkulkap`, rôles de la section 9.1, rôle composite
   `mfa_requise` porté par les rôles sensibles, flux d'authentification qui exigent un code TOTP pour
   ce rôle (navigateur et mot de passe direct), clients, utilisateurs FICTIFS.
2. Fineract (API, en mode Basic) : rôles de même nom avec leurs permissions, utilisateurs de même nom.

Rejouable : tout est recherché avant création. Données FICTIVES : deploy/keycloak/fictif-users.json.
Réseau : délai de 30 s par requête, aucune nouvelle tentative, arrêt au premier échec.

Usage : python deploy/keycloak/provision.py   (KEYCLOAK_URL, KEYCLOAK_ADMIN, KEYCLOAK_ADMIN_PASSWORD, FINERACT_*)
"""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent.parent / "testing" / "fixtures" / "seed"))
from fineract_client import Fineract  # noqa: E402

REALM = "nkulkap"
ROLES = json.loads((HERE / "roles.json").read_text(encoding="utf-8"))
FICTIF = json.loads((HERE / "fictif-users.json").read_text(encoding="utf-8"))
TIMEOUT_S = 30


def log(msg: str) -> None:
    sys.stdout.reconfigure(encoding="utf-8")
    print(msg, flush=True)


class Keycloak:
    def __init__(self) -> None:
        self.base = os.environ.get("KEYCLOAK_URL", "http://localhost:8180").rstrip("/")
        # Compte d'administration FICTIF de deploy/compose/dev.yml.
        form = urllib.parse.urlencode({
            "grant_type": "password", "client_id": "admin-cli",
            "username": os.environ.get("KEYCLOAK_ADMIN", "admin"),
            "password": os.environ.get("KEYCLOAK_ADMIN_PASSWORD", "admin"),
        }).encode()
        self.token = self._send("POST", "/realms/master/protocol/openid-connect/token", form,
                                "application/x-www-form-urlencoded")[1]["access_token"]

    def _send(self, method: str, path: str, data: bytes | None, ctype: str = "application/json"):
        req = urllib.request.Request(self.base + path, data=data, method=method)
        if data is not None:
            req.add_header("Content-Type", ctype)
        if getattr(self, "token", None):
            req.add_header("Authorization", f"Bearer {self.token}")
        try:
            with urllib.request.urlopen(req, timeout=TIMEOUT_S) as r:
                raw = r.read()
                return r.status, (json.loads(raw) if raw else None), r.headers
        except urllib.error.HTTPError as e:
            return e.code, e.read().decode(errors="replace"), e.headers

    def call(self, method: str, path: str, body=None, ok=(200, 201, 204)):
        status, out, headers = self._send(method, "/admin/realms" + path, json.dumps(body).encode() if body is not None else None)
        if status not in ok:
            raise RuntimeError(f"Keycloak {method} {path} -> HTTP {status} : {out}")
        return out, headers

    def exists(self, path: str) -> bool:
        status, _, _ = self._send("GET", "/admin/realms" + path, None)
        return status == 200


def provision_keycloak(kc: Keycloak) -> None:
    if not kc.exists(f"/{REALM}"):
        kc.call("POST", "", {"realm": REALM, "enabled": True, "displayName": "NKULKAP (développement, FICTIF)"})
        log(f"Royaume créé : {REALM}")
    kc.call("PUT", f"/{REALM}", {
        "realm": REALM, "enabled": True,
        "accessTokenLifespan": 300, "ssoSessionIdleTimeout": 1800,
        "bruteForceProtected": True, "failureFactor": 5,
        "passwordPolicy": "length(12) and upperCase(1) and lowerCase(1) and digits(1) and specialChars(1)",
        "otpPolicyType": "totp", "otpPolicyAlgorithm": "HmacSHA1", "otpPolicyDigits": 6, "otpPolicyPeriod": 30,
    })

    mfa = ROLES["mfaRole"]
    for name, desc in [(mfa, "Double authentification requise")] + [(r, v["description"]) for r, v in ROLES["roles"].items()]:
        if not kc.exists(f"/{REALM}/roles/{name}"):
            kc.call("POST", f"/{REALM}/roles", {"name": name, "description": desc})
            log(f"Rôle créé : {name}")
    mfa_role, _ = kc.call("GET", f"/{REALM}/roles/{mfa}")
    for name, spec in ROLES["roles"].items():
        if spec["mfa"]:
            kc.call("POST", f"/{REALM}/roles/{name}/composites", [mfa_role])

    provision_flows(kc, mfa)
    provision_clients(kc)
    provision_users(kc)


def flow_executions(kc: Keycloak, alias: str) -> list[dict]:
    out, _ = kc.call("GET", f"/{REALM}/authentication/flows/{urllib.parse.quote(alias)}/executions")
    return out


def add_execution(kc: Keycloak, flow: str, provider: str, requirement: str, config: dict | None = None) -> None:
    kc.call("POST", f"/{REALM}/authentication/flows/{urllib.parse.quote(flow)}/executions/execution", {"provider": provider})
    ex = [e for e in flow_executions(kc, flow) if e.get("providerId") == provider and e["level"] == 0][-1]
    ex["requirement"] = requirement
    kc.call("PUT", f"/{REALM}/authentication/flows/{urllib.parse.quote(flow)}/executions", ex)
    if config:
        kc.call("POST", f"/{REALM}/authentication/executions/{ex['id']}/config", {"alias": f"{flow}-{provider}", "config": config})


def add_subflow(kc: Keycloak, parent: str, alias: str, requirement: str) -> None:
    kc.call("POST", f"/{REALM}/authentication/flows/{urllib.parse.quote(parent)}/executions/flow",
            {"alias": alias, "type": "basic-flow", "provider": "registration-page-form", "description": alias})
    ex = [e for e in flow_executions(kc, parent) if e.get("displayName") == alias and e["level"] == 0][0]
    ex["requirement"] = requirement
    kc.call("PUT", f"/{REALM}/authentication/flows/{urllib.parse.quote(parent)}/executions", ex)


def provision_flows(kc: Keycloak, mfa: str) -> None:
    """Flux NKULKAP : un code TOTP est exigé de tout utilisateur portant le rôle mfa_requise."""
    flows, _ = kc.call("GET", f"/{REALM}/authentication/flows")
    existing = {f["alias"] for f in flows}
    role_condition = {"condUserRole": mfa, "negate": "false"}

    if "nkulkap direct grant" not in existing:
        kc.call("POST", f"/{REALM}/authentication/flows",
                {"alias": "nkulkap direct grant", "providerId": "basic-flow", "topLevel": True, "builtIn": False,
                 "description": "Mot de passe direct, OTP obligatoire pour mfa_requise"})
        add_execution(kc, "nkulkap direct grant", "direct-grant-validate-username", "REQUIRED")
        add_execution(kc, "nkulkap direct grant", "direct-grant-validate-password", "REQUIRED")
        add_subflow(kc, "nkulkap direct grant", "nkulkap direct grant otp", "CONDITIONAL")
        add_execution(kc, "nkulkap direct grant otp", "conditional-user-role", "REQUIRED", role_condition)
        add_execution(kc, "nkulkap direct grant otp", "direct-grant-validate-otp", "REQUIRED")
        log("Flux créé : nkulkap direct grant")

    if "nkulkap browser" not in existing:
        kc.call("POST", f"/{REALM}/authentication/flows",
                {"alias": "nkulkap browser", "providerId": "basic-flow", "topLevel": True, "builtIn": False,
                 "description": "Navigateur, OTP obligatoire pour mfa_requise"})
        add_execution(kc, "nkulkap browser", "auth-cookie", "ALTERNATIVE")
        add_subflow(kc, "nkulkap browser", "nkulkap browser forms", "ALTERNATIVE")
        add_execution(kc, "nkulkap browser forms", "auth-username-password-form", "REQUIRED")
        add_subflow(kc, "nkulkap browser forms", "nkulkap browser otp", "CONDITIONAL")
        add_execution(kc, "nkulkap browser otp", "conditional-user-role", "REQUIRED", role_condition)
        add_execution(kc, "nkulkap browser otp", "auth-otp-form", "REQUIRED")
        log("Flux créé : nkulkap browser")

    kc.call("PUT", f"/{REALM}", {"realm": REALM, "browserFlow": "nkulkap browser", "directGrantFlow": "nkulkap direct grant"})


def tenant_mapper() -> dict:
    # Locataire Fineract porté par le jeton (fineract.security.oidc-federation.tenant-claim-name).
    return {"name": "fineract_tenant", "protocol": "openid-connect", "protocolMapper": "oidc-hardcoded-claim-mapper",
            "config": {"claim.name": "fineract_tenant", "claim.value": FICTIF["tenant"], "jsonType.label": "String",
                       "access.token.claim": "true", "id.token.claim": "true", "userinfo.token.claim": "true"}}


def provision_clients(kc: Keycloak) -> None:
    for client_id, spec in ROLES["clients"].items():
        found, _ = kc.call("GET", f"/{REALM}/clients?clientId={client_id}")
        body = {"clientId": client_id, "enabled": True, "protocol": "openid-connect", "protocolMappers": [tenant_mapper()],
                "frontchannelLogout": True, "attributes": {}}
        if spec["type"] == "public":
            body.update(publicClient=True, standardFlowEnabled=True, directAccessGrantsEnabled=False, serviceAccountsEnabled=False,
                        redirectUris=spec["redirectUris"], webOrigins=spec.get("webOrigins", []))
            body["attributes"]["pkce.code.challenge.method"] = "S256"
        elif spec["type"] == "service":
            body.update(publicClient=False, standardFlowEnabled=False, directAccessGrantsEnabled=False, serviceAccountsEnabled=True,
                        clientAuthenticatorType="client-secret", secret=FICTIF["serviceClients"][client_id])
        else:  # tests : développement uniquement
            body.update(publicClient=True, standardFlowEnabled=False, directAccessGrantsEnabled=True, serviceAccountsEnabled=False)
        if found:
            body.pop("protocolMappers")
            kc.call("PUT", f"/{REALM}/clients/{found[0]['id']}", {**found[0], **body})
        else:
            kc.call("POST", f"/{REALM}/clients", body)
            log(f"Client créé : {client_id}")


def provision_users(kc: Keycloak) -> None:
    users = []
    for u in FICTIF["users"]:
        creds = [{"type": "password", "value": u["password"], "temporary": False}]
        if "totpSecret" in u:
            creds.append({"type": "otp", "userLabel": "FICTIF",
                          "secretData": json.dumps({"value": u["totpSecret"]}),
                          "credentialData": json.dumps({"subType": "totp", "digits": 6, "counter": 0, "period": 30,
                                                        "algorithm": "HmacSHA1"})})
        users.append({"username": u["username"], "enabled": True, "emailVerified": True,
                      "email": f"{u['username']}@example.invalid", "firstName": u["firstName"], "lastName": u["lastName"],
                      "realmRoles": [u["role"]], "credentials": creds})
    # partialImport : seul moyen d'importer un secret TOTP connu ; SKIP laisse intacts les utilisateurs existants.
    out, _ = kc.call("POST", f"/{REALM}/partialImport", {"ifResourceExists": "SKIP", "users": users})
    log(f"Utilisateurs Keycloak : {out.get('added', 0)} ajoutés, {out.get('skipped', 0)} déjà présents")


def random_password() -> str:
    """Mot de passe conforme à la politique Fineract : 12 à 50 caractères, sans caractère répété consécutif."""
    out = "Nk!"
    for c in os.urandom(16).hex():
        if c != out[-1]:
            out += c
    return out + "Zq9#"


def provision_fineract(api: Fineract) -> None:
    known = {p["code"] for p in api.get("/permissions")}
    roles = {r["name"]: r["id"] for r in api.get("/roles")}
    for name, spec in ROLES["roles"].items():
        unknown = [p for p in spec["permissions"] if p not in known]
        if unknown:
            raise RuntimeError(f"Permissions Fineract inconnues pour {name} : {unknown}")
        if name not in roles:
            roles[name] = api.post("/roles", {"name": name, "description": spec["description"]})["resourceId"]
            log(f"Rôle Fineract créé : {name}")
        api.put(f"/roles/{roles[name]}/permissions", {"permissions": {p: True for p in spec["permissions"]}})
    existing = {u["username"] for u in api.get("/users")}
    for u in FICTIF["users"]:
        if u["username"] in existing:
            continue
        # Mot de passe Fineract aléatoire et jamais utilisé : l'utilisateur s'authentifie par Keycloak.
        pwd = random_password()
        api.post("/users", {"username": u["username"], "firstname": u["firstName"], "lastname": u["lastName"],
                            "email": f"{u['username']}@example.invalid", "officeId": FICTIF["officeId"],
                            "roles": [roles[u["role"]]], "sendPasswordToEmail": False, "passwordNeverExpires": True,
                            "password": pwd, "repeatPassword": pwd})
        log(f"Utilisateur Fineract créé : {u['username']} ({u['role']})")


def main() -> int:
    provision_keycloak(Keycloak())
    if "--keycloak-only" not in sys.argv:
        provision_fineract(Fineract())
    log("Identité provisionnée.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
