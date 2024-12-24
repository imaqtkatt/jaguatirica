package com.github.imaqtkatt;

import com.github.imaqtkatt.packet.Packet;
import com.github.imaqtkatt.packet.PacketReader;
import com.github.imaqtkatt.term.Term;
import com.github.imaqtkatt.term.TermWriter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class JaguatiricaServer {

    private static final int BUF_SIZE = 4096;

    private final Map<String, Term> state = new HashMap<>();

    private final List<SocketChannel> socketChannels = new ArrayList<>();

    private static final Term TERM_IS_NOT_AN_INTEGER = new Term.Error("Term is not an integer");
    private static final Term INTEGER_OVERFLOW = new Term.Error("Integer overflow");

    public void start(InetSocketAddress inetSocketAddress) {
        try (var selector = Selector.open(); var serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(inetSocketAddress);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server running @ " + inetSocketAddress.toString());

            while (true) {
                if (selector.select() == 0) {
                    continue;
                }

                for (var key : selector.selectedKeys()) {
                    if (key.isAcceptable()) {
                        onAccept(selector, key);
                    } else if (key.isReadable()) {
                        onRequest(key);
                    }
                }

                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            for (var socketChannel : socketChannels) {
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void onAccept(Selector selector, SelectionKey selectionKey) throws IOException {
        if (selectionKey.channel() instanceof ServerSocketChannel serverSocketChannel) {
            var clientChannel = serverSocketChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            socketChannels.add(clientChannel);
        } else {
            throw new RuntimeException("Invalid state");
        }
    }

    private void onRequest(SelectionKey selectionKey) throws IOException {
        if (selectionKey.channel() instanceof SocketChannel clientChannel) {
            var byteBuffer = ByteBuffer.allocate(BUF_SIZE);

            try {
                int read = clientChannel.read(byteBuffer);
                if (read == -1) {
                    clientChannel.close();
                    return;
                }

                byteBuffer.flip();
                var packet = PacketReader.readPacket(byteBuffer);
                var response = handlePacket(packet);

                byteBuffer.clear();

                TermWriter.write(response, byteBuffer);
                byteBuffer.flip();

                while (byteBuffer.hasRemaining()) {
                    clientChannel.write(byteBuffer);
                }

                byteBuffer.clear();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    clientChannel.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            throw new RuntimeException("Invalid state");
        }
    }

    private Term handlePacket(Packet packet) {
        return switch (packet) {
            case Packet.Get(String key) -> state.getOrDefault(key, new Term.Error("Unbound term"));

            case Packet.Set(String key, Term term) -> {
                state.put(key, term);
                yield Term.OK;
            }

            case Packet.Increment(String key) -> {
                var term = state.computeIfAbsent(key, _ -> new Term.Integer(0));

                if (term instanceof Term.Integer(long i)) {
                    try {
                        var incremented = Math.addExact(i, 1);
                        yield state.compute(key, (_, _) -> new Term.Integer(incremented));
                    } catch (ArithmeticException e) {
                        yield INTEGER_OVERFLOW;
                    }
                } else {
                    yield TERM_IS_NOT_AN_INTEGER;
                }
            }

            case Packet.Unknown() -> throw new IllegalStateException("Unexpected value");
        };
    }
}
