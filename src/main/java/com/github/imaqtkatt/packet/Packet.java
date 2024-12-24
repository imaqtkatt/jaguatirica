package com.github.imaqtkatt.packet;

import com.github.imaqtkatt.term.Term;

public sealed interface Packet {

    Packet UNKNOWN = new UnknownPacket();

    byte TYPE_GET = 1;
    byte TYPE_SET = 2;
    byte TYPE_INCREMENT = 3;

    record UnknownPacket() implements Packet {
    }

    record GetPacket(String key) implements Packet {
    }

    record SetPacket(String key, Term value) implements Packet {
    }

    record Increment(String key) implements Packet {
    }

}
