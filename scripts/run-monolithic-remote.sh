#!/usr/bin/env bash
set -euo pipefail

DATA_DIR="/opt/sitm-mio"
LINES_FILE="$DATA_DIR/lines-241-ActiveGT.csv"
MINI_FILE="/home/swarch/sitm-data/datagrams-MiniPilot.csv"
PILOT_FILE="$DATA_DIR/datagrams4Pilot.csv"
RESULTS_DIR="results"

require_file() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    echo "ERROR: required file not found: $path" >&2
    echo "Run this script directly on one of the university Linux servers where /opt/sitm-mio exists." >&2
    exit 1
  fi
}

print_header() {
  local label="$1"
  local path="$2"
  echo "== $label header =="
  head -n 1 "$path"
  echo
}

require_file "$LINES_FILE"
if [[ ! -f "$MINI_FILE" ]]; then
  echo "ERROR: required MiniPilot CSV not found: $MINI_FILE" >&2
  echo "Extract it on the university server with:" >&2
  echo "mkdir -p ~/sitm-data && unzip /opt/sitm-mio/datagrams-MiniPilot.zip -d ~/sitm-data" >&2
  exit 1
fi
require_file "$PILOT_FILE"

mkdir -p "$RESULTS_DIR"

print_header "lines-241-ActiveGT.csv" "$LINES_FILE"
print_header "datagrams-MiniPilot.csv first row (headerless file)" "$MINI_FILE"
print_header "datagrams4Pilot.csv" "$PILOT_FILE"

echo "== Running MiniPilot monolithic calculation =="
./gradlew v1-monolithic:run --args="--lines $LINES_FILE --datagrams $MINI_FILE --output $RESULTS_DIR/route_month_speeds_minipilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" \
  2>&1 | tee "$RESULTS_DIR/route_month_speeds_minipilot.log"

echo
echo "== Running full pilot monolithic calculation =="
./gradlew v1-monolithic:run --args="--lines $LINES_FILE --datagrams $PILOT_FILE --output $RESULTS_DIR/route_month_speeds_pilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000" \
  2>&1 | tee "$RESULTS_DIR/route_month_speeds_pilot.log"

echo
echo "Remote monolithic validation runs completed."
echo "Outputs:"
echo "  $RESULTS_DIR/route_month_speeds_minipilot.csv"
echo "  $RESULTS_DIR/route_month_speeds_pilot.csv"
echo "Logs:"
echo "  $RESULTS_DIR/route_month_speeds_minipilot.log"
echo "  $RESULTS_DIR/route_month_speeds_pilot.log"
