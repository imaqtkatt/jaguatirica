package com.github.imaqtkatt.term;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

import static com.github.imaqtkatt.packet.Utils.readString;

public final class TermReader {

    public static Term read(ByteBuffer byteBuffer) {
        var readerFunction = REGISTRY.get(byteBuffer.get());
        if (readerFunction == null) {
            throw new InvalidTermException();
        } else {
            return readerFunction.apply(byteBuffer);
        }
    }

    private static final Map<Byte, Function<ByteBuffer, Term>> REGISTRY = Map.of(
            Term.TYPE_TEXT, TermReader::readText,
            Term.TYPE_INTEGER, TermReader::readInteger
    );

    private static Term readText(ByteBuffer buf) {
        return new Term.Text(readString(buf));
    }

    private static Term readInteger(ByteBuffer buf) {
        return new Term.Integer(buf.getLong());
    }

    private static Term readSet(ByteBuffer buf) {
        throw new UnsupportedOperationException();
    }
}
