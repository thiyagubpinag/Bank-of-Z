package com.ibm.cics.botz.crecust.service.adapters;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Adapter for database operations.
 */
public class DBAdapter {

    private static final DBAdapter INSTANCE = new DBAdapter();

    public DBAdapter() {
    }

    public static DBAdapter getInstance() {
        return INSTANCE;
    }

    public DataSource getDataSource(String jndiName) throws NamingException {
        return (DataSource) new InitialContext().lookup(jndiName);
    }

    public Connection getConnection(String jndiName) throws NamingException, SQLException {
        DataSource ds = getDataSource(jndiName);
        return ds.getConnection();
    }

    public Connection getConnection(DataSource dataSource) throws SQLException {
        return dataSource.getConnection();
    }

    public PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    public ResultSet executeQuery(PreparedStatement ps) throws SQLException {
        return ps.executeQuery();
    }

    public boolean next(ResultSet rs) throws SQLException {
        return rs.next();
    }

    public int executeUpdate(PreparedStatement ps) throws SQLException {
        return ps.executeUpdate();
    }

    public void close(Connection conn) throws SQLException {
        if (conn != null) {
            conn.close();
        }
    }

    public void close(Statement stmt) throws SQLException {
        if (stmt != null) {
            stmt.close();
        }
    }

    public void close(ResultSet rs) throws SQLException {
        if (rs != null) {
            rs.close();
        }
    }

    public void close(AutoCloseable resource) throws Exception {
        if (resource != null) {
            resource.close();
        }
    }
}
