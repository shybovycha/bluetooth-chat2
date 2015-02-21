package uj.edu.android.bluetooth2.sockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by shybovycha on 21.02.15.
 */
public interface IClientSocket extends ISocket {
    public void connect(String address) throws IOException;
    public InputStream getInputStream();
    public OutputStream getOutputStream();
}
