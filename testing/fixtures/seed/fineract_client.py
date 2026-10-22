# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Client HTTP minimal de l'API Fineract, sans dépendance hors bibliothèque standard.

Réseau : délai de 30 s par requête ; nouvelle tentative (3 au plus, attente 2, 4, 8 s)
sur erreur de connexion, délai dépassé ou réponse 502/503/504 ; toute autre erreur
lève FineractError avec le code HTTP et le corps renvoyé.
"""

from __future__ import annotations

import base64
import json
import os
import ssl
import time
import urllib.error
import urllib.request
from typing import Any

DEFAULT_URL = "https://localhost:8443/fineract-provider/api/v1"
TIMEOUT_S = 30
RETRIES = 3
RETRY_STATUSES = {502, 503, 504}


class FineractError(Exception):
    def __init__(self, method: str, path: str, status: int, body: Any):
        super().__init__(f"{method} {path} -> HTTP {status}: {json.dumps(body, ensure_ascii=False)[:2000]}")
        self.status = status
        self.body = body


class Fineract:
    def __init__(self) -> None:
        self.base = os.environ.get("FINERACT_URL", DEFAULT_URL).rstrip("/")
        self.tenant = os.environ.get("FINERACT_TENANT", "default")
        # Identifiants FICTIFS par défaut de l'image de développement Fineract.
        user = os.environ.get("FINERACT_USER", "mifos")
        password = os.environ.get("FINERACT_PASSWORD", "password")
        self.auth = "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode()
        # Certificat autosigné de la pile locale : vérification désactivée sauf FINERACT_TLS_VERIFY=1.
        self.ssl = ssl.create_default_context()
        if os.environ.get("FINERACT_TLS_VERIFY") != "1":
            self.ssl.check_hostname = False
            self.ssl.verify_mode = ssl.CERT_NONE

    def request(self, method: str, path: str, body: Any = None, headers: dict[str, str] | None = None) -> tuple[int, dict, Any]:
        """Renvoie (statut, en-têtes, corps JSON) ; lève FineractError si statut >= 400."""
        url = self.base + path
        data = json.dumps(body).encode() if body is not None else None
        hdrs = {
            "Authorization": self.auth,
            "Fineract-Platform-TenantId": self.tenant,
            "Accept": "application/json",
        }
        if data is not None:
            hdrs["Content-Type"] = "application/json"
        hdrs.update(headers or {})
        for attempt in range(RETRIES + 1):
            req = urllib.request.Request(url, data=data, method=method, headers=hdrs)
            try:
                with urllib.request.urlopen(req, timeout=TIMEOUT_S, context=self.ssl) as resp:
                    return resp.status, dict(resp.headers), _decode(resp.read())
            except urllib.error.HTTPError as e:
                status, payload = e.code, _decode(e.read())
                if status in RETRY_STATUSES and attempt < RETRIES:
                    time.sleep(2 ** (attempt + 1))
                    continue
                raise FineractError(method, path, status, payload) from None
            except (urllib.error.URLError, TimeoutError, ConnectionError) as e:
                if attempt < RETRIES:
                    time.sleep(2 ** (attempt + 1))
                    continue
                raise FineractError(method, path, 0, str(e)) from None
        raise AssertionError("inaccessible")

    def get(self, path: str) -> Any:
        return self.request("GET", path)[2]

    def post(self, path: str, body: Any, headers: dict[str, str] | None = None) -> Any:
        return self.request("POST", path, body, headers)[2]

    def put(self, path: str, body: Any) -> Any:
        return self.request("PUT", path, body)[2]


def _decode(raw: bytes) -> Any:
    if not raw:
        return None
    try:
        return json.loads(raw)
    except ValueError:
        return raw.decode(errors="replace")
