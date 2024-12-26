package com.github.imaqtkatt.term;

import com.github.imaqtkatt.packet.Utils;

import java.nio.ByteBuffer;

public final class TermWriter {
    public static void write(Term term, ByteBuffer byteBuffer) {
        switch (term) {
            case Term.Text(String text) -> {
                byteBuffer.put(Term.TYPE_TEXT);
                Utils.writeString(text, byteBuffer);
            }
            case Term.Integer(long i) -> {
                byteBuffer.put(Term.TYPE_INTEGER);
                byteBuffer.putLong(i);
            }
            case Term.Set _ -> {
                throw new UnsupportedOperationException();
            }
        }
    }
}
