// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.

// Package toxiproxy applique les profils de réseau dégradé de profiles.yaml (section 10.2) à un serveur
// Toxiproxy par son API HTTP. Les tests Rust, Java, Scala et les scripts E2E l'utilisent par la commande
// toxictl ; ce paquet sert aussi directement aux tests Go.
//
// Réseau : chaque appel à l'API Toxiproxy a un délai de 10 s, sans nouvelle tentative ; une erreur est
// renvoyée telle quelle à l'appelant.
package toxiproxy

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"sort"
	"time"

	"gopkg.in/yaml.v3"
)

// Toxic est un toxique de profil, au format de l'API Toxiproxy.
type Toxic struct {
	Name       string         `yaml:"name" json:"name"`
	Type       string         `yaml:"type" json:"type"`
	Stream     string         `yaml:"stream" json:"stream"`
	Toxicity   *float64       `yaml:"toxicity" json:"toxicity,omitempty"`
	Attributes map[string]int `yaml:"attributes" json:"attributes"`
}

// Proxy décrit un mandataire Toxiproxy.
type Proxy struct {
	Listen   string `yaml:"listen" json:"listen"`
	Upstream string `yaml:"upstream" json:"upstream"`
}

// Profiles est le contenu de profiles.yaml.
type Profiles struct {
	Proxies  map[string]Proxy   `yaml:"proxies"`
	Profiles map[string][]Toxic `yaml:"profiles"`
}

// LoadProfiles lit et contrôle un fichier de profils.
func LoadProfiles(path string) (*Profiles, error) {
	raw, err := os.ReadFile(path)
	if err != nil {
		return nil, err
	}
	var p Profiles
	if err := yaml.Unmarshal(raw, &p); err != nil {
		return nil, fmt.Errorf("%s : %w", path, err)
	}
	for name, toxics := range p.Profiles {
		for i, t := range toxics {
			if t.Type == "" || (t.Stream != "upstream" && t.Stream != "downstream") {
				return nil, fmt.Errorf("profil %s, toxique %d : type ou stream invalide", name, i)
			}
			if t.Toxicity != nil && (*t.Toxicity < 0 || *t.Toxicity > 1) {
				return nil, fmt.Errorf("profil %s, toxique %d : toxicity hors de [0, 1]", name, i)
			}
		}
	}
	return &p, nil
}

// Names renvoie les noms de profils, triés.
func (p *Profiles) Names() []string {
	names := make([]string, 0, len(p.Profiles))
	for n := range p.Profiles {
		names = append(names, n)
	}
	sort.Strings(names)
	return names
}

// Client parle à l'API d'administration de Toxiproxy (par défaut http://localhost:8474).
type Client struct {
	BaseURL string
	HTTP    *http.Client
}

// NewClient crée un client ; TOXIPROXY_URL remplace l'adresse par défaut.
func NewClient() *Client {
	base := os.Getenv("TOXIPROXY_URL")
	if base == "" {
		base = "http://localhost:8474"
	}
	return &Client{BaseURL: base, HTTP: &http.Client{Timeout: 10 * time.Second}}
}

func (c *Client) call(method, path string, body any, okStatus ...int) error {
	var r io.Reader
	if body != nil {
		b, err := json.Marshal(body)
		if err != nil {
			return err
		}
		r = bytes.NewReader(b)
	}
	req, err := http.NewRequest(method, c.BaseURL+path, r)
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.HTTP.Do(req)
	if err != nil {
		return fmt.Errorf("toxiproxy %s %s : %w", method, path, err)
	}
	defer resp.Body.Close()
	msg, _ := io.ReadAll(resp.Body)
	for _, s := range okStatus {
		if resp.StatusCode == s {
			return nil
		}
	}
	return fmt.Errorf("toxiproxy %s %s : HTTP %d %s", method, path, resp.StatusCode, bytes.TrimSpace(msg))
}

// EnsureProxy crée le mandataire s'il manque, ou le remet à jour.
func (c *Client) EnsureProxy(name string, p Proxy) error {
	body := map[string]any{"name": name, "listen": p.Listen, "upstream": p.Upstream, "enabled": true}
	if err := c.call(http.MethodPost, "/proxies", body, http.StatusCreated, http.StatusConflict); err != nil {
		return err
	}
	return c.call(http.MethodPost, "/proxies/"+name, body, http.StatusOK)
}

// Reset retire tous les toxiques de tous les mandataires et les réactive.
func (c *Client) Reset() error {
	return c.call(http.MethodPost, "/reset", nil, http.StatusNoContent, http.StatusOK)
}

// AddToxic ajoute un toxique ; il s'applique aussi aux connexions déjà ouvertes.
func (c *Client) AddToxic(proxy string, t Toxic) error {
	return c.call(http.MethodPost, "/proxies/"+proxy+"/toxics", t, http.StatusOK)
}

// Apply remplace les toxiques du mandataire par ceux du profil.
func (c *Client) Apply(p *Profiles, proxy, profile string) error {
	toxics, ok := p.Profiles[profile]
	if !ok {
		return fmt.Errorf("profil inconnu : %s (connus : %v)", profile, p.Names())
	}
	if err := c.Reset(); err != nil {
		return err
	}
	for i, t := range toxics {
		t.Name = fmt.Sprintf("%s_%d_%s_%s", profile, i, t.Type, t.Stream)
		if err := c.AddToxic(proxy, t); err != nil {
			return err
		}
	}
	return nil
}
