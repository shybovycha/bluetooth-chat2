package uj.edu.android.bluetooth2.sockets;

import java.net.ServerSocket;

/**
 * Created by shybovycha on 21.02.15.
 */
public class TcpSocketFactory implements ISocketFactory {
    protected int mPort;
    protected String mName;
    protected String mAddress;

    public TcpSocketFactory(int port) {
        mPort = port;

        IServerSocket tmpSocket = createServerSocket();

        mAddress = tmpSocket.getAddress();
        mName = tmpSocket.getName();
    }

    @Override
    public IClientSocket createClientSocket() {
        return new MyTcpClientSocket();
    }

    @Override
    public IServerSocket createServerSocket() {
        return new MyTcpServerSocket(mPort);
    }

    @Override
    public String getAddress() {
        return mAddress;
    }

    @Override
    public String getName() {
        return mName;
    }
}
