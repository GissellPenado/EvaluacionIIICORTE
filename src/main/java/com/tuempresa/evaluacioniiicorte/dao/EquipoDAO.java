package com.tuempresa.evaluacioniiicorte.dao;

import com.tuempresa.evaluacioniiicorte.database.JPAUtil;
import com.tuempresa.evaluacioniiicorte.model.Equipo;
import com.tuempresa.evaluacioniiicorte.model.TipoEquipo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.function.Consumer;

public class EquipoDAO {

    public static final List<String> ESTADOS = List.of(
            "Disponible",
            "En mantenimiento",
            "Fuera de servicio"
    );

    public List<Equipo> buscar(String nombre, Long idTipo) {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            String jpql = """
                    SELECT e FROM Equipo e
                    JOIN FETCH e.tipoEquipo
                    WHERE 1 = 1
                    """;

            boolean buscarNombre = nombre != null && !nombre.isBlank();

            if (buscarNombre) {
                jpql += " AND LOWER(e.nombre) LIKE LOWER(:nombre) ESCAPE '!'";
            }

            if (idTipo != null) {
                jpql += " AND e.tipoEquipo.idTipo = :idTipo";
            }

            jpql += " ORDER BY e.idEquipo";

            TypedQuery<Equipo> query = em.createQuery(jpql, Equipo.class);

            if (buscarNombre) {
                String fragmento = nombre.trim()
                        .replace("!", "!!")
                        .replace("%", "!%")
                        .replace("_", "!_");

                query.setParameter("nombre", "%" + fragmento + "%");
            }

            if (idTipo != null) {
                query.setParameter("idTipo", idTipo);
            }

            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public void guardar(Equipo datos) {
        validar(datos);

        ejecutarTransaccion(em -> {
            verificarCodigo(em, datos.getCodigo(), null);

            TipoEquipo tipo = buscarTipo(em, datos.getTipoEquipo().getIdTipo());

            Equipo nuevo = new Equipo();
            copiarDatos(datos, nuevo, tipo);

            em.persist(nuevo);
        });
    }

    public void actualizar(Long id, Equipo datos) {
        if (id == null) {
            throw new IllegalArgumentException("Selecciona un equipo.");
        }

        validar(datos);

        ejecutarTransaccion(em -> {
            Equipo existente = em.find(Equipo.class, id);

            if (existente == null) {
                throw new IllegalArgumentException(
                        "El equipo ya no existe. Actualiza el listado."
                );
            }

            verificarCodigo(em, datos.getCodigo(), id);

            TipoEquipo tipo = buscarTipo(em, datos.getTipoEquipo().getIdTipo());

            // La entidad está managed: Hibernate detecta los cambios.
            copiarDatos(datos, existente, tipo);
        });
    }

    public void eliminar(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Selecciona un equipo.");
        }

        ejecutarTransaccion(em -> {
            Equipo equipo = em.find(Equipo.class, id);

            if (equipo == null) {
                throw new IllegalArgumentException(
                        "El equipo ya no existe. Actualiza el listado."
                );
            }

            em.remove(equipo);
        });
    }

    private void ejecutarTransaccion(Consumer<EntityManager> operacion) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();
            operacion.accept(em);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                try {
                    tx.rollback();
                } catch (RuntimeException rollbackError) {
                    ex.addSuppressed(rollbackError);
                }
            }

            throw ex;
        } finally {
            em.close();
        }
    }

    private TipoEquipo buscarTipo(EntityManager em, Long idTipo) {
        TipoEquipo tipo = em.find(TipoEquipo.class, idTipo);

        if (tipo == null) {
            throw new IllegalArgumentException(
                    "El tipo seleccionado ya no existe."
            );
        }

        return tipo;
    }

    private void verificarCodigo(EntityManager em, String codigo, Long excluirId) {
        String jpql = "SELECT COUNT(e) FROM Equipo e WHERE e.codigo = :codigo";

        if (excluirId != null) {
            jpql += " AND e.idEquipo <> :id";
        }

        TypedQuery<Long> query = em.createQuery(jpql, Long.class);
        query.setParameter("codigo", codigo.trim());

        if (excluirId != null) {
            query.setParameter("id", excluirId);
        }

        if (query.getSingleResult() > 0) {
            throw new IllegalArgumentException(
                    "Ya existe un equipo con el código " + codigo.trim() + "."
            );
        }
    }

    private void validar(Equipo equipo) {
        if (equipo == null) {
            throw new IllegalArgumentException("Faltan los datos del equipo.");
        }

        validarTexto(equipo.getCodigo(), "El código", 50, true);
        validarTexto(equipo.getNombre(), "El nombre", 150, true);
        validarTexto(equipo.getMarca(), "La marca", 100, false);

        if (equipo.getCantidad() < 0) {
            throw new IllegalArgumentException(
                    "La cantidad no puede ser negativa."
            );
        }

        if (equipo.getEstado() == null
                || !ESTADOS.contains(equipo.getEstado())) {
            throw new IllegalArgumentException(
                    "Selecciona un estado permitido."
            );
        }

        if (equipo.getTipoEquipo() == null
                || equipo.getTipoEquipo().getIdTipo() == null) {
            throw new IllegalArgumentException(
                    "Selecciona un tipo de equipo."
            );
        }
    }

    private void validarTexto(
            String texto, String campo, int maximo, boolean obligatorio
    ) {
        if (obligatorio && (texto == null || texto.isBlank())) {
            throw new IllegalArgumentException(campo + " es obligatorio.");
        }

        if (texto != null && texto.trim().length() > maximo) {
            throw new IllegalArgumentException(
                    campo + " admite hasta " + maximo + " caracteres."
            );
        }
    }

    private void copiarDatos(
            Equipo origen, Equipo destino, TipoEquipo tipo
    ) {
        destino.setCodigo(origen.getCodigo().trim());
        destino.setNombre(origen.getNombre().trim());

        String marca = origen.getMarca();
        destino.setMarca(
                marca == null || marca.isBlank() ? null : marca.trim()
        );

        destino.setEstado(origen.getEstado());
        destino.setCantidad(origen.getCantidad());
        destino.setTipoEquipo(tipo);
    }
}