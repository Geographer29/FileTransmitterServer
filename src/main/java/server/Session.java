package server;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class Session {
    private final static int BUFFER_SIZE = 4096;
    private SelectionKey key;
    private SocketChannel channel;
    private static SocketChannel writeChannel;
    private ByteBuffer buffer;

    public Session(SelectionKey key, SocketChannel channel){
        try {
            channel.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Fail to initialize session");
        }

        this.key = key;
        this.channel = channel; // asynchronous/non-blocking
        writeChannel = channel;
        buffer = ByteBuffer.allocateDirect(BUFFER_SIZE); // 64 byte capacity
    }

    void disconnect() {
        Server.clients.remove(key);

        try {
            if (key != null) key.cancel();

            if (channel == null) return;

            System.out.println("User disconnected " + channel.getRemoteAddress());
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to disconnect user");
        }
    }

    void receiveMessage() {
        try {
            channel.read(buffer);
            String result = new String(buffer.array()).trim();
            String[] strings = result.split("\\s");
            ConcurrentLinkedDeque<String> queue = new ConcurrentLinkedDeque<>();

            for (String s: strings){
                queue.push(s);
            }

            BaseHandler authHandler = new BaseHandler();
            authHandler.parseMassage(queue);
            System.out.println("Reading message from client: " + channel.getRemoteAddress().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendMessage(String msg){
        ByteBuffer buffer;
        byte[] bytes = msg.getBytes();

        try {
            buffer = ByteBuffer.allocate(BUFFER_SIZE);
            buffer.get(bytes);
            writeChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to send message");
        }
    }
}