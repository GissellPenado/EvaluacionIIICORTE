package com.tuempresa.evaluacioniiicorte.database;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public final class JPAUtil {

    private static EntityManagerFactory entityManagerFactory;

    private JPAUtil() {
    }

    public static synchronized void iniciar() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            return;
        }

        String password = System.getenv("DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Configura DB_PASSWORD en las variables de entorno "
                            + "de la ejecución de IntelliJ."
            );
        }

        // Los proxies se generan dentro del módulo de nuestras entidades.
        // Deben poder acceder al módulo donde Maven carga Hibernate.
        try {
            Module moduloHibernate = Class.forName(
                    "org.hibernate.proxy.HibernateProxy"
            ).getModule();
            JPAUtil.class.getModule().addReads(moduloHibernate);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "No se encontró Hibernate. Recarga las dependencias Maven.", ex
            );
        }

        Map<String, Object> propiedades = new HashMap<>();

        propiedades.put(
                "jakarta.persistence.jdbc.password",
                password
        );

        String usuario = System.getenv("DB_USER");
        if (usuario != null && !usuario.isBlank()) {
            propiedades.put("jakarta.persistence.jdbc.user", usuario);
        }

        String url = System.getenv("DB_URL");
        if (url != null && !url.isBlank()) {
            propiedades.put("jakarta.persistence.jdbc.url", url);
        }

        entityManagerFactory = Persistence.createEntityManagerFactory(
                "laboratorioPU",
                propiedades
        );
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        iniciar();
        return entityManagerFactory;
    }

    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    public static synchronized void cerrar() {
        if (entityManagerFactory != null) {
            if (entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
            }
            entityManagerFactory = null;
        }
    }
}