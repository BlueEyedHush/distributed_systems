package knawara.zad2.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class ConsoleIO {
    private static final int ASYNC_CAPACITY = 100;
    private static final String PROMPT = "> ";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleIO.class.getSimpleName());
    private static final String COMMAND_UNRECOGNIZED = "No such command";
    private CommandHandler commandHandler;

    private BlockingQueue<String> asyncMessages = new LinkedBlockingQueue<>(ASYNC_CAPACITY);
    private StringBuilder lineBuffer = new StringBuilder();

    public ConsoleIO() {

    }

    public void start() {
        resetLineBuffer();
        printLineBuffer();

        boolean quit = false;
        Optional<String> line = Optional.empty();
        while(!quit) {
            boolean atLeastOneAsync = printAsyncMessages();
            if(atLeastOneAsync || line.isPresent()) {
                printLineBuffer();
            }
            line = appendNonBlockingly(lineBuffer);
            quit = handleUserInput(line);
        }
    }

    public void enqueueAsyncMessage(String message) {
        boolean retry = true;
        while(retry) {
            try {
                asyncMessages.put(message);
                retry = false;
            } catch (InterruptedException e) {
                LOGGER.warn("unexpected interrupt when waiting to enqueue async message", e);
            }
        }
    }

    public void setCommandHandler(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    private boolean printAsyncMessages() {
        boolean atLeastOne = false;
        for(String msg = asyncMessages.poll(); msg != null; msg = asyncMessages.poll()) {
            System.out.printf("\n%s", msg);
            atLeastOne = true;
        }
        return atLeastOne;
    }

    private Optional<String> appendNonBlockingly(StringBuilder buf) {
        String ls = System.lineSeparator();
        String fullLine = null;
        try {
            int avaliable = System.in.available();
            for(; avaliable > 0; avaliable--) {
                buf.append((char) System.in.read());

                if(buf.substring(buf.length() - ls.length()).equals(ls)) {
                    buf.delete(buf.length() - ls.length(), buf.length());
                    fullLine = buf.toString();
                    resetLineBuffer();
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error during console IO: ", e);
        }

        return Optional.ofNullable(fullLine);
    }

    private void resetLineBuffer() {
        lineBuffer.setLength(0);
        lineBuffer.append(PROMPT);
    }

    private void printLineBuffer() {
        System.out.printf("\n%s", lineBuffer.toString());
    }

    private boolean handleUserInput(Optional<String> input) {
        Optional<String> processedInput = input
                .map(i -> i.substring(PROMPT.length()))
                .map(i -> i.trim().toLowerCase());
        if(processedInput.isPresent()) {
            if(processedInput.get().equals("quit")) return true;
            else if(commandHandler != null){
                commandHandler.handleCommand(processedInput.get(), System.out);
            } else {
                System.out.println(COMMAND_UNRECOGNIZED);
            }
        }

        /* none matches */

        return false;
    }
}
