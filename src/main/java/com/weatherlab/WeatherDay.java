package com.weatherlab;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/// A daily observation: temperature in °C, humidity in %, and precipitation in mm.
///
/// @param date observation date
/// @param temperature Celsius temperature
/// @param humidity relative humidity (0–100)
/// @param precipitation nonnegative rainfall in millimeters
public record WeatherDay(LocalDate date, double temperature, int humidity, double precipitation) {

    /// Validates the observation's measurements.
    /// @throws IllegalArgumentException if any value is invalid
    public WeatherDay {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null.");
        }
        if (!Double.isFinite(temperature)) {
            throw new IllegalArgumentException("Temperature must be a finite number.");
        }
        if (humidity < 0 || humidity > 100) {
            throw new IllegalArgumentException("Humidity must be between 0 and 100.");
        }
        if (!Double.isFinite(precipitation) || precipitation < 0) {
            throw new IllegalArgumentException("Precipitation must be a nonnegative, finite number.");
        }
    }

    /// Parses a weather observation from four comma-separated values.
    ///
    /// ```java
    /// var day = WeatherDay.fromCsv("2023-08-01,32.5,65,0.0", 2);
    /// ```
    ///
    /// @param row CSV row without its header
    /// @param lineNumber 1-based line number for error messages
    /// @return parsed observation
    /// @throws IllegalArgumentException if the row is invalid
    public static WeatherDay fromCsv(String row, int lineNumber) {
        String[] fields = row.split(",", -1);
        if (fields.length != 4) {
            throw new IllegalArgumentException("Line " + lineNumber + ": expected four CSV columns.");
        }

        try {
            return new WeatherDay(
                    LocalDate.parse(fields[0].strip()),
                    Double.parseDouble(fields[1].strip()),
                    Integer.parseInt(fields[2].strip()),
                    Double.parseDouble(fields[3].strip()));
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw new IllegalArgumentException("Line " + lineNumber + ": invalid date or number: " + row,
                    exception);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + exception.getMessage(),
                    exception);
        }
    }
}