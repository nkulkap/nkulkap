// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.

// Package engine exécute une règle Numscript en calcul pur (ADR-005) : les sources sont déclarées
// « allowing unbounded overdraft », aucun solde n'est lu, et le résultat est une liste d'écritures
// à poster dans Fineract, seule vérité comptable (ADR-001).
package engine

import (
	"context"
	"errors"
	"fmt"

	"github.com/formancehq/numscript"
)

// Posting est un mouvement produit par une règle. Montant en unités entières de la devise (XAF).
type Posting struct {
	Source      string `json:"source"`
	Destination string `json:"destination"`
	Amount      int64  `json:"amount"`
	Asset       string `json:"asset"`
}

// ErrInvalidScript signale une règle qui ne se compile pas.
var ErrInvalidScript = errors.New("règle Numscript invalide")

// Run compile et exécute une règle avec ses variables (format texte de Numscript, ex. "XAF 3100", "40%").
func Run(script string, vars map[string]string) ([]Posting, error) {
	parsed := numscript.Parse(script)
	if errs := parsed.GetParsingErrors(); len(errs) > 0 {
		return nil, fmt.Errorf("%w : %s", ErrInvalidScript, numscript.ParseErrorsToString(errs, script))
	}
	// Magasin vide : une règle qui lirait un solde échoue au lieu de dépendre d'un état externe.
	store := numscript.StaticStore{Balances: numscript.Balances{}, Meta: numscript.AccountsMetadata{}}
	res, ierr := parsed.Run(context.Background(), numscript.VariablesMap(vars), store)
	if ierr != nil {
		return nil, fmt.Errorf("exécution de la règle : %w", ierr)
	}
	out := make([]Posting, 0, len(res.Postings))
	for _, p := range res.Postings {
		if p.Amount == nil || !p.Amount.IsInt64() || p.Amount.Sign() < 0 {
			return nil, fmt.Errorf("montant hors bornes pour %s -> %s : %v", p.Source, p.Destination, p.Amount)
		}
		out = append(out, Posting{Source: p.Source, Destination: p.Destination, Amount: p.Amount.Int64(), Asset: p.Asset})
	}
	return out, nil
}
