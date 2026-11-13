# EvaluacionIIICorte

Aplicación de escritorio para administrar el catálogo de equipos de un laboratorio. Integra JavaFX, Hibernate ORM y PostgreSQL para registrar, consultar, actualizar y eliminar equipos, buscar por nombre y filtrar por tipo.

El alcance comprende equipos y tipos de equipo. No incluye préstamos, usuarios, autenticación ni reportes.

## Tecnologías y requisitos

| Componente | Versión configurada |
| --- | --- |
| JDK | 25 |
| JavaFX | 21.0.6 |
| Hibernate ORM | 7.1.36.Final |
| Jakarta Persistence | 3.2.0 |
| PostgreSQL JDBC | 42.7.14 |
| Maven | A través de `mvnw.cmd` |

Se necesita PostgreSQL en ejecución y conexión a Internet en la primera compilación para descargar dependencias. El entorno local verificado utiliza PostgreSQL 18.4. IntelliJ IDEA es el IDE utilizado para el desarrollo.

## Estructura

```text
src/main/java/
├── module-info.java
└── com/tuempresa/evaluacioniiicorte/
    ├── LaboratorioApplication.java
    ├── Launcher.java
    ├── controller/EquipoController.java
    ├── database/JPAUtil.java
    ├── dao/
    │   ├── EquipoDAO.java
    │   └── TipoEquipoDAO.java
    └── model/
        ├── Equipo.java
        └── TipoEquipo.java

src/main/resources/
├── META-INF/persistence.xml
└── com/tuempresa/evaluacioniiicorte/equipos-view.fxml
```

Los archivos `HelloApplication`, `HelloController` y `hello-view.fxml` pertenecen a la plantilla inicial y no forman parte del flujo principal.

## Base de datos

La estructura se crea manualmente con SQL. La conexión predeterminada es:

```text
Base de datos: bd_laboratorio_orm
Esquema: catalogos
Servidor: localhost
Puerto: 5432
Usuario: postgres
```

Antes de iniciar la aplicación, deben existir las tablas siguientes:

| Tabla | Columnas |
| --- | --- |
| `catalogos.tipo_equipo` | `id_tipo BIGINT` generado, `nombre VARCHAR(100)`, `descripcion VARCHAR(250)` |
| `catalogos.equipo` | `id_equipo BIGINT` generado, `codigo VARCHAR(50)`, `nombre VARCHAR(150)`, `marca VARCHAR(100)`, `estado VARCHAR(30)`, `cantidad INTEGER`, `id_tipo BIGINT` |

Ambas tablas tienen su identificador como clave primaria. El nombre del tipo es obligatorio y único. En equipo, el código es obligatorio y único; nombre, estado, cantidad y tipo son obligatorios. La cantidad debe ser mayor o igual a cero. La descripción y la marca son opcionales.

La clave foránea `equipo.id_tipo` referencia `tipo_equipo.id_tipo`. La política de eliminación es `ON DELETE RESTRICT`: no se permite eliminar un tipo que tenga equipos asociados.

Los estados permitidos son:

- Disponible
- En mantenimiento
- Fuera de servicio

La carga inicial debe incluir al menos tres tipos y ocho equipos distribuidos entre ellos, con diferentes estados.

### Preparación en pgAdmin

1. Conectarse a PostgreSQL y abrir Query Tool sobre la base `postgres`.
2. Ejecutar `CREATE DATABASE bd_laboratorio_orm;` fuera de una transacción, únicamente si todavía no existe.
3. Abrir Query Tool sobre `bd_laboratorio_orm` y ejecutar el script SQL de creación del esquema, tablas, restricciones y datos iniciales.
4. Verificar la estructura y los registros con la consulta siguiente.

```sql
SELECT e.id_equipo, e.codigo, e.nombre, e.marca,
       e.estado, e.cantidad, t.nombre AS tipo_equipo
FROM catalogos.equipo e
JOIN catalogos.tipo_equipo t ON t.id_tipo = e.id_tipo
ORDER BY e.id_equipo;
```

**Pendiente de entrega:** incorporar al repositorio el archivo `.sql` reproducible. Las instrucciones anteriores describen su ejecución; este README no sustituye ese archivo.

## Configuración de persistencia

