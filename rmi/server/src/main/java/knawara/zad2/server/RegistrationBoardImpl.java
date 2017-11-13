package knawara.zad2.server;

import knawara.zad2.common.Player;
import knawara.zad2.common.RegistrationBoard;
import knawara.zad2.common.RegistrationInfo;
import knawara.zad2.common.exceptions.GameFullException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class RegistrationBoardImpl implements RegistrationBoard {

    private static final Logger LOGGER = LoggerFactory.getLogger("RegistrationBoardImpl");

    private final int numberOfPlayers;
    private final String allowedIds;
    private final Consumer<PlayerImpl> playerRegisteredCallback;
    private final int boardDim;
    private final Supplier<Game> gameReferenceSupplier;

    private AtomicInteger alreadyRegistered = new AtomicInteger(0);

    public RegistrationBoardImpl(int numberOfPlayers,
                                 int boardDim,
                                 String allowedIds,
                                 Supplier<Game> gameReferenceSupplier,
                                 Consumer<PlayerImpl> playerRegisteredCallback) {
        this.boardDim = boardDim;
        this.gameReferenceSupplier = gameReferenceSupplier;
        assert allowedIds.length() >= numberOfPlayers;
        assert playerRegisteredCallback != null;

        this.numberOfPlayers = numberOfPlayers;
        this.allowedIds = allowedIds;
        this.playerRegisteredCallback = playerRegisteredCallback;
    }

    @Override
    public RegistrationInfo join() throws RemoteException, GameFullException {
        int previousAlreadyRegisteredValue = alreadyRegistered.getAndUpdate(i -> {
            if(i < numberOfPlayers) return i+1;
            else return i;
        });

        if(previousAlreadyRegisteredValue < numberOfPlayers) {
            /* it was smaller, we can progress with registration */
            LOGGER.info("Accepting joining attempt");

            /* get id */
            char id = allowedIds.charAt(previousAlreadyRegisteredValue);

            /* create and export client specific object for communication with server */
            PlayerImpl player = new PlayerImpl(id, gameReferenceSupplier);
            Player playerStub = (Player) UnicastRemoteObject.exportObject(player, 0);

            RegistrationInfo registrationInfo = new RegistrationInfo(id, playerStub, boardDim);

            playerRegisteredCallback.accept(player);

            return registrationInfo;
        } else {
            LOGGER.info("Joining attempt rejected - game full");
            throw new GameFullException();
        }
    }
}
