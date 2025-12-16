package com.gamma.spool.db;

import static com.gamma.spool.db.SDBUtil.AbstractConnection;
import static com.gamma.spool.db.SDBUtil.ConnectionState;
import static com.gamma.spool.db.SDBUtil.INET_SOCKET;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ServerSocketFactory;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.core.SpoolLogger;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import it.unimi.dsi.fastutil.bytes.BytePriorityQueue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

public class SDBSocket {

    private static final ServerSocketFactory factory = ServerSocketFactory.getDefault();
    private static ServerSocket socket;

    private static final ExecutorService managerExecutor = Executors.newSingleThreadExecutor(
        new ThreadFactoryBuilder().setNameFormat("Spool-SBD-Manager")
            .build());

    private static Thread managerThread;

    private static final ExecutorService IOExecutor = Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("Spool-SBD-Net-IO")
            .build());

    private static final ObjectList<ServerConnection> SERVER_CONNECTIONS = ObjectLists
        .synchronize(new ObjectArrayList<>());

    private static boolean isRunning = false;

    public static int count() {
        return SERVER_CONNECTIONS.size();
    }

    public static void start() {
        if (isRunning) return;
        isRunning = true;
        try {
            socket = factory.createServerSocket(INET_SOCKET.getPort());
            socket.setReuseAddress(true);
            socket.setSoTimeout(50);
            startManager();
        } catch (Exception ex) {
            SpoolLogger.error("SDB server failed to start", ex);
        }
    }

    public static void stop() {
        if (!isRunning) return;
        managerThread.interrupt();
        while (!managerThread.isInterrupted()) {
            Thread.yield();
        }
        for (ServerConnection serverConnection : SERVER_CONNECTIONS) {
            try {
                serverConnection.stopTransaction();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        isRunning = false;
    }

    public static void startManager() {
        managerExecutor.execute(() -> {
            managerThread = Thread.currentThread();
            while (!Thread.interrupted()) {
                try {
                    Socket currentSocket = socket.accept();
                    if (SERVER_CONNECTIONS.size() >= DebugConfig.maxSDBConnections) currentSocket.close();
                    ServerConnection conn = new ServerConnection(currentSocket);
                    conn.startTransaction();
                } catch (SocketTimeoutException ignored) {
                    Thread.yield();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static class ServerConnection extends AbstractConnection {

        protected ServerConnection(Socket socket) {
            super(socket);
        }

        @Override
        public void startTransaction() throws SocketException {
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            SERVER_CONNECTIONS.add(this);
            IOExecutor.execute(this::transaction);
        }

        @Override
        public void stopTransaction() throws IOException {
            SERVER_CONNECTIONS.remove(this);
            super.stopTransaction();
        }

        protected void transaction() {
            try {
                // Begin lifecycle.
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();

                // Check for client init.
                // Keep re-checking the data until it matches our expectations.
                byte value;
                do {
                    if (Thread.currentThread()
                        .isInterrupted()) {
                        out.write(ConnectionState.SERVER_DISCONNECT.sequence);
                        out.flush();
                        stopTransaction();
                        return;
                    }
                    value = (byte) in.read();
                } while (!ConnectionState.CLIENT_HELLO.sequenceEquals(value));

                out.write(ConnectionState.OK_NODATA.sequence);
                out.flush();

                // Ready for data.
                BytePriorityQueue queue = new ByteArrayFIFOQueue();
                outer: while (true) {
                    value = (byte) in.read();
                    if (!ConnectionState.CLIENT_DATA.sequenceEquals(value)) {
                        continue;
                    }

                    while (true) {
                        if (Thread.currentThread()
                            .isInterrupted()) {
                            break outer;
                        }

                        value = (byte) in.read();

                        if (ConnectionState.OK_NODATA.sequenceEquals(value)) {
                            break;
                        }

                        if (value == -1) break;
                        queue.enqueue(value);
                    }

                    // Reached the end of the line, as denoted by the null byte.
                    int size = queue.size();
                    byte[] arr = new byte[size];
                    for (int i = 0; i < size; i++) {
                        arr[i] = queue.dequeueByte();
                    }

                    ObjectList<String> results = process(new String(arr));

                    if (results == null) {
                        out.write(ConnectionState.ERR.sequence);
                        out.flush();
                        continue;
                    }
                    out.write(ConnectionState.OK.sequence);
                    out.write(ConnectionState.SERVER_DATA.sequence);
                    out.write(ByteUtil.bytesFromInt(results.size()));
                    for (String result : results) {
                        out.write(result.getBytes());
                        out.write(ConnectionState.OK.sequence);
                    }
                    out.flush();

                    if (Thread.currentThread()
                        .isInterrupted()) {
                        break;
                    }
                    value = (byte) in.read();
                    if (!ConnectionState.OK_NODATA.sequenceEquals(value)) {
                        break;
                    }

                }

                // End lifecycle.
                out.write(ConnectionState.SERVER_DISCONNECT.sequence);
                out.flush();
                stopTransaction();
            } catch (Exception ex) {
                if (ex.getMessage()
                    .equalsIgnoreCase("Connection reset")) // Resets shouldn't trigger a whole error.
                    return;
                SpoolLogger.error("SDB server failed transaction", ex);
            }
        }

        private ObjectList<String> process(String instruction) {
            // We can interpret things as raw SQL queries, what's the worst that can happen?
            try {
                return SpoolDBManager.runQuery(instruction);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
