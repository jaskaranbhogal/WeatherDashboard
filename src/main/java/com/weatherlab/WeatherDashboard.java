package com.weatherlab;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/// JavaFX weather dashboard built with interface methods and lambdas.
public interface WeatherDashboard {

    /// Opens the dashboard with the supplied default CSV file.
    /// @param defaultFile path to the initial weather CSV
    public static void open(Path defaultFile) {
        Platform.startup(() -> createWindow(defaultFile));
    }

    /// Builds the window and connects its controls.
    /// @param defaultFile initial weather CSV
    private static void createWindow(Path defaultFile) {
        Stage stage = new Stage();
        stage.setTitle("WeatherScope | Data Analyzer");
        stage.setMinWidth(850);
        stage.setMinHeight(630);

        Label heading = new Label("WeatherScope");
        heading.getStyleClass().add("hero-title");
        Label subtitle = new Label("Explore temperature, humidity & rainfall patterns");
        subtitle.getStyleClass().add("hero-subtitle");
        Label eyebrow = new Label("WEATHER INTELLIGENCE  /  JAVA 23");
        eyebrow.getStyleClass().add("eyebrow");
        VBox headingGroup = new VBox(5, eyebrow, heading, subtitle);

        Button openButton = new Button("Open CSV file");
        openButton.getStyleClass().add("outlined-button");
        HBox header = new HBox(16, headingGroup, flexibleSpace(), openButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        ChoiceBox<YearMonth> monthSelector = new ChoiceBox<>();
        monthSelector.setPrefWidth(180);
        TextField thresholdInput = new TextField("30");
        thresholdInput.setPrefWidth(115);
        thresholdInput.setPromptText("Threshold °C");
        Button analyzeButton = new Button("Update analysis");
        analyzeButton.getStyleClass().add("primary-button");
        Label fileLabel = new Label("No file loaded");
        fileLabel.getStyleClass().add("source-label");

        HBox filters = new HBox(14,
                labeledControl("MONTH", monthSelector),
                labeledControl("HOT-DAY THRESHOLD (°C)", thresholdInput),
                analyzeButton,
                flexibleSpace());
        filters.setAlignment(Pos.BOTTOM_LEFT);
        filters.getStyleClass().add("filter-bar");

        Label averageValue = new Label("—");
        Label aboveValue = new Label("—");
        Label rainyValue = new Label("—");
        Label highestValue = new Label("—");
        HBox metrics = new HBox(14,
                metric("AVERAGE TEMPERATURE", averageValue, "Monthly mean", "metric-blue"),
                metric("ABOVE THRESHOLD", aboveValue, "Days exceeding limit", "metric-purple"),
                metric("RAINY DAYS", rainyValue, "Precipitation > 0 mm", "metric-teal"),
                metric("HIGHEST TEMPERATURE", highestValue, "Monthly peak", "metric-orange"));

        LineChart<String, Number> trendChart = makeTrendChart();
        VBox chartSection = new VBox(12,
                sectionHeading("Temperature trend", "Daily readings and your selected temperature threshold"),
                trendChart);
        chartSection.getStyleClass().add("panel");

        TableView<WeatherDay> dailyTable = makeTable();
        VBox tableSection = new VBox(12,
                sectionHeading("Daily observations", "All weather entries for the selected month"),
                dailyTable);
        tableSection.getStyleClass().add("panel");

        Label status = new Label("Loading weather data...");
        status.getStyleClass().add("footer-note");
        Label sourceTitle = new Label("DATA SOURCE");
        sourceTitle.getStyleClass().add("control-label");
        HBox footer = new HBox(12, sourceTitle, fileLabel, flexibleSpace(), status);
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(18, header, filters, metrics, chartSection, tableSection, footer);
        content.setPadding(new Insets(26, 35, 32, 35));
        content.getStyleClass().add("app-content");
        ScrollPane scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("root-scroll");

        Scene scene = new Scene(scroller, 1130, 890);
        var css = WeatherDashboard.class.getResource("/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);

        DashboardState state = new DashboardState(stage, monthSelector, thresholdInput,
                averageValue, aboveValue, rainyValue, highestValue,
                fileLabel, status, dailyTable, trendChart,
                new AtomicReference<>(List.of()));

        monthSelector.setOnAction(event -> refresh(state));
        analyzeButton.setOnAction(event -> refresh(state));
        thresholdInput.setOnAction(event -> refresh(state));
        openButton.setOnAction(event -> browseForCsv(state));
        loadFile(state, defaultFile);
        stage.show();
    }

    /// Creates a flexible spacer for the dashboard layout.
    /// @return expanding spacer
    private static Region flexibleSpace() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /// Adds a caption above a JavaFX control.
    /// @param caption input label
    /// @param input control
    /// @return labeled control container
    private static VBox labeledControl(String caption, javafx.scene.Node input) {
        Label label = new Label(caption);
        label.getStyleClass().add("control-label");
        return new VBox(8, label, input);
    }

    /// Creates a statistic tile.
    /// @param caption metric title
    /// @param value label holding the result
    /// @param note explanatory subtitle
    /// @param accent CSS style name
    /// @return statistic tile
    private static VBox metric(String caption, Label value, String note, String accent) {
        Label title = new Label(caption);
        title.getStyleClass().add("metric-title");
        value.getStyleClass().add("metric-number");
        Label subtext = new Label(note);
        subtext.getStyleClass().add("metric-note");
        VBox tile = new VBox(12, title, value, subtext);
        tile.getStyleClass().addAll("metric-tile", accent);
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    /// Creates a title and subtitle for a panel.
    /// @param title section title
    /// @param subtitle supporting text
    /// @return heading container
    private static VBox sectionHeading(String title, String subtitle) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        Label caption = new Label(subtitle);
        caption.getStyleClass().add("section-caption");
        return new VBox(3, label, caption);
    }

    /// Creates the daily temperature chart.
    /// @return temperature line chart
    private static LineChart<String, Number> makeTrendChart() {
        CategoryAxis dates = new CategoryAxis();
        dates.setLabel("Date");
        dates.setTickLabelRotation(-35);
        NumberAxis temperatures = new NumberAxis();
        temperatures.setLabel("Temperature (°C)");
        LineChart<String, Number> chart = new LineChart<>(dates, temperatures);
        chart.setAnimated(false);
        chart.setCreateSymbols(true);
        chart.setLegendVisible(true);
        chart.setMinHeight(290);
        chart.setPrefHeight(305);
        return chart;
    }

    /// Creates the table of weather observations.
    /// @return configured weather table
    private static TableView<WeatherDay> makeTable() {
        TableView<WeatherDay> table = new TableView<>();
        table.setEditable(false);
        table.setPrefHeight(265);
        table.setPlaceholder(new Label("No observations for this month"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<WeatherDay, String> date = new TableColumn<>("DATE");
        date.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(cell.getValue().date().toString()));
        TableColumn<WeatherDay, String> temperature = new TableColumn<>("TEMP (°C)");
        temperature.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(String.format(Locale.ROOT, "%.1f", cell.getValue().temperature())));
        TableColumn<WeatherDay, String> humidity = new TableColumn<>("HUMIDITY (%)");
        humidity.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(Integer.toString(cell.getValue().humidity())));
        TableColumn<WeatherDay, String> rain = new TableColumn<>("RAIN (MM)");
        rain.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(String.format(Locale.ROOT, "%.1f", cell.getValue().precipitation())));
        TableColumn<WeatherDay, String> category = new TableColumn<>("CATEGORY");
        category.setCellValueFactory(cell ->
                new ReadOnlyStringWrapper(WeatherAnalytics.category(cell.getValue())));

        table.getColumns().addAll(date, temperature, humidity, rain, category);
        return table;
    }

