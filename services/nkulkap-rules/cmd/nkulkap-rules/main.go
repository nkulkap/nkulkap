// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.

// Commande nkulkap-rules (squelette, étape 1.7) : exécute une règle sur une entrée JSON et affiche les écritures.
//
//	nkulkap-rules <règle.num> <entrée.json>   avec entrée.json = {"variables": {"frais": "XAF 3100", ...}}
//
// Le service HTTP sans état viendra en phase 5.
package main

import (
	"encoding/json"
	"fmt"
	"os"

	"github.com/nkulkap/nkulkap/services/nkulkap-rules/internal/engine"
)

func main() {
	if err := run(os.Args[1:]); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func run(args []string) error {
	if len(args) != 2 {
		return fmt.Errorf("usage : nkulkap-rules <règle.num> <entrée.json>")
	}
	script, err := os.ReadFile(args[0])
	if err != nil {
		return err
	}
	raw, err := os.ReadFile(args[1])
	if err != nil {
		return err
	}
	var input struct {
		Variables map[string]string `json:"variables"`
	}
	if err := json.Unmarshal(raw, &input); err != nil {
		return fmt.Errorf("entrée JSON invalide : %w", err)
	}
	postings, err := engine.Run(string(script), input.Variables)
	if err != nil {
		return err
	}
	enc := json.NewEncoder(os.Stdout)
	enc.SetIndent("", "  ")
	return enc.Encode(postings)
}
