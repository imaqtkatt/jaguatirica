package com.github.imaqtkatt;

import java.net.InetSocketAddress;

public class Main {

    static final String DEFAULT_HOST = "localhost";
    static final int DEFAULT_PORT = 8345;

    public static void main(String[] args) {
        try {
            String host;
            int port;

            if (args.length == 2) {
                host = args[0];
                port = Integer.parseInt(args[1]);
            } else {
                host = DEFAULT_HOST;
                port = DEFAULT_PORT;
            }

            new JaguatiricaServer().start(new InetSocketAddress(host, port));
        } catch (NumberFormatException e) {
            throw new RuntimeException(e);
        }
    }
}
