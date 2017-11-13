package knawara.zad2.client;

import knawara.zad2.common.RegistrationBoard;
import knawara.zad2.common.RegistrationInfo;
import knawara.zad2.common.exceptions.GameFullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class ClientMain {
    private static final String REGISTRATION_BOARD_REMOTE_NAME = "registration";
    private static final Logger LOGGER = LoggerFactory.getLogger("ClientMain");

    private static ClientMain app;
    public static ClientMain get() {
        return app;
    }

    /* Server's Rmi Registry ip and port */
    private String serverIp;
    private String serverPort;

    private RegistrationInfo info;
    private ServerComms scomms;
    private Printer printer;
    private BoardThreadSafeProxy board;

    public static void main(String[] args) {
        app = new ClientMain();
        app.run(args);
    }

    private void run(String[] args) {
        if(!parseArgs(args)) return;
        if(!joinGame()) return;

        ConsoleIO io = new ConsoleIO();
        startCommsThread(io);
        io.start();
    }

    private boolean parseArgs(String[] args) {
        if(args.length < 2) {
            LOGGER.error("Not enough arguments. Required: <ip> <port>");
            return false;
        }

        serverIp = args[0];
        serverPort = args[1];

        return true;
    }

    private boolean joinGame() {
        RegistrationBoard registrationBoard;
        try {
            registrationBoard = (RegistrationBoard) Naming.lookup(getRegistrationBoardAddress());
        } catch (NotBoundException | MalformedURLException | RemoteException e) {
            LOGGER.error("Fatal error when trying to retrieve registration board's stub: ", e);
            return false;
        }

        try {
            info = registrationBoard.join();
        } catch (RemoteException e) {
            LOGGER.error("Communication error occured while trying to join the game:", e);
            return false;
        } catch (GameFullException e) {
            System.out.println("Game already full, cannot join");
            return false;
        }

        board = new BoardThreadSafeProxy(info.getBoardDim());
        return true;
    }

    private String getRegistrationBoardAddress() {
        return String.format("//%s:%s/%s", serverIp, serverPort, REGISTRATION_BOARD_REMOTE_NAME);
    }

    private void startCommsThread(ConsoleIO io) {
        printer = new Printer(io);
        scomms = new ServerComms(printer, info);
        Thread runner = new Thread(scomms, "server_comms");
        runner.setDaemon(true);
        runner.start();
    }

    public ServerComms getServerCommunication() {
        return scomms;
    }

    public Printer getPrinter() {
        return printer;
    }

    public BoardThreadSafeProxy getBoard() {
        return board;
    }
}
