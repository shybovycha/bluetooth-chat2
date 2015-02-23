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

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import uj.edu.android.bluetooth2.R;
import uj.edu.android.bluetooth2.common.logger.Log;
import uj.edu.android.bluetooth2.sockets.BluetoothSocketFactory;
import uj.edu.android.bluetooth2.sockets.ISocketFactory;
import uj.edu.android.bluetooth2.sockets.TcpSocketFactory;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_SELECT_FILE = 4;

    private static final int CHAT_OVER_BLUETOOTH = 16;
    private static final int CHAT_OVER_WIFI = 17;

    private static final String CHAT_SERVICE = "mChatService";

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private ChatService mChatService = null;

//    @Override
//    public void onSaveInstanceState(Bundle savedInstanceState) {
//        savedInstanceState.putSerializable(CHAT_SERVICE, mChatService);
//
//        super.onSaveInstanceState(savedInstanceState);
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (savedInstanceState != null) {
//            mChatService = (ChatService) savedInstanceState.getSerializable(CHAT_SERVICE);
//        }

        setHasOptionsMenu(true);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available. Trying to use WiFi", Toast.LENGTH_LONG).show();
            // activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat(CHAT_OVER_BLUETOOTH);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == ChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat(int mode) {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();
                    sendMessage(message);
                }
            }
        });

        ISocketFactory socketFactory = null;

        if (mode == CHAT_OVER_BLUETOOTH) {
            socketFactory = new BluetoothSocketFactory(BluetoothAdapter.getDefaultAdapter());
        } else if (mode == CHAT_OVER_WIFI) {
            socketFactory = new TcpSocketFactory(5432);
        }

        mChatService = new ChatService(getActivity(), mHandler, socketFactory);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    /**
     * Makes this device discoverable.
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != ChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            if (message.toLowerCase().compareTo("graph") == 0) {
                mChatService.sendGraph("*");
            } else {
                // Get the message bytes and tell the BluetoothChatService to write
                mChatService.sendText(message);

                // Reset out string buffer to zero and clear the edit text field
                mOutStringBuffer.setLength(0);
                mOutEditText.setText(mOutStringBuffer);
            }
        }
    }

    private void sendFile(String filePath) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != ChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
            // Get the message bytes and tell the ChatService to write
//            byte[] send = message.getBytes();
        mChatService.sendFile(new File(filePath));

        // Reset out string buffer to zero and clear the edit text field
        mOutStringBuffer.setLength(0);
        mOutEditText.setText(mOutStringBuffer);
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the ChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case ChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case ChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case ChatService.STATE_LISTEN:
                        case ChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    handleOutcome(writeMessage);
                    // mConversationArrayAdapter.add("Me:  " + content);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    handleIncome(readBuf, msg.arg1);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat(CHAT_OVER_BLUETOOTH);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled. Switching to WiFi.");
                    Toast.makeText(getActivity(), "BT not enabled. Switching to WiFi.",
                            Toast.LENGTH_SHORT).show();
                    // getActivity().finish();
                    setupChat(CHAT_OVER_WIFI);
                }
                break;
            case REQUEST_SELECT_FILE:
                if (resultCode == Activity.RESULT_OK) {
                    // Get the Uri of the selected file
                    Uri uri = data.getData();
                    android.util.Log.d(TAG, "File Uri: " + uri.toString());
                    // Get the path
                    String path = getPath(getActivity(), uri);
                    android.util.Log.d(TAG, "File Path: " + path);
                    // Get the file instance
                    // File file = new File(path);
                    // Initiate the upload
                    sendFile(path);
                }
                break;
        }
    }

    private static String getPath(Context context, Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Establish connection with other divice
     *
     * @param data   An {@link android.content.Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        // BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(address, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.disconnect: {
                disconnect();
            }
            case R.id.send_file: {
                // Ensure this device is discoverable by others
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                try {
                    Intent fileChooser = Intent.createChooser(intent, "Select a File to Upload");
                    startActivityForResult(fileChooser, REQUEST_SELECT_FILE);
                } catch (android.content.ActivityNotFoundException ex) {
                    // Potentially direct the user to the Market with a Dialog
                    Log.e(TAG, "Please install a File Manager.");
                }
                return true;
            }
        }
        return false;
    }

    private void disconnect() {
        mChatService.disconnect();
    }

    private void handleOutcome(String messageStr) {
        if (messageStr.length() < 1)
            return;

        ChatMessage message = ChatProtocol.parseMessage(messageStr);

        if (message.isText()) {
            String content = message.getContent();

            // Toast.makeText(getActivity(), String.format("T<%s> to <%s>: %s", parts.get("messageType"), destination, parts.get("content")), Toast.LENGTH_LONG).show();

            mConversationArrayAdapter.add("Me: " + content);
        } else if (message.isFile()) {
            mConversationArrayAdapter.add(String.format("Sent file %s", message.getFileName()));
        }
    }

    private void handleIncome(byte[] buffer, int bytes) {
        if (bytes < 1)
            return;

        try {
            String messageStr = new String(buffer, 0, bytes);

            ChatMessage message = ChatProtocol.parseMessage(messageStr);
            List<String> route = message.getRoute();

            String destination = "*";

            if (route != null) {
                destination = route.get(route.size() - 1);
            }

            if (message.isText()) {
                String content = message.getContent();

                // String myAddress = mBluetoothAdapter.getAddress();

                // Toast.makeText(getActivity(), String.format("T<%s> to <%s>: %s", messageType, destination, content), Toast.LENGTH_LONG).show();

                mConversationArrayAdapter.add(String.format("%s: %s", message.getSenderName(), content));
            } else if (message.isFile()) {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/bluetooth2/received_files");
                dir.mkdirs();
                File file = new File(dir, message.getFileName());

                if (!file.exists())
                    file.createNewFile();

                FileOutputStream f = new FileOutputStream(file);
                DataOutputStream writer = new DataOutputStream(f);
                writer.write( message.getFileContent());
                writer.flush();
                writer.close();

                mConversationArrayAdapter.add(String.format("Saving file to %s", file.getPath()));
            } else if (message.isFilePiece()) {
                File sdCard = Environment.getExternalStorageDirectory();
                File dir = new File (sdCard.getAbsolutePath() + "/bluetooth2/received_files");
                dir.mkdirs();
                File file = new File(dir, message.getFileName());

                if (!file.exists()) {
                    mConversationArrayAdapter.add(String.format("Could not save file %s", file.getPath()));
                    return;
                }

                FileOutputStream f = new FileOutputStream(file, true);
                DataOutputStream writer = new DataOutputStream(f);
                writer.write( message.getFileContent());
                writer.flush();
                writer.close();

                // mConversationArrayAdapter.add(String.format("File saved to %s", file.getPath()));
            }
        } catch (Exception ex) {
            Toast.makeText(getActivity(), String.format("%s::%s", ex.getClass().getName(), ex.getStackTrace()[0].toString()), Toast.LENGTH_LONG).show();
        }
    }
}
