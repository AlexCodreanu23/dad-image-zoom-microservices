package rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ZoomServiceImpl extends UnicastRemoteObject implements ZoomService {

    public ZoomServiceImpl() throws RemoteException { super(); }

    @Override
    public byte[] zoom(byte[] bmpBytes, String zoomType, int percent) throws RemoteException {
        System.out.println("C04 RMI zoom called... bytes=" + (bmpBytes == null ? 0 : bmpBytes.length)
                + " zoomType=" + zoomType + " percent=" + percent);
        return bmpBytes;
    }
}
