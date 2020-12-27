package core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import core.schedule.MainScheduler;
import core.util.ExceptionUtil;
import org.checkerframework.checker.units.qual.C;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class CustomWebSocketServer extends WebSocketServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(CustomWebSocketServer.class);
    private final Random r = new Random();

    private final HashMap<String, BiFunction<String, JSONObject, JSONObject>> eventHandlers = new HashMap<>();
    private final ArrayList<BiFunction<String, ClientHandshake, Boolean>> connectedHandlers = new ArrayList<>();
    private final ArrayList<Function<String, Boolean>> disconnectedHandlers = new ArrayList<>();
    private final HashMap<Integer, CompletableFuture<JSONObject>> outCache = new HashMap<>();
    private final BiMap<String, WebSocket> socketBiMap = HashBiMap.create();

    public CustomWebSocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        LOGGER.info("Web socket connected");
        String socketId = clientHandshake.getFieldValue("socket_id");
        socketBiMap.put(socketId, webSocket);
        connectedHandlers.removeIf(connectedHandlerFunction -> connectedHandlerFunction.apply(socketId, clientHandshake));
    }

    /* returns true if it should be removed immediately after */
    public void addConnectedHandler(BiFunction<String, ClientHandshake, Boolean> function) {
        connectedHandlers.add(function);
    }

    public void removeConnectedHandler(BiFunction<String, ClientHandshake, Boolean> function) {
        connectedHandlers.remove(function);
    }

    public void addDisconnectedHandler(Function<String, Boolean> function) {
        disconnectedHandlers.add(function);
    }

    public void removeDisconnectedHandler(Function<String, Boolean> function) {
        disconnectedHandlers.remove(function);
    }

    public void addEventHandler(String event, BiFunction<String, JSONObject, JSONObject> eventFunction) {
        eventHandlers.put(event, eventFunction);
    }

    public void removeEventHandler(String event) {
        eventHandlers.remove(event);
    }

    public void removeEventHandler(String event, BiFunction<String, JSONObject, JSONObject> eventFunction) {
        eventHandlers.remove(event, eventFunction);
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        LOGGER.info("Web socket disconnected");
        String socketId = socketBiMap.inverse().remove(webSocket);
        disconnectedHandlers.removeIf(disconnectedHandlerFunction -> disconnectedHandlerFunction.apply(socketId));
    }

    public boolean isConnected(String socketId) {
        return socketBiMap.containsKey(socketId);
    }

    public List<CompletableFuture<JSONObject>> sendBroadcast(String event, JSONObject content) {
        ArrayList<CompletableFuture<JSONObject>> futureList = new ArrayList<>();
        socketBiMap.values().forEach(webSocket -> futureList.add(send(webSocket, event, content)));
        return futureList;
    }

    public CompletableFuture<JSONObject> sendSecure(String socketId, String event, JSONObject content) {
        WebSocket webSocket = socketBiMap.get(socketId);
        if (webSocket != null) {
            return send(webSocket, event, content);
        } else {
            CompletableFuture<JSONObject> future = new CompletableFuture<>();
            addConnectedHandler((sId, clientHandshake) -> {
                if (sId.equals(socketId)) {
                    send(socketBiMap.get(socketId), event, content)
                            .exceptionally(e -> {
                                future.completeExceptionally(e);
                                return null;
                            })
                            .thenAccept(future::complete);
                    return true;
                }
                return false;
            });
            return future;
        }
    }

    public synchronized CompletableFuture<JSONObject> send(String socketId, String event, JSONObject content) {
        WebSocket webSocket = socketBiMap.get(socketId);
        if (webSocket != null)
            return send(webSocket, event, content);

        return CompletableFuture.failedFuture(new NoSuchElementException("Invalid socketId"));
    }

    public synchronized CompletableFuture<JSONObject> send(WebSocket webSocket, String event, JSONObject content) {
        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        int id = r.nextInt();

        content.put("request_id", id);
        try {
            webSocket.send(event + "::" + content.toString());
        } catch (Throwable e) {
            future.completeExceptionally(e);
            return future;
        }
        outCache.put(id, future);

        MainScheduler.getInstance().schedule(5, ChronoUnit.SECONDS, "websocket_" + event, () -> {
            if (outCache.containsKey(id)) {
                outCache.remove(id)
                        .completeExceptionally(new SocketTimeoutException("No response"));
            }
        });

        return future;
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        String event = message.split("::")[0];
        JSONObject contentJson = new JSONObject(message.substring(event.length() + 2));
        
        int requestId = contentJson.getInt("request_id");
        contentJson.remove("request_id");
        
        if (outCache.containsKey(requestId)) {
            outCache.remove(requestId)
                    .complete(contentJson);
        } else {
            BiFunction<String, JSONObject, JSONObject> eventFunction = eventHandlers.get(event);
            if (eventFunction != null) {
                Thread t = new CustomThread(() -> {
                    JSONObject responseJson = eventFunction.apply(socketBiMap.inverse().get(webSocket), contentJson);
                    if (responseJson == null) responseJson = new JSONObject();
                    responseJson.put("request_id", requestId);
                    webSocket.send(event + "::" + responseJson.toString());
                }, "websocket_" + event);

                MainScheduler.getInstance().schedule(2, ChronoUnit.SECONDS, "websocket_" + event + "_observer", () -> {
                    if (t.isAlive()) {
                        Exception e = ExceptionUtil.generateForStack(t);
                        LOGGER.error("websocket_" + event + " took too long to respond!", e);
                        t.interrupt();
                    }
                });
                t.start();
            }
        }
    }

    public Set<String> getSocketIds() {
        return socketBiMap.keySet();
    }

    public int size() {
        return socketBiMap.size();
    }

    @Override
    public void onError(WebSocket webSocket, Exception ex) {
        LOGGER.error("Web socket error", ex);
    }

    @Override
    public void onStart() {
        //Ignore
    }

}
