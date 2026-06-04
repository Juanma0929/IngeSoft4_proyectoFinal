#!/usr/bin/env bash
# =============================================================================
# deploy-v3.sh — Despliega y ejecuta la versión 3 distribuida (Master-Worker)
#
# CONFIGURACIÓN: edita las variables de abajo para cambiar de servidor.
# =============================================================================

set -e  # detiene el script si cualquier comando falla

# ── Servidores ────────────────────────────────────────────────────────────────
MASTER_HOST="swarch@104m03"
WORKER1_HOST="swarch@205m03"
WORKER2_HOST="swarch@206m03"

# Hostname que cada worker anuncia al master (debe ser alcanzable desde el master)
WORKER1_HOSTNAME="205m03"
WORKER2_HOSTNAME="206m03"

WORKER_PORT=1099

# ── Rutas en los servidores ───────────────────────────────────────────────────
REMOTE_INSTALL_DIR="~/v3"
LINES_PATH="/opt/sitm-mio/lines-241-ActiveGT.csv"
DATAGRAMS_PATH="/opt/sitm-mio/datagrams4Pilot.csv"
OUTPUT_PATH="~/results/v3-pilot.csv"

# ── Ruta local del artefacto construido ──────────────────────────────────────
LOCAL_DIST="v3-distributed/build/install/v3-distributed"
SCRIPT_BIN="$REMOTE_INSTALL_DIR/bin/v3-distributed"

# =============================================================================
# PASO 1 — Build local
# =============================================================================
echo ">>> [1/4] Construyendo v3-distributed..."
./gradlew v3-distributed:installDist
echo "    Build OK: $LOCAL_DIST"

# =============================================================================
# PASO 2 — SCP a los tres servidores
# =============================================================================
echo ">>> [2/4] Copiando distribución a los servidores..."

for HOST in "$MASTER_HOST" "$WORKER1_HOST" "$WORKER2_HOST"; do
    echo "    -> $HOST"
    ssh "$HOST" "mkdir -p $REMOTE_INSTALL_DIR"
    scp -r "$LOCAL_DIST/." "$HOST:$REMOTE_INSTALL_DIR/"
    ssh "$HOST" "chmod +x $SCRIPT_BIN"
done

echo "    SCP OK"

# =============================================================================
# PASO 3 — Arrancar workers en background
# =============================================================================
echo ">>> [3/4] Arrancando workers..."

ssh "$WORKER1_HOST" \
    "nohup $SCRIPT_BIN --mode worker --port $WORKER_PORT --hostname $WORKER1_HOSTNAME \
     > ~/worker.log 2>&1 & echo \$! > ~/worker.pid"
echo "    Worker 1 arrancado en $WORKER1_HOST:$WORKER_PORT"

ssh "$WORKER2_HOST" \
    "nohup $SCRIPT_BIN --mode worker --port $WORKER_PORT --hostname $WORKER2_HOSTNAME \
     > ~/worker.log 2>&1 & echo \$! > ~/worker.pid"
echo "    Worker 2 arrancado en $WORKER2_HOST:$WORKER_PORT"

echo "    Esperando 5 segundos a que los workers registren su RMI..."
sleep 5

# =============================================================================
# PASO 4 — Ejecutar el master (bloquea hasta terminar)
# =============================================================================
echo ">>> [4/4] Ejecutando master en $MASTER_HOST..."

ssh "$MASTER_HOST" "
    mkdir -p ~/results
    $SCRIPT_BIN \
        --mode master \
        --workers $WORKER1_HOSTNAME:$WORKER_PORT,$WORKER2_HOSTNAME:$WORKER_PORT \
        --lines $LINES_PATH \
        --datagrams $DATAGRAMS_PATH \
        --output $OUTPUT_PATH \
        --active-route-col LINEID \
        --datagrams-has-header false \
        --route-index 7 --bus-index 11 --timestamp-index 10 \
        --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000
"

# =============================================================================
# LIMPIEZA — Detener workers
# =============================================================================
echo ">>> Deteniendo workers..."
ssh "$WORKER1_HOST" "kill \$(cat ~/worker.pid) 2>/dev/null && rm -f ~/worker.pid || true"
ssh "$WORKER2_HOST" "kill \$(cat ~/worker.pid) 2>/dev/null && rm -f ~/worker.pid || true"

echo ""
echo "=== Completado. Resultado en $MASTER_HOST:$OUTPUT_PATH ==="
