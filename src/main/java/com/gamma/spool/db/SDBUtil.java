package com.gamma.spool.db;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class SDBUtil {

    protected static final InetSocketAddress INET_SOCKET = new InetSocketAddress(7655);

    protected abstract static class AbstractConnection {

        protected final Socket socket;

        protected AbstractConnection(Socket socket) {
            this.socket = socket;
        }

        protected void startTransaction() throws SocketException {
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            transaction();
        }

        protected void stopTransaction() throws IOException {
            socket.close();
        }

        protected abstract void transaction();
    }

    protected enum ConnectionState {

        OK((byte) 0x00),
        OK_NODATA((byte) 0x01),
        CLIENT_HELLO((byte) 0x02),
        CLIENT_DATA((byte) 0x03),
        SERVER_DATA((byte) 0x04),
        SERVER_DISCONNECT((byte) 0x05),
        CLIENT_DISCONNECT((byte) 0x06),
        ERR((byte) 0x07);

        protected final byte sequence;

        ConnectionState(byte thisSequence) {
            sequence = thisSequence;
        }

        protected boolean sequenceEquals(byte seq) {
            return seq == this.sequence;
        }
    }
}
