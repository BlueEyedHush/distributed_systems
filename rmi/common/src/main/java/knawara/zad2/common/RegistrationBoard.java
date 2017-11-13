package knawara.zad2.common;

import knawara.zad2.common.exceptions.GameFullException;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by blueeyedhush on 20.03.16.
 */
public interface RegistrationBoard extends Remote {
    RegistrationInfo join() throws RemoteException, GameFullException;
}
