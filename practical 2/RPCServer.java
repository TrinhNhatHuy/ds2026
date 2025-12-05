import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.io.*;

public class RPCServer extends UnicastRemoteObject implements FileTransferInterface {

    private FileOutputStream fos;
    private String currentFilename;
    private long expectedSize;
    private long receivedBytes;

    protected RPCServer() throws RemoteException {
        super();
        this.receivedBytes = 0;
    }

    @Override
    public boolean initTransfer(String filename, long filesize) throws RemoteException {
        try {
            this.currentFilename = "received_" + filename;
            this.expectedSize = filesize;
            this.receivedBytes = 0;

            fos = new FileOutputStream(currentFilename);
            System.out.println("Initialized transfer for: " + filename + " (" + filesize + " bytes)");

            return true;
        } catch (IOException e) {
            System.err.println("Error initializing transfer: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean transferChunk(byte[] data, long offset) throws RemoteException {
        try {
            if (fos == null) {
                System.err.println("Transfer not initialized!");
                return false;
            }

            fos.write(data);
            receivedBytes += data.length;

            double progress = (receivedBytes * 100.0) / expectedSize;
            System.out.printf("Progress: %.2f%% (%d/%d bytes)\n",
                    progress, receivedBytes, expectedSize);

            return true;
        } catch (IOException e) {
            System.err.println("Error writing chunk: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String finalizeTransfer() throws RemoteException {
        try {
            if (fos != null) {
                fos.close();
                fos = null;
            }

            if (receivedBytes == expectedSize) {
                System.out.println("File transfer completed successfully!");
                return "ACK: File received successfully (" + receivedBytes + " bytes)";
            } else {
                System.out.println("Warning: Size mismatch!");
                return "WARNING: Expected " + expectedSize + " but received " + receivedBytes;
            }
        } catch (IOException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    @Override
    public String getStatus() throws RemoteException {
        return "Server ready. Current file: " +
                (currentFilename != null ? currentFilename : "none");
    }

    public static void main(String[] args) {
        try {
            // Create and export the remote object
            RPCServer server = new RPCServer();

            Registry registry = LocateRegistry.createRegistry(1099);

            // Bind the remote object in the registry
            registry.rebind("FileTransferService", server);

            System.out.println("RPC Server started and ready!");
            System.out.println("Waiting for client connections...");

        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}