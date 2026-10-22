# ADR-017 — Écritures comptables : pas d'événement externe Fineract, lecture par l'API

- Statut : Accepté (validé par l'utilisateur le 2026-10-22)
- Date : 2026-10-22
- Source : étape 1.3, [fineract-events.md](../specs/fineract-events.md)

## Contexte

L'étape 1.3 du cahier demandait d'activer, parmi les événements externes de Fineract, ceux des « écritures comptables ». À la version `b94b2ddf`, aucun type d'événement ne porte les écritures des prêts et de l'épargne : le seul est `WorkingCapitalLoanJournalEntryCreatedBusinessEvent`, réservé aux prêts de fonds de roulement. L'hypothèse du cahier est donc fausse.

Les événements de transaction existent (`SavingsDepositBusinessEvent`, `LoanTransactionMakeRepaymentPostBusinessEvent`…), sont écrits dans la transaction métier et publiés par lot chaque minute. Les écritures se lisent par `GET /journalentries?transactionId=S<id>` (épargne) ou `L<id>` (prêt), formats vérifiés par `IdempotencySpikeIT`, ou par période et par agence. Les écritures manuelles (`POST /journalentries`) et celles des traitements de fin de journée n'ont pas d'événement de transaction associé.

## Décision

1. NKULKAP ne compte sur aucun événement d'écriture comptable de Fineract.
2. Un consommateur qui a besoin des écritures d'une transaction les lit par l'API, à partir de l'identifiant porté par l'événement de transaction (`S<id>`, `L<id>`). Le reporting (phase 9) lit toujours par période et par agence, seule façon d'inclure les écritures manuelles et celles des traitements de fin de journée.
3. Publier un événement d'écriture depuis un module custom reste possible (écouteur métier, étape 1.5) ; ce choix se fera en phase 9 s'il est justifié par un besoin de flux, avec un nouvel ADR.

## Conséquences

- L'étape 1.3 du cahier est corrigée : elle active les événements de clients et de transactions d'épargne et de prêt.
- `nkulkap-sentinel` (FR-L8) et l'instantané de `nkulkap-sync` (FR-L5-06) s'appuient sur les événements de transaction, avec une latence de 0 à 60 s (cron du job « Send Asynchronous Events »).
- Le reporting réglementaire (phase 9) lit les écritures par l'API ou par des rapports Fineract (`runreports`), Fineract restant la seule vérité comptable (ADR-001).
