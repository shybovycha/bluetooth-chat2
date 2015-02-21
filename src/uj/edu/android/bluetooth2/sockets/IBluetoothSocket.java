package uj.edu.android.bluetooth2.sockets;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;

/**
 * Created by shybovycha on 21.02.15.
 */
public interface IBluetoothSocket extends ISocket {
    public BluetoothAdapter getAdapter();
}
