package com.crimsonwarpedcraft.cwcommons.user;

import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Data about players.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public abstract class PlayerData {
  /** Returns a copy of ths PlayerData object. */
  public abstract PlayerData copy();

  /** Writes the player's data to the file. */
  public PlayerData write(File dataFile) throws IOException {
    try (
        Writer writer = new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(
                    Objects.requireNonNull(dataFile)
                ),
                StandardCharsets.UTF_8
            )
        )
    ) {
      Gson gson = new Gson();
      gson.toJson(
          this,
          writer
      );
    }

    return this;
  }
}
