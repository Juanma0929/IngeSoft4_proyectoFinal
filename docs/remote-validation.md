# Remote Real-Data Validation

The real SITM-MIO input files are not available on the local Windows machine. They exist only on the university Linux servers:

```text
swarch@104M03:/opt/sitm-mio
swarch@206M03:/opt/sitm-mio
swarch@205M03:/opt/sitm-mio
```

On Windows, `/opt/sitm-mio` is interpreted as `C:\opt\sitm-mio`, so real-data validation cannot be done locally unless the files are explicitly copied or mounted. Do not copy large CSV files into this repository and do not commit input datasets.

Confirmed environment:

```text
Server: swarch@10.147.17.103
Host: 104m03
Project path: ~/sitm-mio-speed
Java: OpenJDK 11.0.26
Gradle wrapper: 8.12
```

## SSH Into A Server

Use one of the assigned servers:

```bash
ssh swarch@104M03
ssh swarch@206M03
ssh swarch@205M03
```

## Clone Or Update The Repository

If the repository is not present yet:

```bash
git clone <repo-url> sitm-mio-speed
cd sitm-mio-speed
```

If the repository already exists:

```bash
cd sitm-mio-speed
git pull
```

Make the scripts executable if needed:

```bash
chmod +x scripts/run-monolithic-remote.sh scripts/check-real-data-output.sh
```

## Java And Gradle On The Server

The university servers currently use Java 11 as the active JVM. Version 1 is configured to compile with Java 11 compatibility, and the Gradle wrapper points to Gradle 8.12 because it runs on Java 11.

Check the environment before running real data:

```bash
java -version
./gradlew --version
```

Expected:

- Java 11 or newer.
- Gradle 8.12 from the project wrapper.

Do not use Gradle 9.x on the university server with Java 11; Gradle 9 requires JVM 17 or newer.

## MiniPilot Data Preparation

The active routes CSV is:

```text
/opt/sitm-mio/lines-241-ActiveGT.csv
```

The MiniPilot datagram CSV should be extracted to:

```text
/home/swarch/sitm-data/datagrams-MiniPilot.csv
```

If it is missing, extract it on the server:

```bash
mkdir -p ~/sitm-data && unzip /opt/sitm-mio/datagrams-MiniPilot.zip -d ~/sitm-data
```

Do not automatically extract `datagrams4Pilot.zip` yet.

## Headerless MiniPilot Mapping

`datagrams-MiniPilot.csv` has no header. The official data dictionary mapping is:

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

Use these CLI indexes:

```text
active route column = LINEID
route-index         = 7
bus-index           = 11
timestamp-index     = 10
latitude-index      = 4
longitude-index     = 5
coordinate-scale    = 10000000
```

Index `3` is `odometer`, not route.

## Run Real-Data Validation

Run the monolithic Version 1 calculations directly on the Linux server:

```bash
scripts/run-monolithic-remote.sh
```

The MiniPilot command inside that script is:

```bash
./gradlew run --args="--lines /opt/sitm-mio/lines-241-ActiveGT.csv --datagrams /home/swarch/sitm-data/datagrams-MiniPilot.csv --output results/route_month_speeds_minipilot.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```

Then validate the generated outputs:

```bash
scripts/check-real-data-output.sh
```

The scripts write generated CSV files and console logs under `results/`. These files are ignored by Git.

## Evidence To Save

For the experiment document, save:

- Header lines printed by `scripts/run-monolithic-remote.sh`.
- Console logs:
  - `results/route_month_speeds_minipilot.log`
  - `results/route_month_speeds_pilot.log`
- First 10 lines of each output CSV.
- Row counts for each output CSV.
- Confirmation that `avg_speed_kmh` has no empty, `NaN`, or `Infinity` values.
- Runtime metrics printed by the Java CLI.

Current status from local Windows development:

```text
Local build/test: passed.
Real-data validation: pending until executed on swarch@104M03, swarch@206M03, or swarch@205M03.
Java/Gradle compatibility: Java 11 target with Gradle wrapper 8.12.
```