La unidad de persistencia se llama `laboratorioPU` y está definida en `src/main/resources/META-INF/persistence.xml`. Utiliza Hibernate y transacciones locales (`RESOURCE_LOCAL`).

`hibernate.hbm2ddl.auto=validate` comprueba que las tablas sean compatibles con las entidades. Hibernate no crea, modifica ni elimina el esquema: debe prepararse mediante SQL antes de iniciar.

`JPAUtil` permite configurar la conexión con estas variables de entorno:

| Variable | Uso |
| --- | --- |
| `DB_PASSWORD` | Contraseña obligatoria del usuario PostgreSQL |
| `DB_USER` | Usuario opcional; por defecto `postgres` |
| `DB_URL` | URL JDBC opcional; por defecto `jdbc:postgresql://localhost:5432/bd_laboratorio_orm` |

La contraseña se obtiene del entorno y no se escribe en el código ni en este documento. Los ejemplos utilizan un valor de demostración que debe sustituirse localmente. No publicar configuraciones del IDE que contengan credenciales.

## Ejecución en PowerShell

Abrir una terminal en la carpeta que contiene `pom.xml` y `mvnw.cmd`:

```powershell
$env:DB_PASSWORD = "TU_CONTRASENA_LOCAL"
.\mvnw.cmd javafx:run
```

Si el servidor o usuario son diferentes:

```powershell
$env:DB_USER = "postgres"
$env:DB_URL = "jdbc:postgresql://localhost:5432/bd_laboratorio_orm"
```

Para compilar sin abrir la ventana:

```powershell
.\mvnw.cmd compile
```

`JAVA_HOME` y el SDK del proyecto deben apuntar a JDK 25.

## Ejecución en IntelliJ IDEA

1. Abrir el proyecto como proyecto Maven y recargar sus dependencias.
2. Seleccionar JDK 25 como Project SDK y como JRE de Maven.
3. Crear o seleccionar una configuración de ejecución Maven.
4. En Run / Command line escribir `javafx:run`.
5. Dejar Profiles vacío.
6. En Environment variables agregar `DB_PASSWORD=TU_CONTRASENA_LOCAL`.
7. Ejecutar la configuración.

La clase de inicio configurada en Maven es `com.tuempresa.evaluacioniiicorte.LaboratorioApplication`. También existe `Launcher` como punto de entrada.

## Uso de la aplicación

- **Guardar:** completar código, nombre, cantidad, tipo y estado. La marca es opcional. El identificador se genera automáticamente.
- **Actualizar:** seleccionar una fila, cambiar los datos y pulsar Actualizar.
- **Eliminar:** seleccionar una fila, pulsar Eliminar y confirmar la operación.
- **Limpiar:** quitar la selección y vaciar el formulario para registrar otro equipo.
- **Aplicar filtros:** escribir parte del nombre, seleccionar un tipo o combinar ambos criterios.
- **Quitar filtros:** restablecer el listado completo.

Al seleccionar una fila, sus datos se cargan en el formulario. Mientras exista una selección, Guardar queda deshabilitado para evitar confundir una actualización con un registro nuevo.

## Entidades y asociación ORM

`TipoEquipo` representa la clasificación del equipo. `Equipo` contiene sus datos y una referencia a un objeto `TipoEquipo`.

La relación se implementa mediante `@ManyToOne(fetch = FetchType.LAZY)` y `@JoinColumn(name = "id_tipo")` en `Equipo`. Varios equipos pueden compartir un tipo y cada equipo pertenece a un único tipo.

Se utiliza una asociación unidireccional: `TipoEquipo` no necesita una colección inversa para consultar y administrar los equipos. No se configura borrado en cascada.

## CRUD y transacciones

`EquipoController` administra los controles y eventos de JavaFX. Los DAO administran la persistencia.

- **Crear:** se obtiene un tipo existente con `find()` y se guarda el equipo con `persist()`.
- **Leer:** se consultan entidades con JPQL y se muestran en el `TableView`.
- **Actualizar:** se busca el equipo con `find()` dentro de una transacción y se modifican sus atributos. Al estar administrado por el EntityManager, Hibernate detecta los cambios; no se necesita `merge()` en este flujo.
- **Eliminar:** se recupera la entidad y se elimina con `remove()` tras la confirmación del usuario.

