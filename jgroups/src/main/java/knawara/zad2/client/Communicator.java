package knawara.zad2.client;

import com.google.protobuf.InvalidProtocolBufferException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.*;
import org.jgroups.stack.ProtocolStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static pl.edu.agh.dsrg.sr.chat.protos.ChatOperationProtos.*;


import java.net.UnknownHostException;
import java.util.function.BiConsumer;

public final class Communicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Communicator.class.getSimpleName());

    private static final int MIN_ROOMID = 1;
    private static final int MAX_ROOMID = 200;

    private TriConsumer<Integer, String, String> onIncomingMessage;
    private String username;
    private JChannel[] channels = new JChannel[MAX_ROOMID+1];

    public Communicator(String username) {
        this.username = username;
    }

    public void setOnIncomingMessage(TriConsumer<Integer, String, String> onIncomingMessage) {
        this.onIncomingMessage = onIncomingMessage;
    }

    public void join(int roomId) throws LibException, CustomException {
        throwIfInvalidId(roomId);

        if(channels[roomId] == null) {
            JChannel channel = new JChannel(false);
            setupProtocols(channel, roomId);

            channel.setName(username);
            try {
                channel.connect(Integer.toString(roomId));
            } catch (Exception e) {
                throw new CustomException("cannot connect channel", e);
            }

            channel.setReceiver(new ReceiverAdapter() {
                @Override
                public void receive(Message msg) {
                    if(onIncomingMessage != null) {
                        String name = channel.getName(msg.getSrc());

                        if(!name.equals(username)) {
                            try {
                                ChatMessage mesg = ChatMessage.parseFrom(msg.getRawBuffer());
                                onIncomingMessage.accept(roomId, name, mesg.getMessage());
                            } catch (InvalidProtocolBufferException e) {
                                LOGGER.error("ProtocolBuffers deserialization exception", e);
                            }
                        }
                    }
                }
            });

            channels[roomId] = channel;
        } else {
            throw new CustomException("Already member of the room");
        }
    }

    public void leave(int roomId) {
        throwIfInvalidId(roomId);

        if(channels[roomId] != null) {
            channels[roomId].close();
            channels[roomId] = null;
        } else {
            throw new CustomException("You weren't member of channel " + roomId);
        }
    }

    public void send(int roomId, String message) {
        throwIfInvalidId(roomId);

        JChannel channel = channels[roomId];
        if(channel != null) {
            ChatMessage source = ChatMessage.newBuilder()
                    .setMessage(message)
                    .build();

            Message msg = new Message(null, null, source.toByteArray());
            try {
                channel.send(msg);
            } catch (Exception e) {
                throw new CustomException("Failure when trying to send the following message: \n" + message);
            }
        } else {
            throw new CustomException("You must join channel before sending to it");
        }
    }

    private static void throwIfInvalidId(int roomId) {
        if(roomId < MIN_ROOMID || roomId > MAX_ROOMID) {
            throw new IndexOutOfBoundsException("roomId out of range");
        };
    }

    private static void setupProtocols(JChannel channel, int roomId) throws LibException {
        try {
            ProtocolStack stack = new ProtocolStack();
            channel.setProtocolStack(stack);
            stack.addProtocol(new UDP().setValue("mcast_group_addr",Addressing.getInetAddressForRoom(roomId)))
                    .addProtocol(new PING())
                    .addProtocol(new MERGE2())
                    .addProtocol(new FD_SOCK())
                    .addProtocol(new FD_ALL().setValue("timeout", 12000).setValue("interval", 3000))
                    .addProtocol(new VERIFY_SUSPECT())
                    .addProtocol(new BARRIER())
                    .addProtocol(new NAKACK())
                    .addProtocol(new UNICAST2())
                    .addProtocol(new STABLE())
                    .addProtocol(new GMS())
                    .addProtocol(new UFC())
                    .addProtocol(new MFC())
                    .addProtocol(new FRAG2())
                    .addProtocol(new STATE_TRANSFER())
                    .addProtocol(new FLUSH());

            stack.init();
        } catch (UnknownHostException e) {
            throw new LibException("Java cannot convert string representing room address to InetAddress", e);
        } catch (Exception e) {
            throw new LibException("JGroups protocol stack initialization failed", e);
        }
    }
}
