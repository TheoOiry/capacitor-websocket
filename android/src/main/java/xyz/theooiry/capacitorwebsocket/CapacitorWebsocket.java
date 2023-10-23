package xyz.theooiry.capacitorwebsocket;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.neovisionaries.ws.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@CapacitorPlugin(name = "CapacitorWebsocket")
public class CapacitorWebsocket extends Plugin {
    private HashMap<String, SocketConnection> sockets = new HashMap<>();

    @PluginMethod()
    public void build(PluginCall call) throws IOException {
        try {
            String name = call.getString("name");
            if (name == null || name.equals("")) {
                call.reject("Must provide a socket name");
                return;
            }

            sockets.remove(name);

            String url = call.getString("url");
            WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);

            WebSocket socket = factory.createSocket(url);

            socket.clearHeaders();

            JSObject json = call.getObject("headers");
            for (Iterator<String> i = json.keys(); i.hasNext ();) {
                String key = i.next();
                Object val = json.get(key);
                socket.addHeader(key, (String) val);
                System.out.println(String.format("key: %s, value: %s", key, val));
            }

            sockets.put(name, new SocketConnection(socket, name));
            call.resolve();
        } catch (Exception e) {
            call.reject("Couldnt create socket, ", e);
        }
    }

    @PluginMethod()
    public void applyListeners(PluginCall call) {
        SocketConnection socket = getSocket(call);
        if (socket == null) {
            call.reject("Must provide a socket name");
            return;
        }

        try {
            socket.socket.clearListeners();
        } catch (Exception e) {
            call.reject("Couldnt remove listners: ", e);
            return;
        }

        try {
            socket.socket.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    //super.onTextMessage(websocket, message);
                    JSObject ret = new JSObject();
                    ret.put("data", message);
                    notifyListeners(String.format("%s:message", socket.name), ret);
                }

                @Override
                public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
                    //super.onStateChanged(websocket, newState);
                    System.out.println(newState);
                    JSObject ret = new JSObject();
                    ret.put("state", newState);
                    notifyListeners(String.format("%s:statechanged", socket.name), ret);
                }

                @Override
                public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                    //super.onCloseFrame(websocket, frame);
                    System.out.println("close frame");
                    JSObject ret = new JSObject();
                    ret.put("frame", frame);
                    notifyListeners(String.format("%s:closeframe", socket.name), ret);
                }

                @Override
                public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                    //super.onConnected(websocket, headers);
                    System.out.println("connected");
                    JSObject ret = new JSObject();
                    ret.put("headers", headers);
                    notifyListeners(String.format("%s:connected", socket.name), ret);
                }

                @Override
                public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                    //super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
                    System.out.println("disconnected");
                    JSObject ret = new JSObject();
                    ret.put("closedByServer", closedByServer);
                    ret.put("serverCloseFrame", serverCloseFrame);
                    ret.put("clientCloseFrame", clientCloseFrame);
                    notifyListeners(String.format("%s:disconnected", socket.name), ret);
                }

                @Override
                public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
                    //super.onConnectError(websocket, exception);
                    JSObject ret = new JSObject();
                    ret.put("exception", exception);
                    notifyListeners(String.format("%s:connecterror", socket.name), ret);
                }

                @Override
                public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
                    //super.onError(websocket, cause);
                    JSObject ret = new JSObject();
                    ret.put("cause", cause);
                    notifyListeners(String.format("%s:error", socket.name), ret);
                }

                @Override
                public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
                    //super.onMessageError(websocket, cause, frames);
                    JSObject ret = new JSObject();
                    ret.put("cause", cause);
                    ret.put("frame", frames);
                    notifyListeners(String.format("%s:messageerror", socket.name), ret);
                }

                @Override
                public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
                    //super.onSendError(websocket, cause, frame);
                    JSObject ret = new JSObject();
                    ret.put("cause", cause);
                    ret.put("frame", frame);
                    notifyListeners(String.format("%s:senderror", socket.name), ret);
                }

                @Override
                public void onTextMessage(WebSocket websocket, byte[] data) throws Exception {
                    //super.onTextMessage(websocket, data);
                    JSObject ret = new JSObject();
                    ret.put("data", data);
                    notifyListeners(String.format("%s:textmessage", socket.name), ret);
                }

                @Override
                public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
                    //super.onTextMessageError(websocket, cause, data);
                    JSObject ret = new JSObject();
                    ret.put("data", data);
                    ret.put("cause", cause);
                    notifyListeners(String.format("%s:textmessageerror", socket.name), ret);
                }
            });
        } catch (Exception e) {
            call.reject("Couldnt add listners: ", e);
            return;
        }
        call.resolve();
    }

    @PluginMethod()
    public void connect(PluginCall call) throws WebSocketException {
        SocketConnection socket = getSocket(call);
        if (socket == null) {
            call.reject("Must provide a socket name");
            return;
        }

        if (!socket.connected) {
            try {
                socket.socket.connect();
                socket.connected = true;
            } catch (Exception e) {
                call.reject("Couldnt connect, ", e);
                return;
            }
        }
        call.resolve();
    }

    @PluginMethod()
    public void disconnect(PluginCall call) {
        SocketConnection socket = getSocket(call);
        if (socket == null) {
            call.reject("Must provide a socket name");
            return;
        }

        if (socket.connected) {
            try {
                socket.socket.disconnect();
                socket.connected = false;
            } catch (Exception e) {
                call.reject("Couldnt disconnect, ", e);
                return;
            }
        }
        call.resolve();
    }

    @PluginMethod()
    public void send(PluginCall call) {
        SocketConnection socket = getSocket(call);
        if (socket == null) {
            call.reject("Must provide a socket name");
            return;
        }

        try {
            socket.socket.sendText(call.getString("data"));
        } catch (Exception e) {
            call.reject("Error sending, ", e);
            return;
        }

        call.resolve();
    }

    private SocketConnection getSocket(PluginCall call) {
        String name = call.getString("name");
        if (name == null) {
            call.reject("Must provide a socket name");
        } else {
            try {
                SocketConnection ret = sockets.get(name);
                if (ret == null) {
                    call.reject(String.format("Socket '%s' doesnt exist", name));
                } else {
                    return ret;
                }
            } catch (Exception e) {
                call.reject("Exception when trying to get SocketConnection", e);
            }
        }
        return null;
    }
}

class SocketConnection {
    public boolean connected = false;
    public WebSocket socket;
    final String name;
    public SocketConnection(WebSocket _socket, String _name) {
        socket = _socket;
        name = _name;
    }
}