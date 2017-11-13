package knawara.zad2.common;

import knawara.zad2.common.exceptions.AlreadyOccupiedException;
import knawara.zad2.common.exceptions.NotYourTurnException;
import knawara.zad2.common.exceptions.OutOfBoardException;
import knawara.zad2.common.requests.Request;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public interface Player extends Remote {
    Request nextEvent() throws RemoteException;
    void move(int x, int y) throws RemoteException, AlreadyOccupiedException, NotYourTurnException, OutOfBoardException;
    void leave() throws RemoteException;
}
