package com.github.imaqtkatt.packet;

import com.github.imaqtkatt.term.Term;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class TermReader {

    static final Term INVALID_TERM_TYPE = new Term.Error("Invalid term type");

    public static Term readTerm(final ByteBuffer byteBuffer) {
        var type = byteBuffer.get();

        return switch (type) {
            case Term.TYPE_ERROR -> INVALID_TERM_TYPE;
            case Term.TYPE_TEXT -> {
                var text = readString(byteBuffer);
                yield new Term.Text(text);
            }
            case Term.TYPE_INTEGER -> {
                var i64 = byteBuffer.getLong();
                yield new Term.Integer(i64);
            }
            default -> throw new IllegalStateException(String.valueOf(type));
        };
    }

    private static String readString(final ByteBuffer byteBuffer) {
        var len = byteBuffer.getInt();
        var bytes = new byte[len];
        byteBuffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
