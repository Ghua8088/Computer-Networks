import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.List;
import java.util.ArrayList;

public class ChatServer {
    private final int port = 8888;
    private final List<ObjectOutputStream> clients = new ArrayList<>();
    private JTextArea chatArea = new JTextArea(20, 50);
    private JTextArea inputArea = new JTextArea(3, 40);
    private JButton sendButton = new JButton("Send");

    public ChatServer() {
        setupGUI();
    }

    private void setupGUI() {
        JFrame frame = new JFrame("Chat Server");
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        inputArea.setLineWrap(true);
        sendButton.addActionListener(e -> sendMessage());
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        try{
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(inputPanel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        append("Server started on port " + port);
    }

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            while (true) {
                Socket client = server.accept();
                append("Client connected: " + client.getInetAddress());
                handleClient(client);
            }
        } catch (IOException e) {
            append("Server error: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        new Thread(() -> {
            try (
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
            ) {
                synchronized (clients) {
                    clients.add(out);
                }

                String msg;
                while ((msg = (String) in.readObject()) != null) {
                    append(msg);
                    broadcast(msg);
                }

            } catch (Exception e) {
                append("Client disconnected.");
            } finally {
                synchronized (clients) {
                    clients.removeIf(out -> {
                        try {
                            out.close();
                            return true;
                        } catch (IOException ex) {
                            return false;
                        }
                    });
                }
            }
        }).start();
    }

    private void sendMessage() {
        String msg = inputArea.getText().trim();
        if (!msg.isEmpty()) {
            String fullMessage = "Server: " + msg;
            broadcast(fullMessage);
            append(fullMessage);
            inputArea.setText("");
        }
    }

    private void broadcast(String message) {
        synchronized (clients) {
            for (ObjectOutputStream out : clients) {
                try {
                    out.writeObject(message);
                    out.flush();
                } catch (IOException e) {
                    append("Failed to send to a client.");
                }
            }
        }
    }

    private void append(String msg) {
        SwingUtilities.invokeLater(() -> chatArea.append(msg + "\n"));
    }

    public static void main(String[] args) {
        new ChatServer().start();
    }
}
