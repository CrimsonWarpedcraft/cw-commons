package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteDataStoreIntegrationTest extends AbstractDataStoreIntegrationTest {

  @TempDir
  private Path tempDir;

  @Override
  protected StorageBackend openBackend() throws IOException {
    return new SqliteBackend(tempDir.resolve("store.db").toFile());
  }

  @Test
  void createsMissingParentDirectories() throws IOException {
    File databaseFile = tempDir.resolve("missing").resolve("nested").resolve("store.db").toFile();
    try (SqliteBackend backend = new SqliteBackend(databaseFile)) {
      backend.save("namespace", "key", "value");
      assertEquals("value", backend.load("namespace", "key").orElseThrow());
    }
  }
}
