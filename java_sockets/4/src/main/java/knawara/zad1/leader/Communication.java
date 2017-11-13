package knawara.zad1.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class Communication implements Runnable {
    /* unassigned address from ad hoc III block (233.252.124.0 - 233.255.255.255)
    *  http://www.iana.org/assignments/multicast-addresses/multicast-addresses.xhtml
    */
    public static final String CHAT_MULTICAST = "127.0.0.1";
    private static final int IN_MQUEUE_CAPACITY = 100;
    private static final int READ_TIMEOUT = 500; // in ms
    private static final Logger LOGGER = LoggerFactory.getLogger(Communication.class.getSimpleName());

    private final Consumer<ByteBuffer> messageSink;
    private final Consumer<ByteBuffer> leaderSink;
    private final int bindPort;
    private final int sendPort;
    private final BlockingQueue<ByteBuffer> inMQueue = new ArrayBlockingQueue<>(IN_MQUEUE_CAPACITY);
    private final BlockingQueue<ByteBuffer> inLQueue = new ArrayBlockingQueue<>(IN_MQUEUE_CAPACITY);
    private DatagramSocket socket;
    private InetAddress chatAddress;

    public Communication(Consumer<ByteBuffer> messageSink, Consumer<ByteBuffer> leaderSink, int bindPort, int sendPort) {
        this.messageSink = messageSink;
        this.leaderSink = leaderSink;
        this.bindPort = bindPort;
        this.sendPort = sendPort;
    }

    @Override
    public void run() {
        if(!setup()) return;

        boolean stop = false;
        while(!stop) {
            receiveMessages();
            sendEnqueuedMessages();
        }

        cleanup();
    }

    public void enqueueMessage(ByteBuffer m) {
        if(!inMQueue.offer(m)) {
            LOGGER.warn("Message queue overflowing, dropping message");
        }
    }

    public void enqueueLeaderMessage(ByteBuffer m) {
        if(!inLQueue.offer(m)) {
            LOGGER.warn("Leader message queue overflowing, dropping message");
        }
    }

    private boolean setup() {
        try {
            chatAddress = InetAddress.getByName(CHAT_MULTICAST);
        } catch (UnknownHostException e) {
            LOGGER.error("Unknown host", e);
            return false;
        }

        try {
            socket = new DatagramSocket(bindPort);
            socket.setReuseAddress(true);
        } catch (IOException e) {
            LOGGER.error("IOException while creating socket:", e);
            return false;
        }

        /*try {
            socket.joinGroup(chatAddress);
        } catch (IOException e) {
            LOGGER.error("IOException when joining the group", e);
            socket.close();
            return false;
        }*/

        try {
            socket.setSoTimeout(READ_TIMEOUT);
        } catch (SocketException e) {
            LOGGER.error("Exception when setting socket timeout: ", e);
            socket.close();
            return false;
        }

        return true;
    }

    private void prependAndDrainTo(byte prependByte, BlockingQueue<ByteBuffer> src, Set<ByteBuffer> dst) {
        ByteBuffer t = src.poll();
        while(t != null) {
            ByteBuffer n = ByteBuffer.allocate(1 + t.capacity());
            n.put(prependByte);
            n.put(t);
            n.flip();
            dst.add(n);
            t = src.poll();
        }
    }

    private void sendEnqueuedMessages() {
        Set<ByteBuffer> messages = new HashSet<>();

        prependAndDrainTo((byte) 0, inMQueue, messages);
        prependAndDrainTo((byte) 1, inLQueue, messages);

        messages.stream()
                .map(bb -> {
                    byte[] bytes = new byte[bb.remaining()];
                    bb.get(bytes);
                    return bytes;
                })
                .map(bytes -> new DatagramPacket(bytes, bytes.length))
                .map(p -> {
                    p.setAddress(chatAddress);
                    p.setPort(sendPort);
                    return p;
                })
                .forEach(p -> {
                    try {
                        socket.send(p);
                    } catch (IOException e) {
                        LOGGER.error("Error when sending message: ", e);
                    }
                });
    }

    private void receiveMessages() {
        boolean avaliable = true;
        while(avaliable) {
            byte[] buff = new byte[MessageEncoderDecoder.MAX_MESG_LEN];
            DatagramPacket packet = new DatagramPacket(buff, MessageEncoderDecoder.MAX_MESG_LEN);
            try {
                socket.receive(packet);
            } catch (SocketTimeoutException e) {
                avaliable = false;
            } catch (IOException e) {
                LOGGER.error("Error during message receiving: ", e);
                avaliable = false;
            }

            if(avaliable) {
                byte[] data = packet.getData();
                if(data.length < 1) {
                    LOGGER.error("Empty packet, dropping");
                } else {
                    byte messageType = data[0];
                    byte[] message = Arrays.copyOfRange(data, 1, data.length);

                    if(messageType == 0) {
                        messageSink.accept(ByteBuffer.wrap(message));
                    } else if(messageType == 1) {
                        leaderSink.accept(ByteBuffer.wrap(message));
                    } else {
                        LOGGER.error("Unknown message type {}. Dropping.", (int) messageType);
                    }
                }
            }
        }
    }

    private void cleanup() {
        if(socket != null) {
            socket.close();
        }
    }
}
