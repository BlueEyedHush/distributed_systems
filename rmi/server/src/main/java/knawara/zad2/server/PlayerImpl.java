package knawara.zad2.server;

import knawara.zad2.common.Player;
import knawara.zad2.common.exceptions.AlreadyOccupiedException;
import knawara.zad2.common.exceptions.NotYourTurnException;
import knawara.zad2.common.exceptions.OutOfBoardException;
import knawara.zad2.common.requests.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public class PlayerImpl implements Player, Unreferenced {
    private static final int REQUEST_QUEUE_CAPACITY = Main.MAX_PLAYERS*2;
    private static final Logger LOGGER = LoggerFactory.getLogger("PlayerImpl");

    private final char id;
    private final Supplier<Game> gameReferenceSupplier;

    private BlockingQueue<Request> queue = new LinkedBlockingQueue<>(REQUEST_QUEUE_CAPACITY);

    public PlayerImpl(char id, Supplier<Game> gameReferenceSupplier) {
        this.id = id;
        this.gameReferenceSupplier = gameReferenceSupplier;
    }

    public BlockingQueue<Request> getQueue() {
        return queue;
    }

    public char getId() {
        return id;
    }

    @Override
    public Request nextEvent() throws RemoteException {
        Request r = null;
        while(r == null) {
            try {
                r = queue.take();
            } catch (InterruptedException e) {
                LOGGER.warn("Unexpected interrupt while waiting for next event to arrive", e);
            }
        }
        return r;
    }

    @Override
    public void move(int x, int y) throws RemoteException, AlreadyOccupiedException, NotYourTurnException, OutOfBoardException {
        gameReferenceSupplier.get().submitMove(id, x, y);
    }

    @Override
    public void leave() throws RemoteException {
        Game game = gameReferenceSupplier.get();
        if(game != null) {
            game.submitLeave(id);
        } else {
            LOGGER.warn("Player {} wants to leave, but Game is null...");
        }
    }

    @Override
    public void unreferenced() {
        try {
            leave();
        } catch (RemoteException e) {
            LOGGER.error("This should never happen, it calls leave() locally on server - why RemoteException?");
        }
    }
}
