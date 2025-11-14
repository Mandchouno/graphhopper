#!/usr/bin/env bash
set -euo pipefail

# Cherche mutations.xml avec ou sans sous-dossier daté
REPORT_XML="$(find . -type f \( -path '*/target/pit-reports/mutations.xml' -o -path '*/target/pit-reports/*/mutations.xml' \) 2>/dev/null | sort | tail -n1 || true)"

if [[ -z "${REPORT_XML}" || ! -f "${REPORT_XML}" ]]; then
  echo "❌ Aucun rapport PIT trouvé (recherché: */target/pit-reports[/...]/mutations.xml)"
  exit 2
fi

# python3 > python fallback
if command -v python3 >/dev/null 2>&1; then PY=python3
elif command -v python >/dev/null 2>&1; then PY=python
else echo "❌ Python introuvable (python3/python)"; exit 3; fi

"$PY" - "$REPORT_XML" <<'PY'
import xml.etree.ElementTree as ET, sys
xml = sys.argv[1]
root = ET.parse(xml).getroot()
muts = [m for m in root.findall('mutation') if m.get('status') != 'NON_VIABLE']
viable = len(muts)
detected = sum(1 for m in muts if m.get('detected') == 'true')
score = 0.0 if viable == 0 else 100.0 * detected / viable
print(f"{score:.2f}")
PY
