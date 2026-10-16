package com.weatherlab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/// Verifies CSV parsing and stream-based weather calculations.
record WeatherAnalyticsTest() {

    /// Supplies readings from two different months.
    private static List<WeatherDay> sample() {
        return List.of(
                new WeatherDay(LocalDate.of(2023, 8, 1), 28.0, 60, 0.0),
                new WeatherDay(LocalDate.of(2023, 8, 2), 30.0, 75, 0.1),
                new WeatherDay(LocalDate.of(2023, 8, 3), 32.0, 81, 3.0),
                new WeatherDay(LocalDate.of(2023, 9, 1), 10.0, 50, 0.0));
    }

    /// Parses a valid observation.
    @Test
    void parsesCsvRow() {
        WeatherDay day = WeatherDay.fromCsv("2023-08-01,32.5,65,0.0", 2);
        assertEquals(LocalDate.of(2023, 8, 1), day.date());
        assertEquals(32.5, day.temperature());
        assertEquals(65, day.humidity());
        assertEquals(0.0, day.precipitation());
    }

    /// Rejects malformed rows and invalid measurements.
    @Test
    void rejectsInvalidCsvValues() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeatherDay.fromCsv("2023-08-01,25.0,101,0", 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeatherDay.fromCsv("2023-08-01,25.0,65,-1", 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeatherDay.fromCsv("2023-08-01,invalid,65,0", 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeatherDay.fromCsv("2023-08-01,25.0,65", 2)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> WeatherDay.fromCsv("bad-date,25.0,65,0", 2)));
    }

    /// Rejects nonfinite temperatures and rainfall values.
    @Test
    void rejectsNonfiniteMeasurements() {
        LocalDate date = LocalDate.of(2023, 8, 1);
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherDay(date, Double.NaN, 60, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherDay(date, Double.POSITIVE_INFINITY, 60, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherDay(date, 25, 60, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new WeatherDay(null, 25, 60, 0));
    }

    /// Includes only observations in the chosen month.
    @Test
    void filtersByMonth() {
        var august = WeatherAnalytics.inMonth(sample(), YearMonth.of(2023, 8));
        assertEquals(3, august.size());
        assertTrue(august.stream().allMatch(day -> day.date().getMonthValue() == 8));
    }

    /// Computes the monthly mean temperature.
    @Test
    void averagesSpecifiedMonth() {
        assertEquals(30.0, WeatherAnalytics.averageTemperature(sample(),
                YearMonth.of(2023, 8)).orElseThrow());
    }

    /// Represents a month without readings as an empty result.
    @Test
    void handlesEmptyMonth() {
        YearMonth month = YearMonth.of(2024, 1);
        assertTrue(WeatherAnalytics.averageTemperature(sample(), month).isEmpty());
        assertTrue(WeatherAnalytics.report(sample(), month, 30).contains("N/A"));
    }

    /// Excludes temperatures exactly equal to the threshold.
    @Test
    void filtersStrictlyAboveThreshold() {
        var days = WeatherAnalytics.inMonth(sample(), YearMonth.of(2023, 8));
        var hotter = WeatherAnalytics.daysAboveThreshold(days, 30.0);
        assertEquals(1, hotter.size());
        assertEquals(32.0, hotter.getFirst().temperature());
        assertThrows(IllegalArgumentException.class,
                () -> WeatherAnalytics.daysAboveThreshold(days, Double.NaN));
    }

    /// Counts positive precipitation readings.
    @Test
    void countsRainyDays() {
        assertEquals(2, WeatherAnalytics.countRainyDays(sample()));
    }

    /// Finds the highest recorded temperature.
    @Test
    void findsHighestTemperature() {
        assertEquals(32.0, WeatherAnalytics.highestTemperature(sample()).orElseThrow());
        assertTrue(WeatherAnalytics.highestTemperature(List.of()).isEmpty());
    }

    /// Checks temperature categories at their boundaries.
    @Test
    void categorizesTemperatures() {
        LocalDate date = LocalDate.of(2023, 8, 1);
        assertEquals("Hot", WeatherAnalytics.category(new WeatherDay(date, 30, 50, 0)));
        assertEquals("Warm", WeatherAnalytics.category(new WeatherDay(date, 20, 50, 0)));
        assertEquals("Mild", WeatherAnalytics.category(new WeatherDay(date, 10, 50, 0)));
        assertEquals("Cold", WeatherAnalytics.category(new WeatherDay(date, 9.9, 50, 0)));
        assertEquals("Unknown", WeatherAnalytics.category(null));
    }

    /// Includes the selected month and correct calculated totals.
    @Test
    void reportsMonthlySummary() {
        String report = WeatherAnalytics.report(sample(), YearMonth.of(2023, 8), 30);
        assertAll(
                () -> assertTrue(report.contains("2023-08")),
                () -> assertTrue(report.contains("30.0 °C")),
                () -> assertTrue(report.contains("32.0 °C")),
                () -> assertTrue(report.contains("Observations:          3")),
                () -> assertTrue(report.contains("Rainy days:            2")));
    }

    /// Sorts observations and skips blank CSV lines.
    @Test
    void sortsAndSkipsBlankLines(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("weather.csv");
        Files.writeString(csv, """
                Date,Temperature,Humidity,Precipitation
                2023-08-02,20,40,0

                2023-08-01,21,41,0.1
                """);
        var days = WeatherDataset.load(csv);
        assertEquals(2, days.size());
        assertEquals(LocalDate.of(2023, 8, 1), days.getFirst().date());
    }

    /// Rejects a CSV file with the wrong column names.
    @Test
    void rejectsIncorrectHeader(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("wrong-header.csv");
        Files.writeString(csv, "Date,Temperature,Wrong,Precipitation\n");
        assertThrows(IllegalArgumentException.class, () -> WeatherDataset.load(csv));
    }

    /// Identifies malformed rows by their source line number.
    @Test
    void identifiesInvalidRowLine(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("invalid-row.csv");
        Files.writeString(csv, """
                Date,Temperature,Humidity,Precipitation
                2023-08-01,25,65,0
                2023-08-02,invalid,70,0
                """);
        var failure = assertThrows(IllegalArgumentException.class, () -> WeatherDataset.load(csv));
        assertTrue(failure.getMessage().contains("Line 3"));
    }

    /// Rejects a completely empty CSV file.
    @Test
    void rejectsEmptyCsvFile(@TempDir Path directory) throws IOException {
        Path csv = directory.resolve("empty.csv");
        Files.writeString(csv, "");
        assertThrows(IllegalArgumentException.class, () -> WeatherDataset.load(csv));
    }

    /// Loads the sample dataset included in the repository.
    @Test
    void loadsBundledSampleCsv() throws IOException {
        var days = WeatherDataset.load(Path.of("weatherdata.csv"));
        assertEquals(76, days.size());
        assertEquals(LocalDate.of(2023, 8, 1), days.getFirst().date());
        assertEquals(LocalDate.of(2023, 10, 15), days.getLast().date());
    }
}