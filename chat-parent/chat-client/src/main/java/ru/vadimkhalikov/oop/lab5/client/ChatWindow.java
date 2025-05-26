package ru.vadimkhalikov.oop.lab5.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import ru.vadimkhalikov.oop.lab5.common.Message;

public class ChatWindow extends JFrame {

    private JList<Message> chatArea;
    private JTextField messageInput;
    private JButton sendButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JButton refreshUserListButton;
    private JButton logoutButton;
    private JButton reconnectButton;
    private JLabel statusLabel;
    private JLabel connectionStatusIcon;

    private final Consumer<Message> messageSender;
    private final Runnable userListRequester;
    private final Runnable logoutRequester;
    private final Runnable reconnectRequester;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private DefaultListModel<Message> chatModel;
    
    // Базовые иконки
    private final ImageIcon onlineIcon = createCircleIcon(12, new Color(80, 200, 120));
    private final ImageIcon offlineIcon = createCircleIcon(12, new Color(220, 80, 80));
    private final ImageIcon connectingIcon = createCircleIcon(12, new Color(230, 180, 80));

    public ChatWindow(Consumer<Message> messageSender, Runnable userListRequester, Runnable logoutRequester, Runnable reconnectRequester) {
        super("Чат");
        this.messageSender = messageSender;
        this.userListRequester = userListRequester;
        this.logoutRequester = logoutRequester;
        this.reconnectRequester = reconnectRequester;

        initComponents();
        layoutComponents();
        addListeners();

        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void initComponents() {
        chatModel = new DefaultListModel<>();
        chatArea = new JList<>(chatModel);
        chatArea.setCellRenderer(new MessageCellRenderer());
        chatArea.setSelectionModel(new DisabledListSelectionModel());
        chatArea.setBackground(new Color(45, 45, 50));

        messageInput = new JTextField();
        messageInput.setBackground(new Color(55, 55, 60));
        messageInput.setForeground(Color.WHITE);
        messageInput.setCaretColor(Color.WHITE);
        
        sendButton = new JButton("Отправить");
        sendButton.setIcon(createIconFromEmoji("➤"));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setPreferredSize(new Dimension(180, 0));
        userList.setBackground(new Color(45, 45, 50));
        userList.setForeground(Color.WHITE);

        refreshUserListButton = new JButton("Обновить");
        refreshUserListButton.setIcon(createIconFromEmoji("⟳"));
        
        logoutButton = new JButton("Выйти");
        logoutButton.setIcon(createIconFromEmoji("⏏"));
        
        reconnectButton = new JButton("Переподключиться");
        reconnectButton.setIcon(createIconFromEmoji("⟲"));
        reconnectButton.setEnabled(false);
        
        connectionStatusIcon = new JLabel(offlineIcon);
        statusLabel = new JLabel("Инициализация...", JLabel.LEFT);
        statusLabel.setForeground(Color.GRAY);
    }

    private void layoutComponents() {
        // Верхняя панель с информацией о статусе
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(new Color(40, 40, 45));
        statusPanel.add(connectionStatusIcon);
        statusPanel.add(Box.createHorizontalStrut(5));
        statusPanel.add(statusLabel);
        
        // Панель ввода сообщения
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(new Color(50, 50, 55));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        inputPanel.add(messageInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Панель списка пользователей
        JPanel userPanel = new JPanel(new BorderLayout(5, 5));
        userPanel.setBackground(new Color(50, 50, 55));
        userPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(70, 70, 75)),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        
        JLabel userListLabel = new JLabel("Пользователи онлайн:", JLabel.CENTER);
        userListLabel.setForeground(Color.WHITE);
        userListLabel.setBackground(new Color(60, 60, 65));
        userListLabel.setOpaque(true);
        userListLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        
        JPanel userButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        userButtonPanel.setBackground(new Color(50, 50, 55));
        userButtonPanel.add(refreshUserListButton);
        userButtonPanel.add(logoutButton);
        userButtonPanel.add(reconnectButton);
        
        userPanel.add(userListLabel, BorderLayout.NORTH);
        
        JScrollPane userListScroll = new JScrollPane(userList);
        userListScroll.setBackground(new Color(50, 50, 55));
        userListScroll.setBorder(BorderFactory.createEmptyBorder());
        userListScroll.getViewport().setBackground(new Color(45, 45, 50));
        
        userPanel.add(userListScroll, BorderLayout.CENTER);
        userPanel.add(userButtonPanel, BorderLayout.SOUTH);

        // Основная панель чата
        JPanel chatPanel = new JPanel(new BorderLayout(0, 0));
        chatPanel.setBackground(new Color(50, 50, 55));
        chatPanel.add(statusPanel, BorderLayout.NORTH);
        
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBackground(new Color(45, 45, 50));
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.getViewport().setBackground(new Color(45, 45, 50));
        
        chatPanel.add(chatScroll, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        // Разделение окна
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatPanel, userPanel);
        splitPane.setResizeWeight(0.8);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(5);
        
        // Устанавливаем цвета для разделителя
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void setBorder(javax.swing.border.Border border) {
                        // Без границы
                    }
                    
                    @Override
                    public void paint(java.awt.Graphics g) {
                        g.setColor(new Color(60, 60, 65));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        super.paint(g);
                    }
                };
            }
        });

        add(splitPane, BorderLayout.CENTER);
    }

    private void addListeners() {
        Action sendMessageAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        };
        sendButton.addActionListener(sendMessageAction);
        messageInput.addActionListener(sendMessageAction);

        refreshUserListButton.addActionListener(e -> userListRequester.run());

        logoutButton.addActionListener(e -> handleLogout());

        reconnectButton.addActionListener(e -> {
            if (reconnectRequester != null) {
                reconnectRequester.run();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleLogout();
            }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().trim();
        if (!text.isEmpty()) {
            Message serverMsg = new Message(Message.MessageType.USER_MESSAGE, text);
            Message displayMsg = new Message(serverMsg.getType(), "You", serverMsg.getContent()); 
            displayMessage(displayMsg);
            
            messageSender.accept(serverMsg);
            messageInput.setText(""); 
        }
        messageInput.requestFocusInWindow(); 
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите выйти из приложения?",
                "Подтверждение выхода",
                JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            logoutRequester.run(); 
        }
    }

    public void displayMessage(Message message) {
        if (message.getType() == Message.MessageType.USER_LEFT && "You".equals(message.getSender())) {
            return; 
        }
    
        SwingUtilities.invokeLater(() -> {
            chatModel.addElement(message);
            int lastIndex = chatModel.getSize() - 1;
            if (lastIndex >= 0) {
                chatArea.ensureIndexIsVisible(lastIndex);
            }
        });
    }

    public void updateUserList(List<String> users) {
        userListModel.clear();
        for (String user : users) {
            userListModel.addElement(user);
        }
    }

    public void updateConnectionStatus(String status, boolean isConnected) {
        statusLabel.setText(status);
        
        // Обновляем иконку статуса
        if (isConnected) {
            connectionStatusIcon.setIcon(onlineIcon);
            statusLabel.setForeground(new Color(100, 220, 120));
        } else if (status.contains("Reconnecting") || status.contains("Connecting")) {
            connectionStatusIcon.setIcon(connectingIcon);
            statusLabel.setForeground(new Color(230, 180, 80));
        } else {
            connectionStatusIcon.setIcon(offlineIcon);
            statusLabel.setForeground(new Color(230, 100, 100));
        }
        
        // Обновляем состояние компонентов
        messageInput.setEnabled(isConnected);
        sendButton.setEnabled(isConnected);
        reconnectButton.setEnabled(!isConnected);
    }
    
    // Создает иконку из эмодзи
    private ImageIcon createIconFromEmoji(String emoji) {
        // Вместо создания изображения из JLabel, создадим простой цветной квадрат
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
        // Заполняем прямоугольник цветом в зависимости от символа
        if (emoji.equals("➤")) {
            // Зеленая стрелка для отправки
            g2.setColor(new Color(100, 180, 100));
            int[] xPoints = {4, 4, 12};
            int[] yPoints = {4, 12, 8};
            g2.fillPolygon(xPoints, yPoints, 3);
        } else if (emoji.equals("⟳")) {
            // Синий круг для обновления
            g2.setColor(new Color(100, 100, 180));
            g2.fillOval(3, 3, 10, 10);
        } else if (emoji.equals("⏏")) {
            // Красный треугольник для выхода
            g2.setColor(new Color(180, 100, 100));
            int[] xPoints = {8, 3, 13};
            int[] yPoints = {3, 13, 13};
            g2.fillPolygon(xPoints, yPoints, 3);
        } else if (emoji.equals("⟲")) {
            // Оранжевый круг для переподключения
            g2.setColor(new Color(200, 150, 50));
            g2.fillOval(3, 3, 10, 10);
        } else {
            // Квадрат по умолчанию
            g2.setColor(new Color(120, 120, 120));
            g2.fillRect(3, 3, 10, 10);
        }
        
        g2.dispose();
        
        return new ImageIcon(image);
    }
    
    // Создаёт иконку в виде круга заданного размера и цвета
    private ImageIcon createCircleIcon(int size, Color color) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
        g2.setColor(color);
        g2.fillOval(0, 0, size, size);
        g2.dispose();
        
        return new ImageIcon(image);
    }
}

class DisabledListSelectionModel extends DefaultListSelectionModel {
    @Override
    public void setSelectionInterval(int index0, int index1) {
        super.setSelectionInterval(-1, -1);
    }
} 