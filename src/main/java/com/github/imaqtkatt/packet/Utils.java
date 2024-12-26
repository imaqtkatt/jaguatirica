package com.github.imaqtkatt.packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class Utils {
    public static String readString(final ByteBuffer byteBuffer) {
        if (byteBuffer.remaining() < Integer.BYTES) {
            throw new IllegalArgumentException("Not enough bytes for string length");
        }
        var len = byteBuffer.getInt();
        if (byteBuffer.remaining() < len) {
            throw new IllegalArgumentException("Not enough bytes for string data");
        }
        var bytes = new byte[len];
        byteBuffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeString(String s, ByteBuffer byteBuffer) {
        var bytes = s.getBytes(StandardCharsets.UTF_8);
        byteBuffer.putInt(bytes.length);
        byteBuffer.put(bytes);
    }
}
