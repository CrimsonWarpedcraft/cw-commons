package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings("unchecked")
class MongoDbBackendTest {

  private MongoClient mockClient;
  private MongoCollection<Document> mockCollection;
  private MongoDbBackend backend;

  @BeforeEach
  void setUp() throws IOException {
    mockClient = mock(MongoClient.class);
    MongoDatabase mockDatabase = mock(MongoDatabase.class);
    mockCollection = mock(MongoCollection.class);
    when(mockClient.getDatabase(any())).thenReturn(mockDatabase);
    when(mockDatabase.getCollection(any())).thenReturn(mockCollection);
    try (MockedStatic<MongoClients> mc = mockStatic(MongoClients.class)) {
      mc.when(() -> MongoClients.create(any(String.class))).thenReturn(mockClient);
      backend = new MongoDbBackend("mongodb://test", "db");
    }
  }

  @Test
  void nullUriThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new MongoDbBackend(null, "db"));
  }

  @Test
  void nullDatabaseThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new MongoDbBackend("uri", null));
  }

  @Test
  void constructorThrowsOnConnectionFailure() {
    assertThrows(IOException.class, () -> new MongoDbBackend("notavaliduri", "db"));
  }

  @Test
  void saveCallsReplaceOneWithCorrectDocument() throws IOException {
    backend.save("ns", "k", "v");
    verify(mockCollection).replaceOne(
        any(Bson.class),
        argThat(d -> "v".equals(d.getString("value")) && "k".equals(d.getString("_id"))),
        any()
    );
  }

  @Test
  void loadReturnsValueWhenDocumentFound() throws IOException {
    FindIterable<Document> mockIterable = mock(FindIterable.class);
    when(mockCollection.find(any(Bson.class))).thenReturn(mockIterable);
    when(mockIterable.first()).thenReturn(new Document("_id", "k").append("value", "v"));

    Optional<String> result = backend.load("ns", "k");

    assertTrue(result.isPresent());
    assertEquals("v", result.get());
  }

  @Test
  void loadReturnsEmptyWhenDocumentNotFound() throws IOException {
    FindIterable<Document> mockIterable = mock(FindIterable.class);
    when(mockCollection.find(any(Bson.class))).thenReturn(mockIterable);
    when(mockIterable.first()).thenReturn(null);

    Optional<String> result = backend.load("ns", "k");

    assertFalse(result.isPresent());
  }

  @Test
  void loadAllReturnsAllDocuments() throws IOException {
    Document doc = new Document("_id", "k").append("value", "v");
    MongoCursor<Document> mockCursor = mock(MongoCursor.class);
    when(mockCursor.hasNext()).thenReturn(true, false);
    when(mockCursor.next()).thenReturn(doc);
    FindIterable<Document> mockIterable = mock(FindIterable.class);
    when(mockCollection.find()).thenReturn(mockIterable);
    when(mockIterable.iterator()).thenReturn(mockCursor);

    Map<String, String> result = backend.loadAll("ns");

    assertEquals(1, result.size());
    assertEquals("v", result.get("k"));
  }

  @Test
  void deleteCallsDeleteOne() throws IOException {
    backend.delete("ns", "k");
    verify(mockCollection).deleteOne(any(Bson.class));
  }

  @Test
  void closeCallsClientClose() throws IOException {
    backend.close();
    verify(mockClient).close();
  }
}
