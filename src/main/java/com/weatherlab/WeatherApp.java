package com.weatherlab;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;

/// Launches the JavaFX dashboard or runs the command-line analyzer.
public record WeatherApp() {

    /// Opens the dashboard by default or runs the CLI with optional arguments.
    ///
    /// ```sh
    /// java -cp target/classes com.weatherlab.WeatherApp --cli weatherdata.csv 2023-08 30
    /// ```
    ///
    /// @param args `--cli [csv] [YYYY-MM] [threshold]` or `--help`
    public static void main(String[] args) {
        if (args.length == 1 && args[0].equals("--help")) {
            System.out.print("""
                    Weather Data Analyzer
                    Usage:
                      ./mvnw javafx:run
                      java -cp target/classes com.weatherlab.WeatherApp --cli [csv] [YYYY-MM] [threshold]
                      java -cp target/classes com.weatherlab.WeatherApp --help
                    CLI defaults: weatherdata.csv 2023-08 30
                    """);
            return;
        }

        if (args.length == 0) {
            WeatherDashboard.open(Path.of("weatherdata.csv"));
            return;
        }

        if (args.length > 4 || (args.length > 0 && !args[0].equals("--cli"))) {
            System.err.println("Invalid arguments. Run with --help for usage.");
            System.exit(2);
            return;
        }

        try {
            Path file = Path.of(args.length >= 2 ? args[1] : "weatherdata.csv");
            YearMonth month = YearMonth.parse(args.length >= 3 ? args[2] : "2023-08");
            double threshold = Double.parseDouble(args.length >= 4 ? args[3] : "30");
            var days = WeatherDataset.load(file);
            var monthly = WeatherAnalytics.inMonth(days, month);
            var aboveThreshold = WeatherAnalytics.daysAboveThreshold(monthly, threshold);

            System.out.printf("Loaded %d observations from %s%n%n", days.size(), file);
            System.out.print(WeatherAnalytics.report(days, month, threshold));
            System.out.printf("%nDays above %.1f °C:%n", threshold);

            if (aboveThreshold.isEmpty()) {
                System.out.println("  None");
            } else {
                aboveThreshold.forEach(day -> System.out.printf(
                        "  %s | %.1f °C | %s%n",
                        day.date(), day.temperature(), WeatherAnalytics.category(day)));
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("Unable to analyze weather data: " + exception.getMessage());
            System.exit(1);
        }
    }
}