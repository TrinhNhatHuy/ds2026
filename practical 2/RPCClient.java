import java.rmi.*;
import java.rmi.registry.*;
import java.io.*;
import java.util.Scanner;

public class RPCClient {

    public static void main(String[] args) {
        try {
            String filename;
            if (args.length > 0) {
                filename = args[0];
            } else {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter filename to send: ");
                filename = scanner.nextLine();
                scanner.close();
            }

            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("File not found!");
                return;
            }

            System.out.println("Connecting to RPC server...");
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            FileTransferInterface fileService =
                    (FileTransferInterface) registry.lookup("FileTransferService");

            System.out.println("Connected to server.");
            System.out.println("Server status: " + fileService.getStatus());

            System.out.println("Initializing transfer for: " + file.getName());
            boolean initSuccess = fileService.initTransfer(file.getName(), file.length());

            if (!initSuccess) {
                System.out.println("Failed to initialize transfer!");
                return;
            }

            // Read and send file in chunks
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            long offset = 0;
            long totalSent = 0;

            System.out.println("Sending file...");

            while ((bytesRead = fis.read(buffer)) != -1) {
                // Create array of exact size for this chunk
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);

                // Call remote method to transfer chunk
                boolean success = fileService.transferChunk(chunk, offset);

                if (!success) {
                    System.out.println("Error sending chunk at offset " + offset);
                    break;
                }

                offset += bytesRead;
                totalSent += bytesRead;

                double progress = (totalSent * 100.0) / file.length();
                System.out.printf("\rProgress: %.2f%%", progress);
            }

            System.out.println("\n\nFinalizing transfer...");

            String response = fileService.finalizeTransfer();
            System.out.println("Server response: " + response);

            fis.close();
            System.out.println("Transfer complete!");

        } catch (NotBoundException e) {
            System.err.println("Service not found: " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("RPC error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}