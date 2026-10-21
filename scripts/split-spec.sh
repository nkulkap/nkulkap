#!/usr/bin/env bash
# Découpe le cahier des charges de référence en fiches par section.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SRC_NAME="NKULKAP_cahier_des_charges_et_plan.md"
SRC="$ROOT/docs/specs/$SRC_NAME"
OUT="$ROOT/docs/specs/sections"
TOTAL=1593

# fichier:début-fin
RANGES=(
  "00-en-tete-et-mode-emploi.md:1-68"
  "01-vision-perimetre.md:69-108"
  "02-contraintes-terrain.md:109-131"
  "03-adr.md:132-174"
  "04-architecture-cible.md:175-243"
  "05-stack.md:244-296"
  "06-organisation-depots.md:297-353"
  "07-00-exigences-introduction.md:354-360"
  "07-L1-plan-comptable.md:361-381"
  "07-L2-teg.md:382-405"
  "07-L3-numscript.md:406-423"
  "07-L4-collecte-tontine.md:424-476"
  "07-L5-hors-ligne.md:477-513"
  "07-L6-mobile-money.md:514-544"
  "07-L7-reporting.md:545-562"
  "07-L8-lab-ft.md:563-596"
  "07-L9-interbancaire.md:597-613"
  "08-exigences-non-fonctionnelles.md:614-635"
  "09-securite-conformite.md:636-679"
  "10-strategie-tests.md:680-734"
  "11-00-plan-vue-ensemble.md:735-761"
  "11-phase-00-preparer.md:762-793"
  "11-phase-01-comprendre.md:794-841"
  "11-phase-02-socle-commun.md:842-876"
  "11-phase-03-L1-plan-comptable.md:877-904"
  "11-phase-04-L2-teg.md:905-937"
  "11-phase-05-L3-numscript.md:938-957"
  "11-phase-06-L4-collecte-tontine.md:958-985"
  "11-phase-07-L5-hors-ligne.md:986-1035"
  "11-phase-08-L6-mobile-money.md:1036-1075"
  "11-phase-09-L7-reporting.md:1076-1107"
  "11-phase-10-L8-lab-ft.md:1108-1140"
  "11-phase-11-L9-interbancaire.md:1141-1149"
  "11-phase-12-bout-en-bout.md:1150-1181"
  "11-phase-13-migration-pilote.md:1182-1197"
  "12-entrees-humaines.md:1198-1220"
  "13-risques-points-ouverts.md:1221-1243"
  "14-annexe-A-gabarit-claude-md.md:1244-1281"
  "14-annexe-B-numscript.md:1282-1382"
  "14-annexe-C-protocole-sync.md:1383-1465"
  "14-annexe-D-machines-etats.md:1466-1518"
  "14-annexe-E-calcul-teg.md:1519-1541"
  "14-annexe-F-glossaire.md:1542-1568"
  "14-annexe-G-sources.md:1569-1593"
)

[ -f "$SRC" ] || { echo "Référence introuvable : $SRC" >&2; exit 1; }
actual=$(wc -l < "$SRC" | tr -d ' ')
[ "$actual" -eq "$TOTAL" ] || { echo "La référence a $actual lignes (attendu $TOTAL) : mettre à jour les plages." >&2; exit 1; }

# Contrôle de couverture sur la liste des plages : pas de trou, pas de chevauchement.
expected=1
for entry in "${RANGES[@]}"; do
  range="${entry#*:}"; a="${range%-*}"; b="${range#*-}"
  if [ "$a" -ne "$expected" ] || [ "$b" -lt "$a" ]; then
    echo "Plage incohérente pour ${entry%%:*} : $a-$b (début attendu $expected)" >&2; exit 1
  fi
  expected=$((b + 1))
done
[ "$expected" -eq $((TOTAL + 1)) ] || { echo "Couverture incomplète : s'arrête à $((expected - 1)) sur $TOTAL" >&2; exit 1; }

mkdir -p "$OUT"
rm -f "$OUT"/*.md

count=0
for entry in "${RANGES[@]}"; do
  name="${entry%%:*}"; range="${entry#*:}"; a="${range%-*}"; b="${range#*-}"
  {
    printf '<!-- Généré par scripts/split-spec.sh depuis %s, lignes %s à %s. Ne pas modifier à la main : modifier la référence puis relancer le script. -->\n\n' "$SRC_NAME" "$a" "$b"
    sed -n "${a},${b}p" "$SRC" | awk '
      { lines[NR] = $0 }
      END {
        n = NR
        if (n > 0 && lines[n] == "---") n--
        for (i = 1; i <= n; i++) print lines[i]
      }'
  } > "$OUT/$name"
  count=$((count + 1))
done

echo "$count fiches écrites dans docs/specs/sections/ (couverture 1 à $TOTAL vérifiée)"
