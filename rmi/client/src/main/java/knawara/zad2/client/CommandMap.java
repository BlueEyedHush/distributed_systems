package knawara.zad2.client;

import knawara.zad2.common.exceptions.AlreadyOccupiedException;
import knawara.zad2.common.exceptions.NotYourTurnException;
import knawara.zad2.common.exceptions.OutOfBoardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class CommandMap {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandMap.class.getSimpleName());

    public static final Map<Pattern, Consumer<Matcher>> REGEX_T0_COMMAND = new HashMap<>();
    static {
        i(p("move ([0-9]*) ([0-9]*)"), m -> {
            int x = Integer.valueOf(m.group(1));
            int y = Integer.valueOf(m.group(2));
            LOGGER.debug("args: {} {}", x, y);
            Printer prt = ClientMain.get().getPrinter();
            try {
                ClientMain.get().getServerCommunication().tryMove(x,y);
            } catch(OutOfBoardException e) {
                prt.moveOutOfBoard();
            } catch(AlreadyOccupiedException e) {
                prt.placeAlreadyOccupied();
            } catch(NotYourTurnException e) {
                prt.notYourTurnBuddy();
            } catch(RuntimeException e) {
                LOGGER.error("Attempt to submit move failed", e);
                prt.fatalError();
            }
        });

        i(p("leave"), m -> {
            ClientMain.get().getServerCommunication().leave();
        });

        i(p("print ([0-9]*) ([0-9]*) ([0-9]*)"), m -> {
            int x = Integer.valueOf(m.group(1));
            int y = Integer.valueOf(m.group(2));
            int size = Integer.valueOf(m.group(3));
            String board = "";
            BoardThreadSafeProxy b = ClientMain.get().getBoard();
            Printer prt = ClientMain.get().getPrinter();
            if(b != null && prt != null) {
                prt.any(b.printExcerpt(x, y, size));
            }
        });
    }

    private static void i(Pattern p, Consumer<Matcher> c) {REGEX_T0_COMMAND.put(p,c);}
    private static Pattern p(String s) {return Pattern.compile(s);}
}
