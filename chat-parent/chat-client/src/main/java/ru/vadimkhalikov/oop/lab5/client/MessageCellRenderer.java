package ru.vadimkhalikov.oop.lab5.client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import ru.vadimkhalikov.oop.lab5.common.Message;

public class MessageCellRenderer extends JPanel implements ListCellRenderer<Message> {

    private JLabel senderLabel;
    private JTextArea messageArea; 
    private JLabel timeLabel;
    private JPanel textPanel; 
    private JPanel bubblePanel; 
    private JLabel eventLabel;
    private JLabel avatarLabel;

    // Кэш для хранения цветов аватаров пользователей
    private final Map<String, Color> userColors = new HashMap<>();
    // Кэш для хранения последнего отправителя и времени сообщения
    private String lastSender = null;
    private long lastMessageTime = 0;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    // Темная цветовая схема
    private final Color MY_BUBBLE_COLOR = new Color(50, 120, 70); 
    private final Color OTHER_BUBBLE_COLOR = new Color(60, 60, 80);
    private final Color EVENT_FG_COLOR = new Color(180, 180, 180);
    private final Color MY_SENDER_COLOR = new Color(160, 240, 160); 
    private final Color OTHER_SENDER_COLOR = new Color(150, 180, 255);
    private final Font SENDER_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
    private final Font MESSAGE_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    private final Font TIME_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
    private final Font EVENT_FONT = new Font(Font.SANS_SERIF, Font.ITALIC, 12);
    
    // Константы для отступов
    private static final int GROUP_MESSAGE_SPACING = 2;
    private static final int NEW_SENDER_SPACING = 8;
    private static final int AVATAR_SIZE = 30;
    private static final int TIME_THRESHOLD = 60000; // 1 минута между группами сообщений

