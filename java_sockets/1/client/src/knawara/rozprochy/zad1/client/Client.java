package knawara.rozprochy.zad1.client;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by blueeyedhush on 14.03.16.
 *
 * litf this funny 0-255 sending req!
 */
public class Client {
    public static final String SERVER_IP = "127.0.0.1";

    public static void main(String[] argv) {
        if(argv.length < 1) {
            System.out.println("port number must be passed as argument");
            return;
        }

        int port = Integer.valueOf(argv[0]);
        if(port < 0 || port > 65535) {
            System.err.println("Port number not in range");
            return;
        }

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("How long should be the type used for encoding (1,2,4,8)?");
        int encodedTypeWidth;
        try {
            encodedTypeWidth = Integer.valueOf(inFromUser.readLine().trim());
        } catch (IOException e) {
            System.err.println("Error occured while attempting to query user for type width");
            return;
        } catch (NumberFormatException e) {
            System.err.println("Misformed input passed: " + e.getMessage());
            return;
        }

        if(encodedTypeWidth != 1 && encodedTypeWidth != 2 && encodedTypeWidth != 4 && encodedTypeWidth != 8) {
            System.err.println("Width out of range!");
            return;
        }

        System.out.println("Give number to send (must be within chosen width range).");
        long numberToSend;
        try {
            numberToSend = Long.valueOf(inFromUser.readLine().trim());
        } catch (IOException e) {
            System.err.println("Error occured while attempting to query user for number");
            return;
        }

        byte[] buff;
        try {
            buff = encode(encodedTypeWidth, numberToSend);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            return;
        }

        try(Socket clientSocket = new Socket(SERVER_IP, port)) {
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream inFromServer = new DataInputStream(clientSocket.getInputStream());

            outToServer.write(buff);
            int received = inFromServer.readByte();

            System.out.println("Received: " + received);
        } catch (IOException e) {
            System.err.println("Client-server communnication error: " + e.getMessage());
            return;
        }
    }

    private static byte[] encode(int width, long number) {
        ByteBuffer buff = ByteBuffer.allocate(1 + width);
        buff.put((byte) width);
        switch (width) {
            case 1:
                if(number < Byte.MIN_VALUE || number > Byte.MAX_VALUE) {
                    throw new RuntimeException("Number out of range for requested type!");
                }
                buff.put((byte) number);
                break;
            case 2:
                if(number < Short.MIN_VALUE || number > Short.MAX_VALUE) {
                    throw new RuntimeException("Number out of range for requested type!");
                }
                buff.putShort((short) number);
                break;
            case 4:
                if(number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
                    throw new RuntimeException("Number out of range for requested type!");
                }
                buff.putInt((int) number);
                break;
            case 8:
                buff.putLong(number);
                break;
            default:
                throw new RuntimeException("Incorrect width!");
        }

        return buff.array();
    }
}
