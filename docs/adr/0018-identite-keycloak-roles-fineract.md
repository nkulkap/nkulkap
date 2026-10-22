# ADR-018 — Identité : Keycloak authentifie, Fineract autorise ; mode OIDC distinct du mode Basic de développement

- Statut : Proposé
- Date : 2026-10-22
- Source : étape 2.4 ; lecture de `OidcFederationSecurityConfig`, `FineractOidcUserService` et `DynamicJwtIssuerAuthenticationManagerResolver` (Fineract `b94b2ddf`)

## Contexte

ADR-010 retient Keycloak (OIDC) et Fineract en serveur de ressources OAuth2. Fineract `b94b2ddf` offre une « fédération OIDC » (`fineract.security.oidc-federation.*`) dont le fonctionnement impose trois contraintes :
- le jeton est associé à l'utilisateur Fineract de même nom (`preferred_username`) ; les **autorisations viennent des rôles Fineract** de cet utilisateur, et les rôles Keycloak sont ignorés ;
- l'émetteur (`iss`) doit figurer dans la liste configurée ; les clés peuvent être lues à une autre adresse (`jwks-uri`), ce qui règle l'écart entre `localhost:8180` (poste) et `keycloak:8080` (réseau Compose) ;
- la chaîne de sécurité OIDC (ordre 100) passe avant la chaîne Basic et traite tout `/api/**` : en mode OIDC, l'authentification Basic n'est plus acceptée. Le seed, les tests d'exploration et les outils de la phase 1 reposent sur Basic.

## Décision

1. **Keycloak authentifie, Fineract autorise.** Chaque rôle de la section 9.1 (collecteur, caissier, chef_agence, comptable, conformite, auditeur, administrateur) existe sous le même nom dans le royaume `nkulkap` et dans Fineract, avec ses permissions Fineract (`deploy/keycloak/roles.json`). Les rôles Keycloak servent aux satellites et aux interfaces ; les contrôles d'accès aux données comptables restent ceux de Fineract.
2. **Aucune création automatique d'utilisateur** (`auto-create-user=false`) : un utilisateur Keycloak sans compte Fineract est refusé. Les comptes sont provisionnés ensemble (`make identity-provision`).
3. **Double authentification dans Keycloak** : les rôles comptable, conformite et administrateur portent le rôle composite `mfa_requise` ; les flux `nkulkap browser` et `nkulkap direct grant` exigent un code TOTP de tout porteur de ce rôle. Le mécanisme 2FA propre à Fineract n'est pas utilisé.
4. **Deux modes pour la pile de développement** : `AUTH=basic` (défaut, outillage de développement) et `AUTH=oidc` (`deploy/compose/oidc.yml`). Recette et production n'utilisent que le mode OIDC ; les satellites y obtiennent leurs jetons par leurs clients de service.
5. Le client `nkulkap-tests` (mot de passe direct) n'existe que dans le royaume de développement ; `nkulkap-web` et `nkulkap-field` utilisent le flux code d'autorisation avec PKCE.

## Conséquences

- Tout changement de rôle se fait dans `roles.json`, puis `make identity-provision` met à jour Keycloak et Fineract ; l'export versionné `deploy/keycloak/realm-nkulkap.json` est importé au démarrage de Keycloak.
- La permission fine reste dans Fineract : un jeton valide n'ouvre que ce que le rôle Fineract autorise (vérifié : un collecteur reçoit `403 error.msg.not.authorized` sur les écritures comptables).
- Les outils de développement en Basic devront passer en jetons quand le mode OIDC deviendra le mode par défaut (au plus tard en phase 12).
- Les secrets du royaume de développement (mots de passe, TOTP, secrets clients) sont FICTIFS et versionnés ; en recette et en production, ils sont gérés hors du dépôt (SOPS + age, section 9.1).
- Les permissions par rôle sont FICTIVES jusqu'à validation par les EMF pilotes.
- Fineract `b94b2ddf` ne démarre pas en mode OIDC (dépendance circulaire entre `OidcFederationSecurityConfig` et `DynamicJwtIssuerAuthenticationManagerResolver`) ; contournement par configuration, `SPRING_MAIN_ALLOWCIRCULARREFERENCES=true`, sans modifier le cœur. À signaler à Apache Fineract et à retirer quand le défaut sera corrigé.
- La protection anti-force brute de Keycloak est active : deux échecs à moins d'une seconde verrouillent le compte une minute, cinq échecs le verrouillent plus longtemps.
