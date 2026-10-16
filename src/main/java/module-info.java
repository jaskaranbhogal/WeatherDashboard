module com.example.weatheranalyzerproject {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.weatheranalyzerproject to javafx.fxml;
    exports com.example.weatheranalyzerproject;
}