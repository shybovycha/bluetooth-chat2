package uj.edu.android.bluetooth2.sockets;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import uj.edu.android.bluetooth2.common.logger.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by shybovycha on 18.02.15.
 */
public class MyBluetoothServerSocket implements IBluetoothSocket, IServerSocket {
    protected BluetoothAdapter mAdapter;
    protected BluetoothServerSocket mServerSocket;

    private static final String TAG = "BluetoothConnection";
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    public MyBluetoothServerSocket(BluetoothAdapter adapter) {
        mAdapter = adapter;
        mServerSocket = null;
    }

    @Override
    public void listen() {
        if (mAdapter == null) {
            Log.e(TAG, "No bluetooth adapter is set");
            return;
        }

        try {
            mServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
        } catch (IOException e) {
            Log.e(TAG, "BluetoothSocket listen() failed", e);
        }
    }

    @Override
    public ISocket accept() {
        ISocket socket = null;

        try {
            socket = new MyBluetoothClientSocket(mAdapter, mServerSocket.accept());
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
            Log.e(TAG, "Could not close socket");
        }
    }

    @Override
    public String getAddress() {
        if (mAdapter == null)
            return null;

        return mAdapter.getAddress();
    }

    @Override
    public String getName() {
        if (mAdapter == null)
            return null;

        return mAdapter.getName();
    }

    @Override
    public BluetoothAdapter getAdapter() {
        return mAdapter;
    }
}
