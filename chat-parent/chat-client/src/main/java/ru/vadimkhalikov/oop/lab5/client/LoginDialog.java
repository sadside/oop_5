package ru.vadimkhalikov.oop.lab5.client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class LoginDialog extends JDialog {

    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private JButton connectButton;
    private JButton cancelButton;
    private JLabel statusLabel;
    private JLabel statusIcon;

    @FunctionalInterface
    interface LoginAttemptHandler {
        boolean attempt(String server, String port, String username);
    }

    private final LoginAttemptHandler loginHandler;
    
    // Иконки для статуса
    private final ImageIcon errorIcon;
    private final ImageIcon waitingIcon;
    private final ImageIcon successIcon;

    public LoginDialog(LoginAttemptHandler handler) {
        super((Frame) null, "Чат - Подключение", true);
        this.loginHandler = handler;
        
        // Создаем иконки
        errorIcon = createCircleIcon(10, new Color(220, 80, 80));
        waitingIcon = createCircleIcon(10, new Color(230, 180, 80));
        successIcon = createCircleIcon(10, new Color(80, 200, 120));

        initComponents();
        layoutComponents();
        addListeners();

        pack();
        setMinimumSize(new Dimension(450, getHeight() + 20));
        setPreferredSize(new Dimension(450, getHeight() + 40));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setResizable(true);
    }

    private void initComponents() {
        // Поля ввода с современным стилем
        serverField = new JTextField("localhost", 20);
        serverField.setBackground(new Color(55, 55, 60));
        serverField.setForeground(Color.WHITE);
        serverField.setCaretColor(Color.WHITE);
        serverField.setBorder(BorderFactory.createCompoundBorder(
            serverField.getBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        portField = new JTextField("8080", 5);
        portField.setBackground(new Color(55, 55, 60));
        portField.setForeground(Color.WHITE);
        portField.setCaretColor(Color.WHITE);
        portField.setBorder(BorderFactory.createCompoundBorder(
            portField.getBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        usernameField = new JTextField(15);
        usernameField.setBackground(new Color(55, 55, 60));
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            usernameField.getBorder(),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Кнопки с иконками
        connectButton = new JButton("Подключиться");
        connectButton.setIcon(createIconFromEmoji("↗"));
        
        cancelButton = new JButton("Отмена");
        cancelButton.setIcon(createIconFromEmoji("✕"));
        
        // Статус с иконкой
        statusIcon = new JLabel();
        statusIcon.setVisible(false);
        
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(new Color(230, 100, 100));
        statusLabel.setFont(statusLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
    }

    private void layoutComponents() {
        // Главная панель
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(new Color(50, 50, 55));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Заголовок
        JLabel titleLabel = new JLabel("Подключение к серверу", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        mainPanel.add(titleLabel);
        
        // Панель полей ввода
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBackground(new Color(50, 50, 55));
        fieldsPanel.setAlignmentX(CENTER_ALIGNMENT);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Сервер
        JLabel serverLabel = new JLabel("Адрес сервера:");
        serverLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        fieldsPanel.add(serverLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        fieldsPanel.add(serverField, gbc);
        
        // Порт
        JLabel portLabel = new JLabel("Порт:");
        portLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        fieldsPanel.add(portLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        fieldsPanel.add(portField, gbc);
        
        // Никнейм
        JLabel usernameLabel = new JLabel("Ваше имя:");
        usernameLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        fieldsPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        fieldsPanel.add(usernameField, gbc);
        
        mainPanel.add(fieldsPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        
        // Панель статуса
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBackground(new Color(50, 50, 55));
        statusPanel.setAlignmentX(CENTER_ALIGNMENT);
        statusPanel.add(statusIcon);
        statusPanel.add(statusLabel);
        
        mainPanel.add(statusPanel);
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Панель кнопок
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(new Color(50, 50, 55));
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);
        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel);
        
        setContentPane(mainPanel);
    }

    private void addListeners() {
        Action connectAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectButton.doClick();
            }
        };
        serverField.addActionListener(connectAction);
        portField.addActionListener(connectAction);
        usernameField.addActionListener(connectAction);

        connectButton.addActionListener(e -> performLogin());

        cancelButton.addActionListener(e -> dispose());
    }

    private void performLogin() {
        String server = serverField.getText().trim();
        String port = portField.getText().trim();
        String username = usernameField.getText().trim();

        if (server.isEmpty() || port.isEmpty() || username.isEmpty()) {
            setStatus("Необходимо заполнить все поля", false);
            return;
        }

        try {
            int portNum = Integer.parseInt(port);
            if (portNum <= 0 || portNum > 65535) {
                setStatus("Некорректный номер порта (1-65535)", false);
                return;
            }
        } catch (NumberFormatException ex) {
            setStatus("Некорректный формат номера порта", false);
            return;
        }

        setStatus("Подключение...", true);
        connectButton.setEnabled(false);
        cancelButton.setEnabled(false);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return loginHandler.attempt(server, port, username);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                    } else {
                        setStatus("Не удалось подключиться. Проверьте данные и попробуйте снова.", false);
                    }
                } catch (Exception ex) {
                    setStatus("Ошибка подключения", false);
                    ex.printStackTrace();
                }
                 connectButton.setEnabled(true);
                 cancelButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    void setStatus(String text) {
        setStatus(text, true);
    }
    
    void setStatus(String text, boolean isWaiting) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusIcon.setVisible(!text.isEmpty());
            
            if (text.isEmpty()) {
                statusIcon.setIcon(null);
            } else if (text.contains("Подключение")) {
                statusIcon.setIcon(waitingIcon);
                statusLabel.setForeground(new Color(230, 180, 80));
            } else if (isWaiting) {
                statusIcon.setIcon(waitingIcon);
                statusLabel.setForeground(new Color(230, 180, 80));
            } else {
                statusIcon.setIcon(errorIcon);
                statusLabel.setForeground(new Color(255, 0, 0));
            }
        });
    }
    
    private ImageIcon createIconFromEmoji(String emoji) {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            
        if (emoji.equals("↗")) {
            g2.setColor(new Color(100, 180, 100));
        } else if (emoji.equals("✕")) {
            g2.setColor(new Color(180, 100, 100));
        } else {
            g2.setColor(new Color(100, 100, 180));
        }
        
        g2.fillRect(3, 3, 10, 10);
        g2.dispose();
        
        return new ImageIcon(image);
    }
    
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