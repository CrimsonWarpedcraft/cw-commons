package com.crimsonwarpedcraft.cwcommons.mock;

import com.crimsonwarpedcraft.cwcommons.user.PlayerData;

/**
 * Mock object for PlayerData.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class MockPlayerData extends PlayerData {
  private int randomVal;

  public MockPlayerData() {
    randomVal = 0;
  }

  public MockPlayerData(MockPlayerData data) {
    this.randomVal = data.randomVal;
  }

  public void setRandomVal(int randomVal) {
    this.randomVal = randomVal;
  }

  public int getRandomVal() {
    return randomVal;
  }

  @Override
  public PlayerData copy() {
    return new MockPlayerData(this);
  }

  public static MockPlayerData of(PlayerData data) {
    return (MockPlayerData) data;
  }
}
