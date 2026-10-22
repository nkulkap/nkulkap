// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.

package engine

import (
	"os"
	"path/filepath"
	"reflect"
	"testing"
)

// Racine des règles du dépôt : services/nkulkap-rules/internal/engine -> rules/.
const rulesDir = "../../../../rules"

func loadRule(t *testing.T, name string) string {
	t.Helper()
	b, err := os.ReadFile(filepath.Join(rulesDir, name))
	if err != nil {
		t.Fatalf("lecture de %s : %v", name, err)
	}
	return string(b)
}

// Sorties de l'annexe B du cahier, obtenues avec Numscript v0.0.24.
func TestAnnexB(t *testing.T) {
	cases := []struct {
		name  string
		rule  string
		vars  map[string]string
		total int64
		want  []Posting
	}{
		{
			name:  "B.1 frais de collecte",
			rule:  "collecte/frais_collecte.num",
			vars:  map[string]string{"frais": "XAF 3100", "part_collecteur": "40%", "collecteur": "collecteur:42"},
			total: 3100,
			want: []Posting{
				{Source: "emf:frais_a_repartir", Destination: "collecteur:42", Amount: 1240, Asset: "XAF"},
				{Source: "emf:frais_a_repartir", Destination: "emf:produits:commissions_collecte", Amount: 1860, Asset: "XAF"},
			},
		},
		{
			name:  "B.2 frais de retrait mobile money",
			rule:  "momo/frais_retrait.num",
			vars:  map[string]string{"frais_ttc": "XAF 1000", "taux_tva": "77/477", "part_agent": "30%", "agent": "agent:7"},
			total: 1000,
			want: []Posting{
				{Source: "emf:frais_a_repartir", Destination: "etat:tva_collectee", Amount: 162, Asset: "XAF"},
				{Source: "emf:frais_a_repartir", Destination: "agent:7", Amount: 252, Asset: "XAF"},
				{Source: "emf:frais_a_repartir", Destination: "emf:produits:commissions_momo", Amount: 586, Asset: "XAF"},
			},
		},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			got, err := Run(loadRule(t, c.rule), c.vars)
			if err != nil {
				t.Fatalf("exécution : %v", err)
			}
			if !reflect.DeepEqual(got, c.want) {
				t.Fatalf("écritures\n obtenues : %+v\n attendues : %+v", got, c.want)
			}
			var sum int64
			for _, p := range got {
				sum += p.Amount
			}
			if sum != c.total {
				t.Fatalf("la répartition ne conserve pas le montant : %d au lieu de %d", sum, c.total)
			}
		})
	}
}

func TestRejectsInvalidScript(t *testing.T) {
	if _, err := Run("send [XAF 10] (", nil); err == nil {
		t.Fatal("un script invalide doit être refusé")
	}
}

func TestRejectsMissingVariable(t *testing.T) {
	_, err := Run(loadRule(t, "collecte/frais_collecte.num"), map[string]string{"frais": "XAF 3100"})
	if err == nil {
		t.Fatal("une variable manquante doit être refusée")
	}
}
