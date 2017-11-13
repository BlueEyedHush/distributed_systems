package knawara.zad2.server;

import knawara.zad2.common.RegistrationBoard;
import knawara.zad2.common.utils.Board;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class Main {
    private static Main app;

    public static Main get() {
        return app;
    }

    public static void main(String[] args) {
        app = new Main();
        app.run(args);
    }


    private static final String PLAYER_IDS = "qwertyuiopasdfghjklzxcvbnm";
    public static final int MAX_PLAYERS = PLAYER_IDS.length();
    private static final String REGISTRATION_BOARD_EXPORT_NAME = "registration";
    private static final Logger LOGGER = LoggerFactory.getLogger("Main");

    private int port = 3000;
    private int boardDimm = 3;
    private int winningChainLength = 3;
    private int playersNumber = 2;

    private Registry registry;
    private RegistrationBoardImpl registrationBoard;
    private Set<PlayerImpl> players = Collections.synchronizedSet(new HashSet<>(MAX_PLAYERS));
    private CountDownLatch playerRegistrationLatch;
    private Game game;

    private final Consumer<PlayerImpl> onPlayerRegistered = (p) -> {
        players.add(p);
        playerRegistrationLatch.countDown();;
    };

    private void run(String args[]) {
        if(!parseArgs(args)) return;
        game = new Game(new Board(boardDimm), winningChainLength);
        if(!setupRegistrationBoard()) return;

        if(!awaitPlayers()) return;
        game.setPlayers(new ArrayList<>(players));
        players = null;
        game.start();

        cleanup();
    }

    /**
     * @param args
     * @return if false, there were errors in arguments and application cannot continue
     */
    private boolean parseArgs(String[] args) {
         if(args.length < 4) {
             LOGGER.error("Not enough args. Required: <port> <boardDimmension> <winningChainLength> <playersNumber>");
             return false;
         }

        try {
            port = Integer.valueOf(args[0], 10);
        } catch (NumberFormatException e) {
            LOGGER.error("Port - wrong format");
            return false;
        }
        if(port < 0 || port > 0xffff) {
            LOGGER.error("Port number out of range");
            return false;
        }

        try {
            boardDimm = Integer.valueOf(args[1], 10);
        } catch (NumberFormatException e) {
            LOGGER.error("Board dimmension - wrong format");
            return false;
        }
        if(boardDimm < 0) {
            LOGGER.error("Board dimmension cannot be negative");
            return false;
        }

        try {
            winningChainLength = Integer.valueOf(args[2], 10);
        } catch (NumberFormatException e) {
            LOGGER.error("Winning chain length - wrong format");
            return false;
        }
        if(winningChainLength > boardDimm) {
            LOGGER.error("Winning chain length cannot be longer than board dimension");
            return false;
        }

        try {
            playersNumber = Integer.valueOf(args[3], 10);
        } catch (NumberFormatException e) {
            LOGGER.error("Players number - wrong format");
            return false;
        }
        int maxPlayers = PLAYER_IDS.length();
        if(playersNumber > maxPlayers) {
            LOGGER.error("No more than {} players allowed", maxPlayers);
            return false;
        }

        return true;
    }

    private boolean setupRegistrationBoard() {
        /* initialize RMI Registry */
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            LOGGER.error("Cannot start RMI Registry due to:", e);
            return false;
        }

        playerRegistrationLatch = new CountDownLatch(playersNumber);
        registrationBoard = new RegistrationBoardImpl(playersNumber, boardDimm, PLAYER_IDS, () -> game, onPlayerRegistered);
        RegistrationBoard boardStub;
        try {
            boardStub = (RegistrationBoard) UnicastRemoteObject.exportObject(registrationBoard, 0);
        } catch (RemoteException e) {
            LOGGER.error("Cannot export registration board due to", e);
            return false;
        }

        try {
            registry.bind(REGISTRATION_BOARD_EXPORT_NAME, boardStub);
        } catch (RemoteException e) {
            LOGGER.error("Cannot bind registration board due to", e);
            return false;
        } catch (AlreadyBoundException e) {
            LOGGER.error("Registration board already bound", e);
            return false;
        }

        return true;
    }

    private boolean awaitPlayers() {
        try {
            playerRegistrationLatch.await();
        } catch (InterruptedException e) {
            LOGGER.warn("Waiting on player registration latch interrupted - this shouldn't happen, aborting");
            return false;
        }
        LOGGER.info("All players are present");
        return true;
    }

    private void cleanup() {
        try {
            registry.unbind(REGISTRATION_BOARD_EXPORT_NAME);
        } catch (RemoteException | NotBoundException e) {
            LOGGER.warn("[cleanup] exception when unbinding registry", e);
        }

        try {
            UnicastRemoteObject.unexportObject(registry, true);
        } catch (NoSuchObjectException e) {
            LOGGER.warn("[cleanup] no such object - registry", e);
        }
        registry = null;

        /* @todo: cleanup players! */
    }

    public int getPort() {
        return port;
    }

    public int getBoardDimm() {
        return boardDimm;
    }

    public int getWinningChainLength() {
        return winningChainLength;
    }

    public int getPlayersNumber() {
        return playersNumber;
    }
}
