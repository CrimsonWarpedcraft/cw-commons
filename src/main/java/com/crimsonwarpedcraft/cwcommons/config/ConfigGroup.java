package com.crimsonwarpedcraft.cwcommons.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

/**
 * A group of config groups and node for a config tree.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class ConfigGroup {
  private final Map<String, ConfigGroup> groups;
  private final Map<String, ConfigNode<?>> nodes;
  private final String name;

  /** Returns a new config group instance with the provided name. */
  public static ConfigGroup getNewConfigGroup(String name) {
    Objects.requireNonNull(name);

    return new ConfigGroup(name);
  }

  protected ConfigGroup(String name) {
    this(name, Map.of(), Map.of());
  }

  protected ConfigGroup(
      String name,
      Map<String, ConfigGroup> groups,
      Map<String, ConfigNode<?>> nodes
  ) {
    Objects.requireNonNull(groups);
    Objects.requireNonNull(name);
    Objects.requireNonNull(nodes);

    this.name = name;
    this.groups = groups;
    this.nodes = nodes;
  }

  /**
   * Adds the config node to the group, overwriting any group or node with the same name.
   *
   * @param node the node to add to the group
   * @return a config group instance containing the added node
   */
  public ConfigGroup addConfigNode(ConfigNode<?> node) {
    Objects.requireNonNull(node);

    Map<String, ConfigNode<?>> nodes = new HashMap<>(this.nodes);
    nodes.put(node.getName(), node);

    Map<String, ConfigGroup> groups = new HashMap<>(this.groups);
    // Remove group with the same name if one exists
    groups.remove(node.getName());

    return new ConfigGroup(name, groups, nodes);
  }

  /**
   * Returns the config node with the provided name.
   *
   * @param name the name of the node
   * @return the config node with the provided name
   * @throws ConfigKeyDoesNotExistException if no node with the name exists
   */
  public ConfigNode<?> getConfigNode(String name) throws ConfigKeyDoesNotExistException {
    Objects.requireNonNull(name);

    ConfigNode<?> node = nodes.get(name);
    if (node == null) {
      throw new ConfigKeyDoesNotExistException("Config node key " + name + " does not exist");
    }

    return node;
  }

  /**
   * Adds the config group to the group, overwriting any group or node with the same name.
   *
   * @param group the group to add to the group
   * @return a config group instance containing the added group
   */
  public ConfigGroup addConfigGroup(ConfigGroup group) {
    Objects.requireNonNull(group);

    Map<String, ConfigGroup> groups = new HashMap<>(this.groups);
    groups.put(group.getName(), group);

    Map<String, ConfigNode<?>> nodes = new HashMap<>(this.nodes);
    // Remove node with the same name if one exists
    nodes.remove(group.getName());


    return new ConfigGroup(name, groups, nodes);
  }

  /**
   * Returns the config group with the provided name.
   *
   * @param name the name of the group
   * @return the config group with the provided name
   * @throws ConfigKeyDoesNotExistException if no group with the name exists
   */
  public ConfigGroup getConfigGroup(String name) throws ConfigKeyDoesNotExistException {
    Objects.requireNonNull(name);

    ConfigGroup group = groups.get(name);
    if (group == null) {
      throw new ConfigKeyDoesNotExistException("Config group key " + name + " does not exist");
    }

    return group;
  }

  /**
   * Recursively sets the values of the stored config groups and ndoe with values from the
   * provided map.
   *
   * @param value map containing the values for the stored groups and nodes
   * @return a new instance with the set values
   * @throws ConfigurationException if a value in the map is invalid
   */
  public ConfigGroup setValue(Map<String, Object> value) throws ConfigurationException {
    Objects.requireNonNull(value);

    Map<String, ConfigGroup> newGroups = new HashMap<>();
    Map<String, ConfigNode<?>> newNodes = new HashMap<>();

    for (Entry<String, Object> data : value.entrySet()) {
      ConfigGroup group = groups.get(data.getKey());
      ConfigNode<?> node = nodes.get(data.getKey());

      // Set the value of the group if exists
      if (group != null) {
        if (!(data.getValue() instanceof Map)) {
          throw new ConfigurationException("Value for group " + group.getName() + " is not a Map");
        }

        newGroups.put(group.getName(), group.setValue((Map<String, Object>) data.getValue()));

      // Set the value of the node if exists
      } else if (node != null) {
        newNodes.put(node.getName(), node.setValue(data.getValue()));
      }

      // Do nothing if the value is not expected in case of deprecated nodes
    }

    return new ConfigGroup(name, newGroups, newNodes);
  }

  /** Returns a Map representing all the values in this group. */
  public Map<String, Object> asMap() {
    Map<String, Object> map = new HashMap<>();

    for (ConfigNode<?> node : nodes.values()) {
      map.put(node.getName(), node.getValue());
    }

    for (ConfigGroup group : groups.values()) {
      map.put(group.getName(), group.asMap());
    }

    return map;
  }

  /** Returns the name of this config group. */
  public String getName() {
    return name;
  }
}
