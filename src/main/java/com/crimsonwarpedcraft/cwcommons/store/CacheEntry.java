package com.crimsonwarpedcraft.cwcommons.store;

final class CacheEntry<V> {

  final V value;
  volatile boolean dirty;

  CacheEntry(V value, boolean dirty) {
    this.value = value;
    this.dirty = dirty;
  }
}
