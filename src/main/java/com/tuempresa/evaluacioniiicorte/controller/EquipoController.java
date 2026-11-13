package com.tuempresa.evaluacioniiicorte.controller;

import com.tuempresa.evaluacioniiicorte.dao.EquipoDAO;
import com.tuempresa.evaluacioniiicorte.dao.TipoEquipoDAO;
import com.tuempresa.evaluacioniiicorte.model.Equipo;
import com.tuempresa.evaluacioniiicorte.model.TipoEquipo;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;

public class EquipoController {

    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextField txtMarca;
    @FXML private TextField txtCantidad;

    @FXML private ComboBox<TipoEquipo> cmbTipo;
    @FXML private ComboBox<String> cmbEstado;

    @FXML private TextField txtBusqueda;
    @FXML private ComboBox<TipoEquipo> cmbFiltroTipo;

    @FXML private TableView<Equipo> tablaEquipos;
    @FXML private TableColumn<Equipo, String> colCodigo;
    @FXML private TableColumn<Equipo, String> colNombre;
    @FXML private TableColumn<Equipo, String> colMarca;
    @FXML private TableColumn<Equipo, String> colEstado;
    @FXML private TableColumn<Equipo, Integer> colCantidad;
    @FXML private TableColumn<Equipo, String> colTipo;

    @FXML private Button btnGuardar;
    @FXML private Button btnActualizar;
    @FXML private Button btnEliminar;
    @FXML private Label lblTotal;

    private final EquipoDAO equipoDAO = new EquipoDAO();
    private final TipoEquipoDAO tipoEquipoDAO = new TipoEquipoDAO();

    private Equipo seleccionado;

    @FXML
    private void initialize() {
        configurarTabla();

        cmbEstado.setItems(
                FXCollections.observableArrayList(EquipoDAO.ESTADOS)
        );

        configurarCombo(cmbTipo, "Selecciona un tipo");
        configurarCombo(cmbFiltroTipo, "Todos los tipos");

        tablaEquipos.getSelectionModel()
                .selectedItemProperty()
                .addListener((observable, anterior, actual) -> {
                    seleccionado = actual;

                    if (actual != null) {
                        cargarFormulario(actual);
                    }

                    actualizarBotones();
                });

        limpiarFormulario();
    }

