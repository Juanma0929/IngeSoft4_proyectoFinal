package edu.icesi.sitmmio.visualizer;

import edu.icesi.sitmmio.visualizer.data.VisualizationDataLoader;
import edu.icesi.sitmmio.visualizer.model.BusTrack;
import edu.icesi.sitmmio.visualizer.model.GeoPoint;
import edu.icesi.sitmmio.visualizer.model.VisualizationRoute;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class VisualizerMain extends Application {
    private static final double FRAME_STEP = 0.0015;

    private final List<VisualizationRoute> routes = loadRoutes();
    private final ComboBox<VisualizationRoute> routeSelector = new ComboBox<>();
    private final Label metrics = new Label();
    private final Slider speedSlider = new Slider(0.1, 2.0, 0.6);

    private WebEngine webEngine;
    private Timeline timeline;
    private VisualizationRoute currentRoute;
    private double progress;

    @Override
    public void start(Stage stage) {
        if (getParameters().getRaw().contains("--validate")) {
            printValidation();
            Platform.exit();
            return;
        }

        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Safari/537.36");

        URL url = getClass().getResource("/map.html");
        if (url != null) {
            webEngine.load(url.toExternalForm());
        } else {
            throw new IllegalStateException("No se encontro el archivo map.html en resources");
        }

        BorderPane root = new BorderPane(webView);
        root.setTop(toolbar());

        routeSelector.getItems().setAll(routes);
        routeSelector.getSelectionModel().selectFirst();
        routeSelector.setOnAction(event -> loadSelectedRoute());
        webEngine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                loadSelectedRoute();
                startAnimation();
            }
        });

        stage.setTitle("SITM-MIO - Monitoreo en Tiempo Real");
        stage.setScene(new Scene(root, 1024, 768));
        stage.setResizable(true);
        stage.show();
    }

    private HBox toolbar() {
        Button playPause = new Button("Pausar");
        Button restart = new Button("Reiniciar");
        playPause.setOnAction(event -> {
            if (timeline.getStatus() == Animation.Status.RUNNING) {
                timeline.pause();
                playPause.setText("Reproducir");
            } else {
                timeline.play();
                playPause.setText("Pausar");
            }
        });
        restart.setOnAction(event -> {
            progress = 0.0;
            updateBuses();
        });

        routeSelector.setPrefWidth(250);
        speedSlider.setPrefWidth(130);
        speedSlider.setShowTickMarks(false);
        speedSlider.setShowTickLabels(false);

        HBox toolbar = new HBox(12,
                new Label("Ruta:"), routeSelector,
                playPause, restart,
                new Label("Ritmo:"), speedSlider,
                metrics);
        toolbar.setPadding(new Insets(10));
        toolbar.setStyle("-fx-background-color: white; -fx-border-color: #d9e1e8; -fx-border-width: 0 0 1 0;");
        return toolbar;
    }

    private void startAnimation() {
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(new KeyFrame(Duration.millis(1000), event -> {
            progress = (progress + FRAME_STEP * speedSlider.getValue()) % 1.0;
            updateBuses();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void loadSelectedRoute() {
        currentRoute = routeSelector.getSelectionModel().getSelectedItem();
        progress = 0.0;
        if (currentRoute == null || webEngine == null) {
            return;
        }

        metrics.setText(String.format(Locale.US,
                "Velocidad %.1f km/h | Segmentos %,d | Buses %,d | Mes %s",
                currentRoute.metrics().averageSpeedKmh(),
                currentRoute.metrics().validSegments(),
                currentRoute.metrics().busesObserved(),
                currentRoute.metrics().month()));

        execute("clearMap()");
        drawRouteLine();
        centerMapOnRoute();
        updateBuses();
    }

    private void drawRouteLine() {
        BusTrack routeShape = currentRoute.buses().stream()
                .max((left, right) -> Integer.compare(left.points().size(), right.points().size()))
                .orElse(null);
        if (routeShape == null || routeShape.points().size() < 2) {
            return;
        }

        StringBuilder script = new StringBuilder("drawRouteLine([");
        List<GeoPoint> points = routeShape.points();
        for (int i = 0; i < points.size(); i++) {
            GeoPoint point = points.get(i);
            if (i > 0) {
                script.append(',');
            }
            script.append('[')
                    .append(String.format(Locale.US, "%.7f", point.latitude()))
                    .append(',')
                    .append(String.format(Locale.US, "%.7f", point.longitude()))
                    .append(']');
        }
        script.append("])");
        execute(script.toString());
    }

    private void centerMapOnRoute() {
        for (BusTrack bus : currentRoute.buses()) {
            if (!bus.points().isEmpty()) {
                GeoPoint point = bus.points().get(0);
                execute(String.format(Locale.US, "centerMap(%f,%f)", point.latitude(), point.longitude()));
                return;
            }
        }
    }

    private void updateBuses() {
        if (currentRoute == null) {
            return;
        }
        List<BusTrack> buses = currentRoute.buses();
        for (int i = 0; i < buses.size(); i++) {
            BusTrack bus = buses.get(i);
            GeoPoint point = pointAt(bus, (progress + i * 0.075) % 1.0);
            execute(String.format(Locale.US,
                    "updateBus(%s,%f,%f,%s,%s)",
                    quote(bus.busId()),
                    point.latitude(),
                    point.longitude(),
                    quote(currentRoute.shortName()),
                    quote(point.time().toString())));
        }
    }

    private static GeoPoint pointAt(BusTrack bus, double routeProgress) {
        List<GeoPoint> points = bus.points();
        if (points.size() == 1) {
            return points.get(0);
        }
        double scaled = routeProgress * (points.size() - 1);
        int index = Math.min(points.size() - 2, (int) Math.floor(scaled));
        double local = scaled - index;
        GeoPoint a = points.get(index);
        GeoPoint b = points.get(index + 1);
        return new GeoPoint(
                a.latitude() + (b.latitude() - a.latitude()) * local,
                a.longitude() + (b.longitude() - a.longitude()) * local,
                local < 0.5 ? a.time() : b.time());
    }

    private void execute(String script) {
        webEngine.executeScript(script);
    }

    private void printValidation() {
        System.out.println("SITM-MIO JavaFX visualizer data loaded");
        System.out.println("Routes: " + routes.size());
        System.out.println("First route: " + routes.get(0).shortName() + " (" + routes.get(0).routeId() + ")");
    }

    private static String quote(String value) {
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\r", "")
                .replace("\n", " ")
                + "'";
    }

    private static List<VisualizationRoute> loadRoutes() {
        Path root = Path.of("").toAbsolutePath();
        Path lines = root.resolve("docs").resolve("lines-241-ActiveGT.csv");
        Path datagrams = root.resolve("docs").resolve("datagrams-MiniPilot.csv");
        Path speeds = root.resolve("results").resolve("v1-final-review.csv");
        if (!speeds.toFile().isFile()) {
            speeds = root.resolve("results").resolve("v1-review.csv");
        }
        return new VisualizationDataLoader(lines, datagrams, speeds).loadRoutes();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
