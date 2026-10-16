package com.weatherlab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/// CSV loading operations for weather observations.
public interface WeatherDataset {

    /// Reads a UTF-8 weather CSV and returns records sorted by date.
    ///
    /// ```java
    /// var days = WeatherDataset.load(Path.of("weatherdata.csv"));
    /// ```
    ///
    /// @param file CSV file with Date,Temperature,Humidity,Precipitation columns
    /// @return immutable list of observations
    /// @throws IOException if the file cannot be read
    /// @throws IllegalArgumentException if the header or a row is invalid
    static List<WeatherDay> load(Path file) throws IOException {
        List<String> rows = Files.readAllLines(file, StandardCharsets.UTF_8);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The CSV file is empty: " + file);
        }

        String header = rows.getFirst().replace("\uFEFF", "").strip();
        List<String> columns = Arrays.stream(header.split(",", -1))
                .map(String::strip)
                .toList();
        if (!columns.equals(List.of("Date", "Temperature", "Humidity", "Precipitation"))) {
            throw new IllegalArgumentException("Invalid CSV header. Expected Date,Temperature,Humidity,Precipitation.");
        }

        return IntStream.range(1, rows.size())
                .filter(index -> !rows.get(index).isBlank())
                .mapToObj(index -> WeatherDay.fromCsv(rows.get(index), index + 1))
                .sorted(Comparator.comparing(WeatherDay::date))
                .toList();
    }
}