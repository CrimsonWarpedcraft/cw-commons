package com.crimsonwarpedcraft.cwcommons.store;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class KeySerializersTest {

  @Test
  void uuidRoundtrip() {
    KeySerializer<UUID> s = KeySerializers.forUuid();
    UUID id = UUID.randomUUID();
    assertEquals(id, s.deserialize(s.serialize(id)));
  }

  @Test
  void uuidSerializesToString() {
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    assertEquals("00000000-0000-0000-0000-000000000001",
        KeySerializers.forUuid().serialize(id));
  }

  @Test
  void stringRoundtrip() {
    KeySerializer<String> s = KeySerializers.forString();
    assertEquals("hello", s.deserialize(s.serialize("hello")));
  }

  @Test
  void stringSerializerIsIdentity() {
    assertEquals("test-key", KeySerializers.forString().serialize("test-key"));
  }
}
