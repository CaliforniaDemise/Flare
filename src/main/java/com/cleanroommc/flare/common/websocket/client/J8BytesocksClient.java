package com.cleanroommc.flare.common.websocket.client;

import com.google.common.collect.ImmutableList;
import com.neovisionaries.ws.client.*;
import me.lucko.bytesocks.client.BytesocksClient;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * @author embeddedt
 */
public class J8BytesocksClient implements BytesocksClient {

    public static J8BytesocksClient create(String host, String userAgent) {
        return new J8BytesocksClient(host, userAgent);
    }

    /* The bytesocks urls */
    private final String httpsUrl;
    private final String wssUrl;

    /** The client user agent */
    private final String userAgent;

    private J8BytesocksClient(String host, String userAgent) {
        this.httpsUrl = "https://" + host + "/";
        this.wssUrl = "wss://" + host + "/";
        this.userAgent = userAgent;
    }

    public String httpsUrl() {
        return httpsUrl;
    }

    public String wssUrl() {
        return wssUrl;
    }

    public String userAgent() {
        return userAgent;
    }

    @Override
    public BytesocksClient.Socket createAndConnect(BytesocksClient.Listener listener) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(this.httpsUrl + "create").openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", this.userAgent);
        if (con.getResponseCode() != 201) {
            throw new RuntimeException(String.format("Request failed: %s [%s]", con.getResponseCode(), con.getResponseMessage()));
        }

        String channelId = null;

        for (Map.Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if (key != null && key.equalsIgnoreCase("Location") && value != null && !value.isEmpty()) {
                channelId = value.get(0);
                if (channelId != null) {
                    break;
                }
            }
        }

        if (channelId == null) {
            throw new RuntimeException("Location header not Returned.");
        }

        return connect(channelId, listener);
    }

    @Override
    public BytesocksClient.Socket connect(String channelId, BytesocksClient.Listener listener) throws Exception {
        WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(5000);
        WebSocket socket = factory.createSocket(URI.create(this.wssUrl + channelId))
                .addHeader("User-Agent", this.userAgent)
                .addListener(new ListenerImpl(listener))
                .connect();

        return new SocketImpl(channelId, socket);
    }

    private static final class SocketImpl implements BytesocksClient.Socket {

        /** Ugly hacks to track sending of websocket **/
        private static final MethodHandle SPLIT_METHOD;

        static {
            try {
                Method m = WebSocket.class.getDeclaredMethod("splitIfNecessary", WebSocketFrame.class);
                m.setAccessible(true);
                SPLIT_METHOD = MethodHandles.lookup().unreflect(m);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        private final String id;
        private final WebSocket ws;
        private final WeakHashMap<WebSocketFrame, CompletableFuture<?>> frameFutures = new WeakHashMap<>();

        private SocketImpl(String id, WebSocket ws) {
            this.id = id;
            this.ws = ws;
            this.ws.addListener(new WebSocketAdapter() {
                @Override
                public void onFrameSent(WebSocket websocket, WebSocketFrame frame) {
                    synchronized (frameFutures) {
                        CompletableFuture<?> future = frameFutures.remove(frame);
                        if (future != null) {
                            future.complete(null);
                        } else {
                            System.err.println("Sent Frame without Associated CompletableFuture.");
                        }
                    }
                }

                @Override
                public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) {
                    synchronized (frameFutures) {
                        CompletableFuture<?> future = frameFutures.remove(frame);
                        if (future != null) {
                            future.completeExceptionally(new Exception("Failed to send Frame."));
                        } else {
                            System.err.println("Received Error without Associated CompletableFuture.");
                        }
                    }
                }
            });
        }

        @Override
        public String getChannelId() {
            return this.id;
        }

        @Override
        public boolean isOpen() {
            return this.ws.isOpen();
        }

        @Override
        public CompletableFuture<?> send(CharSequence msg) {
            WebSocketFrame targetFrame = WebSocketFrame.createTextFrame(msg.toString());
            // Split ourselves so we know what the last frame was
            List<WebSocketFrame> splitFrames;
            try {
                splitFrames = (List<WebSocketFrame>) SPLIT_METHOD.invokeExact(this.ws, targetFrame);
            } catch(Throwable e) {
                throw new RuntimeException(e);
            }
            if (splitFrames == null) {
                splitFrames = ImmutableList.of(targetFrame);
            }
            // FIXME this code is not really that efficient (allocating a whole new CompletableFuture for every frame),
            //  but it's the simplest solution for now and seems to be good enough. We have to track all frames to correctly
            //  report errors/success
            List<CompletableFuture<?>> futures = new ArrayList<>();
            for (WebSocketFrame frame : splitFrames) {
                CompletableFuture<?> future = new CompletableFuture<>();
                synchronized (frameFutures) {
                    frameFutures.put(frame, future);
                }
                futures.add(future);
                this.ws.sendFrame(frame);
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        }

        @Override
        public void close(int statusCode, String reason) {
            this.ws.sendClose(statusCode, reason);
        }
    }

    private static final class ListenerImpl extends WebSocketAdapter {

        private final Listener listener;

        private ListenerImpl(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            this.listener.onOpen();
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            this.listener.onClose(serverCloseFrame.getCloseCode(), serverCloseFrame.getCloseReason());
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            this.listener.onError(cause);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            this.listener.onText(text);
        }

    }

}
