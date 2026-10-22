// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.

// Commande toxictl : bascule de profil réseau pour les tests et scénarios E2E.
//
//	toxictl init                         crée les mandataires de profiles.yaml
//	toxictl apply <profil> [mandataire]  applique un profil (mandataire par défaut : fineract)
//	toxictl reset                        retire tous les toxiques
//	toxictl list                         liste les profils
//
// TOXIPROXY_URL (défaut http://localhost:8474) et TOXIPROXY_PROFILES (défaut profiles.yaml du dépôt).
// Code de sortie non nul en cas d'échec : un test qui l'appelle doit échouer, pas continuer en réseau normal.
package main

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"

	"github.com/nkulkap/nkulkap/testing/toxiproxy"
)

func main() {
	if err := run(os.Args[1:]); err != nil {
		fmt.Fprintln(os.Stderr, "toxictl :", err)
		os.Exit(1)
	}
}

func profilesPath() string {
	if p := os.Getenv("TOXIPROXY_PROFILES"); p != "" {
		return p
	}
	_, file, _, _ := runtime.Caller(0)
	return filepath.Join(filepath.Dir(file), "..", "..", "profiles.yaml")
}

func run(args []string) error {
	if len(args) == 0 {
		return fmt.Errorf("usage : toxictl init | apply <profil> [mandataire] | reset | list")
	}
	profiles, err := toxiproxy.LoadProfiles(profilesPath())
	if err != nil {
		return err
	}
	c := toxiproxy.NewClient()
	switch args[0] {
	case "list":
		for _, n := range profiles.Names() {
			fmt.Println(n)
		}
		return nil
	case "init":
		for name, p := range profiles.Proxies {
			if err := c.EnsureProxy(name, p); err != nil {
				return err
			}
			fmt.Printf("mandataire %s : %s -> %s\n", name, p.Listen, p.Upstream)
		}
		return nil
	case "reset":
		return c.Reset()
	case "apply":
		if len(args) < 2 {
			return fmt.Errorf("apply : profil manquant")
		}
		proxy := "fineract"
		if len(args) > 2 {
			proxy = args[2]
		}
		if err := c.Apply(profiles, proxy, args[1]); err != nil {
			return err
		}
		fmt.Printf("profil %s appliqué à %s\n", args[1], proxy)
		return nil
	default:
		return fmt.Errorf("commande inconnue : %s", args[0])
	}
}
