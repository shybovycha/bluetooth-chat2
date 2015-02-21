package uj.edu.android.bluetooth2.sockets;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import uj.edu.android.bluetooth2.common.logger.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by shybovycha on 13.02.15.
 */
public class MyBluetoothClientSocket implements IBluetoothSocket, IClientSocket {
    protected android.bluetooth.BluetoothSocket mClientSocket;
    protected BluetoothAdapter mAdapter;

    private static final String TAG = "BluetoothConnection";
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public MyBluetoothClientSocket(BluetoothAdapter adapter) {
        mAdapter = adapter;
        mClientSocket = null;
    }

    public MyBluetoothClientSocket(BluetoothAdapter adapter, BluetoothSocket socket) {
        mAdapter = adapter;
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

        return mClientSocket.getRemoteDevice().getAddress();
    }

    @Override
    public String getName() {
        if (mClientSocket == null)
            return null;

        return mClientSocket.getRemoteDevice().getName();
    }

    @Override
    public void connect(String address) throws IOException {
        mClientSocket = mAdapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(MY_UUID_SECURE);
        mClientSocket.connect();
    }

    @Override
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }
}
