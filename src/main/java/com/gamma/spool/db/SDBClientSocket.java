package com.gamma.spool.db;

import static com.gamma.spool.db.SDBUtil.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import javax.net.SocketFactory;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import it.unimi.dsi.fastutil.bytes.BytePriorityQueue;

public class SDBClientSocket {

    private static final SocketFactory factory = SocketFactory.getDefault();

    private static boolean restart = false;
    private static final Scanner scanner = new Scanner(System.in);

    private static InetSocketAddress clientSocket;

    public static void start() {
        try {
            System.out.print("IP to connect to (leave blank for localhost): ");
            String input = scanner.nextLine();
            InetAddress address;
            if (input == null || input.isEmpty() || input.equals(" ")) address = InetAddress.getByName("localhost");
            else address = InetAddress.getByName(input);

            clientSocket = new InetSocketAddress(address.getHostAddress(), INET_SOCKET.getPort());

            do {
                Socket socket = factory.createSocket();
                socket.setReuseAddress(true);
                socket.setSoTimeout(5000);
                socket.connect(clientSocket);
                ClientConnection conn = new ClientConnection(socket);
                conn.startTransaction();
                if (restart) {
                    System.out.println("Restarting connection...");
                    Thread.sleep(2000);
                }
            } while (restart);
        } catch (SocketTimeoutException ignored) {
            System.out.println("SDB timed out");
        } catch (Exception ex) {
            System.out.println("SDBClientSocket startup failed");
            System.out.println(ex.getMessage());
            if (ex.getMessage()
                .equals("Connection refused: connect")) System.out.println("Is the server running?");
        }
    }

    public static class ClientConnection extends AbstractConnection {

        protected ClientConnection(Socket socket) {
            super(socket);
        }

        protected void transaction() {
            try {
                // Begin lifecycle.
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                out.write(ConnectionState.CLIENT_HELLO.sequence);
                out.flush();

                byte value = (byte) in.read();
                if (!ConnectionState.OK_NODATA.sequenceEquals(value)) {
                    out.write(ConnectionState.CLIENT_DISCONNECT.sequence);
                    out.flush();
                    stopTransaction();
                    System.out.println("err, disconnecting");
                    return;
                }

                System.out.println("Connected to SDB terminal on port " + clientSocket.getPort());
                while (true) {
                    System.out.print("> ");
                    String input = scanner.nextLine();
                    if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("exit;")) break;

                    if (input.equalsIgnoreCase("reconnect") || input.equalsIgnoreCase("reconnect;")) {
                        if (restart) System.out.println("SDB will no longer reconnect if disconnected.");
                        else System.out.println("SDB will reconnect if disconnected.");
                        restart = !restart;
                        continue;
                    }

                    out.write(ConnectionState.CLIENT_DATA.sequence);
                    out.write(input.getBytes());
                    out.write(ConnectionState.OK_NODATA.sequence);
                    out.flush();

                    value = (byte) in.read();
                    if (ConnectionState.ERR.sequenceEquals(value)) {
                        System.out.println("Server error.");
                        continue;
                    } else if (!ConnectionState.OK.sequenceEquals(value)) {
                        break;
                    }

                    value = (byte) in.read();
                    if (!ConnectionState.SERVER_DATA.sequenceEquals(value)) {
                        System.out.println("err, disconnecting");
                        break;
                    }

                    byte[] bytes = new byte[4];
                    in.read(bytes);
                    int numResults = ByteUtil.intFromBytes(bytes);
                    BytePriorityQueue queue = new ByteArrayFIFOQueue();
                    String[] results = new String[numResults];
                    int idx = 0;
                    while (numResults > 0) {
                        value = (byte) in.read();

                        if (!ConnectionState.OK.sequenceEquals(value)) {
                            if (value == -1) throw new IllegalStateException("Input stream closed unexpectedly");
                            queue.enqueue(value);
                            continue;
                        }

                        int size = queue.size();
                        byte[] arr = new byte[size];
                        for (int i = 0; i < size; i++) {
                            arr[i] = queue.dequeueByte();
                        }
                        results[idx++] = new String(arr);
                        numResults--;
                        queue.clear();
                    }

                    out.write(ConnectionState.OK_NODATA.sequence);
                    out.flush();

                    System.out.println("Server OK. Response:");
                    for (String result : results) {
                        System.out.println(result);
                    }
                }

                // End lifecycle.
                out.write(ConnectionState.CLIENT_DISCONNECT.sequence);
                out.flush();
                socket.close();
                stopTransaction();
                System.out.println("Exited.");
            } catch (Exception ex) {
                System.out.println("ERROR: " + ex.getMessage());
            }
        }
    }
}
