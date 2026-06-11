package com.crimsonwarpedcraft.cwcommons.store;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link StorageBackend} backed by a local SQLite database file.
 *
 * <p>Usage:
 * <pre>{@code
 * DataStore store = CachedDataStore.builder("MyPlugin", new SqliteBackend(dbFile)).build();
 * }</pre>
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public final class SqliteBackend implements StorageBackend {

  private final Connection conn;

  /**
   * Creates a {@link SqliteBackend} backed by the given database file.
   *
   * <p>Parent directories are created if they do not already exist.
   *
   * @param dbFile the SQLite database file to open or create
   * @throws IOException if the database cannot be opened or parent directories cannot be created
   */
  public SqliteBackend(File dbFile) throws IOException {
    Objects.requireNonNull(dbFile);
    File parent = dbFile.getParentFile();
    if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
      throw new IOException("Failed to create parent directories for " + dbFile);
    }
    this.conn = openConnection("jdbc:sqlite:" + dbFile.getAbsolutePath(), dbFile.toString());
  }

  SqliteBackend(String url) throws IOException {
    this.conn = openConnection(url, url);
  }

  private static Connection openConnection(String url, String label) throws IOException {
    try {
      Connection c = DriverManager.getConnection(url);
      try (Statement stmt = c.createStatement()) {
        stmt.execute(
            """
            CREATE TABLE IF NOT EXISTS data (
                namespace TEXT NOT NULL,
                key       TEXT NOT NULL,
                value     TEXT NOT NULL,
                PRIMARY KEY (namespace, key)
            )
            """);
      }
      return c;
    } catch (SQLException e) {
      throw new IOException("Failed to initialize SQLite database at " + label, e);
    }
  }

  @Override
  public void save(String namespace, String key, String json) throws IOException {
    try (PreparedStatement stmt = conn.prepareStatement(
        "INSERT OR REPLACE INTO data (namespace, key, value) VALUES (?, ?, ?)")) {
      stmt.setString(1, namespace);
      stmt.setString(2, key);
      stmt.setString(3, json);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Optional<String> load(String namespace, String key) throws IOException {
    try (PreparedStatement stmt = conn.prepareStatement(
        "SELECT value FROM data WHERE namespace = ? AND key = ?")) {
      stmt.setString(1, namespace);
      stmt.setString(2, key);
      try (ResultSet rs = stmt.executeQuery()) {
        if (rs.next()) {
          return Optional.of(rs.getString("value"));
        }
        return Optional.empty();
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Map<String, String> loadAll(String namespace) throws IOException {
    try (PreparedStatement stmt = conn.prepareStatement(
        "SELECT key, value FROM data WHERE namespace = ?")) {
      stmt.setString(1, namespace);
      try (ResultSet rs = stmt.executeQuery()) {
        Map<String, String> result = new HashMap<>();
        while (rs.next()) {
          result.put(rs.getString("key"), rs.getString("value"));
        }
        return result;
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void delete(String namespace, String key) throws IOException {
    try (PreparedStatement stmt = conn.prepareStatement(
        "DELETE FROM data WHERE namespace = ? AND key = ?")) {
      stmt.setString(1, namespace);
      stmt.setString(2, key);
      stmt.executeUpdate();
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      conn.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }
}
