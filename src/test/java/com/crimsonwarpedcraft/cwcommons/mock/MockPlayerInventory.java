package com.crimsonwarpedcraft.cwcommons.mock;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Mock object for PlayerInventory.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
@SuppressFBWarnings("NP_NONNULL_RETURN_VIOLATION")
public class MockPlayerInventory implements PlayerInventory {
  private final List<ItemStack> items = new ArrayList<>();

  @Override
  public @NotNull ItemStack[] getArmorContents() {
    return new ItemStack[0];
  }

  @Override
  public @NotNull ItemStack[] getExtraContents() {
    return new ItemStack[0];
  }

  @Override
  public @Nullable ItemStack getHelmet() {
    return null;
  }

  @Override
  public @Nullable ItemStack getChestplate() {
    return null;
  }

  @Override
  public @Nullable ItemStack getLeggings() {
    return null;
  }

  @Override
  public @Nullable ItemStack getBoots() {
    return null;
  }

  @Override
  public int getSize() {
    return items.size();
  }

  @Override
  public int getMaxStackSize() {
    return 0;
  }

  @Override
  public void setMaxStackSize(int size) {

  }

  @Override
  public @NotNull HashMap<Integer, ItemStack> addItem(@NotNull ItemStack... items)
      throws IllegalArgumentException {
    this.items.addAll(Arrays.asList(items));

    return new HashMap<>();
  }

  @Override
  public @NotNull HashMap<Integer, ItemStack> removeItem(@NotNull ItemStack... items)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull HashMap<Integer, ItemStack> removeItemAnySlot(@NotNull ItemStack... items)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @org.checkerframework.checker.nullness.qual.Nullable ItemStack @NonNull [] getContents() {
    return new ItemStack[0];
  }

  @Override
  public void setContents(@NotNull ItemStack[] items) throws IllegalArgumentException {

  }

  @Override
  public @NotNull ItemStack[] getStorageContents() {
    return new ItemStack[0];
  }

  @Override
  public void setStorageContents(@NotNull ItemStack[] items) throws IllegalArgumentException {

  }

  @Override
  public boolean contains(@NotNull Material material) throws IllegalArgumentException {
    return false;
  }

  @Override
  public boolean contains(@Nullable ItemStack item) {
    return false;
  }

  @Override
  public boolean contains(@NotNull Material material, int amount) throws IllegalArgumentException {
    return false;
  }

  @Override
  public boolean contains(@Nullable ItemStack item, int amount) {
    return false;
  }

  @Override
  public boolean containsAtLeast(@Nullable ItemStack item, int amount) {
    return false;
  }

  @Override
  public @NotNull HashMap<Integer, ? extends ItemStack> all(@NotNull Material material)
      throws IllegalArgumentException {
    return null;
  }

  @Override
  public @NotNull HashMap<Integer, ? extends ItemStack> all(@Nullable ItemStack item) {
    return null;
  }

  @Override
  public int first(@NotNull Material material) throws IllegalArgumentException {
    return 0;
  }

  @Override
  public int first(@NotNull ItemStack item) {
    return 0;
  }

  @Override
  public int firstEmpty() {
    return 0;
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  @Override
  public void remove(@NotNull Material material) throws IllegalArgumentException {

  }

  @Override
  public void remove(@NotNull ItemStack item) {

  }

  @Override
  public void clear(int index) {

  }

  @Override
  public void clear() {
    items.clear();
  }

  @Override
  public int close() {
    return 0;
  }

  @Override
  public @NotNull List<HumanEntity> getViewers() {
    return null;
  }

  @Override
  public @NotNull InventoryType getType() {
    return null;
  }

  @Override
  public void setItem(@NotNull EquipmentSlot slot, @Nullable ItemStack item) {

  }

  @Override
  public void setItem(int index, @Nullable ItemStack item) {

  }

  @Override
  public @Nullable ItemStack getItem(int index) {
    return null;
  }

  @Override
  public @Nullable ItemStack getItem(@NotNull EquipmentSlot slot) {
    return null;
  }

  @Override
  public void setArmorContents(@Nullable ItemStack[] items) {

  }

  @Override
  public void setExtraContents(@Nullable ItemStack[] items) {

  }

  @Override
  public void setHelmet(@Nullable ItemStack helmet) {

  }

  @Override
  public void setChestplate(@Nullable ItemStack chestplate) {

  }

  @Override
  public void setLeggings(@Nullable ItemStack leggings) {

  }

  @Override
  public void setBoots(@Nullable ItemStack boots) {

  }

  @Override
  public @NotNull ItemStack getItemInMainHand() {
    return null;
  }

  @Override
  public void setItemInMainHand(@Nullable ItemStack item) {

  }

  @Override
  public @NotNull ItemStack getItemInOffHand() {
    return null;
  }

  @Override
  public void setItemInOffHand(@Nullable ItemStack item) {

  }

  @Override
  public @NotNull ItemStack getItemInHand() {
    return null;
  }

  @Override
  public void setItemInHand(@Nullable ItemStack stack) {

  }

  @Override
  public int getHeldItemSlot() {
    return 0;
  }

  @Override
  public void setHeldItemSlot(int slot) {

  }

  @Override
  public @Nullable HumanEntity getHolder() {
    return null;
  }

  @Override
  public @Nullable InventoryHolder getHolder(boolean useSnapshot) {
    return null;
  }

  @Override
  public @NotNull ListIterator<ItemStack> iterator() {
    return null;
  }

  @Override
  public @NotNull ListIterator<ItemStack> iterator(int index) {
    return null;
  }

  @Override
  public @Nullable Location getLocation() {
    return null;
  }
}
