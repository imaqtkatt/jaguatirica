package com.github.imaqtkatt;

import com.github.imaqtkatt.term.InvalidTermException;
import com.github.imaqtkatt.packet.Packet;
import com.github.imaqtkatt.packet.PacketReader;
import com.github.imaqtkatt.packet.PacketWriter;
import com.github.imaqtkatt.term.Term;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.BufferOverflowException;
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
                    packet = handlePacket(PacketReader.read(byteBuffer));
                } catch (InvalidTermException e) {
                    packet = Packet.INVALID_TERM;
                } catch (BufferOverflowException e) {
                    packet = Packet.BUFFER_OVERFLOW;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                byteBuffer.clear();

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
    private static final String TERM_IS_NOT_A_SET_MSG = "Term is not a set";

    private static final Packet UNBOUND_TERM_PACKET = new Packet.Error(UNBOUND_TERM_MSG);
    private static final Packet TERM_IS_NOT_AN_INTEGER_PACKET = new Packet.Error(TERM_IS_NOT_AN_INTEGER_MSG);
    private static final Packet INTEGER_OVERFLOW_PACKET = new Packet.Error(INTEGER_OVERFLOW_MSG);
    private static final Packet INTEGER_UNDERFLOW_PACKET = new Packet.Error(INTEGER_UNDERFLOW_MSG);
    private static final Packet TERM_IS_NOT_A_SET_PACKET = new Packet.Error(TERM_IS_NOT_A_SET_MSG);

    private static final Packet OK_PACKET = new Packet.Ok(Term.OK);

    private Packet handlePacket(Packet packet) {
        return switch (packet) {
            case Packet.Ok _, Packet.Error _ -> Packet.INVALID;

            case Packet.Get(String key) -> handleGet(key);

            case Packet.Set(String key, Term term) -> handleSet(key, term);

            case Packet.Increment(String key) -> handleIncrement(key);

            case Packet.Decrement(String key) -> handleDecrement(key);

            case Packet.SetAdd(String key, ArrayList<String> values) -> handleSetAdd(key, values);

            case Packet.SetUnion(ArrayList<String> keys) -> handleSetUnion(keys);

            case Packet.SetIntersection(ArrayList<String> keys) -> handleSetIntersection(keys);
        };
    }

    private Packet handleGet(String key) {
        Term r;
        if ((r = state.get(key)) == null) {
            return UNBOUND_TERM_PACKET;
        } else {
            return new Packet.Ok(r);
        }
    }

    private Packet handleSet(String key, Term term) {
        state.put(key, term);
        return OK_PACKET;
    }

    private Packet handleIncrement(String key) {
        var term = state.computeIfAbsent(key, _ -> new Term.Integer(0));

        if (term instanceof Term.Integer(long i)) {
            long r = i + 1;
            if (((i ^ r) & (1 ^ r)) < 0) {
                return INTEGER_OVERFLOW_PACKET;
            } else {
                var r2 = state.compute(key, (_, _) -> new Term.Integer(r));
                return new Packet.Ok(r2);
            }
        } else {
            return TERM_IS_NOT_AN_INTEGER_PACKET;
        }
    }

    private Packet handleDecrement(String key) {
        var term = state.computeIfAbsent(key, _ -> new Term.Integer(0));

        if (term instanceof Term.Integer(long i)) {
            long r = i - 1;
            if (((i ^ 1) & (i ^ r)) < 0) {
                return INTEGER_UNDERFLOW_PACKET;
            } else {
                var r2 = state.compute(key, (_, _) -> new Term.Integer(r));
                return new Packet.Ok(r2);
            }
        } else {
            return TERM_IS_NOT_AN_INTEGER_PACKET;
        }
    }

    private Packet handleSetAdd(String key, ArrayList<String> values) {
        var term = state.computeIfAbsent(key, _ -> new Term.Set(HashSet.newHashSet(2)));

        if (term instanceof Term.Set(HashSet<String> set)) {
            var added = values.stream().filter(set::add).count();
            return new Packet.Ok(new Term.Integer(added));
        } else {
            return TERM_IS_NOT_A_SET_PACKET;
        }
    }

    private Packet handleSetUnion(ArrayList<String> keys) {
        var result = new HashSet<String>();

        for (var key : keys) {
            Term term;
            if ((term = state.get(key)) == null) {
                continue;
            }

            if (term instanceof Term.Set(HashSet<String> set)) {
                result.addAll(set);
            } else {
                return TERM_IS_NOT_A_SET_PACKET;
            }
        }

        return new Packet.Ok(new Term.Set(result));
    }

    private Packet handleSetIntersection(ArrayList<String> keys) {
        HashSet<String> result;

        var iter = keys.iterator();
        if (!iter.hasNext()) {
            throw new RuntimeException();
        }

        Term term;
        if ((term = state.get(iter.next())) == null) {
            throw new IllegalStateException();
        }
        if (term instanceof Term.Set(HashSet<String> set)) {
            result = set;
        } else {
            return TERM_IS_NOT_A_SET_PACKET;
        }

        while (iter.hasNext()) {
            var key = iter.next();
            Term term2;
            if ((term2 = state.get(key)) == null) {
                continue;
            }

            if (term2 instanceof Term.Set(HashSet<String> set2)) {
                result.retainAll(set2);
            } else {
                return TERM_IS_NOT_A_SET_PACKET;
            }
        }

        return new Packet.Ok(new Term.Set(result));
    }
}
