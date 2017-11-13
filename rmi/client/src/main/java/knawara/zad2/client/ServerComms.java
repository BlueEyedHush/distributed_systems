package knawara.zad2.client;

import knawara.zad2.common.RegistrationInfo;
import knawara.zad2.common.exceptions.AlreadyOccupiedException;
import knawara.zad2.common.exceptions.NotYourTurnException;
import knawara.zad2.common.exceptions.OutOfBoardException;
import knawara.zad2.common.requests.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;

/**
 * Created by blueeyedhush on 21.03.16.
 */
public class ServerComms implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerComms.class.getSimpleName());

    private final Printer io;
    private final RegistrationInfo info;

    public ServerComms(Printer io, RegistrationInfo info) {
        this.io = io;
        this.info = info;
    }

    @Override
    public void run() {
        boolean stop = false;
        while(!stop) {
            try {
                Request request = info.getPlayer().nextEvent();

                if(request instanceof GameStarted) {
                    io.gameStarted();
                } else if(request instanceof MoveRequest) {
                    io.yourMove();
                } else if (request instanceof MovePerformed) {
                    MovePerformed e = (MovePerformed) request;
                    ClientMain.get().getBoard().occupy(e.getX(), e.getY(), e.getWho());
                } else if (request instanceof OverWithoutWinner) {
                    io.overWithoutWinner();
                    stop = true;
                } else if (request instanceof Won) {
                    char winnerId = ((Won) request).getPlayerId();
                    if(winnerId == info.getId()) {
                        /* we won! */
                        io.youWon();
                    } else {
                        /* it was someone else ... */
                        io.someoneElseWon(winnerId);
                    }
                    stop = true;
                }
            } catch (RemoteException e) {
                LOGGER.error("Aborting, RemoteException: ", e);
                stop = true;
            }
        }
    }

    public void tryMove(int x, int y) {
        try {
            info.getPlayer().move(x,y);
        } catch (RemoteException e) {
            if(e.getCause() instanceof OutOfBoardException) {
                throw (OutOfBoardException) e.getCause();
            } else if(e.getCause() instanceof AlreadyOccupiedException) {
                throw (AlreadyOccupiedException) e.getCause();
            } else if (e.getCause() instanceof NotYourTurnException) {
                throw (NotYourTurnException) e.getCause();
            } else {
                throw new RuntimeException("Communication failure", e);
            }
        }
    }

    public void leave() {
        try {
            info.getPlayer().leave();
        } catch (RemoteException e) {
            LOGGER.error("Error when trying to leave game. But don't worry, just use 'quit'", e);
        }
    }
}
