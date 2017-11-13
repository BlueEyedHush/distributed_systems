package knawara.zad2.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserInteractions implements CommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserInteractions.class.getSimpleName());
    private static final Pattern SELECT_COMMAND = Pattern.compile("select\\s+(\\d{1,3})");
    private static final Pattern LEAVE_COMMAND = Pattern.compile("leave\\s+(\\d{1,3})");

    private final ConsoleIO io;
    private final Communicator communicator;
    private Deque<Integer> enteredChannels;

    public UserInteractions(ConsoleIO io, Communicator communicator) {
        this.io = io;
        this.communicator = communicator;
        this.enteredChannels = new ArrayDeque<>();
    }

    /**
     *
     * @param cmd assumed to be passed already trimmed
     * @param out
     */
    @Override
    public void handleCommand(String cmd, PrintStream out) {
        Matcher matcher = SELECT_COMMAND.matcher(cmd);
        try {
            if (matcher.matches()) {
                Integer channelId = Integer.valueOf(matcher.group(1), 10);
                if (!enteredChannels.contains(channelId)) {
                    communicator.join(channelId);
                    enteredChannels.addLast(channelId);
                } else {
                /* move to the top */
                    enteredChannels.remove(channelId);
                    enteredChannels.addLast(channelId);
                }
            } else {
                matcher = LEAVE_COMMAND.matcher(cmd);
                if (matcher.matches()) {
                    Integer channelId = Integer.valueOf(matcher.group(1), 10);
                    if (enteredChannels.contains(channelId)) {
                        communicator.leave(channelId);
                        enteredChannels.remove(channelId);
                    } else {
                        out.println("You weren't a member of " + channelId.toString());
                    }
                } else {
                    if (!enteredChannels.isEmpty()) {
                        communicator.send(enteredChannels.peekLast(), cmd);
                    } else {
                        out.println("No channel to post to!");
                    }
                }
            }
        } catch(CustomException e) {
            out.println(e.getMessage());
            /* for debugging purposes */
        } catch(LibException e) {
            LOGGER.error("unrecoverable exception", e);
        } catch(IndexOutOfBoundsException e) {
            out.println("Room out of range");
        }
    }

    public void onIncomingMessage(Integer roomId, String name, String message) {
        String m = String.format("[%d, %s]: %s", roomId, name, message);
        io.enqueueAsyncMessage(m);
    };
}
