package com.crimsonwarpedcraft.cwcommons.store;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;

class MongoDataStoreIntegrationTest extends AbstractDataStoreIntegrationTest {

  private static String mongoUri;
  private static String databaseName;

  @BeforeAll
  static void readConnectionSettings() {
    mongoUri = requiredEnvironmentVariable("CW_COMMONS_MONGO_URI");
    databaseName = requiredEnvironmentVariable("CW_COMMONS_MONGO_DATABASE");
  }

  @Override
  protected StorageBackend openBackend() throws IOException {
    return new MongoDbBackend(mongoUri, databaseName);
  }

  @Override
  protected void cleanUp(String createdNamespace) {
    try (MongoClient client = MongoClients.create(mongoUri)) {
      client.getDatabase(databaseName).getCollection(createdNamespace).drop();
    }
  }

  private static String requiredEnvironmentVariable(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " must be set before running integration tests");
    }
    return value;
  }
}
