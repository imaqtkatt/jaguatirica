package com.github.imaqtkatt.packet;

import com.github.imaqtkatt.term.Term;
import com.github.imaqtkatt.term.TermWriter;

import java.nio.ByteBuffer;

import static com.github.imaqtkatt.packet.Utils.writeString;

public final class PacketWriter {
    public static void write(Packet packet, ByteBuffer byteBuffer) {
        switch (packet) {
            case Packet.Ok(Term term) -> {
                byteBuffer.put(Packet.TYPE_OK);
                TermWriter.write(term, byteBuffer);
            }
            case Packet.Error(String reason) -> {
                byteBuffer.put(Packet.TYPE_ERROR);
                writeString(reason, byteBuffer);
            }
            default -> throw new IllegalArgumentException();
        }
    }
}
