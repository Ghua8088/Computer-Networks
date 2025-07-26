import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
public class ChatClient {
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private Socket socket;
    private String name;
    private final String serverIP;
    private final int port = 8888;

    private JTextArea chatArea = new JTextArea(20, 50);
    private JTextArea inputArea = new JTextArea(3, 40);
    private JButton sendButton = new JButton("Send");

    public ChatClient(String serverIP) {
        this.serverIP = serverIP;
        this.name = JOptionPane.showInputDialog("Enter your name:");
        if (name == null || name.trim().isEmpty()) name = "Client";
        buildUI();
    }

    private void buildUI() {
        JFrame frame = new JFrame("Chat - " + name);
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        inputArea.setLineWrap(true);
        sendButton.addActionListener(e -> send());
        try{
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void connect() {
        try {
            socket = new Socket(serverIP, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
            append("Connected to " + serverIP + ":" + port);
        } catch (IOException e) {
            append("Connection failed: " + e.getMessage());
        }
    }

    public void start() {
        connect();
        new Thread(() -> {
            try {
                while (true) append((String) input.readObject());
            } catch (Exception e) {
                append("Connection closed.");
            } finally {
                close();
            }
        }).start();
    }

    private void send() {
        String msg = inputArea.getText().trim();
        if (!msg.isEmpty()) {
            try {
                output.writeObject(name + " : " + msg);
                output.flush();
            } catch (IOException e) {
                append("Send failed: " + e.getMessage());
            }
            inputArea.setText("");
        }
    }

    private void append(String msg) {
        SwingUtilities.invokeLater(() -> chatArea.append(msg + "\n"));
    }

    private void close() {
        try {
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient("localhost").start());
    }
}
