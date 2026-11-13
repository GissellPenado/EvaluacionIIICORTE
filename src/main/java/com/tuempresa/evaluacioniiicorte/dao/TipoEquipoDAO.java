package com.tuempresa.evaluacioniiicorte.dao;

import com.tuempresa.evaluacioniiicorte.database.JPAUtil;
import com.tuempresa.evaluacioniiicorte.model.TipoEquipo;
import jakarta.persistence.EntityManager;

import java.util.List;

public class TipoEquipoDAO {

    public List<TipoEquipo> listar() {
        EntityManager em = JPAUtil.getEntityManager();

        try {
            return em.createQuery(
                    "SELECT t FROM TipoEquipo t ORDER BY t.nombre",
                    TipoEquipo.class
            ).getResultList();
        } finally {
            em.close();
        }
    }
}