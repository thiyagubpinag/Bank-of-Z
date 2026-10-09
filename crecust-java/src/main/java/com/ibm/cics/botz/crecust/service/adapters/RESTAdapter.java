package com.ibm.cics.botz.crecust.service.adapters;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Adapter for REST operations.
 */
public class RESTAdapter {

    private static final RESTAdapter INSTANCE = new RESTAdapter();

    public RESTAdapter() {
    }

    public static RESTAdapter getInstance() {
        return INSTANCE;
    }

    public DataSource getDataSource(String jndiName) throws NamingException {
        return (DataSource) new InitialContext().lookup(jndiName);
    }
}
