package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;

public class ChatClient extends Application {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private TextArea messagesArea;
    private TextField inputField;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Мессенджер");

        messagesArea = new TextArea();
        messagesArea.setEditable(false);

        inputField = new TextField();
        inputField.setPromptText("Введите сообщение...");
        inputField.setDisable(true); // Поле недоступно до завершения регистрации
        inputField.setOnAction(event -> sendMessage());

        VBox layout = new VBox(10, messagesArea, inputField);
        layout.setPrefSize(400, 300);

        primaryStage.setScene(new Scene(layout));
        primaryStage.setOnCloseRequest(event -> disconnect());
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        // Подключение к серверу в фоновом потоке
        new Thread(() -> {
            try {
                socket = new Socket("localhost", 12345);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                registerUser(); // Регистрация пользователя
                listenForMessages(); // Начать прослушивание сообщений от сервера
            } catch (IOException e) {
                updateMessagesArea("Ошибка подключения: " + e.getMessage());
            }
        }).start();
    }

    private void registerUser() throws IOException {
        while (true) {
            String serverMessage = in.readLine();
            if (serverMessage == null) {
                updateMessagesArea("Соединение с сервером закрыто.");
                break;
            }

            // Обновление UI через Platform.runLater
            updateMessagesArea(serverMessage);

            if (serverMessage.startsWith("Введите логин:")) {
                // Диалоговое окно логина - синхронный вызов в основном потоке
                String userName = askForInput("Введите ваш логин:");
                out.println(userName); // Отправка логина на сервер
            } else if (serverMessage.startsWith("Добро пожаловать")) {
                updateInputFieldState(false); // Разблокируем поле для ввода сообщений
                break;
            }
        }
    }

    private void listenForMessages() throws IOException {
        String message;
        while ((message = in.readLine()) != null) {
            updateMessagesArea(message); // Обновляем UI при получении нового сообщения
        }
    }

    private String askForInput(String promptText) {
        final String[] userName = new String[1];
        Platform.runLater(() -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Регистрация");
            dialog.setHeaderText(promptText);
            dialog.setContentText("Логин:");
            userName[0] = dialog.showAndWait().orElse("");
        });

        // Ожидаем завершения ввода
        while (userName[0] == null) {
            Thread.yield();
        }
        return userName[0];
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            inputField.clear();
        }
    }

    private void disconnect() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Ошибка при закрытии соединения: " + e.getMessage());
        }
    }

    // Метод для обновления текстовой области через Platform.runLater
    private void updateMessagesArea(String message) {
        Platform.runLater(() -> messagesArea.appendText(message + "\n"));
    }

    // Метод для изменения состояния текстового поля через Platform.runLater
    private void updateInputFieldState(boolean isDisabled) {
        Platform.runLater(() -> inputField.setDisable(isDisabled));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
