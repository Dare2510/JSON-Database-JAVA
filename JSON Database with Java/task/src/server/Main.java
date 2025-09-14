package server;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import client.Main.Command;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Main {
    private static final File DB_FILE = new File("data/db.json");
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    // Hilfsmethode: Wert aus verschachteltem JSON per Pfad auslesen
    public static JsonElement getValueByPath(JsonObject root, List<String> path) {
        JsonElement current = root;
        for (String keyPart : path) {
            if (current == null || !current.isJsonObject()) {
                return null;
            }
            JsonObject obj = current.getAsJsonObject();
            if (!obj.has(keyPart)) {
                return null;
            }
            current = obj.get(keyPart);
        }
        return current;
    }

    // Hilfsmethode: Wert in verschachteltem JSON per Pfad setzen 
    public static void setValueByPath(JsonObject root, List<String> path, JsonElement value) {
        JsonObject current = root;
        for (int i = 0; i < path.size(); i++) {
            String keyPart = path.get(i);

            if (i == path.size() - 1) {
                current.add(keyPart, value);
            } else {
                if (!current.has(keyPart) || !current.get(keyPart).isJsonObject()) {
                    current.add(keyPart, new JsonObject());
                }
                current = current.getAsJsonObject(keyPart);
            }
        }
    }

    // Hilfsmethode: Wert in verschachteltem JSON per Pfad löschen
    public static boolean deleteValueByPath(JsonObject root, List<String> path) {
        JsonObject current = root;
        for (int i = 0; i < path.size() - 1; i++) {
            String keyPart = path.get(i);
            if (!current.has(keyPart) || !current.get(keyPart).isJsonObject()) {
                return false; 
            }
            current = current.getAsJsonObject(keyPart);
        }
        String lastKey = path.get(path.size() - 1);
        if (!current.has(lastKey)) {
            return false; 
        }
        current.remove(lastKey);
        return true;
    }

    public static void handleClient(Socket socket) {
        try (
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream())
        ) {
            String msg = input.readUTF();
            System.out.println("Received: " + msg);

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Command command = gson.fromJson(msg, Command.class);

            String type = command.getType();
            String key = command.getKey();

            // Versuche, key als JSON Array zu parsen, ansonsten als einzelnes Element in Liste packen
            List<String> keyPath;
            try {
                keyPath = gson.fromJson(key, new TypeToken<List<String>>() {
                }.getType());
            } catch (Exception e) {
                keyPath = Collections.singletonList(key);
            }

            String value = command.getValue();

            // Datenbank aus Datei laden (mit readLock)
            LOCK.readLock().lock();
            JsonObject database;
            try (Reader reader = new FileReader(DB_FILE)) {
                database = JsonParser.parseReader(reader).getAsJsonObject();
            } catch (Exception e) {
                database = new JsonObject(); // Falls Datei nicht existiert oder leer
            } finally {
                LOCK.readLock().unlock();
            }

            JsonObject response = new JsonObject();

            switch (type) {
                case "get":
                    JsonElement valueElement = getValueByPath(database, keyPath);
                    if (valueElement == null) {
                        response.addProperty("response", "ERROR");
                        response.addProperty("reason", "No such key");
                    } else {
                        response.addProperty("response", "OK");
                        response.add("value", valueElement);
                    }
                    break;

                case "set":
                    // Wert aus String parsen (JSON)
                    valueElement = JsonParser.parseString(value);
                    setValueByPath(database, keyPath, valueElement);

                    // Daten speichern (writeLock)
                    LOCK.writeLock().lock();
                    try (Writer writer = new FileWriter(DB_FILE)) {
                        gson.toJson(database, writer);
                    } finally {
                        LOCK.writeLock().unlock();
                    }

                    response.addProperty("response", "OK");
                    break;

                case "delete":
                    boolean deleted = deleteValueByPath(database, keyPath);
                    if (!deleted) {
                        response.addProperty("response", "ERROR");
                        response.addProperty("reason", "No such key");
                    } else {
                        LOCK.writeLock().lock();
                        try (Writer writer = new FileWriter(DB_FILE)) {
                            gson.toJson(database, writer);
                        } finally {
                            LOCK.writeLock().unlock();
                        }
                        response.addProperty("response", "OK");
                    }
                    break;

                case "exit":
                    response.addProperty("response", "OK");
                    output.writeUTF(gson.toJson(response));
                    System.exit(0);
                    break;

                default:
                    response.addProperty("response", "ERROR");
                    response.addProperty("reason", "Invalid command");
            }

            String responseStr = gson.toJson(response);
            output.writeUTF(responseStr);
            System.out.println("Sent: " + responseStr);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        int poolSize = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        int port = 23451;
        System.out.println("Server started!");
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket socket = server.accept();
                executor.submit(() -> handleClient(socket));
            }
        } finally {
            executor.shutdown();
        }
    }
}
