package uj.edu.android.bluetooth2.sockets;

/**
 * Created by shybovycha on 21.02.15.
 */
public interface ISocketFactory {
    public IClientSocket createClientSocket();
    public IServerSocket createServerSocket();
    public String getAddress();
    public String getName();
}
