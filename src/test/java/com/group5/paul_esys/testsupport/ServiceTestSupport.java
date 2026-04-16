package com.group5.paul_esys.testsupport;

import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public abstract class ServiceTestSupport {

  @FunctionalInterface
  protected interface SqlRunnable {
    void run() throws Exception;
  }

  protected record JdbcMock(Connection connection, PreparedStatement statement, ResultSet resultSet) {
  }

  protected void withConnection(Connection connection, SqlRunnable runnable) throws Exception {
    try (MockedStatic<ConnectionService> mockedConnectionService = mockStatic(ConnectionService.class)) {
      mockedConnectionService.when(ConnectionService::getConnection).thenReturn(connection);
      runnable.run();
    }
  }

  protected JdbcMock mockEmptyQueryConnection() throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(statement.executeUpdate()).thenReturn(0);
    when(resultSet.next()).thenReturn(false);

    return new JdbcMock(connection, statement, resultSet);
  }

  protected JdbcMock mockUpdateConnection(int affectedRows) throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    when(statement.executeUpdate()).thenReturn(affectedRows);
    when(resultSet.next()).thenReturn(false);

    return new JdbcMock(connection, statement, resultSet);
  }

  protected Connection mockEmptyQueryConnectionOnly() throws SQLException {
    return mockEmptyQueryConnection().connection();
  }

  protected Connection mockUpdateConnectionOnly(int affectedRows) throws SQLException {
    return mockUpdateConnection(affectedRows).connection();
  }
}