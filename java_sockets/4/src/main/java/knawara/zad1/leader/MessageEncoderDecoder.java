package knawara.zad1.leader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.zip.CRC32;

/**
 * Created by blueeyedhush on 22.03.16.
 */
public class MessageEncoderDecoder {
    public static final int MAX_MESG_LEN = 1 /* sender len */ + 4 /* utf8 */ * 255 + 1 + 4*255 + 2 + 4;
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageEncoderDecoder.class.getSimpleName());

    public static Optional<ByteBuffer> encode(Message m) {
        ByteBuffer sender = CHARSET.encode(m.sender);
        int senderLen = sender.remaining();
        if(senderLen > 255) {
            LOGGER.error("Sender field is too long");
            return Optional.empty();
        }

        ByteBuffer message = CHARSET.encode(m.message);
        int messageLen = message.remaining();
        if(messageLen > 255) {
            LOGGER.error("Message field is too long");
            return Optional.empty();
        }

        if(m.timestampHour < Byte.MIN_VALUE || m.timestampHour > Byte.MAX_VALUE) {
            LOGGER.error("Messages hour field out of allowed range");
            return Optional.empty();
        }

        if(m.timestampMinute < Byte.MIN_VALUE || m.timestampMinute > Byte.MAX_VALUE) {
            LOGGER.error("Messages minute field out of allowed range");
            return Optional.empty();
        }

        ByteBuffer encodedMessage = ByteBuffer.allocate(1 + senderLen + 1 + messageLen + 2 + 4);
        encodedMessage.put(packLen(senderLen));
        encodedMessage.put(sender);
        encodedMessage.put(packLen(messageLen));
        encodedMessage.put(message);
        encodedMessage.put((byte) m.timestampHour);
        encodedMessage.put((byte) m.timestampMinute);

        byte[] forCrc = new byte[encodedMessage.position()];
        encodedMessage.rewind();
        encodedMessage.get(forCrc);

        OptionalInt crc = crc32(forCrc);
        if(!crc.isPresent()) return Optional.empty();

        encodedMessage.putInt(crc.getAsInt());

        encodedMessage.flip();
        return Optional.of(encodedMessage);
    }

    public static Optional<Message> decode(ByteBuffer bb) {
        int senderLen = unpackLen(bb.get());
        byte[] senderBytes = new byte[senderLen];
        bb.get(senderBytes);

        int mesgLen = unpackLen(bb.get());
        byte[] mesgBytes = new byte[mesgLen];
        bb.get(mesgBytes);

        int timestampHour = (int) bb.get();
        int timestampMinute = (int) bb.get();

        int dataLen = bb.position();
        int crc = bb.getInt();
        bb.rewind();
        byte[] forCrc = new byte[dataLen];
        bb.get(forCrc);

        if(!checkCrc(forCrc, crc)) {
            LOGGER.warn("CRC error");
            return Optional.empty();
        }

        String sender = new String(senderBytes, CHARSET);
        String mesg = new String(mesgBytes, CHARSET);

        return Optional.of(new Message(sender, mesg, timestampHour, timestampMinute));
    }

    private static byte packLen(int len) {
        assert len >= 0 && len <= 255;
        return (byte) (Byte.MIN_VALUE + len);
    }

    private static int unpackLen(byte len) {
        return ((int) len) - Byte.MIN_VALUE;
    }

    private static OptionalInt crc32(byte[] buff) {
        CRC32 crc = new CRC32();
        crc.update(buff);

        long signedCrc = Integer.MIN_VALUE + crc.getValue();
        if(signedCrc < Integer.MIN_VALUE || signedCrc > Integer.MAX_VALUE) {
            LOGGER.error("Cannot encode CRC32 to int... original value: {}", crc.getValue());
            return OptionalInt.empty();
        }

        return OptionalInt.of((int) signedCrc);
    }

    private static boolean checkCrc(byte[] bytes, int crc) {
        long unpackedCrc = ((long) crc) - Integer.MIN_VALUE;

        CRC32 dataCrc = new CRC32();
        dataCrc.update(bytes);

        return dataCrc.getValue() == unpackedCrc;
    }
}
