package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class MongoDbBackendTest {

  @Test
  void nullUriThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new MongoDbBackend(null, "db"));
  }

  @Test
  void nullDatabaseThrowsNpe() {
    assertThrows(NullPointerException.class, () -> new MongoDbBackend("uri", null));
  }

  @Test
  void malformedConnectionStringIsRejected() {
    assertThrows(IOException.class, () -> new MongoDbBackend("notavaliduri", "db"));
  }
}
