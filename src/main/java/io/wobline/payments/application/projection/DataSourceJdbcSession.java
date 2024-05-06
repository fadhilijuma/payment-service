package io.wobline.payments.application.projection;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.apache.pekko.japi.function.Function;
import org.apache.pekko.projection.jdbc.JdbcSession;

public class DataSourceJdbcSession implements JdbcSession {

    private final Connection connection;

    public DataSourceJdbcSession(DataSource dataSource) {
        try {
            this.connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            throw new RuntimeException(exception);
        }
    }

    @Override
    public <R> R withConnection(Function<Connection, R> func) throws Exception {
        return func.apply(connection);
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public void rollback() throws SQLException {
        connection.rollback();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
