package uj.edu.android.bluetooth2.sockets;

import uj.edu.android.bluetooth2.common.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by shybovycha on 13.02.15.
 */
public class MyTcpClientSocket implements ITcpSocket, IClientSocket {
    protected Socket mClientSocket;

    private static final String TAG = "BluetoothConnection";

    public MyTcpClientSocket() {
        mClientSocket = null;
    }

    public MyTcpClientSocket(Socket socket) {
        mClientSocket = socket;
    }

    @Override
    public void close() {
        if (mClientSocket == null)
            return;

        try {
            mClientSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "BluetoothSocket close() failed", e);
        }
    }

    @Override
    public InputStream getInputStream() {
        if (mClientSocket == null)
            return null;

        try {
            return mClientSocket.getInputStream();
        } catch (IOException e) {
            Log.e(TAG, "BluetoothSocket getInputStream() failed", e);
            return null;
        }
    }

    @Override
    public OutputStream getOutputStream() {
        if (mClientSocket == null)
            return null;

        try {
            return mClientSocket.getOutputStream();
        } catch (IOException e) {
            Log.e(TAG, "BluetoothSocket getInputStream() failed", e);
            return null;
        }
    }

    @Override
    public String getAddress() {
        if (mClientSocket == null)
            return null;

        return mClientSocket.getRemoteSocketAddress().toString().replaceAll("[^\\d\\.:]", "");
    }

    @Override
    public String getName() {
        if (mClientSocket == null)
            return null;

        return getAddress();
    }

    @Override
    public void connect(String address) throws IOException {
//        String[] parts = address.split("^(\\d{0,3}\\.\\d{0,3}\\.\\d{0,3}\\.\\d{0,3})(:)?$")
        String[] parts = address.split(":");

        if (parts.length < 2) {
            throw new IOException("Wrong address to connect to. Expected: \"ip_address:port\"");
        }

        InetAddress inetAddr = InetAddress.getByName(parts[0]);
        int port = Integer.parseInt(parts[1]);

        mClientSocket = new Socket(inetAddr, port);
    }
}
