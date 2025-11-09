mkdir -p scripts
cat > scripts/pit_score.sh <<'BASH'
#!/usr/bin/env bash
set -euo pipefail
REPORT_DIR=$(ls -d target/pit-reports/* 2>/dev/null | sort | tail -n1 || true)
XML="$REPORT_DIR/mutations.xml"
if [ ! -f "$XML" ]; then
  echo "❌ Aucun rapport PIT trouvé"
  exit 2
fi
python3 - <<'PY' "$XML"
import xml.etree.ElementTree as ET, sys
xml = sys.argv[1]
root = ET.parse(xml).getroot()
muts = [m for m in root.findall('mutation') if m.get('status') != 'NON_VIABLE']
viable = len(muts)
detected = sum(1 for m in muts if m.get('detected') == 'true')
score = 0.0 if viable == 0 else 100.0 * detected / viable
print(f"{score:.2f}")
PY
BASH
