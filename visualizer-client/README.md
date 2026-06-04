# SITM-MIO Java Visualizer

This module implements the bonus route visualization in Java.

It follows the same idea as the professor's base `visualizer-client`: a desktop client that displays
bus movement on an OpenStreetMap view. Like the professor's version, it uses JavaFX `WebView` to
load `map.html`, Leaflet, and the same `updateBus(...)` marker-update function. The difference is
that this version feeds the map with our MiniPilot datagrams and route/month speed results instead
of subscribing to the Ice event processor.

## What It Shows

- Active routes from `docs/lines-241-ActiveGT.csv`.
- Bus trajectories from `docs/datagrams-MiniPilot.csv`.
- Animated buses moving over GPS tracks on top of the selected route line.
- Average speed metrics from `results/v1-final-review.csv` or `results/v1-review.csv`.
- The same `updateBus(...)` Java-to-map integration style used in the professor's repository.

## Runtime Note

The window is JavaFX, but the map itself uses Leaflet, just like the professor's `map.html`.
The Cali base map is a fixed local OpenStreetMap image (`src/main/resources/cali-map-z13.png`), so
JavaFX WebView does not need to download map tiles at runtime and does not show gray tile gaps.

## Run

From the repository root:

```powershell
.\gradlew.bat visualizer-client:run
```

Validate data loading without opening the window:

```powershell
.\gradlew.bat visualizer-client:run --args="--validate"
```

If no speed result CSV exists yet, run the monolithic calculator first:

```powershell
.\gradlew.bat v1-monolithic:run --args="--lines docs/lines-241-ActiveGT.csv --datagrams docs/datagrams-MiniPilot.csv --output results/v1-final-review.csv --active-route-col LINEID --datagrams-has-header false --route-index 7 --bus-index 11 --timestamp-index 10 --latitude-index 4 --longitude-index 5 --coordinate-scale 10000000"
```
