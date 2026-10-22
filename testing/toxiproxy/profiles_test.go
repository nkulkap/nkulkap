// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.

package toxiproxy

import (
	"net"
	"reflect"
	"testing"
	"time"
)

var netDialer = net.Dialer{Timeout: 10 * time.Second}

// Les profils de la section 10.2 sont tous présents et bien formés (sans pile).
func TestProfilesFile(t *testing.T) {
	p, err := LoadProfiles("profiles.yaml")
	if err != nil {
		t.Fatal(err)
	}
	want := []string{"3g_urbain", "coupure_longue", "edge_rural", "reponse_perdue", "vsat_agence"}
	if got := p.Names(); !reflect.DeepEqual(got, want) {
		t.Fatalf("profils %v, attendus %v", got, want)
	}
	if _, ok := p.Proxies["fineract"]; !ok {
		t.Fatal("mandataire fineract manquant")
	}
	lost := p.Profiles["reponse_perdue"]
	if len(lost) != 1 || lost[0].Type != "limit_data" || lost[0].Stream != "downstream" || lost[0].Attributes["bytes"] != 0 {
		t.Fatalf("reponse_perdue doit couper le flux de retour : %+v", lost)
	}
	edge := p.Profiles["edge_rural"]
	if edge[0].Attributes["latency"] != 600 || edge[0].Attributes["jitter"] != 300 || *edge[3].Toxicity != 0.10 {
		t.Fatalf("edge_rural ne correspond pas à la section 10.2 : %+v", edge)
	}
}
