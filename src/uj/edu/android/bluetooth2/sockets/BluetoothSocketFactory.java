package uj.edu.android.bluetooth2.sockets;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by shybovycha on 21.02.15.
 */
public class BluetoothSocketFactory implements ISocketFactory {
    protected BluetoothAdapter mAdapter;

    public BluetoothSocketFactory(BluetoothAdapter adapter) {
        mAdapter = adapter;
    }

    @Override
    public IClientSocket createClientSocket() {
        return new MyBluetoothClientSocket(mAdapter);
    }

    @Override
    public IServerSocket createServerSocket() {
        return new MyBluetoothServerSocket(mAdapter);
    }

    @Override
    public String getAddress() {
        return mAdapter.getAddress();
    }

    @Override
    public String getName() {
        return mAdapter.getName();
    }
}
