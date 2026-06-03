# SITM-MIO Average Speed Experiment

This repository contains the Version 1 baseline for a software architecture assignment about calculating average speeds by route and month for active SITM-MIO pilot routes in Cali, Colombia.

Version 1 is intentionally monolithic:

- Single Java process and single JVM.
- Local files only.
- No Spring Boot.
- No network access at runtime.
- No concurrency or distributed architecture patterns.
- Focus on correctness, repeatable verification, and measurable performance.

## Input Files

The assignment data is expected on the university servers:

```text
/opt/sitm-mio/lines-241-ActiveGT.csv
/opt/sitm-mio/datagrams4Pilot.csv
/opt/sitm-mio/Diccionario_De_Datos-OkGTM.pdf
```

MiniPilot must be extracted on the university server before the real-data run:

```bash
mkdir -p ~/sitm-data && unzip /opt/sitm-mio/datagrams-MiniPilot.zip -d ~/sitm-data
```

The extracted MiniPilot CSV path is:

```text
/home/swarch/sitm-data/datagrams-MiniPilot.csv
```

Do not commit copies of these files. If local development needs sample data, place it under `data/`, which is ignored by Git.

## Project Structure

```text
src/main/java/edu/icesi/sitmmio
  Main.java                        CLI entry point
  cli/                             Argument parsing and command wiring
  csv/                             CSV configuration and parsing helpers
  geo/                             Geographic calculations
  model/                           Immutable domain records
  service/                         Monolithic application services
  output/                          Result writers

src/test/java/edu/icesi/sitmmio
  cli/                             CLI parser tests
  geo/                             Haversine tests

specs/001-monolithic-speed
  spec.md                          Version 1 requirements
  plan.md                          Implementation plan
  tasks.md                         Task checklist
  research.md                      Data and algorithm research notes
```

## Local Development Validation

Use Java 11 or newer. The university servers currently expose Java 11, so Version 1 targets Java 11 bytecode and uses the Gradle 8.12 wrapper.

```bash
./gradlew clean build
./gradlew test
```

On Windows PowerShell, use:

```powershell
.\gradlew.bat clean build
.\gradlew.bat test
```

Run the CLI help:

```bash
./gradlew run --args="--help"
```

Do not try to validate `/opt/sitm-mio` paths from Windows. PowerShell maps `/opt/sitm-mio` to `C:\opt\sitm-mio`, where the university files do not exist.

## Remote Real-Data Validation

Real-data validation must be executed directly on one of the university Linux servers:

```text
swarch@104M03:/opt/sitm-mio
swarch@206M03:/opt/sitm-mio
swarch@205M03:/opt/sitm-mio
```

Do not copy large input CSV files into the repository. Do not commit input datasets.

The project is configured for the server environment:

```bash
java -version
./gradlew --version
```

Expected compatibility:

- Java: 11 or newer.
- Gradle wrapper: 8.12.

Confirmed server status:

```text
Server: swarch@10.147.17.103
Host: 104m03
Project path: ~/sitm-mio-speed
Java: OpenJDK 11.0.26
```

## Real MiniPilot Datagram Mapping

`datagrams-MiniPilot.csv` is headerless. The official schema from `Diccionario_De_Datos-OkGTM.pdf` is:

```text
0  eventType
1  registerdate
2  stopId
3  odometer
4  latitude
5  longitude
6  taskId
7  lineId
8  tripId
9  unknown1
10 datagramDate
11 busId
```

Speed calculation mapping:

```text
active route id = LINEID from lines-241-ActiveGT.csv
route id        = lineId       = index 7
bus id          = busId        = index 11
timestamp       = datagramDate = index 10
latitude        = index 4 / 10000000
longitude       = index 5 / 10000000
```

Do not use index `3` as the route. Index `3` is `odometer`.

On the Linux server, make scripts executable if needed:

```bash
chmod +x scripts/run-monolithic-remote.sh scripts/check-real-data-output.sh
```

Run the full remote validation workflow:

```bash
scripts/run-monolithic-remote.sh
scripts/check-real-data-output.sh
```

The remote script checks that `/opt/sitm-mio` files exist, prints CSV headers, runs MiniPilot, runs `datagrams4Pilot`, writes CSV outputs under `results/`, and saves console logs under `results/`.

Manual MiniPilot command used by the script:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

Manual full-pilot command used by the script:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams4Pilot.csv --output results/route_month_speeds_pilot.csv"
```

If auto-detection fails on the server, inspect the headers printed by the script and rerun manually with the correct column names:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /opt/sitm-mio/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv --active-route-col ruta --route-col ruta --bus-col vehiculo --timestamp-col fecha_hora --latitude-col latitud --longitude-col longitud"
```

More detail is in [docs/remote-validation.md](docs/remote-validation.md).

## Output CSV

The output columns are:

```text
route_id,month,total_distance_km,total_time_hours,avg_speed_kmh,avg_segment_speed_kmh,valid_segments,buses_observed
```

The run also prints active route count, raw datagram count, cleaned datagram count, valid segment count, output row count, and total runtime in milliseconds.

## Version 1 Scope

Version 1 will read the active route file and datagram CSV files, compute valid movement speeds, group them by route and month, and write deterministic average-speed results. Later versions may add concurrency or distributed deployment, but those concerns are out of scope for this repository state.
