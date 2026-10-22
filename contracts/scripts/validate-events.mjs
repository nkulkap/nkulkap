// Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
// Valide les schémas Avro de contracts/events : syntaxe, références croisées, conventions NKULKAP,
// et aller-retour d'encodage de l'exemple FICTIF de chaque schéma (fichier .example.json voisin).
import avro from 'avsc';
import { readdirSync, readFileSync, existsSync } from 'node:fs';
import { join, dirname, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..', 'events');
const files = [];
(function walk(dir) {
  for (const e of readdirSync(dir, { withFileTypes: true })) {
    const p = join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith('.avsc')) files.push(p);
  }
})(root);

const errors = [];
const registry = {};
// Les types partagés (common/) d'abord, puis les autres : ordre de dépendance simple.
const isCommon = (f) => relative(root, f).startsWith('common');
files.sort((a, b) => Number(isCommon(b)) - Number(isCommon(a)) || a.localeCompare(b));

function checkConventions(schema, file) {
  const visit = (s, path) => {
    if (!s || typeof s !== 'object') return;
    if (Array.isArray(s)) return s.forEach((x, i) => visit(x, `${path}[${i}]`));
    if (s.type === 'record') {
      if (!s.namespace?.startsWith('cm.nkulkap.events') && path === '$') errors.push(`${file} : namespace hors cm.nkulkap.events`);
      if (!s.doc) errors.push(`${file} : ${s.name} sans doc`);
      for (const f of s.fields ?? []) {
        if (!f.doc) errors.push(`${file} : champ ${s.name}.${f.name} sans doc`);
        // Règle d'or : jamais de flottant pour l'argent.
        const t = JSON.stringify(f.type);
        if (/amount|montant|total|ecart|fee/i.test(f.name) && /"(float|double)"/.test(t)) {
          errors.push(`${file} : ${s.name}.${f.name} est un flottant`);
        }
        visit(f.type, `${path}.${f.name}`);
      }
    }
  };
  visit(schema, '$');
}

for (const file of files) {
  const rel = relative(root, file);
  try {
    const schema = JSON.parse(readFileSync(file, 'utf8'));
    checkConventions(schema, rel);
    const type = avro.Type.forSchema(schema, { registry });
    const example = file.replace(/\.avsc$/, '.example.json');
    if (existsSync(example)) {
      const value = JSON.parse(readFileSync(example, 'utf8'));
      if (!type.isValid(value, { errorHook: (p) => errors.push(`${rel} : exemple invalide au champ ${p.join('.')}`) })) continue;
      const back = type.fromBuffer(type.toBuffer(value));
      if (JSON.stringify(type.toJSON(back)) !== JSON.stringify(type.toJSON(value))) {
        errors.push(`${rel} : aller-retour d'encodage différent`);
      }
    } else if (!rel.startsWith('common')) {
      errors.push(`${rel} : exemple FICTIF manquant (${relative(root, example)})`);
    }
    console.log(`ok  ${rel}`);
  } catch (e) {
    errors.push(`${rel} : ${e.message}`);
  }
}

if (errors.length) {
  console.error(errors.map((e) => `ERREUR ${e}`).join('\n'));
  process.exit(1);
}
console.log(`${files.length} schémas valides`);
