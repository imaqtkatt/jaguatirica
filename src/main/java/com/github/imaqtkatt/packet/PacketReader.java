package com.github.imaqtkatt.packet;

import com.github.imaqtkatt.term.TermReader;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.function.Function;

import static com.github.imaqtkatt.packet.Utils.readString;

public final class PacketReader {

    public static Packet read(final ByteBuffer byteBuffer) {
        byte type = byteBuffer.get();
        return REGISTRY.getOrDefault(type, _ -> Packet.INVALID).apply(byteBuffer);
    }

    private static final Map<Byte, Function<ByteBuffer, Packet>> REGISTRY = Map.of(
            Packet.TYPE_GET, buf -> new Packet.Get(readString(buf)),
            Packet.TYPE_SET, buf -> new Packet.Set(readString(buf), TermReader.read(buf)),
            Packet.TYPE_INCREMENT, buf -> new Packet.Increment(readString(buf)),
            Packet.TYPE_DECREMENT, buf -> new Packet.Decrement(readString(buf))
    );
}
