package uj.edu.android.bluetooth2.sockets;

import android.bluetooth.BluetoothAdapter;

/**
 * Created by shybovycha on 21.02.15.
 */
public interface IBluetoothSocket extends ISocket {
    public BluetoothAdapter getAdapter();
}
