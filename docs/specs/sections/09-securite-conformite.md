<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 636 à 679. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 9. Sécurité, conformité, données personnelles et licences

### 9.1 Sécurité

- TLS 1.2 minimum (1.3 de préférence) partout. Épinglage de certificat dans `nkulkap-field` et `nkulkap-edge`.
- Rôles : collecteur, caissier, chef d'agence, comptable, conformité, auditeur (lecture seule), administrateur. Maker-checker de Fineract activé pour les opérations sensibles : écritures manuelles, modification de produits, déblocage de compte.
- Appareils : clé Ed25519 par appareil, lots signés, révocation immédiate.
- Journal d'audit en ajout seul pour les modules NKULKAP ; celui des commandes Fineract est déjà présent.
- Secrets hors du code (SOPS + age) ; rotation documentée.
- Analyse de dépendances (Renovate + audit en CI : `cargo audit`, OWASP Dependency-Check, `npm audit`, sbt-dependency-check).
- Revue de sécurité avant le pilote : test d'intrusion externe recommandé.

### 9.2 Données personnelles (loi n° 2024/017)

Points relevés dans une source secondaire **[À VÉRIFIER sur le texte officiel avec un juriste]** :
- consentement explicite, retirable ;
- minimisation des données ;
- droits d'accès, de rectification, d'effacement et d'opposition ;
- délégué à la protection des données pour les grandes organisations bancaires ;
- autorisation préalable pour tout transfert hors du Cameroun.

Conséquences pour NKULKAP :
- registre des consentements par client ;
- export des données d'un client sur demande ;
- effacement ou anonymisation compatible avec les obligations de conservation bancaire et LAB-FT (la conservation légale prime) ;
- hébergement au Cameroun par défaut (ADR-011) ;
- en SaaS, rôle de sous-traitant formalisé par contrat avec chaque EMF.

### 9.3 Licences

| Composant amont | Licence | Conséquence |
|---|---|---|
| Apache Fineract | Apache 2.0 | Les modules `custom/nkulkap` peuvent rester propriétaires ; conserver les fichiers NOTICE et LICENSE |
| `openMF/web-app`, `openMF/android-client` | MPL-2.0 | Copyleft **par fichier** : tout fichier MPL modifié doit rester sous MPL et son source être mis à disposition des utilisateurs ; les fichiers nouveaux peuvent être propriétaires. Placer le code NKULKAP dans des fichiers et modules nouveaux autant que possible |
| Numscript | MIT | Libre usage ; conserver la mention de copyright |
| Kafka, Keycloak | Apache 2.0 | Libre usage |
| PostgreSQL | Licence PostgreSQL | Libre usage |
| Camunda 8.6+ / Zeebe | Licence de production payante | **Évité** (ADR-007) |
| k6 | AGPL-3.0 | Outil interne uniquement, jamais distribué |

> Ce tableau n'est pas un avis juridique : à faire valider par un juriste avant la commercialisation.

---

