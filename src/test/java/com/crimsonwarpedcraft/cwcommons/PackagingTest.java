package com.crimsonwarpedcraft.cwcommons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathNodes;
import org.junit.jupiter.api.Test;

class PackagingTest {

  private static final String JDBC_SERVICE = "META-INF/services/java.sql.Driver";

  @Test
  void jarBundlesOnlyCommonsAndSqlite() throws IOException {
    try (var jar = new ZipFile(System.getProperty("packaging.jar"))) {
      var unexpected = jar.stream().filter(entry -> !entry.isDirectory()).map(ZipEntry::getName)
          .filter(PackagingTest::unexpectedEntry).sorted().toList();
      assertTrue(unexpected.isEmpty(), () -> "Unexpected bundled entries: " + unexpected);

      for (String name : List.of("com/crimsonwarpedcraft/cwcommons/config/ConfigManager.class",
          "com/crimsonwarpedcraft/cwcommons/store/Repository.class", "org/sqlite/JDBC.class",
          "org/sqlite/native/Linux/x86_64/libsqlitejdbc.so",
          "org/sqlite/native/Windows/x86_64/sqlitejdbc.dll",
          "org/sqlite/native/Mac/x86_64/libsqlitejdbc.dylib")) {
        ZipEntry entry = jar.getEntry(name);
        assertNotNull(entry, name);
        assertTrue(entry.getSize() > 0, name);
      }
      assertEquals(1, jar.stream().filter(e -> JDBC_SERVICE.equals(e.getName())).count(),
          JDBC_SERVICE);
      try (var stream = jar.getInputStream(jar.getEntry(JDBC_SERVICE))) {
        var providers = new String(stream.readAllBytes(), StandardCharsets.UTF_8).lines()
            .map(line -> line.split("#", 2)[0].strip()).filter(line -> !line.isEmpty()).toList();
        assertEquals(List.of("org.sqlite.JDBC"), providers, JDBC_SERVICE);
      }
    }
  }

  private static boolean unexpectedEntry(String name) {
    String path = name.replaceFirst("^META-INF/versions/[0-9]+/", "");
    if (path.endsWith(".class")) {
      return !path.startsWith("com/crimsonwarpedcraft/cwcommons/")
          && !path.startsWith("org/sqlite/");
    }
    return path.startsWith("META-INF/services/") && !name.equals(JDBC_SERVICE);
  }

  @Test
  void pomDeclaresThePublicDependencies() throws Exception {
    var factory = DocumentBuilderFactory.newDefaultInstance();
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    var pom = factory.newDocumentBuilder().parse(new File(System.getProperty("packaging.pom")));
    var xpath = XPathFactory.newDefaultInstance().newXPath();
    var actual = new HashMap<String, String>();
    for (var dep : xpath.evaluateExpression("/project/dependencies/dependency", pom,
        XPathNodes.class)) {
      String module = xpath.evaluate("groupId", dep) + ":" + xpath.evaluate("artifactId", dep);
      assertEquals("compile", xpath.evaluate("scope", dep), module + " scope");
      assertTrue(List.of("", "false").contains(xpath.evaluate("optional", dep)),
          module + " must be a required dependency");
      assertNull(actual.put(module, xpath.evaluate("version", dep)),
          "Duplicate dependency: " + module);
    }
    assertEquals(Map.of(
        "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml",
        System.getProperty("packaging.jacksonVersion"),
        "org.hibernate.validator:hibernate-validator",
        System.getProperty("packaging.validatorVersion")), actual);
  }
}