    /// Opens a file picker to load another dataset.
    /// @param state dashboard controls and data
    private static void browseForCsv(DashboardState state) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose weather CSV data");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        var selected = chooser.showOpenDialog(state.stage());
        if (selected != null) {
            loadFile(state, selected.toPath());
        }
    }

    /// Loads and validates CSV data while preserving the previous dataset on failure.
    /// @param state dashboard controls and data
    /// @param file selected CSV file
    private static void loadFile(DashboardState state, Path file) {
        try {
            List<WeatherDay> data = WeatherDataset.load(file);
            List<YearMonth> months = data.stream()
                    .map(day -> YearMonth.from(day.date()))
                    .distinct()
                    .sorted()
                    .toList();
            YearMonth previous = state.monthSelector().getValue();

            state.observations().set(data);
            state.fileLabel().setText(file.toAbsolutePath().toString());
            state.monthSelector().getItems().setAll(months);
            state.monthSelector().setDisable(months.isEmpty());
            if (!months.isEmpty()) {
                state.monthSelector().setValue(months.contains(previous) ? previous : months.getFirst());
            } else {
                refresh(state);
            }
        } catch (IOException | IllegalArgumentException exception) {
            showError("Unable to load CSV", exception.getMessage());
            state.status().setText("Could not load file");
        }
    }

    /// Updates metrics, chart, and table for the selected month.
    /// @param state dashboard controls and data
    private static void refresh(DashboardState state) {
        YearMonth month = state.monthSelector().getValue();
        if (month == null) {
            state.averageValue().setText("N/A");
            state.aboveValue().setText("0");
            state.rainyValue().setText("0");
            state.highestValue().setText("N/A");
            state.dailyTable().getItems().clear();
            state.trendChart().getData().clear();
            state.status().setText("No weather observations found");
            return;
        }

        try {
            double threshold = Double.parseDouble(state.thresholdInput().getText().strip());
            List<WeatherDay> monthDays = WeatherAnalytics.inMonth(state.observations().get(), month);
            var aboveDays = WeatherAnalytics.daysAboveThreshold(monthDays, threshold);

            state.averageValue().setText(WeatherAnalytics.display(
                    WeatherAnalytics.averageTemperature(state.observations().get(), month)));
            state.aboveValue().setText(Integer.toString(aboveDays.size()));
            state.rainyValue().setText(Long.toString(WeatherAnalytics.countRainyDays(monthDays)));
            state.highestValue().setText(WeatherAnalytics.display(
                    WeatherAnalytics.highestTemperature(monthDays)));
            state.dailyTable().setItems(FXCollections.observableArrayList(monthDays));
            plot(state.trendChart(), monthDays, threshold);
            state.status().setText(monthDays.size() + " observations  •  " + month);
        } catch (NumberFormatException exception) {
            showError("Invalid threshold", "Enter a valid numeric temperature in Celsius.");
        } catch (IllegalArgumentException exception) {
            showError("Invalid threshold", exception.getMessage());
        }
    }

    /// Draws the temperature series and threshold line.
    /// @param chart chart to update
    /// @param days days for the selected month
    /// @param threshold Celsius threshold
    private static void plot(LineChart<String, Number> chart, List<WeatherDay> days,
                             double threshold) {
        XYChart.Series<String, Number> temperatureSeries = new XYChart.Series<>();
        temperatureSeries.setName("Temperature");
        XYChart.Series<String, Number> thresholdSeries = new XYChart.Series<>();
        thresholdSeries.setName("Threshold");
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);

        days.forEach(day -> {
            String date = day.date().format(format);
            temperatureSeries.getData().add(new XYChart.Data<>(date, day.temperature()));
            thresholdSeries.getData().add(new XYChart.Data<>(date, threshold));
        });
        chart.getData().setAll(temperatureSeries, thresholdSeries);
    }

    /// Displays an error dialog.
    /// @param title dialog title
    /// @param message error details
    private static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

/// Holds the dashboard controls and current weather data.
/// @param stage application window
/// @param monthSelector month filter
/// @param thresholdInput Celsius threshold field
/// @param averageValue monthly average label
/// @param aboveValue hot-day count label
/// @param rainyValue rainy-day count label
/// @param highestValue maximum temperature label
/// @param fileLabel loaded CSV path label
/// @param status status label
/// @param dailyTable weather observations table
/// @param trendChart temperature chart
/// @param observations active weather observations
record DashboardState(Stage stage,
                      ChoiceBox<YearMonth> monthSelector,
                      TextField thresholdInput,
                      Label averageValue,
                      Label aboveValue,
                      Label rainyValue,
                      Label highestValue,
                      Label fileLabel,
                      Label status,
                      TableView<WeatherDay> dailyTable,
                      LineChart<String, Number> trendChart,
                      AtomicReference<List<WeatherDay>> observations) { }