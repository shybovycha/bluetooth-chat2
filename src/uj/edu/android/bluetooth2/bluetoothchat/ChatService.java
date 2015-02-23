/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uj.edu.android.bluetooth2.bluetoothchat;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import uj.edu.android.bluetooth2.common.logger.Log;
import uj.edu.android.bluetooth2.sockets.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class ChatService implements Serializable {
    // Debugging
    private static final String TAG = "ChatService";

    // Member fields
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    private Map<String, ConnectedThread> mConnectedThreadPool;

    private ISocketFactory mSocketFactory;

    private Map<String, List<String>> mGraph;
    private List<ISocket> mNeighbours;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public ChatService(Context context, Handler handler, ISocketFactory factory) {
        // TODO: could be rewritten to any socket provider you want
        mSocketFactory = factory;

        mState = STATE_NONE;
        mHandler = handler;
        mConnectedThreadPool = new TreeMap<String, ConnectedThread>();
        mNeighbours = new ArrayList<ISocket>();
        mGraph = new TreeMap<String, List<String>>();
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param remoteAddress The socket to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(String remoteAddress, boolean secure) {
        Log.d(TAG, "connect to: " + remoteAddress);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(remoteAddress, secure);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     */
    public synchronized void connected(IClientSocket socket, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        /*if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }*/

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, socket.getAddress());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    protected void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    protected void write(String address, byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;

            r = mConnectedThreadPool.get(address);
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void sendRaw(String message, String address) {
        write(message.getBytes());
    }

    public void sendGraph(String address, Map<String, List<String>> graph) {
        String res = ChatProtocol.createGraphMessage(graph, address, mSocketFactory.getAddress(), mSocketFactory.getName());

        write(res.getBytes());
    }

    public void sendGraph(String address) {
        sendGraph(address, mGraph);
    }

    public void sendText(String message, String address) {
        String res = ChatProtocol.createMessage("TEXT", message, address, mSocketFactory.getAddress(), mSocketFactory.getName()); // String.format("DESTINATION:%s;TYPE:TEXT;%s", address, message);

        write(res.getBytes());
    }

    public void sendText(String message) {
        sendText(message, "*");
    }

    public void sendFile(File f) {
        sendFile(f, "*");
    }

    public void sendFile(File f, String address) {
        BufferedReader br = null;
        StringBuilder sb = null;

        try {
            br = new BufferedReader(new FileReader(f));
            sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }

            br.close();

            byte[] fileBytes = sb.toString().getBytes();

            String headerMessage = ChatProtocol.createFileMessage(fileBytes, f.getName(), address, mSocketFactory.getAddress(), mSocketFactory.getName());
            write(headerMessage.getBytes());

            int pieceSize = 100;

            for (int pieceStart = 0; pieceStart < fileBytes.length; pieceStart += pieceSize) {
                byte[] filePiece = new byte[pieceSize];
                System.arraycopy(fileBytes, pieceStart, filePiece, 0, pieceSize);
                String msg = ChatProtocol.createFilePieceMessage(filePiece, f.getName(), address, mSocketFactory.getAddress(), mSocketFactory.getName());

                write(msg.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        ChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        ChatService.this.start();
    }

    public void disconnect() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final IServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            mmServerSocket = mSocketFactory.createServerSocket();
        }

        public void run() {
            Log.d(TAG, "Socket Type: " + mSocketType + "BEGIN mAcceptThread" + this);
            setName("AcceptThread" + mSocketType);

            mmServerSocket.listen();

            IClientSocket socket;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                socket = mmServerSocket.accept();

                // If a connection was accepted
                if (socket != null) {
                    synchronized (ChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                socket.close();
                                break;
                        }
                    }
                }
            }

            Log.i(TAG, "END mAcceptThread, socket Type: " + mSocketType);
        }

        public void cancel() {
            Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            mmServerSocket.close();
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final IClientSocket mmSocket;
        private final String mmRemoteAddress;
        private String mSocketType;

        public ConnectThread(String remoteAddress, boolean secure) {
            mmRemoteAddress = remoteAddress;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            mmSocket = mSocketFactory.createClientSocket();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect(mmRemoteAddress);
            } catch (IOException e) {
                // Close the socket
                mmSocket.close();
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (ChatService.this) {
                mConnectThread = null;
            }

            sendGraph(mmSocket.getAddress(), mGraph);

            // Start the connected thread
            connected(mmSocket, mSocketType);
        }

        public void cancel() {
            mmSocket.close();
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final IClientSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(IClientSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

            sendGraph(socket.getAddress(), mGraph);

            mNeighbours.add(socket);

            String myAddress = mSocketFactory.getAddress();

            if (myAddress != null) {
                if (!mGraph.containsKey(myAddress)) {
                    mGraph.put(myAddress, new ArrayList<String>());
                }

                mGraph.get(myAddress).add(socket.getAddress());
            }
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // handleIncome(buffer, bytes);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    // Start the service over to restart listening mode
                    ChatService.this.start();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
                mmOutStream.flush();

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            mmSocket.close();
        }

        protected void handleIncome(byte[] buffer, int bytes) {
            String messageText = new String(buffer, 0, bytes);

            ChatMessage message = ChatProtocol.parseMessage(messageText);
            List<String> route = message.getRoute();

            String destination = route.get(route.size() - 1);

            String myAddress = mSocketFactory.getAddress();

            if (destination.compareTo("*") == 0) {
                if (message.isGraph()) {
                    Map<String, List<String>> graph = message.getGraph();

                    if (!ChatRouter.compareGraphs(graph, mGraph)) {
                        graph = ChatRouter.mergeGraphs(mGraph, graph);
                    }

                    for (ISocket socket : mNeighbours) {
                        sendGraph(socket.getAddress(), graph);
                    }
                } else if (message.isText()) {
                    String fromAddress = message.getSenderAddress();

                    for (ISocket socket : mNeighbours) {
                        String address = socket.getAddress();

                        if (address.compareTo(fromAddress) == 0) {
                            continue;
                        }

                        // sendRaw(message, address);
                        sendText(message.getContent(), address);
                    }
                }
            } else {
                for (int i = 0; i < route.size(); i++) {
                    if (route.get(i).compareTo(myAddress) == 0) {
                        if (i == route.size() - 1) {
                            return;
                        }

                        String target = route.get(i + 1);
                        // sendRaw(message, target);
                    }
                }
            }
        }
    }
}
