package server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private final Map<String, PrintWriter> clients = new HashMap<>(); // Клиенты с именами и потоками

    public static void main(String[] args) {
        new ChatServer().start();
    }

    public void start() {
        System.out.println("Сервер запущен на порту " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Ожидаем подключения клиента
                System.out.println("Новое подключение: " + clientSocket);
                new Thread(() -> handleClient(clientSocket)).start(); // Обрабатываем клиента в отдельном потоке
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) {
        String userName = null;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Отправляем запрос на логин
            out.println("Введите логин:");

            // Получаем логин
            userName = in.readLine();
            if (userName == null || userName.trim().isEmpty()) {
                out.println("Логин не может быть пустым. Соединение будет закрыто.");
                return;
            }
            userName = userName.trim();

            // Сохраняем клиента
            synchronized (clients) {
                if (clients.containsKey(userName)) {
                    out.println("Логин занят. Соединение будет закрыто.");
                    return;
                }
                clients.put(userName, out);
            }

            // Приветствуем пользователя
            out.println("Добро пожаловать, " + userName + "!");
            broadcastMessage("Сервер: Пользователь " + userName + " присоединился к чату!");

            // Обрабатываем сообщения от клиента
            String message;
            while ((message = in.readLine()) != null) {
                if (message.trim().equalsIgnoreCase("/exit")) { // Команда выхода
                    break;
                }
                broadcastMessage(userName + ": " + message); // Трансляция сообщения всем клиентам
            }
        } catch (IOException e) {
            System.err.println("Ошибка общения с клиентом: " + e.getMessage());
        } finally {
            if (userName != null) {
                disconnectClient(userName);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка закрытия сокета клиента: " + e.getMessage());
            }
        }
    }

    private void broadcastMessage(String message) {
        System.out.println("Broadcast: " + message); // Логируем сообщение на сервере
        synchronized (clients) {
            for (PrintWriter clientOut : clients.values()) {
                clientOut.println(message); // Отправляем сообщение каждому подключенному клиенту
            }
        }
    }

    private void disconnectClient(String userName) {
        synchronized (clients) {
            if (clients.remove(userName) != null) {
                broadcastMessage("Сервер: Пользователь " + userName + " покинул чат.");
            }
        }
    }
}
