package com.github.imaqtkatt.term;

public sealed interface Term {

    byte TYPE_TEXT = 1;
    byte TYPE_INTEGER = 2;
    byte TYPE_SET = 3;

    Term OK = new Text("OK");

    record Text(String text) implements Term {
    }

    record Integer(long i) implements Term {
    }

    record Set(java.util.Set<Term> set) implements Term {
    }
}
