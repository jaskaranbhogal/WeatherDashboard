# WeatherDashboard

**WeatherScope** is a Java 23 weather data analyzer that reads daily observations from CSV files and displays monthly statistics in a JavaFX dashboard or the command line. The project uses records, interfaces, streams, lambdas, enhanced `switch` expressions, text blocks, and Markdown Javadoc without declaring conventional application classes.

## Features

- Import a weather CSV file with date, temperature, humidity, and precipitation.
- Calculate the average and highest temperature for a selected month.
- List or count days **strictly above** a chosen Celsius temperature threshold.
- Count rainy days (precipitation **greater than 0 mm**).
- Categorize readings as **Hot**, **Warm**, **Mild**, or **Cold**.
- View a JavaFX dashboard with monthly filters, four statistics, a temperature trend chart, a threshold line, and a daily data table.
- Run the same analysis in a terminal without opening the GUI.
- Validate headers, dates, numeric values, humidity, precipitation, and CSV row structure.

## Requirements

- **JDK 23 or newer** (the Maven project compiles with `--release 23`).
- A desktop environment to open the JavaFX interface; the CLI does not require one.
- Git for cloning the repository (or download the source as a ZIP).

The repository includes the **Maven Wrapper** (`mvnw`, `mvnw.cmd`, and `.mvn/wrapper`), so a separate Maven installation is not required. Maven resolves JavaFX 23.0.2 and JUnit Jupiter 5.11.4 using `pom.xml`.

Check your active JDK:

```sh
java -version
```

## Get started

```sh
git clone https://github.com/jaskaranbhogal/WeatherDashboard.git
cd WeatherDashboard
./mvnw clean compile
```

On Windows, replace `./mvnw` with `mvnw.cmd`. This is a private repository, so GitHub access is required to clone it.

Run all tests:

```sh
./mvnw clean test
```

### Launch the JavaFX dashboard

```sh
./mvnw javafx:run
```

Start the program from the repository root so that the default `weatherdata.csv` file can be found. The dashboard initially loads this file and selects the first available month. Use **Open CSV file** to choose another dataset, select a month, enter a temperature threshold (default **30 °C**), and click **Update analysis** to refresh the results.

### Run in the terminal

The `--cli` option runs the analyzer without opening JavaFX:

```sh
./mvnw compile exec:java -Dexec.args="--cli"
```

Provide an optional CSV path, month (`YYYY-MM`), and Celsius threshold:

```sh
./mvnw compile exec:java -Dexec.args="--cli weatherdata.csv 2023-08 30"
./mvnw compile exec:java -Dexec.args="--cli weatherdata.csv 2023-09 25"
```

Print the built-in help message:

```sh
./mvnw compile exec:java -Dexec.args="--help"
```

CLI defaults are `weatherdata.csv`, `2023-08`, and `30`. The report shows the observation count, monthly average and maximum, number of days over the threshold, rainy-day count, and individual dates above the threshold. For a month without observations, average and maximum temperatures display `N/A`.

## CSV format

The repository includes [`weatherdata.csv`](weatherdata.csv) with **76 sample observations** covering August 1 through October 15, 2023. You can use it immediately without creating a separate dataset.

```csv
Date,Temperature,Humidity,Precipitation
2023-08-01,32.5,65,0.0
2023-08-02,35.0,60,0.2
```

| Column | Format | Units / validation |
| --- | --- | --- |
| `Date` | `YYYY-MM-DD` | Valid calendar date |
| `Temperature` | Decimal | Finite number, degrees Celsius |
| `Humidity` | Integer | Relative humidity, 0–100% |
| `Precipitation` | Decimal | Nonnegative finite number, millimeters |

The first line must contain those four column names in that order. Blank lines are ignored; loaded observations are sorted by date. The parser handles simple comma-delimited rows, **not quoted CSV fields**.

### Temperature categories

| Temperature (°C) | Category |
| --- | --- |
| `>= 30` | Hot |
| `>= 20` and `< 30` | Warm |
| `>= 10` and `< 20` | Mild |
| `< 10` | Cold |

## Project structure

```text
WeatherDashboard/
├── pom.xml
├── mvnw / mvnw.cmd
├── .mvn/wrapper/
├── weatherdata.csv
├── src/main/java/com/weatherlab/
│   ├── WeatherApp.java            # Main entry point (record)
│   ├── WeatherDay.java            # Validated weather observation (record)
│   ├── WeatherDataset.java        # CSV loading (interface)
│   ├── WeatherAnalytics.java      # Stream queries and reports (interface)
│   └── WeatherDashboard.java      # JavaFX UI and DashboardState record
├── src/main/resources/
│   └── styles.css                 # Dashboard theme
└── src/test/java/com/weatherlab/
    └── WeatherAnalyticsTest.java # JUnit tests (record)
```

## Java features demonstrated

| Feature | Where it is used |
| --- | --- |
| Records (Java 16) | `WeatherDay`, `WeatherApp`, `DashboardState`, test fixture |
| Enhanced switch expressions (Java 14) | Weather categories in `WeatherAnalytics.category()` |
| Pattern matching for switch (Java 21) | Guarded `WeatherDay` patterns using `when` |
| Text blocks (Java 15) | CLI help and formatted weather report |
| Streams and lambdas | CSV parsing, monthly filtering, statistics, and UI event handlers |
| Markdown Javadoc (`///`, JDK 23) | Concise API documentation and examples in Java source files |

Application code declares **records and interfaces**, not regular classes. JavaFX itself provides library classes used to render the interface.

## Tests and documentation

`src/test/java/com/weatherlab/WeatherAnalyticsTest.java` contains **16 JUnit 5 test methods** covering valid and invalid CSV rows, finite value constraints, sorting, empty files and months, stream calculations, threshold boundaries, category boundaries, and formatted report results.

```sh
./mvnw test
./mvnw javadoc:javadoc
```

The second command generates API documentation under `target/reports/apidocs/` when Maven uses its standard reporting output; depending on plugin invocation it may instead be under `target/site/apidocs/`.

## Troubleshooting

- **Java version error:** Ensure `java -version` reports JDK 23 or newer and your IDE/Maven uses the same JDK.
- **CSV not found:** Run commands from the repository root or pass a path using `--cli` or the dashboard file picker.
- **JavaFX display error:** Run the GUI on a desktop with a graphical session, or use `--cli` in a headless environment.
- **Missing dependencies:** Allow Maven to download dependencies on the first run.

---

**Course assignment:** Advanced Programming — Weather Data Analyzer. The included weather observations are illustrative sample data, not a live weather feed.
