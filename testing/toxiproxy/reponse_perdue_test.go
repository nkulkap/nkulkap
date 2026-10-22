// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.

package toxiproxy

import (
	"bufio"
	"bytes"
	"crypto/rand"
	"crypto/tls"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"
)

// Étape 2.3 : une requête réussie côté serveur ne reçoit pas de réponse en mode reponse_perdue.
// Exige la pile locale (make up && make seed) : lancé par `make toxiproxy-test`, sauté sinon.

const (
	directURL = "https://localhost:8443/fineract-provider/api/v1"
	proxyAddr = "localhost:21000"
	apiPrefix = "/fineract-provider/api/v1"
)

// Identifiants FICTIFS de l'image de développement Fineract.
var auth = "Basic " + base64.StdEncoding.EncodeToString([]byte("mifos:password"))

// Certificat autosigné de la pile locale.
var tlsConfig = &tls.Config{InsecureSkipVerify: true} //nolint:gosec // pile de test locale uniquement

var direct = &http.Client{Timeout: 30 * time.Second, Transport: &http.Transport{TLSClientConfig: tlsConfig}}

func fineract(t *testing.T, method, path string, body any, key string) (int, http.Header, map[string]any) {
	t.Helper()
	var r io.Reader
	if body != nil {
		b, _ := json.Marshal(body)
		r = bytes.NewReader(b)
	}
	req, err := http.NewRequest(method, directURL+path, r)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("Authorization", auth)
	req.Header.Set("Fineract-Platform-TenantId", "default")
	req.Header.Set("Content-Type", "application/json")
	if key != "" {
		req.Header.Set("Idempotency-Key", key)
	}
	resp, err := direct.Do(req)
	if err != nil {
		t.Fatalf("%s %s : %v", method, path, err)
	}
	defer resp.Body.Close()
	var out map[string]any
	_ = json.NewDecoder(resp.Body).Decode(&out)
	return resp.StatusCode, resp.Header, out
}

func uuid4() string {
	b := make([]byte, 16)
	_, _ = rand.Read(b)
	b[6] = b[6]&0x0f | 0x40
	b[8] = b[8]&0x3f | 0x80
	return fmt.Sprintf("%x-%x-%x-%x-%x", b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}

func TestReponsePerdue(t *testing.T) {
	if os.Getenv("NKULKAP_STACK") != "1" {
		t.Skip("pile locale requise : NKULKAP_STACK=1 (make toxiproxy-test)")
	}
	profiles, err := LoadProfiles(filepath.Join(".", "profiles.yaml"))
	if err != nil {
		t.Fatal(err)
	}
	c := NewClient()
	for name, p := range profiles.Proxies {
		if err := c.EnsureProxy(name, p); err != nil {
			t.Fatal(err)
		}
	}
	if err := c.Reset(); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = c.Reset() })

	status, _, acc := fineract(t, http.MethodGet, "/savingsaccounts/external-id/SEED-EPA-05", nil, "")
	if status != http.StatusOK {
		t.Fatalf("compte du seed introuvable (%d) : lancer make seed", status)
	}
	accountID := int64(acc["id"].(float64))
	opID := uuid4()
	body, _ := json.Marshal(map[string]any{
		"transactionDate": "2026-02-01", "transactionAmount": 1500, "paymentTypeId": paymentTypeID(t),
		"externalId": opID, "dateFormat": "yyyy-MM-dd", "locale": "fr",
	})
	path := fmt.Sprintf("/savingsaccounts/%d/transactions?command=deposit", accountID)

	// 1. Connexion TLS établie à travers Toxiproxy AVANT le toxique : sinon la poignée de main échoue.
	conn, err := tls.DialWithDialer(&netDialer, "tcp", proxyAddr, tlsConfig)
	if err != nil {
		t.Fatalf("connexion au mandataire : %v", err)
	}
	defer conn.Close()

	// 2. Profil reponse_perdue sur la connexion ouverte.
	if err := c.Apply(profiles, "fineract", "reponse_perdue"); err != nil {
		t.Fatal(err)
	}

	// 3. Requête complète, avec Idempotency-Key et externalId = op_id (ADR-016).
	req := fmt.Sprintf("POST %s%s HTTP/1.1\r\nHost: localhost:8443\r\nAuthorization: %s\r\n"+
		"Fineract-Platform-TenantId: default\r\nContent-Type: application/json\r\nIdempotency-Key: %s\r\n"+
		"Content-Length: %d\r\nConnection: close\r\n\r\n%s", apiPrefix, path, auth, opID, len(body), body)
	if _, err := io.WriteString(conn, req); err != nil {
		t.Fatalf("envoi : %v", err)
	}

	// 4. Aucune réponse : la connexion se ferme sans un octet de réponse HTTP.
	_ = conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	line, readErr := bufio.NewReader(conn).ReadString('\n')
	t.Logf("lecture côté client : %q, erreur %v", line, readErr)
	if readErr == nil || strings.HasPrefix(line, "HTTP/") {
		t.Fatalf("une réponse est arrivée malgré reponse_perdue : %q", line)
	}
	if ne, ok := readErr.(interface{ Timeout() bool }); ok && ne.Timeout() {
		t.Fatal("délai dépassé : la connexion aurait dû être fermée par Toxiproxy")
	}
	if err := c.Reset(); err != nil {
		t.Fatal(err)
	}

	// 5. Le serveur a bien enregistré le dépôt (relecture directe par externalId).
	var tx map[string]any
	for i := 0; i < 20; i++ {
		var st int
		st, _, tx = fineract(t, http.MethodGet, fmt.Sprintf("/savingsaccounts/%d/transactions/external-id/%s", accountID, opID), nil, "")
		if st == http.StatusOK {
			break
		}
		time.Sleep(500 * time.Millisecond)
	}
	if tx == nil || tx["externalId"] != opID {
		t.Fatalf("le dépôt n'a pas été enregistré côté serveur : %v", tx)
	}
	t.Logf("dépôt enregistré côté serveur : transaction %v, montant %v", tx["id"], tx["amount"])

	// 6. Le client rejoue avec la même clé : réponse d'origine depuis le cache, aucun doublon.
	var payload map[string]any
	_ = json.Unmarshal(body, &payload)
	st, hdr, res := fineract(t, http.MethodPost, path, payload, opID)
	if st != http.StatusOK || hdr.Get("x-served-from-cache") != "true" {
		t.Fatalf("rejeu : HTTP %d, x-served-from-cache=%q, %v", st, hdr.Get("x-served-from-cache"), res)
	}
	if int64(res["resourceId"].(float64)) != int64(tx["id"].(float64)) {
		t.Fatalf("rejeu : resourceId %v au lieu de %v", res["resourceId"], tx["id"])
	}
	t.Logf("rejeu servi depuis le cache, même transaction %v", res["resourceId"])
}

func paymentTypeID(t *testing.T) int64 {
	t.Helper()
	req, _ := http.NewRequest(http.MethodGet, directURL+"/paymenttypes", nil)
	req.Header.Set("Authorization", auth)
	req.Header.Set("Fineract-Platform-TenantId", "default")
	resp, err := direct.Do(req)
	if err != nil {
		t.Fatal(err)
	}
	defer resp.Body.Close()
	var types []map[string]any
	_ = json.NewDecoder(resp.Body).Decode(&types)
	for _, pt := range types {
		if pt["isCashPayment"] == true {
			return int64(pt["id"].(float64))
		}
	}
	t.Fatal("type de paiement espèces absent : lancer make seed")
	return 0
}
