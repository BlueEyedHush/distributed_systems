package knawara.zad1.chat;

/**
 * Created by blueeyedhush on 22.03.16.
 */
public class Message {
    public final String sender;
    public final String message;
    public final int timestampHour;
    public final int timestampMinute;

    public Message(String sender, String message, int timestampHour, int timestampMinute) {
        this.sender = sender;
        this.message = message;
        this.timestampHour = timestampHour;
        this.timestampMinute = timestampMinute;
    }
}
