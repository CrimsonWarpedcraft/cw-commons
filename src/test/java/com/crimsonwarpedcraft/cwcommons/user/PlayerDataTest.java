package com.crimsonwarpedcraft.cwcommons.user;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.crimsonwarpedcraft.cwcommons.mock.MockPlayerData;
import com.google.gson.Gson;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for PlayerData.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
class PlayerDataTest {
  private static File dataDir;

  @BeforeAll
  static void setUp() {
    dataDir = Paths
        .get(
            System.getProperty("java.io.tmpdir"),
            "CwCommons" + System.currentTimeMillis(),
            "user_data"
        )
        .toFile();

    assertTrue(dataDir.exists() || dataDir.mkdirs());
  }

  @Test
  void write() throws FileNotFoundException {
    MockPlayerData originalData = new MockPlayerData();
    originalData.setRandomVal(5);

    // Check NPE
    assertThrows(
        NullPointerException.class,
        () -> originalData.write(null)
    );

    // Make sure writing the data doesn't throw an error
    File dataFile = new File(dataDir, "player_data.json");
    assertDoesNotThrow(
        () -> originalData.write(
            dataFile
        )
    );

    // Make sure that we can load the data correctly
    Gson gson = new Gson();
    MockPlayerData loadedData = gson.fromJson(
        new BufferedReader(
            new InputStreamReader(
                new FileInputStream(dataFile),
                StandardCharsets.UTF_8
            )
        ),
        MockPlayerData.class
    );
    assertEquals(5, loadedData.getRandomVal());
  }
}