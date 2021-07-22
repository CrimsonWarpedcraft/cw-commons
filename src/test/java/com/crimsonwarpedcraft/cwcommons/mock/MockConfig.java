package com.crimsonwarpedcraft.cwcommons.mock;

import com.crimsonwarpedcraft.cwcommons.config.ConfigFile;
import com.crimsonwarpedcraft.cwcommons.config.ConfigOption;
import com.crimsonwarpedcraft.cwcommons.config.ConfigurationException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Mock object for ConfigFile.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class MockConfig extends ConfigFile {
  public MockConfig(File file) throws ConfigurationException {
    super(file);
  }

  public String getTestVal() {
    return getConfig().getString("test1");
  }

  @Override
  protected Set<ConfigOption> getDefaults() {
    Set<ConfigOption> options = new HashSet<>();
    options.add(
        ConfigOption.getNewConfigOption(
            "test1",
            "test2",
            value -> !value.equals("")
        )
    );

    return options;
  }
}
