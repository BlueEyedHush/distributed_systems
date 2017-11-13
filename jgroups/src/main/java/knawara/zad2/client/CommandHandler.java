package knawara.zad2.client;

import java.io.PrintStream;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public interface CommandHandler {
    void handleCommand(String cmd, PrintStream out);
}
