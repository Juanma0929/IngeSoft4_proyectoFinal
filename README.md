# SITM-MIO — Velocidad Promedio por Ruta y Mes

**Arquitectura de Software — Proyecto Final**  
Universidad Icesi · Cali, Colombia

**Integrantes:**
- Jacobo Rodriguez
- Victor Cardona
- Juan Pablo Arevalo
- Juan Manuel Ramirez

---

## Qué hace este proyecto

El Sistema Integrado de Transporte Masivo de Occidente (SITM-MIO) opera ~1300 buses en Cali. Cada bus emite un datagrama GPS cada 20–30 segundos con su posición, ruta y bus ID. Este proyecto procesa esos datagramas para calcular la **velocidad promedio de desplazamiento por ruta y por mes**, usando tres arquitecturas de complejidad creciente.

### Cálculo de velocidad

Para cada par de datagramas consecutivos del mismo bus en la misma ruta:

1. Se calcula la distancia entre los dos puntos GPS con la fórmula de **Haversine**.
2. Se divide entre el tiempo transcurrido → velocidad del segmento en km/h.
3. Los segmentos con gap de tiempo > 10 min o velocidad > 120 km/h se descartan.
4. Se agregan todos los segmentos válidos por `(routeId, yearMonth)` → velocidad promedio.

### Formato de entrada

`datagrams-MiniPilot.csv` — sin encabezado, columnas relevantes:

| Índice | Campo | Uso |
|--------|-------|-----|
| 4 | `latitude` | Dividir entre `10_000_000` para obtener grados |
| 5 | `longitude` | Dividir entre `10_000_000` para obtener grados |
| 7 | `lineId` | Identificador de ruta |
| 10 | `datagramDate` | Timestamp del evento |
| 11 | `busId` | Identificador del bus |

### Formato de salida

```
route_id, month, total_distance_km, total_time_hours,
avg_speed_kmh, avg_segment_speed_kmh, valid_segments, buses_observed
```

---

## Estructura del proyecto

```
sitm-mio-speed/
├── shared/               Código común a las tres versiones
│   └── src/main/…
│       ├── model/        GpsPoint, RouteMonthSpeed, SpeedSegment
│       ├── geo/          HaversineDistanceCalculator
│       ├── csv/          Lectores de CSV (datagramas y rutas activas)
│       ├── service/      SpeedSegmentCalculator, RouteMonthAggregator
│       ├── cli/          CliParser, CliOptions, ParseResult
│       └── output/       ResultCsvWriter
│
├── v1-monolithic/        Versión 1 — procesamiento secuencial
├── v2-threadpool/        Versión 2 — concurrente con ThreadPool
├── v3-distributed/       Versión 3 — distribuida con Java RMI
│
├── docs/
│   ├── dictionary.md          Diccionario de datos del CSV
│   ├── enunciado.md           Enunciado del proyecto
│   ├── deployment-v3.md       Diagrama de deployment (PlantUML)
│   └── datagrams-MiniPilot.csv  Dataset piloto local
│
├── results/              Salidas generadas (ignoradas por Git)
└── scripts/
    ├── deploy-v3.sh           Despliegue automático en servidores
    └── run-monolithic-remote.sh
```

---

## Versión 1 — Monolítica

### Qué hace

Procesa todo de forma secuencial en un único hilo: lee el CSV completo, ordena los puntos por ruta → bus → tiempo, calcula segmentos de velocidad y agrega por ruta-mes. Simple y correcta; es la línea base para medir speedup.

### Patrones de diseño

| Patrón | Clase | Justificación |
|--------|-------|---------------|
| **Template Method** | `MonolithicSpeedCalculator` | Define el flujo fijo: leer → ordenar → calcular → agregar → escribir. Cada paso es delegado a colaboradores intercambiables. |
| **Strategy** | `HaversineDistanceCalculator` | Encapsula la fórmula de distancia detrás de una clase separada. Permite sustituirla sin tocar el calculador de segmentos. |
| **Builder** | `CliOptions` | Construcción de la configuración CLI con validación en el constructor, separando parsing de uso. |

