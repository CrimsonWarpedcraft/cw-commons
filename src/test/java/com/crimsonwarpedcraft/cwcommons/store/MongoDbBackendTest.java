package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.mongodb.ConnectionString;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class MongoDbBackendTest {

  private static final String URI = "mongodb://dummy-user:dummy-password@localhost:27017";

  @Test
  void nullArgumentsFailBeforeCreatingClient() {
    try (MockedStatic<MongoClients> clients = mockStatic(MongoClients.class)) {
      assertThrows(NullPointerException.class, () -> new MongoDbBackend(null, "db"));
      assertThrows(NullPointerException.class, () -> new MongoDbBackend(URI, null));
      clients.verifyNoInteractions();
    }
  }

  @Test
  void malformedConnectionStringIsRejected() {
    String uri = "mongodb://dummy-user:dummy-password@";
    IllegalArgumentException failure = assertThrows(
        IllegalArgumentException.class, () -> new ConnectionString(uri));
    try (MockedStatic<MongoClients> clients = mockStatic(MongoClients.class)) {
      clients.when(() -> MongoClients.create(uri)).thenThrow(failure);

      IOException thrown = assertThrows(IOException.class, () -> new MongoDbBackend(uri, "db"));

      assertEquals("Failed to initialize MongoDB backend", thrown.getMessage());
      assertSame(failure, thrown.getCause());
    }
  }

  @Test
  void clientCreationFailurePreservesCauseWithoutEchoingUri() {
    IllegalStateException failure = new IllegalStateException("Driver rejected " + URI);
    try (MockedStatic<MongoClients> clients = mockStatic(MongoClients.class)) {
      clients.when(() -> MongoClients.create(URI)).thenThrow(failure);

      IOException thrown = assertThrows(IOException.class, () -> new MongoDbBackend(URI, "db"));

      assertEquals("Failed to initialize MongoDB backend", thrown.getMessage());
      assertSame(failure, thrown.getCause());
      assertArrayEquals(new Throwable[0], failure.getSuppressed());
      clients.verify(() -> MongoClients.create(URI));
      clients.verifyNoMoreInteractions();
    }
  }

  @Test
  void databaseInitializationFailureClosesClientOnce() {
    MongoClient client = mock(MongoClient.class);
    IllegalArgumentException failure = new IllegalArgumentException("Invalid database name");
    when(client.getDatabase("db")).thenThrow(failure);
    try (MockedStatic<MongoClients> clients = mockStatic(MongoClients.class)) {
      clients.when(() -> MongoClients.create(URI)).thenReturn(client);

      IOException thrown = assertThrows(IOException.class, () -> new MongoDbBackend(URI, "db"));

      assertSame(failure, thrown.getCause());
      assertArrayEquals(new Throwable[0], failure.getSuppressed());
      verify(client).getDatabase("db");
      verify(client).close();
      verifyNoMoreInteractions(client);
    }
  }

  @Test
  void clientCleanupFailureIsSuppressedOnOriginalFailure() {
    MongoClient client = mock(MongoClient.class);
    IllegalArgumentException failure = new IllegalArgumentException("Invalid database name");
    when(client.getDatabase("db")).thenThrow(failure);
    IllegalStateException cleanupFailure = new IllegalStateException("Close failed");
    doThrow(cleanupFailure).when(client).close();
    try (MockedStatic<MongoClients> clients = mockStatic(MongoClients.class)) {
      clients.when(() -> MongoClients.create(URI)).thenReturn(client);

      IOException thrown = assertThrows(IOException.class, () -> new MongoDbBackend(URI, "db"));

      assertSame(failure, thrown.getCause());
      assertArrayEquals(new Throwable[] {cleanupFailure}, failure.getSuppressed());
      verify(client).close();
    }
  }

  @Test
  void successfulConstructionIsLazyAndRetainsClientUntilClose() throws IOException {
    MongoClient client = mock(MongoClient.class);
    MongoDatabase database = mock(MongoDatabase.class);
    when(client.getDatabase("db")).thenReturn(database);
    MongoTimeoutException failure = new MongoTimeoutException("Server unavailable");
    when(database.getCollection("ns")).thenThrow(failure);
    try (MockedStatic<MongoClients> clients = mockStatic(MongoClients.class)) {
      clients.when(() -> MongoClients.create(URI)).thenReturn(client);
      try (MongoDbBackend backend = new MongoDbBackend(URI, "db")) {
        verify(client).getDatabase("db");
        verifyNoMoreInteractions(client);
        verifyNoInteractions(database);

        IOException thrown = assertThrows(IOException.class, () -> backend.load("ns", "key"));
        assertSame(failure, thrown.getCause());
      }
      verify(client).close();
      verifyNoMoreInteractions(client);
    }
  }

  @Test
  void initializationErrorClosesClientWithoutWrappingError() {
    MongoClient client = mock(MongoClient.class);
    AssertionError failure = new AssertionError("Initialization failed");
    when(client.getDatabase("db")).thenThrow(failure);
    try (MockedStatic<MongoClients> clients = mockStatic(MongoClients.class)) {
      clients.when(() -> MongoClients.create(URI)).thenReturn(client);

      AssertionError thrown = assertThrows(
          AssertionError.class, () -> new MongoDbBackend(URI, "db"));

      assertSame(failure, thrown);
      verify(client).close();
    }
  }
}
