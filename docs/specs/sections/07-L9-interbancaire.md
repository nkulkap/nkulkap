<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 597 à 613. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L9 — Interbancaire CEMAC (`nkulkap-pay/interbank`)

Cette ligne concerne la cible banques (priorité 3). Les spécifications des systèmes ne sont pas publiques.

**Contexte** : GIMAC exploite la plateforme monétique régionale, et l'écosystème GIMACPAY relie banques, EMF et opérateurs de mobile money. Le règlement final passe par SYGMA (le RTGS de la BEAC). SYSTAC est le système de compensation **[À VÉRIFIER : modalités de connexion, normes de messages]**.

**Exigences**

- **FR-L9-01** — Interface `InterbankAdapter` dans `nkulkap-pay` : `sendCredit`, `receiveCredit`, `status`, `importStatement`, avec la même machine à états et la même idempotence que L6.
- **FR-L9-02** — Implémentation v1 par **fichiers** : export et import de virements dans un format paramétrable. ISO 20022 est l'hypothèse de travail **[À VÉRIFIER]**.
- **FR-L9-03** — Rapprochement des relevés, comme FR-L6-08.
- **FR-L9-04** — Adaptateurs temps réel GIMACPAY et SYGMA réalisés quand les spécifications et l'accès au bac à sable seront obtenus (section 12).

**Acceptation** : test de bout en bout avec un simulateur de fichiers ; le connecteur réel est hors v1 tant que les spécifications manquent.

---