### Cómo correrlo

```powershell
.\gradlew v1-monolithic:run --args="--lines docs/lines-241-ActiveGT.csv --datagrams docs/datagrams-MiniPilot.csv --output results/v1-local.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

---

## Versión 2 — ThreadPool (Producer-Consumer)

### Qué hace

Divide el trabajo por rutas: el hilo principal lee el CSV completo y agrupa los datagramas por `lineId` (Producer). Luego envía cada ruta como un `Callable` al `ExecutorService` (Consumer). Los N hilos del pool procesan las rutas en paralelo e independientemente. El hilo principal recoge los `Future` y fusiona los resultados.

### Patrones de diseño

| Patrón | Clase | Justificación |
|--------|-------|---------------|
| **Producer-Consumer** | `ThreadPoolSpeedCalculator` + `RouteWorker` | El hilo principal produce unidades de trabajo (una por ruta); los hilos del pool las consumen en paralelo. Elimina el cuello de botella serial de v1. |
| **Thread Pool** | `Executors.newFixedThreadPool(N)` | Reutiliza hilos para evitar el overhead de crear y destruir uno por tarea. `N` es configurable con `--threads`. |
| **Strategy** | `HaversineDistanceCalculator` | Heredado de `shared`; cada `RouteWorker` recibe la misma instancia sin estado compartido. |

### Por qué no se ve speedup con el MiniPilot

Con ~188k datagramas el overhead del pool supera el tiempo de cómputo. El speedup real aparece con el dataset completo (67 GB, ~1.7B datagramas), donde cada ruta tiene millones de puntos y el trabajo por hilo justifica el paralelismo.

### Cómo correrlo

```powershell
# Hilos por defecto (= núcleos del CPU)
.\gradlew v2-threadpool:run --args="--lines docs/lines-241-ActiveGT.csv --datagrams docs/datagrams-MiniPilot.csv --output results/v2-local.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"

# Con número explícito de hilos
.\gradlew v2-threadpool:run --args="... --threads 8"
```

---

## Versión 3 — Distribuida (Master-Worker + Java RMI)

### Qué hace

Distribuye el cálculo entre tres servidores. El Master divide las rutas activas en grupos iguales (PARTITION), envía cada grupo a un Worker remoto vía RMI (MAP), cada Worker lee el CSV en su propia máquina y retorna resultados parciales, y el Master fusiona todo (REDUCE). Los 67 GB del CSV **nunca viajan por red** — cada Worker lee su copia local.

### Patrones de diseño

| Patrón | Clase | Justificación |
|--------|-------|---------------|
| **Master-Worker** *(distribución)* | `Master` + `WorkerImpl` | Patrón canónico para cómputo distribuido batch. El Master coordina sin ejecutar cómputo pesado; los Workers son autónomos e independientes entre sí. |
| **Factory Method** | `WorkerConnectionFactory` | Crea stubs RMI (`IWorker`) a partir de direcciones `host:port`. El Master no conoce detalles del protocolo de conexión; la factory los encapsula. |
| **Proxy** | `IWorker` stub RMI | El stub generado por RMI implementa `IWorker` igual que `WorkerImpl`. El Master llama `worker.map(request)` como si fuera local; RMI serializa la llamada y la envía por red de forma transparente. |


### Ejecución local (dos terminales)

**Terminal 1 — Worker:**
```powershell
.\gradlew v3-distributed:run --args="--mode worker --port 1099"
```
Espera hasta ver: `Worker RMI server ready on port 1099 — waiting for master...`

**Terminal 2 — Master:**
```powershell
.\gradlew v3-distributed:run --args="--mode master --workers localhost:1099 --lines docs/lines-241-ActiveGT.csv --datagrams docs/datagrams-MiniPilot.csv --output results/v3-local.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

Cuando el master termine, cierra la Terminal 1 con `Ctrl+C`.

