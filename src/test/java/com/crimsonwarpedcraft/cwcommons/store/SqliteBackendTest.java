package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

@SuppressFBWarnings(
    value = {"ODR_OPEN_DATABASE_RESOURCE", "OBL_UNSATISFIED_OBLIGATION", "SQL_INJECTION_JDBC"},
    justification = "JDBC calls only stub or verify mocks; no real resources or SQL are used.")
class SqliteBackendTest {

  private static final String URL = "jdbc:sqlite:mock-database";

  private Connection connection;
  private Statement statement;

  @BeforeEach
  void setUp() throws SQLException {
    connection = mock(Connection.class);
    statement = mock(Statement.class);
    when(connection.createStatement()).thenReturn(statement);
  }

  @Test
  void nullArgumentsFailBeforeOpeningConnection() {
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      assertThrows(NullPointerException.class, () -> new SqliteBackend((File) null));
      assertThrows(NullPointerException.class, () -> new SqliteBackend((String) null));
      driver.verifyNoInteractions();
      verifyNoInteractions(connection, statement);
    }
  }

  @Test
  void connectionAcquisitionFailurePreservesCause() {
    SQLException failure = new SQLException("Connection failed");
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      driver.when(() -> DriverManager.getConnection(URL)).thenThrow(failure);

      IOException thrown = assertThrows(IOException.class, () -> new SqliteBackend(URL));

      assertSame(failure, thrown.getCause());
      assertArrayEquals(new Throwable[0], failure.getSuppressed());
      verifyNoInteractions(connection, statement);
    }
  }

  @Test
  void statementCreationFailureClosesConnectionOnce() throws SQLException {
    SQLException failure = new SQLException("Statement creation failed");
    when(connection.createStatement()).thenThrow(failure);
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      driver.when(() -> DriverManager.getConnection(URL)).thenReturn(connection);

      IOException thrown = assertThrows(IOException.class, () -> new SqliteBackend(URL));

      assertSame(failure, thrown.getCause());
      verify(connection).close();
      verifyNoInteractions(statement);
    }
  }

  @Test
  void schemaCreationFailureClosesStatementThenConnection() throws SQLException {
    SQLException failure = new SQLException("Schema creation failed");
    when(statement.execute(anyString())).thenThrow(failure);
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      driver.when(() -> DriverManager.getConnection(URL)).thenReturn(connection);

      IOException thrown = assertThrows(IOException.class, () -> new SqliteBackend(URL));

      assertSame(failure, thrown.getCause());
      InOrder cleanup = inOrder(statement, connection);
      cleanup.verify(statement).close();
      cleanup.verify(connection).close();
    }
  }

  @Test
  void statementCloseFailureAlsoClosesConnection() throws SQLException {
    SQLException failure = new SQLException("Statement close failed");
    doThrow(failure).when(statement).close();
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      driver.when(() -> DriverManager.getConnection(URL)).thenReturn(connection);

      IOException thrown = assertThrows(IOException.class, () -> new SqliteBackend(URL));

      assertSame(failure, thrown.getCause());
      verify(statement).close();
      verify(connection).close();
    }
  }

  @Test
  void cleanupFailuresAreSuppressedOnOriginalSchemaFailure() throws SQLException {
    SQLException failure = new SQLException("Schema creation failed");
    when(statement.execute(anyString())).thenThrow(failure);
    SQLException statementFailure = new SQLException("Statement close failed");
    doThrow(statementFailure).when(statement).close();
    SQLException connectionFailure = new SQLException("Connection close failed");
    doThrow(connectionFailure).when(connection).close();
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      driver.when(() -> DriverManager.getConnection(URL)).thenReturn(connection);

      IOException thrown = assertThrows(IOException.class, () -> new SqliteBackend(URL));

      assertSame(failure, thrown.getCause());
      assertArrayEquals(
          new Throwable[] {statementFailure, connectionFailure}, failure.getSuppressed());
      verify(statement).close();
      verify(connection).close();
    }
  }

  @Test
  void uncheckedSchemaFailureStillClosesConnection() throws SQLException {
    IllegalStateException failure = new IllegalStateException("Driver failure");
    when(statement.execute(anyString())).thenThrow(failure);
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      driver.when(() -> DriverManager.getConnection(URL)).thenReturn(connection);

      IllegalStateException thrown = assertThrows(
          IllegalStateException.class, () -> new SqliteBackend(URL));

      assertSame(failure, thrown);
      verify(statement).close();
      verify(connection).close();
    }
  }

  @Test
  void successfulConstructionRetainsConnectionUntilClose() throws IOException, SQLException {
    try (MockedStatic<DriverManager> driver = mockStatic(DriverManager.class)) {
      driver.when(() -> DriverManager.getConnection(URL)).thenReturn(connection);
      try (SqliteBackend backend = new SqliteBackend(URL)) {
        verify(statement).execute(contains("CREATE TABLE IF NOT EXISTS data"));
        verify(statement).close();
        verify(connection, never()).close();
      }
      driver.verify(() -> DriverManager.getConnection(URL));
      verify(connection).close();
    }
  }
}
