package knawara.zad1.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class ConsoleIO {
    private static final int ASYNC_CAPACITY = 100;
    private static final String PROMPT = "mesg> ";
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleIO.class.getSimpleName());

    private final Consumer<String> inputHandler;

    private PrintStream out = wrapSystemOut();
    private InputStreamReader in = wrapSystemIn();
    private BlockingQueue<String> asyncMessages = new LinkedBlockingQueue<>(ASYNC_CAPACITY);
    private StringBuilder lineBuffer = new StringBuilder();

    public ConsoleIO(Consumer<String> inputHandler) {
        this.inputHandler = inputHandler;
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
            if(line.isPresent()) {
                quit = handleUserInput(line.get());
            }
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

    private boolean printAsyncMessages() {
        boolean atLeastOne = false;
        for(String msg = asyncMessages.poll(); msg != null; msg = asyncMessages.poll()) {
            out.printf("\n%s", msg);
            atLeastOne = true;
        }
        return atLeastOne;
    }

    private Optional<String> appendNonBlockingly(StringBuilder buf) {
        String ls = System.lineSeparator();
        String fullLine = null;
        try {
            while(in.ready()) {
                buf.append((char) in.read());

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
        out.printf("\n%s", lineBuffer.toString());
    }

    private boolean handleUserInput(String input) {
        String processedInput = input
                .substring(PROMPT.length())
                .trim();

        if(processedInput.equalsIgnoreCase("quit")) {
            return true;
        } else {
            inputHandler.accept(processedInput);
            return false;
        }
    }

    private static PrintStream wrapSystemOut() {
        try {
            return new PrintStream(System.out, true, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Unsupported encoding depite the fact, that name is taken from StandardCharsets...");
        }
        return System.out;
    }

    private static InputStreamReader wrapSystemIn() {
        return new InputStreamReader(System.in, StandardCharsets.UTF_8);
    }
}
