package uj.edu.android.bluetooth2.sockets;

import uj.edu.android.bluetooth2.common.logger.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by shybovycha on 18.02.15.
 */
public class MyTcpServerSocket implements ITcpSocket, IServerSocket {
    protected ServerSocket mServerSocket;
    protected int mPort;

    private static final String TAG = "BluetoothConnection";

    public MyTcpServerSocket(int port) {
        mPort = port;
        mServerSocket = null;
    }

    @Override
    public void listen() {
        try {
            mServerSocket = new ServerSocket(mPort);
        } catch (IOException e) {
            Log.e(TAG, "Could not create tcp server socket");
        }
    }

    @Override
    public IClientSocket accept() {
        IClientSocket socket = null;

        try {
            Socket tmp = mServerSocket.accept();

            if (tmp == null)
                return null;

            socket = new MyTcpClientSocket(tmp);
        } catch (Exception e) {
            Log.e(TAG, "Could not accept socket");
        }

        return socket;
    }

    @Override
    public void close() {
        if (mServerSocket == null)
            return;

        try {
            mServerSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "BluetoothSocket close() failed", e);
        }
    }

    @Override
    public String getAddress() {
        if (mServerSocket == null)
            return null;

        return mServerSocket.getLocalSocketAddress().toString().replaceAll("[^\\d\\.:]", "");
    }

    @Override
    public String getName() {
        return getAddress();
    }
}
