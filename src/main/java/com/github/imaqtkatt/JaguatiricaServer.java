package com.github.imaqtkatt;

import com.github.imaqtkatt.term.InvalidTermException;
import com.github.imaqtkatt.packet.Packet;
import com.github.imaqtkatt.packet.PacketReader;
import com.github.imaqtkatt.packet.PacketWriter;
import com.github.imaqtkatt.term.Term;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class JaguatiricaServer {

    private static final int BUF_SIZE = 0xFFFF;

    private final Map<String, Term> state = new HashMap<>();

    private final List<SocketChannel> socketChannels = new ArrayList<>();
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(BUF_SIZE);

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
            throw new IllegalStateException();
        }
    }

    private void onRequest(SelectionKey selectionKey) throws IOException {
        if (selectionKey.channel() instanceof SocketChannel clientChannel) {
//            var byteBuffer = ByteBuffer.allocate(BUF_SIZE);

            try {
                int read = clientChannel.read(byteBuffer);
                if (read == -1) {
                    clientChannel.close();
                    return;
                }

                byteBuffer.flip();

                Packet packet;
                try {
                    packet = PacketReader.read(byteBuffer);
                    packet = handlePacket(packet);
                } catch (InvalidTermException e) {
                    packet = Packet.INVALID_TERM;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                byteBuffer.clear();

//                TermWriter.write(response, byteBuffer);
                PacketWriter.write(packet, byteBuffer);
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
            throw new IllegalStateException();
        }
    }

    private static final String UNBOUND_TERM_MSG = "Unbound term";
    private static final String TERM_IS_NOT_AN_INTEGER_MSG = "Term is not an integer";
    private static final String INTEGER_OVERFLOW_MSG = "Integer overflow";
    private static final String INTEGER_UNDERFLOW_MSG = "Integer underflow";

    private static final Packet UNBOUND_TERM_PACKET = new Packet.Error(UNBOUND_TERM_MSG);
    private static final Packet TERM_IS_NOT_AN_INTEGER_PACKET = new Packet.Error(TERM_IS_NOT_AN_INTEGER_MSG);
    private static final Packet INTEGER_OVERFLOW_PACKET = new Packet.Error(INTEGER_OVERFLOW_MSG);
    private static final Packet INTEGER_UNDERFLOW_PACKET = new Packet.Error(INTEGER_UNDERFLOW_MSG);
    private static final Packet OK_PACKET = new Packet.Ok(Term.OK);

    private Packet handlePacket(Packet packet) {
        return switch (packet) {
            case Packet.Ok _, Packet.Error _ -> Packet.INVALID;

            case Packet.Get(String key) -> {
                Term r;
                if ((r = state.get(key)) == null) {
                    yield UNBOUND_TERM_PACKET;
                } else {
                    yield new Packet.Ok(r);
                }
            }

            case Packet.Set(String key, Term term) -> {
                state.put(key, term);
                yield OK_PACKET;
            }

            case Packet.Increment(String key) -> {
                var term = state.computeIfAbsent(key, _ -> new Term.Integer(0));

                if (term instanceof Term.Integer(long i)) {
                    long r = i + 1;
                    if (((i ^ r) & (1 ^ r)) < 0) {
                        yield INTEGER_OVERFLOW_PACKET;
                    } else {
                        var r2 = state.compute(key, (_, _) -> new Term.Integer(r));
                        yield new Packet.Ok(r2);
                    }
                } else {
                    yield TERM_IS_NOT_AN_INTEGER_PACKET;
                }
            }

            case Packet.Decrement(String key) -> {
                var term = state.computeIfAbsent(key, _ -> new Term.Integer(0));

                if (term instanceof Term.Integer(long i)) {
                    long r = i - 1;
                    if (((i ^ 1) & (i ^ r)) < 0) {
                        yield INTEGER_UNDERFLOW_PACKET;
                    } else {
                        var r2 = state.compute(key, (_, _) -> new Term.Integer(r));
                        yield new Packet.Ok(r2);
                    }
                } else {
                    yield TERM_IS_NOT_AN_INTEGER_PACKET;
                }
            }
        };
    }
}
