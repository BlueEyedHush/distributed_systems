package knawara.zad1.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static void main(String[] args) {
        ChatMain app = new ChatMain();
        app.run(args);
    }

    private void run(String[] args) {
        if(!parseArgs(args)) return;

        ConsoleIO io = createConsoleIo();
        startCommsThread(io);

        io.start();
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

    private ConsoleIO createConsoleIo() {
        return new ConsoleIO( s -> {
            LocalDateTime now = LocalDateTime.now();
            Message m = new Message(username, s, now.getHour(), now.getMinute());
            scomms.enqueueMessage(m);
        });
    }

    private void startCommsThread(ConsoleIO io) {
        scomms = new Communication(m -> {
            String msg;
            if(m.isPresent()) {
                msg = MessageFormatter.messageToString(m.get());
            } else {
                msg = "Message Error";
            }

            io.enqueueAsyncMessage(msg);
        }, bindPort, sendPort);
        Thread runner = new Thread(scomms, "server_comms");
        runner.setDaemon(true);
        runner.start();
    }
}
