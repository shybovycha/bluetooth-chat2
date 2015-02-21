package uj.edu.android.bluetooth2.sockets;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import uj.edu.android.bluetooth2.common.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.UUID;

/**
 * Created by shybovycha on 18.02.15.
 */
public class MyTcpServerSocket implements ITcpSocket, IServerSocket {
    protected ServerSocket mServerSocket;
    protected int mPort;

    private static final String TAG = "BluetoothConnection";
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

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
    public ISocket accept() {
        ISocket socket = null;

        try {
            socket = new MyTcpClientSocket(mServerSocket.accept());
        } catch (IOException e) {
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

        return mServerSocket.getInetAddress().toString();
    }

    @Override
    public String getName() {
        return getAddress();
    }
}
