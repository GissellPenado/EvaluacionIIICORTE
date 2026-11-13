package com.tuempresa.evaluacioniiicorte;

import com.tuempresa.evaluacioniiicorte.controller.EquipoController;
import com.tuempresa.evaluacioniiicorte.database.JPAUtil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class LaboratorioApplication extends Application {

    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    LaboratorioApplication.class.getResource(
                            "equipos-view.fxml"
                    )
            );

            Scene scene = new Scene(loader.load());

            stage.setTitle("Control de equipos de laboratorio");
            stage.setScene(scene);
            stage.setMinWidth(950);
            stage.setMinHeight(620);
            stage.show();

            EquipoController controller = loader.getController();

            Platform.runLater(controller::cargarDatosIniciales);
        } catch (Exception ex) {
            ex.printStackTrace();

            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Error al iniciar");
            alerta.setHeaderText("No se pudo abrir la aplicación");
            alerta.setContentText(
                    "Revisa equipos-view.fxml y la consola de IntelliJ."
            );
            alerta.showAndWait();

            Platform.exit();
        }
    }

    @Override
    public void stop() {
        JPAUtil.cerrar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}