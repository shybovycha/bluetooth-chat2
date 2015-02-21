package uj.edu.android.bluetooth2.sockets;

/**
 * Created by shybovycha on 18.02.15.
 */
public interface IServerSocket extends ISocket {
    public void listen();
    public IClientSocket accept();
}
