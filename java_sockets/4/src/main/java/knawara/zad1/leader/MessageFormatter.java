package knawara.zad1.leader;

/**
 * Created by blueeyedhush on 22.03.16.
 */
public class MessageFormatter {
    public static String messageToString(Message m) {
        return String.format("%s @ %d:%d: %s", m.sender, m.timestampHour, m.timestampMinute, m.message);
    }
}
