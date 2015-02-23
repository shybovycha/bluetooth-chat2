package uj.edu.android.bluetooth2.bluetoothchat;

import android.util.Base64;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by shybovycha on 01.02.15.
 */
public class ChatProtocol {
    public static ChatMessage parseMessage(String message) {
        ChatMessage result = new ChatMessage();
        Pattern r = Pattern.compile("^ROUTE:(([a-zA-Z0-9\\-\\.:,]+)|([*]));TYPE:(TEXT|FILE|FILE_PIECE|GRAPH);(FILE:(.+);)?FROM_ADDR:([a-zA-Z0-9\\.:\\-]+);FROM_NAME:(.+);(.*)");
        Matcher m = r.matcher(message);

        if (!m.find()) {
            return result;
        }

        String messageType = m.group(4);

        result.setType(messageType);
        result.setRoute(parseRoute(m.group(1)));
        result.setSenderAddress(m.group(7));
        result.setSenderName(m.group(8));

        if (result.isFile() || result.isFilePiece()) {
            result.setFileName(m.group(6));
            result.setContent(m.group(9));
        } else if (result.isGraph()) {
            result.setGraph(parseGraph(m.group(9)));
        } else {
            result.setContent(m.group(9));
        }

        return result;
    }

    public static List<String> parseRoute(String route) {
        List<String> result = new ArrayList<String>();

        if (route == null) {
            result.add("?");
            return result;
        }

        String[] entries = route.split(",");

        Collections.addAll(result, entries);

        return result;
    }

    public static Map<String, List<String>> parseGraph(String graph) {
        Map<String, List<String>> result = new TreeMap<String, List<String>>();
        String[] neighbours = graph.split(";");

        for (String pair : neighbours) {
            String[] parts = pair.split(",");

            if (!result.containsKey(parts[0])) {
                result.put(parts[0], new ArrayList<String>());
            }

            result.get(parts[0]).add(parts[1]);
        }

        return result;
    }

    public static String graphToString(Map<String, List<String>> graph) {
        String result = "";

        for (Map.Entry<String, List<String>> e : graph.entrySet()) {
            List<String> neighbours = e.getValue();

            for (String v : neighbours) {
                result += String.format("%s,%s;", e.getKey(), v);
            }
        }

        return result;
    }

    public static String createMessage(String messageType, String content, String route, String fromAddress, String fromName) {
        return String.format("ROUTE:%s;TYPE:%s;FROM_ADDR:%s;FROM_NAME:%s;%s", route, messageType, fromAddress, fromName, content);
    }

    public static String createTextMessage(byte[] content, String fileName, String route, String fromAddress, String fromName) {
        return String.format("ROUTE:%s;TYPE:FILE;FILE:%s;FROM_ADDR:%s;FROM_NAME:%s;%s", route, fileName, fromAddress, fromName, Base64.encodeToString(content, Base64.DEFAULT));
    }

    public static String createFileMessage(byte[] content, String fileName, String route, String fromAddress, String fromName) {
        return String.format("ROUTE:%s;TYPE:FILE;FILE:%s;FROM_ADDR:%s;FROM_NAME:%s;%s", route, fileName, fromAddress, fromName, Base64.encodeToString(content, Base64.DEFAULT));
    }

    public static String createFilePieceMessage(byte[] content, String fileName, String route, String fromAddress, String fromName) {
        return String.format("ROUTE:%s;TYPE:FILE_PIECE;FILE:%s;FROM_ADDR:%s;FROM_NAME:%s;%s", route, fileName, fromAddress, fromName, Base64.encodeToString(content, Base64.DEFAULT));
    }

    public static String createGraphMessage(Map<String, List<String>> graph, String address, String fromAddress, String fromName) {
        String route = String.format("%s,%s;", fromAddress, address);

        return createMessage("GRAPH", graphToString(graph), route, fromAddress, fromName);
    }
}
