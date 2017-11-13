package knawara.zad1.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.HashSet;
import java.util.Optional;
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
    public static final String CHAT_MULTICAST = "233.252.141.220";
    private static final int IN_MQUEUE_CAPACITY = 100;
    private static final int READ_TIMEOUT = 500; // in ms
    private static final Logger LOGGER = LoggerFactory.getLogger(Communication.class.getSimpleName());

    private final Consumer<Optional<Message>> messageSink;
    private final int bindPort;
    private final int sendPort;
    private final BlockingQueue<Message> inMQueue = new ArrayBlockingQueue<Message>(IN_MQUEUE_CAPACITY);
    private MulticastSocket socket;
    private InetAddress chatAddress;

    public Communication(Consumer<Optional<Message>> messageSink, int bindPort, int sendPort) {
        this.messageSink = messageSink;
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

    public void enqueueMessage(Message m) {
        if(!inMQueue.offer(m)) {
            LOGGER.warn("Message queue overflowing, dropping message");
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
            socket = new MulticastSocket(bindPort);
        } catch (IOException e) {
            LOGGER.error("IOException while creating socket:", e);
            return false;
        }

        try {
            socket.joinGroup(chatAddress);
        } catch (IOException e) {
            LOGGER.error("IOException when joining the group", e);
            socket.close();
            return false;
        }

        try {
            socket.setSoTimeout(READ_TIMEOUT);
        } catch (SocketException e) {
            LOGGER.error("Exception when setting socket timeout: ", e);
            socket.close();
            return false;
        }

        return true;
    }

    private void sendEnqueuedMessages() {
        Set<Message> messages = new HashSet<>();
        inMQueue.drainTo(messages);
        messages.stream()
                .map(MessageEncoderDecoder::encode)
                .filter(Optional::isPresent)
                .map(Optional::get)
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
                messageSink.accept(MessageEncoderDecoder.decode(packet.getData()));
            }
        }
    }

    private void cleanup() {
        if(socket != null) {
            socket.close();
        }
    }
}
