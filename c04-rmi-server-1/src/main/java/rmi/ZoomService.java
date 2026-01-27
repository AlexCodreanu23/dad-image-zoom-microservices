package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ZoomService extends Remote {
    byte[] zoom(byte[] bmpBytes, String zoomType, int percent) throws RemoteException;
}
