package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.google.gson.Gson;

public class Main {

    public static class Command {
        @Parameter(names = "-t", description = "Command type (get, set, delete, exit)")
        String type;

        @Parameter(names = "-k", description = "Key or key path as JSON array string")
        String key;

        @Parameter(names = "-v", description = "Value to set (for set command)")
        String value;

        public String getType() {
            return type;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }
    }

    public static void main(String[] args) {
        System.out.println("Client started!");

        String address = "127.0.0.1";
        int port = 23451;

        Command command = new Command();
        JCommander jc = JCommander.newBuilder()
                .addObject(command)
                .build();

        jc.parse(args);

        Gson gson = new Gson();
        String msg = gson.toJson(command);

        try (
                Socket socket = new Socket(InetAddress.getByName(address), port);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            output.writeUTF(msg);
            System.out.println("Sent: " + msg);

            String response = input.readUTF();
            System.out.println("Received: " + response);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
