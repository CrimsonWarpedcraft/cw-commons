package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConfigNodeTest {

  @Test
  void getNewConfigOption_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> ConfigNode.getNewConfigNode(null, String.class, "", v -> {})
    );
    assertThrows(
        NullPointerException.class,
        () -> ConfigNode.getNewConfigNode("", null, v -> {})
    );
    assertThrows(
        NullPointerException.class,
        () -> ConfigNode.getNewConfigNode("", String.class, "", null)
    );
  }

  @Test
  void getName_returns_name() {
    ConfigNode<String> node = ConfigNode.getNewConfigNode(
        "test1",
        String.class,
        "test2",
        v -> {}
    );
    assertEquals("test1", node.getName());
  }

  @Test
  void getValue_returns_default_value() {
    ConfigNode<String> node = ConfigNode.getNewConfigNode(
        "test1",
        String.class,
        "test2",
        v -> {}
    );
    assertEquals("test2", node.getValue());
  }

  @Test
  void setValue_sets_valid_value() {
    ConfigNode<String> node = ConfigNode.getNewConfigNode(
        "test1",
        String.class,
        "test2",
        v -> {}
    );

    assertDoesNotThrow(() -> node.setValue("test3"));
    assertEquals("test3", node.getValue());
  }

  @Test
  void setValue_sets_valid_subtype_value() {
    List<String> defaultList = new LinkedList<>();
    ConfigNode<List> node = ConfigNode.getNewConfigNode(
        "test1",
        List.class,
        defaultList,
        v -> {}
    );

    ArrayList<String> list = new ArrayList<>();
    assertDoesNotThrow(() -> node.setValue(list));
    assertSame(list, node.getValue());
  }

  @Test
  void setValue_allows_null_inputs() {
    ConfigNode<String> node = ConfigNode.getNewConfigNode(
        "test1",
        String.class,
        v -> {}
    );

    assertDoesNotThrow(() -> node.setValue(null));
  }

  @Test
  void setValue_throws_ConfigurationException_on_invalid_value() {
    ConfigNode<String> node = ConfigNode.getNewConfigNode(
        "test1",
        String.class,
        "test2",
        v -> {
          throw new ConfigurationException("");
        }
    );

    assertThrows(ConfigurationException.class, () -> node.setValue("test3"));
  }

  @Test
  void setValue_throws_ConfigurationException_on_wrong_type() {
    ConfigNode<String> node = ConfigNode.getNewConfigNode(
        "test1",
        String.class,
        "test2",
        v -> {}
    );

    assertThrows(ConfigurationException.class, () -> node.setValue(List.of()));
  }
}