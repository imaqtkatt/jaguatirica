package com.github.imaqtkatt.packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class PacketReader {

    public static Packet readPacket(final ByteBuffer byteBuffer) {
        byte type = byteBuffer.get();

        return switch (type) {
            case Packet.TYPE_GET -> {
                var key = readKey(byteBuffer);
                yield new Packet.GetPacket(key);
            }
            case Packet.TYPE_SET -> {
                var key = readKey(byteBuffer);
                var term = TermReader.readTerm(byteBuffer);
                yield new Packet.SetPacket(key, term);
            }
            case Packet.TYPE_INCREMENT -> {
                var key = readKey(byteBuffer);
                yield new Packet.Increment(key);
            }
            default -> Packet.UNKNOWN;
        };
    }

    private static String readKey(final ByteBuffer byteBuffer) {
        var len = byteBuffer.getInt();
        var bytes = new byte[len];
        byteBuffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
