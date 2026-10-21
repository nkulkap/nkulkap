# Spécifications NKULKAP

`NKULKAP_cahier_des_charges_et_plan.md` est la référence complète du cahier des charges et du plan d'implémentation. Le dossier `sections/` en est un découpage généré, pour ne charger que la partie utile. Ne jamais éditer une fiche à la main : modifier la référence puis relancer `bash scripts/split-spec.sh`.

## En-tête et cadrage (00-06)

| Fiche | Contenu | Lignes |
|---|---|---|
| [00-en-tete-et-mode-emploi.md](sections/00-en-tete-et-mode-emploi.md) | Titre, table des matières, mode d'emploi (règles d'or, exécution d'une étape) | 1-68 |
| [01-vision-perimetre.md](sections/01-vision-perimetre.md) | Vision, problème, cibles, neuf lignes du périmètre, hors périmètre | 69-108 |
| [02-contraintes-terrain.md](sections/02-contraintes-terrain.md) | Contraintes du terrain camerounais | 109-131 |
| [03-adr.md](sections/03-adr.md) | Décisions d'architecture (ADR) | 132-174 |
| [04-architecture-cible.md](sections/04-architecture-cible.md) | Architecture cible, flux d'une collecte hors-ligne | 175-243 |
| [05-stack.md](sections/05-stack.md) | Stack technologique : langages, bibliothèques, infrastructure | 244-296 |
| [06-organisation-depots.md](sections/06-organisation-depots.md) | Organisation des dépôts | 297-353 |

## Exigences par ligne (07)

| Fiche | Contenu | Lignes |
|---|---|---|
| [07-00-exigences-introduction.md](sections/07-00-exigences-introduction.md) | Introduction aux exigences fonctionnelles | 354-360 |
| [07-L1-plan-comptable.md](sections/07-L1-plan-comptable.md) | L1 : plan comptable CEMAC et hors-bilan | 361-381 |
| [07-L2-teg.md](sections/07-L2-teg.md) | L2 : TEG et plafond réglementaire | 382-405 |
| [07-L3-numscript.md](sections/07-L3-numscript.md) | L3 : commissions et répartitions en Numscript | 406-423 |
| [07-L4-collecte-tontine.md](sections/07-L4-collecte-tontine.md) | L4 : collecte journalière, agent collecteur, tontine | 424-476 |
| [07-L5-hors-ligne.md](sections/07-L5-hors-ligne.md) | L5 : hors-ligne (sync, field, edge) | 477-513 |
| [07-L6-mobile-money.md](sections/07-L6-mobile-money.md) | L6 : MTN MoMo et Orange Money | 514-544 |
| [07-L7-reporting.md](sections/07-L7-reporting.md) | L7 : états COBAC et centrale des risques | 545-562 |
| [07-L8-lab-ft.md](sections/07-L8-lab-ft.md) | L8 : LAB-FT et fraude | 563-596 |
| [07-L9-interbancaire.md](sections/07-L9-interbancaire.md) | L9 : interbancaire CEMAC | 597-613 |

## Exigences transverses (08-10)

| Fiche | Contenu | Lignes |
|---|---|---|
| [08-exigences-non-fonctionnelles.md](sections/08-exigences-non-fonctionnelles.md) | Exigences non fonctionnelles | 614-635 |
| [09-securite-conformite.md](sections/09-securite-conformite.md) | Sécurité, données personnelles, licences | 636-679 |
| [10-strategie-tests.md](sections/10-strategie-tests.md) | Stratégie de tests : pyramide, profils réseau, scénarios E2E, données | 680-734 |

## Plan par phase (11)

| Fiche | Contenu | Lignes |
|---|---|---|
| [11-00-plan-vue-ensemble.md](sections/11-00-plan-vue-ensemble.md) | Plan d'implémentation : vue d'ensemble | 735-761 |
| [11-phase-00-preparer.md](sections/11-phase-00-preparer.md) | Phase 0 : préparer | 762-793 |
| [11-phase-01-comprendre.md](sections/11-phase-01-comprendre.md) | Phase 1 : comprendre | 794-841 |
| [11-phase-02-socle-commun.md](sections/11-phase-02-socle-commun.md) | Phase 2 : socle commun | 842-876 |
| [11-phase-03-L1-plan-comptable.md](sections/11-phase-03-L1-plan-comptable.md) | Phase 3 : L1 plan comptable et hors-bilan | 877-904 |
| [11-phase-04-L2-teg.md](sections/11-phase-04-L2-teg.md) | Phase 4 : L2 TEG et usure | 905-937 |
| [11-phase-05-L3-numscript.md](sections/11-phase-05-L3-numscript.md) | Phase 5 : L3 Numscript | 938-957 |
| [11-phase-06-L4-collecte-tontine.md](sections/11-phase-06-L4-collecte-tontine.md) | Phase 6 : L4 collecte et tontine | 958-985 |
| [11-phase-07-L5-hors-ligne.md](sections/11-phase-07-L5-hors-ligne.md) | Phase 7 : L5 hors-ligne | 986-1035 |
| [11-phase-08-L6-mobile-money.md](sections/11-phase-08-L6-mobile-money.md) | Phase 8 : L6 mobile money et SMS | 1036-1075 |
| [11-phase-09-L7-reporting.md](sections/11-phase-09-L7-reporting.md) | Phase 9 : L7 reporting réglementaire | 1076-1107 |
| [11-phase-10-L8-lab-ft.md](sections/11-phase-10-L8-lab-ft.md) | Phase 10 : L8 LAB-FT | 1108-1140 |
| [11-phase-11-L9-interbancaire.md](sections/11-phase-11-L9-interbancaire.md) | Phase 11 : L9 interbancaire | 1141-1149 |
| [11-phase-12-bout-en-bout.md](sections/11-phase-12-bout-en-bout.md) | Phase 12 : bout en bout et industrialisation | 1150-1181 |
| [11-phase-13-migration-pilote.md](sections/11-phase-13-migration-pilote.md) | Phase 13 : migration et pilote | 1182-1197 |

## Entrées humaines et risques (12-13)

| Fiche | Contenu | Lignes |
|---|---|---|
| [12-entrees-humaines.md](sections/12-entrees-humaines.md) | Entrées humaines requises | 1198-1220 |
| [13-risques-points-ouverts.md](sections/13-risques-points-ouverts.md) | Risques et points ouverts | 1221-1243 |

## Annexes (14)

| Fiche | Contenu | Lignes |
|---|---|---|
| [14-annexe-A-gabarit-claude-md.md](sections/14-annexe-A-gabarit-claude-md.md) | Annexe A : gabarit de CLAUDE.md | 1244-1281 |
| [14-annexe-B-numscript.md](sections/14-annexe-B-numscript.md) | Annexe B : règles Numscript validées | 1282-1382 |
| [14-annexe-C-protocole-sync.md](sections/14-annexe-C-protocole-sync.md) | Annexe C : protocole de synchronisation v1 | 1383-1465 |
| [14-annexe-D-machines-etats.md](sections/14-annexe-D-machines-etats.md) | Annexe D : machines à états | 1466-1518 |
| [14-annexe-E-calcul-teg.md](sections/14-annexe-E-calcul-teg.md) | Annexe E : calcul du TEG | 1519-1541 |
| [14-annexe-F-glossaire.md](sections/14-annexe-F-glossaire.md) | Annexe F : glossaire | 1542-1568 |
| [14-annexe-G-sources.md](sections/14-annexe-G-sources.md) | Annexe G : sources | 1569-1593 |
