import java.rmi.Remote;
import java.rmi.RemoteException;

public interface FileTransferInterface extends Remote {


    boolean initTransfer(String filename, long filesize) throws RemoteException;

    boolean transferChunk(byte[] data, long offset) throws RemoteException;

    String finalizeTransfer() throws RemoteException;

    String getStatus() throws RemoteException;
}