### Despliegue en servidores universitarios

#### Opción A — Script automático

Edita las variables al inicio de `scripts/deploy-v3.sh` si necesitas cambiar servidores:

```bash
MASTER_HOST="swarch@104m03"
WORKER1_HOST="swarch@205m03"
WORKER2_HOST="swarch@206m03"
WORKER1_HOSTNAME="205m03"
WORKER2_HOSTNAME="206m03"
```

Luego ejecuta desde Git Bash o WSL:

```bash
chmod +x scripts/deploy-v3.sh
bash scripts/deploy-v3.sh
```

El script hace: build → scp a los 3 servidores → arranca workers → corre master → detiene workers.

#### Opción B — Paso a paso manual

```bash
# 1. Construir
./gradlew v3-distributed:installDist

# 2. Copiar a los servidores
scp -r v3-distributed/build/install/v3-distributed/. swarch@104m03:~/v3/
scp -r v3-distributed/build/install/v3-distributed/. swarch@205m03:~/v3/
scp -r v3-distributed/build/install/v3-distributed/. swarch@206m03:~/v3/

# 3. Dar permisos de ejecución
ssh swarch@104m03 "chmod +x ~/v3/bin/v3-distributed"
ssh swarch@205m03 "chmod +x ~/v3/bin/v3-distributed"
ssh swarch@206m03 "chmod +x ~/v3/bin/v3-distributed"

# 4. Arrancar workers en background
ssh swarch@205m03 "nohup ~/v3/bin/v3-distributed --mode worker --port 1099 --hostname 205m03 > ~/worker.log 2>&1 & echo \$! > ~/worker.pid"
ssh swarch@206m03 "nohup ~/v3/bin/v3-distributed --mode worker --port 1099 --hostname 206m03 > ~/worker.log 2>&1 & echo \$! > ~/worker.pid"

# 5. Esperar y correr el master
sleep 5
ssh swarch@104m03 "mkdir -p ~/results && ~/v3/bin/v3-distributed \
  --mode master \
  --workers 205m03:1099,206m03:1099 \
  --lines /opt/sitm-mio/lines-241-ActiveGT.csv \
  --datagrams /opt/sitm-mio/datagrams4Pilot.csv \
  --output ~/results/v3-pilot.csv \
  --active-route-col LINEID \
  --datagrams-has-header false \
  --route-index 7 --bus-index 11 --timestamp-index 10 \
  --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"

# 6. Detener workers
ssh swarch@205m03 "kill \$(cat ~/worker.pid)"
ssh swarch@206m03 "kill \$(cat ~/worker.pid)"
```

#### Cambiar los servidores de despliegue

Solo edita las 5 variables al inicio de `scripts/deploy-v3.sh`:

| Variable | Qué controla |
|----------|-------------|
| `MASTER_HOST` | Servidor que ejecuta el Master (usuario@host) |
| `WORKER1_HOST` | Servidor del Worker 1 (usuario@host) |
| `WORKER2_HOST` | Servidor del Worker 2 (usuario@host) |
| `WORKER1_HOSTNAME` | Nombre que Worker 1 anuncia al Master vía RMI |
| `WORKER2_HOSTNAME` | Nombre que Worker 2 anuncia al Master vía RMI |

`WORKER_HOSTNAME` debe ser el nombre con el que el Master puede alcanzar al Worker por red. Si usas IPs, pon la IP.

---

## Requisitos

- **Java 11** o superior (los servidores universitarios usan OpenJDK 11.0.26)
- **Gradle 8.12** (incluido en el wrapper, no requiere instalación)
- `ssh` y `scp` disponibles para el despliegue remoto (Git Bash, WSL o PowerShell con OpenSSH)

## Comandos de build

```powershell
# Compilar y testear todo
.\gradlew build

# Solo compilar
.\gradlew assemble

# Solo tests
.\gradlew test

# Generar distribución ejecutable de v3
.\gradlew v3-distributed:installDist
```
