package uj.edu.android.bluetooth2.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by shybovycha on 13.02.15.
 */
public interface ISocket {
    public void close();
    public String getAddress();
    public String getName();
}
