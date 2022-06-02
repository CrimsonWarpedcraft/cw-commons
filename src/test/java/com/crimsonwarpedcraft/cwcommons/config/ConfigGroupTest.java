package com.crimsonwarpedcraft.cwcommons.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.junit.jupiter.api.Test;

@SuppressFBWarnings(
    value = "RV_RETURN_VALUE_IGNORED_INFERRED",
    justification = "Intentional for these tests"
)
class ConfigGroupTest {

  @Test
  void getNewConfigGroup_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> ConfigGroup.getNewConfigGroup(null)
    );
  }


  @Test
  void addConfigNode_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> ConfigGroup.getNewConfigGroup("test1").addConfigNode(null)
    );
  }

  @Test
  void addConfigNode_returns_new_instance_containing_node() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));


    assertDoesNotThrow(() -> group.getConfigNode("test2"));
    assertEquals("test2", group.getConfigNode("test2").getName());
  }

  @Test
  void addConfigNode_returns_new_instance_with_same_name() {
    ConfigGroup group1 = ConfigGroup.getNewConfigGroup("test1");

    ConfigGroup group2 = group1.addConfigNode(
        ConfigNode.getNewConfigNode("test2", String.class, v -> {})
    );

    assertEquals("test1", group2.getName());
  }

  @Test
  void addConfigNode_returns_new_instance_with_existing_groups() {
    ConfigGroup group1 = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));

    ConfigGroup group2 = group1.addConfigNode(
        ConfigNode.getNewConfigNode("test3", String.class, v -> {})
    );

    assertDoesNotThrow(() -> group2.getConfigGroup("test2").getName());
    assertEquals("test2", group2.getConfigGroup("test2").getName());
  }

  @Test
  void addConfigNode_returns_new_instance_with_existing_nodes() {
    ConfigGroup group1 = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));

    ConfigGroup group2 = group1.addConfigNode(
        ConfigNode.getNewConfigNode("test3", String.class, v -> {})
    );

    assertDoesNotThrow(() -> group2.getConfigNode("test2").getName());
    assertEquals("test2", group2.getConfigNode("test2").getName());
  }

  @Test
  void addConfigNode_does_not_modify_current_group() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1");

    group.addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));

    assertThrows(
        ConfigKeyDoesNotExistException.class,
        () -> group.getConfigNode("test2")
    );
  }

  @Test
  void addConfigNode_replaces_config_group_with_same_name() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"))
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));

    assertDoesNotThrow(() -> group.getConfigNode("test2"));
    assertThrows(
        ConfigKeyDoesNotExistException.class,
        () -> group.getConfigGroup("test2")
    );
  }

  @Test
  void getConfigNode_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> ConfigGroup.getNewConfigGroup("test1").getConfigNode(null)
    );
  }

  @Test
  void getConfigNode_returns_existing_node() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, "test3", v -> {}));

    ConfigNode<?> node = group.getConfigNode("test2");

    assertEquals("test2", node.getName());
    assertEquals("test3", node.getValue());
  }

  @Test
  void getConfigNode_throws_ConfigKeyDoesNotExistException_with_missing_node() {
    assertThrows(
        ConfigKeyDoesNotExistException.class,
        () -> ConfigGroup.getNewConfigGroup("test1").getConfigNode("test2")
    );
  }

  @Test
  void addConfigGroup_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> ConfigGroup.getNewConfigGroup("test1").addConfigGroup(null)
    );
  }

  @Test
  void addConfigGroup_returns_new_instance_containing_group() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));


    assertDoesNotThrow(() -> group.getConfigGroup("test2"));
    assertEquals("test2", group.getConfigGroup("test2").getName());
  }

  @Test
  void addConfigGroup_returns_new_instance_with_same_name() {
    ConfigGroup group1 = ConfigGroup.getNewConfigGroup("test1");

    ConfigGroup group2 = group1.addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));

    assertEquals("test1", group2.getName());
  }

  @Test
  void addConfigGroup_returns_new_instance_with_existing_groups() {
    ConfigGroup group1 = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));

    ConfigGroup group2 = group1.addConfigGroup(ConfigGroup.getNewConfigGroup("test3"));

    assertDoesNotThrow(() -> group2.getConfigGroup("test2").getName());
    assertEquals("test2", group2.getConfigGroup("test2").getName());
  }

  @Test
  void addConfigGroup_returns_new_instance_with_existing_nodes() {
    ConfigGroup group1 = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));

    ConfigGroup group2 = group1.addConfigGroup(ConfigGroup.getNewConfigGroup("test3"));

    assertDoesNotThrow(() -> group2.getConfigNode("test2").getName());
    assertEquals("test2", group2.getConfigNode("test2").getName());
  }

  @Test
  void addConfigGroup_does_not_modify_current_group() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1");

    group.addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));

    assertThrows(
        ConfigKeyDoesNotExistException.class,
        () -> group.getConfigGroup("test2")
    );
  }

  @Test
  void addConfigGroup_replaces_config_node_with_same_name() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}))
        .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));

    assertDoesNotThrow(() -> group.getConfigGroup("test2"));
    assertThrows(
        ConfigKeyDoesNotExistException.class,
        () -> group.getConfigNode("test2")
    );
  }

  @Test
  void getConfigGroup_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> ConfigGroup.getNewConfigGroup("test1").getConfigGroup(null)
    );
  }

  @Test
  void getConfigGroup_returns_existing_group() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));

    assertEquals("test2", group.getConfigGroup("test2").getName());
  }

  @Test
  void getConfigGroup_throws_ConfigKeyDoesNotExistException_with_missing_node() {
    assertThrows(
        ConfigKeyDoesNotExistException.class,
        () -> ConfigGroup.getNewConfigGroup("test1").getConfigGroup("test2")
    );
  }

  @Test
  void setValue_throws_NullPointerException_with_null_inputs() {
    assertThrows(
        NullPointerException.class,
        () -> ConfigGroup.getNewConfigGroup("test1").setValue(null)
    );
  }

  @Test
  void setValue_sets_node_value() throws ConfigurationException {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));
    Map<String, Object> map = Map.of("test2", "test3");

    group = group.setValue(map);
    assertEquals("test3", group.getConfigNode("test2").getValue());
  }

  @Test
  void setValue_sets_subgroup_node_value() throws ConfigurationException {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(
            ConfigGroup.getNewConfigGroup("test2")
                .addConfigNode(
                    ConfigNode.getNewConfigNode("test3", String.class, v -> {})
                )
        );
    Map<String, Object> map = Map.of("test2", Map.of("test3", "test4"));

    group = group.setValue(map);
    assertEquals(
        "test4",
        group.getConfigGroup("test2").getConfigNode("test3").getValue()
    );
  }

  @Test
  void setValue_allows_extraneous_data() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));
    Map<String, Object> map = Map.of(
        "test2", "test3",
        "test4", "test5"
    );

    assertDoesNotThrow(() -> group.setValue(map));
  }

  @Test
  void setValue_throws_ConfigurationException_on_group_type_mismatch() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"));
    Map<String, Object> map = Map.of("test2", "test3");

    assertThrows(
        ConfigurationException.class,
        () -> group.setValue(map)
    );
  }

  @Test
  void setValue_throws_ConfigurationException_on_node_type_mismatch() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(ConfigNode.getNewConfigNode("test2", String.class, v -> {}));

    Map<String, Object> map = Map.of("test2", 1);

    assertThrows(
        ConfigurationException.class,
        () -> group.setValue(map)
    );
  }

  @Test
  void setValue_throws_ConfigurationException_on_validation_exception() {
    ConfigGroup group = ConfigGroup.getNewConfigGroup("test1")
        .addConfigNode(
            ConfigNode.getNewConfigNode(
                "test2",
                String.class,
                v -> {
                  throw new ConfigurationException("");
                }
            )
        );

    assertThrows(
        ConfigurationException.class,
        () -> group.setValue(Map.of("test2", Map.of()))
    );
  }

  @Test
  void asMap_returns_empty_map_if_no_contents() {
    assertEquals(Map.of(), ConfigGroup.getNewConfigGroup("test1").asMap());
  }

  @Test
  void asMap_returns_map_with_node_value() {
    assertEquals(
        Map.of("test2", "test3"),
        ConfigGroup.getNewConfigGroup("test1")
            .addConfigNode(
                ConfigNode.getNewConfigNode("test2", String.class, "test3", v -> {})
            )
            .asMap()
    );
  }

  @Test
  void asMap_returns_map_with_empty_group() {
    assertEquals(
        Map.of("test2", Map.of()),
        ConfigGroup.getNewConfigGroup("test1")
            .addConfigGroup(ConfigGroup.getNewConfigGroup("test2"))
            .asMap()
    );
  }

  @Test
  void asMap_returns_map_with_subgroup_node_value() {
    assertEquals(
        Map.of("test2", Map.of("test2", "test3")),
        ConfigGroup.getNewConfigGroup("test1")
            .addConfigGroup(
                ConfigGroup.getNewConfigGroup("test2")
                    .addConfigNode(
                        ConfigNode.getNewConfigNode("test2", String.class, "test3", v -> {})
                    )
            )
            .asMap()
    );
  }

  @Test
  void getName_returns_name_of_group() {
    assertEquals("test1", ConfigGroup.getNewConfigGroup("test1").getName());
  }
}