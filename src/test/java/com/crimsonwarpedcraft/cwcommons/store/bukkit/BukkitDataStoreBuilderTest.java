package com.crimsonwarpedcraft.cwcommons.store.bukkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crimsonwarpedcraft.cwcommons.store.DataStore;
import com.crimsonwarpedcraft.cwcommons.store.KeySerializers;
import com.crimsonwarpedcraft.cwcommons.store.Repository;
import com.crimsonwarpedcraft.cwcommons.store.SqliteBackend;
import java.io.File;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

class BukkitDataStoreBuilderTest {

  @Test
  void buildsWorkingLifecycleOwningStore() throws Exception {
    try (MockedConstruction<SqliteBackend> construction = mockConstruction(SqliteBackend.class,
        (mock, ctx) -> {
          when(mock.load(any(), any())).thenReturn(Optional.empty());
          when(mock.loadAll(any())).thenReturn(new HashMap<>());
        })) {
      try (DataStore store = new BukkitDataStoreBuilder("plugin", new File("ignored")).build()) {
        Repository<String, String> repo =
            store.repository("ns", String.class, KeySerializers.forString());
        repo.put("k", "v").get();
        assertEquals("v", repo.get("k").get().orElseThrow());
      }

      // The builder owns the backend it created: store.close() must close the SQLite backend.
      assertEquals(1, construction.constructed().size());
      verify(construction.constructed().get(0)).close();
    }
  }
}
