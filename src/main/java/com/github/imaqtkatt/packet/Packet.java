package com.github.imaqtkatt.packet;

import com.github.imaqtkatt.term.Term;

public sealed interface Packet {

    Packet UNKNOWN = new Unknown();

    byte TYPE_GET = 1;
    byte TYPE_SET = 2;
    byte TYPE_INCREMENT = 3;

    record Unknown() implements Packet {
    }

    record Get(String key) implements Packet {
    }

    record Set(String key, Term value) implements Packet {
    }

    record Increment(String key) implements Packet {
    }

}