    // La aplicación llama este método después de abrir la ventana.
    public void cargarDatosIniciales() {
        try {
            List<TipoEquipo> tipos = tipoEquipoDAO.listar();

            cmbTipo.setItems(FXCollections.observableArrayList(tipos));

            // null representa "Todos los tipos".
            cmbFiltroTipo.getItems().clear();
            cmbFiltroTipo.getItems().add(null);
            cmbFiltroTipo.getItems().addAll(tipos);

            recargarTabla();
        } catch (RuntimeException ex) {
            mostrarError(ex);
        }
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().getCodigo())
        );

        colNombre.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().getNombre())
        );

        colMarca.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().getMarca())
        );

        colEstado.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(fila.getValue().getEstado())
        );

        colCantidad.setCellValueFactory(
                fila -> new ReadOnlyObjectWrapper<>(
                        fila.getValue().getCantidad()
                )
        );

        colTipo.setCellValueFactory(
                fila -> new ReadOnlyStringWrapper(
                        fila.getValue().getTipoEquipo().getNombre()
                )
        );

        tablaEquipos.setPlaceholder(
                new Label("No hay equipos para mostrar.")
        );
    }

    private void configurarCombo(
            ComboBox<TipoEquipo> combo, String textoVacio
    ) {
        combo.setCellFactory(lista -> crearCeldaTipo(textoVacio));
        combo.setButtonCell(crearCeldaTipo(textoVacio));
        combo.setPromptText(textoVacio);
    }

    private ListCell<TipoEquipo> crearCeldaTipo(String textoVacio) {
        return new ListCell<>() {
            @Override
            protected void updateItem(TipoEquipo tipo, boolean empty) {
                super.updateItem(tipo, empty);

                setText(empty ? null
                        : tipo == null ? textoVacio : tipo.getNombre());
            }
        };
    }

    @FXML
    private void guardar() {
        try {
            Equipo datos = leerFormulario();
            equipoDAO.guardar(datos);
        } catch (RuntimeException ex) {
            mostrarError(ex);
            return;
        }

        despuesDeEscritura("Equipo guardado correctamente.");
    }

    @FXML
    private void actualizar() {
        if (seleccionado == null) {
            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Selecciona un equipo de la tabla."
            );
            return;
        }

        try {
            Equipo datos = leerFormulario();
            equipoDAO.actualizar(seleccionado.getIdEquipo(), datos);
        } catch (RuntimeException ex) {
            mostrarError(ex);
            return;
        }

        despuesDeEscritura("Equipo actualizado correctamente.");
    }

    @FXML
    private void eliminar() {
        if (seleccionado == null) {
            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    "Selecciona un equipo de la tabla."
            );
            return;
        }

        Alert confirmacion = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Deseas eliminar el equipo "
                        + seleccionado.getCodigo()
                        + " - " + seleccionado.getNombre() + "?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("Eliminar equipo");

        if (confirmacion.showAndWait().orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {
            equipoDAO.eliminar(seleccionado.getIdEquipo());
        } catch (RuntimeException ex) {
            mostrarError(ex);
            return;
        }

        despuesDeEscritura("Equipo eliminado correctamente.");
    }

    @FXML
    private void buscar() {
        try {
            recargarTabla();
        } catch (RuntimeException ex) {
            mostrarError(ex);
        }
    }

    @FXML
    private void quitarFiltros() {
        txtBusqueda.clear();
        cmbFiltroTipo.setValue(null);
        buscar();
    }

    @FXML
    private void limpiarFormulario() {
        tablaEquipos.getSelectionModel().clearSelection();
        seleccionado = null;

        txtCodigo.clear();
        txtNombre.clear();
        txtMarca.clear();
        txtCantidad.clear();

        cmbTipo.setValue(null);
        cmbEstado.setValue(null);

        actualizarBotones();
    }

    private void actualizarBotones() {
        boolean haySeleccion = seleccionado != null;

        btnGuardar.setDisable(haySeleccion);
        btnActualizar.setDisable(!haySeleccion);
        btnEliminar.setDisable(!haySeleccion);
    }

    private Equipo leerFormulario() {
        String codigo = txtCodigo.getText().trim();
        String nombre = txtNombre.getText().trim();

        if (codigo.isBlank()) {
            throw new IllegalArgumentException("Ingresa el código.");
        }

        if (nombre.isBlank()) {
            throw new IllegalArgumentException("Ingresa el nombre.");
        }

        int cantidad;

        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser un número entero válido."
            );
        }

        if (cantidad < 0) {
            throw new IllegalArgumentException(
                    "La cantidad no puede ser negativa."
            );
        }

        if (cmbTipo.getValue() == null) {
            throw new IllegalArgumentException(
                    "Selecciona un tipo de equipo."
            );
        }

        if (cmbEstado.getValue() == null
                || !EquipoDAO.ESTADOS.contains(cmbEstado.getValue())) {
            throw new IllegalArgumentException(
                    "Selecciona un estado permitido."
            );
        }

        Equipo equipo = new Equipo();
        equipo.setCodigo(codigo);
        equipo.setNombre(nombre);
        equipo.setMarca(txtMarca.getText().trim());
        equipo.setCantidad(cantidad);
        equipo.setEstado(cmbEstado.getValue());
        equipo.setTipoEquipo(cmbTipo.getValue());

        return equipo;
    }

    private void cargarFormulario(Equipo equipo) {
        txtCodigo.setText(equipo.getCodigo());
        txtNombre.setText(equipo.getNombre());
        txtMarca.setText(equipo.getMarca() == null ? "" : equipo.getMarca());
        txtCantidad.setText(String.valueOf(equipo.getCantidad()));
        cmbEstado.setValue(equipo.getEstado());

        TipoEquipo tipo = cmbTipo.getItems().stream()
                .filter(t -> t.getIdTipo().equals(
                        equipo.getTipoEquipo().getIdTipo()
                ))
                .findFirst()
                .orElse(null);

        cmbTipo.setValue(tipo);
    }

    private void recargarTabla() {
        TipoEquipo filtro = cmbFiltroTipo.getValue();
        Long idTipo = filtro == null ? null : filtro.getIdTipo();

        List<Equipo> equipos = equipoDAO.buscar(
                txtBusqueda.getText(),
                idTipo
        );

        tablaEquipos.setItems(
                FXCollections.observableArrayList(equipos)
        );

        lblTotal.setText("Equipos mostrados: " + equipos.size());
        limpiarFormulario();
    }

    private void despuesDeEscritura(String mensaje) {
        limpiarFormulario();

        try {
            recargarTabla();
        } catch (RuntimeException ex) {
            ex.printStackTrace();

            mostrarMensaje(
                    Alert.AlertType.WARNING,
                    mensaje + "\n\nLa escritura fue confirmada, pero no "
                            + "se pudo actualizar el listado. "
                            + "Pulsa Aplicar filtros para reintentar."
            );
            return;
        }

        mostrarMensaje(Alert.AlertType.INFORMATION, mensaje);
    }

    private void mostrarError(RuntimeException ex) {
        ex.printStackTrace();

        if (ex instanceof IllegalArgumentException) {
            mostrarMensaje(Alert.AlertType.WARNING, ex.getMessage());
            return;
        }

        String mensaje = "No se pudo completar la operación. "
                + "Revisa la conexión y la configuración de PostgreSQL.";

        Throwable causa = ex;

        while (causa != null) {
            if (causa instanceof SQLException sql) {
                String estado = sql.getSQLState();

                if ("23505".equals(estado)) {
                    mensaje = "Ya existe un registro con ese código.";
                } else if ("23503".equals(estado)) {
                    mensaje = "El registro relacionado ya no existe "
                            + "o está siendo utilizado.";
                } else if ("23514".equals(estado)) {
                    mensaje = "Los datos incumplen una restricción: "
                            + "revisa cantidad y estado.";
                } else if ("23502".equals(estado)) {
                    mensaje = "Falta un dato obligatorio.";
                }

                break;
            }

            causa = causa.getCause();
        }

        mostrarMensaje(Alert.AlertType.ERROR, mensaje);
    }

    private void mostrarMensaje(Alert.AlertType tipo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle("Control de equipos");
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}