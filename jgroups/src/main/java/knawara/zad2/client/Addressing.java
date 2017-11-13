package knawara.zad2.client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Addressing {
    private static final String ADRESS_PREFIX = "230.0.0.";

    public static String getAddressForRoom(int roomId) {
        if(roomId < 0 || roomId > 255) {
            throw new IllegalArgumentException("This address format can only support rooms with ids from <0, 255>");
        }

        return ADRESS_PREFIX + Integer.toString(roomId);
    }

    public static InetAddress getInetAddressForRoom(int roomId) throws UnknownHostException {
        return InetAddress.getByName(getAddressForRoom(roomId));
    }
}
