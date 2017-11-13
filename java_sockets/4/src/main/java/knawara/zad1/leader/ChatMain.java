package knawara.zad1.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class ChatMain {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatMain");

    private String username;
    private int bindPort;
    private int sendPort;

    private Communication scomms;
    private LeaderElector lelector;
    private ConsoleIO console;

    public static void main(String[] args) {
        ChatMain app = new ChatMain();
        app.run(args);
    }

    private void run(String[] args) {
        if(!parseArgs(args)) return;

        createConsoleIo();
        lelector = new LeaderElector();
        startCommsThread(console);
        lelector.passArgs(scomms::enqueueLeaderMessage, console::enqueueAsyncMessage);

        console.start();
    }

    private boolean parseArgs(String[] args) {
        if(args.length < 3) {
            LOGGER.error("Not enough args. Should be: <username> <bindPort> <sendPort>");
            return false;
        }

        if(args[0].length() > 6) {
            LOGGER.error("Incorrect username (must be between 1 - 6 chars long)");
            return false;
        }
        username = args[0];

        Optional<Integer> bp = toPort(args[1]);
        if(bp.isPresent()) {
            bindPort = bp.get();
        } else {
            LOGGER.error("Error in bind port");
            return false;
        }

        Optional<Integer> sp = toPort(args[2]);
        if(sp.isPresent()) {
            sendPort = sp.get();
        } else {
            LOGGER.error("Error in send port");
            return false;
        }

        return true;
    }

    private Optional<Integer> toPort(String string) {
        return Optional.of(Integer.valueOf(string, 10))
            .filter(ip -> ip > 0 && ip < 0xffff);
    }

    private void createConsoleIo() {
        console = new ConsoleIO( s -> {
            if(s.trim().equalsIgnoreCase("@elect")) {
                lelector.startElection();
            } else if(!s.trim().isEmpty()){
                LocalDateTime now = LocalDateTime.now();
                Optional<ByteBuffer> encoded = MessageEncoderDecoder.encode(new Message(username, s, now.getHour(), now.getMinute()));
                if(encoded.isPresent()) {
                    scomms.enqueueMessage(encoded.get());
                } else {
                    LOGGER.error("Message encoding error!");
                }
            }
        });
    }

    private void startCommsThread(ConsoleIO io) {
        scomms = new Communication(bb -> {
            Optional<Message> m = MessageEncoderDecoder.decode(bb);
            if(m.isPresent()) {
                Message mm = m.get();
                if(!mm.sender.equals(username)) {
                    io.enqueueAsyncMessage(MessageFormatter.messageToString(mm));
                }
            } else {
                io.enqueueAsyncMessage("Message Error");
            }
        }, lelector::handleMessage, bindPort, sendPort);
        Thread runner = new Thread(scomms, "server_comms");
        runner.setDaemon(true);
        runner.start();
    }
}