Cada escritura ejecuta `begin()` y finaliza con `commit()`. Si ocurre una excepción y la transacción sigue activa, ejecuta `rollback()` y propaga el error al controlador. Cada unidad de trabajo crea y cierra su propio `EntityManager`.

`JPAUtil` mantiene un `EntityManagerFactory` compartido, creado al primer acceso. `LaboratorioApplication.stop()` lo cierra al terminar la aplicación.

Para permitir la creación de proxies en esta ejecución modular, `JPAUtil` habilita la lectura del módulo donde se carga Hibernate antes de construir el EntityManagerFactory. El paquete de entidades está abierto para el acceso reflexivo del ORM.

## Consultas y filtros

La consulta base usa `JOIN FETCH` para recuperar el tipo junto con el equipo y mostrar su nombre después de cerrar el EntityManager:

```sql
SELECT e FROM Equipo e
JOIN FETCH e.tipoEquipo
WHERE 1 = 1
ORDER BY e.idEquipo
```

Cuando corresponde, el DAO agrega estas condiciones antes de ordenar:

```sql
AND LOWER(e.nombre) LIKE LOWER(:nombre) ESCAPE '!'
AND e.tipoEquipo.idTipo = :idTipo
```

Los valores se asignan con `setParameter()`. Los caracteres `%`, `_` y `!` de la búsqueda se escapan para tratarlos como texto literal. El CRUD utiliza Hibernate y no concatena entradas del usuario en sentencias SQL.

## Validaciones y errores

Se rechazan códigos y nombres vacíos, textos que exceden las longitudes permitidas, cantidades no enteras o negativas, ausencia de tipo, estados no permitidos y códigos duplicados.

Además de la comprobación previa de duplicados, la restricción UNIQUE de PostgreSQL protege la integridad ante escrituras concurrentes. Los fallos se comunican mediante Alert. Los mensajes de éxito se muestran después de confirmar la escritura; si solamente falla la actualización del listado, se informa esa situación por separado.

## Verificación y evidencias

Se comprobó la compilación con JDK 25, la conexión a PostgreSQL, el inicio de Hibernate y la ejecución de las consultas iniciales de tipos y equipos. Esto no acredita todavía las pruebas manuales de escritura, filtros y reinicio.

La matriz de entrega debe registrar entrada, resultado esperado, resultado obtenido y evidencia real:

| ID | Prueba | Resultado esperado | Estado de evidencia |
| --- | --- | --- | --- |
| P1 | Registrar equipo válido | Guardado y visible | Pendiente |
| P2 | Intentar código duplicado | Rechazado, sin duplicados | Pendiente |
| P3 | Editar nombre, estado o tipo | Cambios persistidos | Pendiente |
| P4 | Eliminar con confirmación | Ausente en interfaz y PostgreSQL | Pendiente |
| P5 | Buscar por nombre parcial | Solo coincidencias | Pendiente |
| P6 | Filtrar por tipo y combinar búsqueda | Se cumplen ambos criterios | Pendiente |
| P7 | Ingresar cantidad negativa | Dato rechazado | Pendiente |
| P8 | Reiniciar aplicación | Persisten los datos confirmados | Pendiente |

También está pendiente documentar una escritura que falle por una restricción de PostgreSQL y comprobar la ausencia de cambios parciales después del rollback. Un duplicado rechazado antes de `persist()` por la validación de la aplicación no demuestra por sí solo ese caso.

## Problemas frecuentes

- **DB_PASSWORD ausente:** configurarla en el entorno de la ejecución que se está utilizando. No colocarla en Profiles de Maven.
- **No goals have been specified:** escribir `javafx:run` en el comando Maven.
- **Failed to delete target:** detener las ejecuciones activas y utilizar `javafx:run` sin `clean`. Si es necesario, cerrar el IDE y liberar la carpeta antes de limpiarla. OneDrive puede interferir con los archivos generados.
- **Error de conexión:** revisar servicio PostgreSQL, servidor, puerto, usuario y contraseña.
- **Error de validación del esquema:** comprobar base seleccionada, esquema `catalogos`, nombres y tipos de columnas.
- **module not found: jakarta.persistence:** recargar Maven y comprobar que las dependencias se hayan descargado.

## Entrega

Antes de entregar, incorporar el script SQL, completar las evidencias P1-P8 y la demostración de rollback, y publicar el repositorio accesible al docente sin credenciales locales.