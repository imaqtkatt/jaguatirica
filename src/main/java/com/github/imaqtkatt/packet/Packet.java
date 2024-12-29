package com.github.imaqtkatt.packet;

import com.github.imaqtkatt.term.Term;

import java.util.ArrayList;

public sealed interface Packet {

    Packet INVALID = new Error("Invalid packet");
    Packet INVALID_TERM = new Error("Invalid term");
    Packet BUFFER_OVERFLOW = new Error("Buffer overflow");

    byte TYPE_GET = 1;
    byte TYPE_SET = 2;
    byte TYPE_INCREMENT = 3;
    byte TYPE_DECREMENT = 4;
    byte TYPE_SET_ADD = 5;
    byte TYPE_SET_UNION = 6;

    byte RESPONSE_FLAG = (byte) 128;
    byte TYPE_OK = 1 | RESPONSE_FLAG;
    byte TYPE_ERROR = 2 | RESPONSE_FLAG;

    record Ok(Term term) implements Packet {
    }

    record Error(String reason) implements Packet {
    }

    record Get(String key) implements Packet {
    }

    record Set(String key, Term value) implements Packet {
    }

    record Increment(String key) implements Packet {
    }

    record Decrement(String key) implements Packet {
    }

    record SetAdd(String key, ArrayList<String> values) implements Packet {
    }

    record SetUnion(ArrayList<String> keys) implements Packet {
    }
}
