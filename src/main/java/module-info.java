module com.tuempresa.evaluacioniiicorte {
    requires javafx.controls;
    requires javafx.fxml;
    requires jakarta.persistence;
    requires java.sql;

    opens com.tuempresa.evaluacioniiicorte to javafx.fxml;
    opens com.tuempresa.evaluacioniiicorte.controller to javafx.fxml;
    opens com.tuempresa.evaluacioniiicorte.model;

    exports com.tuempresa.evaluacioniiicorte;
}