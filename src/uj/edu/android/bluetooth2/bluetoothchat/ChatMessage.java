package uj.edu.android.bluetooth2.bluetoothchat;

import android.bluetooth.BluetoothDevice;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Map;

/**
 * Created by shybovycha on 08.02.15.
 */
public class ChatMessage {
    protected String type;
    protected List<String> route;
    protected String senderAddress;
    protected String senderName;
    protected String content;
    protected String fileName;
    private Map<String, List<String>> graph;

    public ChatMessage() {
        this.type = "UNKNOWN";
        this.content = "";
    }

    public ChatMessage(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public ChatMessage(String type, String content, BluetoothDevice sender) {
        this.type = type;
        this.content = content;
        this.senderAddress = sender.getAddress();
        this.senderName = sender.getName();
    }

    public ChatMessage(File file, BluetoothDevice sender) {
        this.type = "FILE";
        this.fileName = file.getName();
        this.senderAddress = sender.getAddress();
        this.senderName = sender.getName();

        BufferedReader br = null;
        StringBuilder sb = null;

        try {
            br = new BufferedReader(new FileReader(file));
            sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append("\n");
                line = br.readLine();
            }

            br.close();

            this.content = Base64.encodeToString(sb.toString().getBytes(), Base64.DEFAULT);
        } catch (Exception e) {
            this.content = "";
        }
    }

    public List<String> getRoute() {
        return route;
    }

    public void setRoute(List<String> route) {
        this.route = route;
    }

    public boolean isFile() {
        return type.compareTo("FILE") == 0;
    }

    public boolean isText() {
        return type.compareTo("TEXT") == 0;
    }

    public boolean isGraph() {
        return type.compareTo("GRAPH") == 0;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getContent() {
        return content;
    }

    public String getFileName() {
        return fileName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileContent() {
        if (!isFile())
            return null;

        return Base64.decode(this.content, Base64.DEFAULT);
    }

    public void setGraph(Map<String, List<String>> graph) {
        this.graph = graph;
    }

    public Map<String, List<String>> getGraph() {
        return this.graph;
    }
}
