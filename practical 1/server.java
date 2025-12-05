import java.io.*;
import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            ServerSocket serverSocket = new ServerSocket(5000);
            System.out.println("Server started. Waiting for client...");

            Socket socket = serverSocket.accept();
            System.out.println("Client connected!");

            DataInputStream dis = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

            String filename = dis.readUTF();
            long filesize = dis.readLong();
            System.out.println("Receiving: " + filename + " (" + filesize + " bytes)");

            FileOutputStream fos = new FileOutputStream("received_" + filename);
            byte[] buffer = new byte[4096];
            
            int bytesRead;
            long totalRead = 0;

            while (totalRead < filesize) {
                int remaining = (int) (filesize - totalRead);
                int toRead = Math.min(buffer.length, remaining);
                
                bytesRead = dis.read(buffer, 0, toRead);
                if (bytesRead == -1) break;
                
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }

            System.out.println("File saved successfully.");

            dos.writeUTF("ACK");

            fos.close();
            dis.close();
            dos.close();
            socket.close();
            serverSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}