import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            String filename;
            if (args.length > 0) {
                filename = args[0];
            } else {
                Scanner scanner = new Scanner(System.in);
                System.out.print("Enter filename to send: ");
                filename = scanner.nextLine();
            }

            File file = new File(filename);
            if (!file.exists()) {
                System.out.println("File not found!");
                return;
            }

            Socket socket = new Socket("localhost", 5000);
            System.out.println("Connected to server.");

            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            DataInputStream dis = new DataInputStream(socket.getInputStream());
            FileInputStream fis = new FileInputStream(file);

            dos.writeUTF(file.getName());
            dos.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytesRead;
            
            System.out.println("Sending file...");
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }

            System.out.println("\nDone sending.");

            String response = dis.readUTF();
            System.out.println("Server says: " + response);

            fis.close();
            dos.close();
            dis.close();
            socket.close();

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}