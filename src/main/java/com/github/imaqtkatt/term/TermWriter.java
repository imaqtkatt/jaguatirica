package com.github.imaqtkatt.term;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class TermWriter {
    public static void write(Term term, ByteBuffer byteBuffer) {
        switch (term) {
            case Term.Error(String reason) -> {
                byteBuffer.put(Term.TYPE_ERROR);
                var bytes = reason.getBytes(StandardCharsets.UTF_8);
                byteBuffer.putInt(bytes.length);
                byteBuffer.put(bytes);
            }
            case Term.Text(String text) -> {
                byteBuffer.put(Term.TYPE_TEXT);
                var bytes = text.getBytes(StandardCharsets.UTF_8);
                byteBuffer.putInt(bytes.length);
                byteBuffer.put(bytes);
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
