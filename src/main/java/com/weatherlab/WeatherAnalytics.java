package com.weatherlab;

import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;

/// Stream-based weather analysis using static methods and no explicit classes.
public interface WeatherAnalytics {

    /// Returns observations from the requested month.
    /// @param days weather observations
    /// @param month requested year and month
    /// @return matching observations in their original order
    static List<WeatherDay> inMonth(List<WeatherDay> days, YearMonth month) {
        return days.stream()
                .filter(day -> YearMonth.from(day.date()).equals(month))
                .toList();
    }

    /// Calculates the average Celsius temperature for a month.
    /// @param days weather observations
    /// @param month requested year and month
    /// @return monthly average, or empty when no readings exist
    static OptionalDouble averageTemperature(List<WeatherDay> days, YearMonth month) {
        return inMonth(days, month).stream()
                .mapToDouble(WeatherDay::temperature)
                .average();
    }

    /// Filters readings strictly above a Celsius threshold.
    ///
    /// ```java
    /// var hotDays = WeatherAnalytics.daysAboveThreshold(days, 30.0);
    /// ```
    ///
    /// @param days weather observations
    /// @param threshold exclusive temperature cutoff in Celsius
    /// @return observations exceeding the threshold
    /// @throws IllegalArgumentException if the threshold is not finite
    static List<WeatherDay> daysAboveThreshold(List<WeatherDay> days, double threshold) {
        if (!Double.isFinite(threshold)) {
            throw new IllegalArgumentException("Threshold must be a finite number.");
        }
        return days.stream()
                .filter(day -> day.temperature() > threshold)
                .toList();
    }

    /// Counts observations with precipitation above zero.
    /// @param days weather observations
    /// @return number of rainy days
    static long countRainyDays(List<WeatherDay> days) {
        return days.stream()
                .filter(day -> day.precipitation() > 0.0)
                .count();
    }

    /// Finds the highest Celsius temperature.
    /// @param days weather observations
    /// @return maximum temperature, or empty when no readings exist
    static OptionalDouble highestTemperature(List<WeatherDay> days) {
        return days.stream()
                .mapToDouble(WeatherDay::temperature)
                .max();
    }

    /// Categorizes a reading: Hot (30+ °C), Warm (20–29.9 °C),
    /// Mild (10–19.9 °C), or Cold (below 10 °C).
    /// @param day weather observation, possibly null
    /// @return category, or Unknown for null
    static String category(WeatherDay day) {
        return switch (day) {
            case null -> "Unknown";
            case WeatherDay reading when reading.temperature() >= 30.0 -> "Hot";
            case WeatherDay reading when reading.temperature() >= 20.0 -> "Warm";
            case WeatherDay reading when reading.temperature() >= 10.0 -> "Mild";
            case WeatherDay reading -> "Cold";
        };
    }

    /// Formats a monthly summary using a Java text block.
    /// @param days complete weather dataset
    /// @param month requested year and month
    /// @param threshold exclusive Celsius cutoff
    /// @return formatted monthly report
    static String report(List<WeatherDay> days, YearMonth month, double threshold) {
        List<WeatherDay> monthly = inMonth(days, month);
        String average = display(averageTemperature(days, month));
        String maximum = display(highestTemperature(monthly));
        int aboveCount = daysAboveThreshold(monthly, threshold).size();
        long rainyCount = countRainyDays(monthly);

        return """
                WEATHER DATA ANALYZER
                =====================
                Month:                 %s
                Observations:          %d
                Average temperature:   %s
                Highest temperature:   %s
                Days above %.1f °C:    %d
                Rainy days:            %d
                """.formatted(month, monthly.size(), average, maximum,
                threshold, aboveCount, rainyCount).stripIndent();
    }

    /// Formats a Celsius reading, or N/A when absent.
    /// @param temperature optional Celsius temperature
    /// @return display-ready measurement
    static String display(OptionalDouble temperature) {
        return temperature.isPresent()
                ? String.format(Locale.ROOT, "%.1f °C", temperature.getAsDouble())
                : "N/A";
    }
}