    public MessageCellRenderer() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);

        // Аватар пользователя (отображается как цветной кружок с инициалами)
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setOpaque(true);

        bubblePanel = new JPanel(new BorderLayout(3, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                int arc = 15;
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.dispose();
            }
            @Override
            public Dimension getMaximumSize() {
                Dimension pref = getPreferredSize();
                if (getParent() != null && getParent().getParent() instanceof JList) {
                    JList<?> list = (JList<?>) getParent().getParent();
                    int maxWidth = (int)(list.getWidth() * 0.75);
                     if (pref.width > maxWidth) {
                        pref.width = maxWidth;
                     }
                }
                return pref;
            }
        };
        bubblePanel.setBorder(new EmptyBorder(5, 8, 5, 8)); 
        bubblePanel.setOpaque(false); 

        senderLabel = new JLabel();
        senderLabel.setFont(SENDER_FONT);
        senderLabel.setBorder(new EmptyBorder(0, 0, 2, 0)); 

        messageArea = new JTextArea();
        messageArea.setFont(MESSAGE_FONT);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setOpaque(false); 
        messageArea.setFocusable(false);

        timeLabel = new JLabel();
        timeLabel.setFont(TIME_FONT);
        timeLabel.setForeground(new Color(180, 180, 180));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        textPanel = new JPanel(new BorderLayout(0, 0));
        textPanel.setOpaque(false);
        textPanel.add(messageArea, BorderLayout.CENTER);
        textPanel.add(timeLabel, BorderLayout.SOUTH);

        bubblePanel.add(senderLabel, BorderLayout.NORTH);
        bubblePanel.add(textPanel, BorderLayout.CENTER);
        
        eventLabel = new JLabel();
        eventLabel.setFont(EVENT_FONT);
        eventLabel.setForeground(EVENT_FG_COLOR);
        eventLabel.setHorizontalAlignment(SwingConstants.CENTER);
        eventLabel.setBorder(new EmptyBorder(5, 20, 5, 20)); 
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Message> list,
                                                  Message message,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus) {

        removeAll();

        String time = timeFormat.format(new Date()); 
        String sender = message.getSender();
        String content = message.getContent();
        boolean isMyMessage = "You".equals(sender);
        long currentMessageTime = System.currentTimeMillis();
        
        // Определение, является ли сообщение частью группы от одного пользователя
        boolean isGroupedMessage = sender != null && sender.equals(lastSender) && 
                                  (currentMessageTime - lastMessageTime < TIME_THRESHOLD);
        
        // Настройка отступов в зависимости от группировки
        setBorder(new EmptyBorder(
            isGroupedMessage ? GROUP_MESSAGE_SPACING : NEW_SENDER_SPACING, 
            5, 
            2, 
            5
        ));

        // Обновляем последнего отправителя и время для следующего сообщения
        lastSender = sender;
        lastMessageTime = currentMessageTime;

        // Настройка рендеринга в зависимости от типа сообщения
        switch (message.getType()) {
            case USER_JOINED:
            case USER_LEFT:
            case SERVER_MESSAGE:
                eventLabel.setText(getEventText(message, time));
                add(Box.createHorizontalGlue());
                add(eventLabel);
                add(Box.createHorizontalGlue());
                return this;
                
            case USER_MESSAGE:
                senderLabel.setText(sender);
                senderLabel.setForeground(isMyMessage ? MY_SENDER_COLOR : OTHER_SENDER_COLOR);
                senderLabel.setVisible(!isMyMessage && !isGroupedMessage);
                
                messageArea.setText(content);
                messageArea.setForeground(Color.WHITE); 
                timeLabel.setText(time);
                bubblePanel.setBackground(isMyMessage ? MY_BUBBLE_COLOR : OTHER_BUBBLE_COLOR);
                bubblePanel.setVisible(true);
                
                // Аватар отображаем только если новый пользователь и не свое сообщение
                if (!isMyMessage && !isGroupedMessage) {
                    // Получаем или создаем цвет для пользователя
                    Color userColor = getUserColor(sender);
                    
                    // Настраиваем аватар
                    avatarLabel.setBackground(userColor);
                    avatarLabel.setText(getInitials(sender));
                    avatarLabel.setBorder(new EmptyBorder(1, 1, 1, 1));
                    
                    // Добавляем компоненты
                    add(avatarLabel);
                    add(Box.createHorizontalStrut(5));
                    add(bubblePanel);
                    add(Box.createHorizontalGlue());
                } else if (!isMyMessage && isGroupedMessage) {
                    // Для последующих сообщений от того же пользователя - отступ вместо аватара
                    add(Box.createHorizontalStrut(AVATAR_SIZE + 5));
                    add(bubblePanel);
                    add(Box.createHorizontalGlue());
                } else {
                    // Для своих сообщений - справа
                    add(Box.createHorizontalGlue());
                    add(bubblePanel);
                }
                break;
                
            default:
                break;
        }

        revalidate();
        repaint();

        return this;
    }

    private String getEventText(Message message, String time) {
         switch (message.getType()) {
            case USER_JOINED: return String.format("→ %s присоединился к чату (%s)", message.getSender(), time);
            case USER_LEFT: return String.format("← %s покинул чат (%s)", message.getSender(), time);
            case SERVER_MESSAGE: return String.format("⚙ %s (%s)", message.getContent(), time);
            default: return "";
         }
    }
    
    // Получение инициалов пользователя для аватара
    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        
        if (name.length() == 1) return name.toUpperCase();
        
        StringBuilder initials = new StringBuilder();
        String[] parts = name.split("\\s+");
        
        if (parts.length == 1) {
            // Если имя из одного слова, берем первую букву
            return String.valueOf(name.charAt(0)).toUpperCase();
        } else {
            // Иначе первые буквы имени и фамилии
            for (int i = 0; i < Math.min(2, parts.length); i++) {
                if (!parts[i].isEmpty()) {
                    initials.append(parts[i].charAt(0));
                }
            }
        }
        
        return initials.toString().toUpperCase();
    }
    
    // Получение цвета для пользователя (сохраняется между сообщениями)
    private Color getUserColor(String username) {
        return userColors.computeIfAbsent(username, name -> {
            // Генерация уникального цвета для каждого пользователя
            int hash = Objects.hash(name) & 0xFFFFFF;
            // Преобразуем в HSB и ограничиваем яркость и насыщенность для лучшей видимости
            float hue = (hash & 0xFF) / 255.0f;
            float saturation = 0.7f + ((hash >> 8) & 0xFF) / 1020.0f; // 0.7-0.95
            float brightness = 0.7f + ((hash >> 16) & 0xFF) / 1020.0f; // 0.7-0.95
            
            return Color.getHSBColor(hue, saturation, brightness);
        });
    }
} 