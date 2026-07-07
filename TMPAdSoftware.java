import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.prefs.Preferences;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TMPAdSoftware extends JFrame {

    private static final long serialVersionUID = 1L;

    // ── 配置常量 ──────────────────────────────
    private static final int MESSAGE_COUNT       = 5;      // 消息输入框数量
    private static final int SEND_DELAY_MS       = 500;    // 按键间延迟(ms)
    private static final int MAX_COUNTDOWN_MINUTES = 1440; // 倒计时上限(24小时)
    private static final int LOG_PREVIEW_LENGTH   = 30;     // 日志中消息预览最大长度
    private static final int LOG_MAX_LINES        = 500;    // 日志区最大保留行数

    // ── 字体常量 ──────────────────────────────
    private static final Font FONT_HEADER = createFont("Microsoft YaHei UI", Font.BOLD, 20);
    private static final Font FONT_BODY   = createFont("Microsoft YaHei UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD   = createFont("Microsoft YaHei UI", Font.BOLD, 14);
    private static final Font FONT_VALUE  = createFont("Microsoft YaHei UI", Font.PLAIN, 13);

    /**
     * 创建字体，失败时回退到系统默认字体
     */
    private static Font createFont(String name, int style, int size) {
        // new Font() 不会抛异常，Java 自动处理缺失字体
        return new Font(name, style, size);
    }

    // ── 颜色方案 ──────────────────────────────
    private static final Color BG_MAIN       = new Color(0xF0, 0xF2, 0xF5);  // 整体背景
    private static final Color BG_CARD       = new Color(0xFF, 0xFF, 0xFF);  // 卡片背景
    private static final Color TEXT_PRIMARY  = new Color(0x30, 0x3F, 0x4F);  // 主文字
    private static final Color TEXT_MUTED    = new Color(0x90, 0x99, 0xA9);  // 次要文字
    private static final Color ACCENT_BLUE   = new Color(0x40, 0x90, 0xE0);  // 主题蓝
    private static final Color GREEN_START   = new Color(0x3C, 0xB3, 0x71);  // 开始绿
    private static final Color GREEN_DARK    = new Color(0x2E, 0x96, 0x5A);  // 开始绿(hover)
    private static final Color RED_STOP      = new Color(0xE7, 0x4C, 0x3C);  // 暂停红
    private static final Color RED_DARK      = new Color(0xC0, 0x39, 0x2B);  // 暂停红(hover)
    private static final Color HIGHLIGHT     = new Color(0xFF, 0xF3, 0xCD);  // 高亮黄
    private static final Color FIELD_BG      = new Color(0xF8, 0xF9, 0xFB);  // 输入框背景
    private static final Color BORDER_LIGHT  = new Color(0xDC, 0xDF, 0xE6);  // 卡片/输入框边框
    private static final Color LOG_BG        = new Color(0xFA, 0xFB, 0xFC);  // 日志背景
    private static final Color BORDER_CARD   = new Color(0xE4, 0xE7, 0xED);  // 卡片边框
    private static final Color GREEN_START_PRESS = new Color(0x27, 0x7A, 0x48); // 开始绿(press)
    private static final Color RED_STOP_PRESS    = new Color(0xA8, 0x23, 0x1C); // 暂停红(press)

    private static final Color[] MSG_DOT_COLORS = {
        new Color(0xE7, 0x4C, 0x3C),  // 1 红
        new Color(0xE6, 0x7E, 0x22),  // 2 橙
        new Color(0x3C, 0xB3, 0x71),  // 3 绿
        new Color(0x40, 0x90, 0xE0),  // 4 蓝
        new Color(0x9B, 0x59, 0xB6),  // 5 紫
    };

    // ── 字段 ──────────────────────────────────
    private volatile int currentMessageIndex;
    private volatile boolean isRunning;
    private volatile int countdownSeconds;
    private transient Timer timer;
    private transient Robot robot;
    private transient ExecutorService executor;
    private transient Preferences prefs;
    private JTextField countdownField;
    private JLabel remainingTimeLabel;
    private JLabel currentMessageLabel;
    private JLabel[] msgDotLabels;
    private JTextArea[] messageAreas;
    private JTextArea logArea;
    private RoundedButton startButton;
    private RoundedButton stopButton;
    private JLabel statusLabel;

    // ════════════════════════════════════════════
    //  自定义圆角按钮
    // ════════════════════════════════════════════
    private static class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;
        private Color normalBg, hoverBg, pressBg;
        private boolean hovered, pressed;
        private final int radius = 8;

        RoundedButton(String text, Color bg, Color hoverBg, Color pressBg, Color fg) {
            super(text);
            this.normalBg = bg;
            this.hoverBg = hoverBg;
            this.pressBg = pressBg;
            setForeground(fg);
            setFont(FONT_BOLD);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(8, 28, 8, 28));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                public void mouseExited(MouseEvent e)  { hovered = false; pressed = false; repaint(); }
                public void mousePressed(MouseEvent e)  { pressed = true; repaint(); }
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = pressed ? pressBg : hovered ? hoverBg : normalBg;
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, radius * 2, radius * 2));
            g2.setColor(getForeground());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(getText(), tx, ty);
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════
    //  构造
    // ════════════════════════════════════════════
    public TMPAdSoftware() {
        setTitle("TMP 广告软件");
        currentMessageIndex = 0;
        isRunning = false;
        prefs = Preferences.userNodeForPackage(TMPAdSoftware.class);
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "msg-sender");
            t.setDaemon(true);
            return t;
        });

        // ── 主面板 ──
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(BG_MAIN);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.weighty = 0;

        // ── 头部 ──
        mainPanel.add(buildHeader(), gbc);

        // ── 基本设置卡片 ──
        gbc.gridy = 1;
        mainPanel.add(buildSettingsCard(), gbc);

        // ── 消息卡片 ──
        gbc.gridy = 2;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(buildMessagesCard(), gbc);

        // ── 按钮 ──
        gbc.gridy = 3;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(buildButtonBar(), gbc);

        // ── 日志卡片 ──
        gbc.gridy = 4;
        gbc.weighty = 2;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(buildLogCard(), gbc);

        setContentPane(mainPanel);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveSettings();
                releaseResources();
                dispose();
                System.exit(0);
            }
        });

        // Robot 初始化（必须在 UI 组件创建之后，以便失败时禁用按钮）
        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            startButton.setEnabled(false);
            startButton.setToolTipText("Robot 初始化失败");
            appendLog("错误: Robot 初始化失败，自动发送功能不可用");
            JOptionPane.showMessageDialog(this,
                "无法初始化 Robot，自动发送功能可能无法正常使用",
                "错误", JOptionPane.ERROR_MESSAGE);
        }

        setSize(540, 860);
        setMinimumSize(new Dimension(460, 700));
        setVisible(true);

        loadSettings();
        showUsageInstructions();
    }

    // ── 头部面板 ──────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG_MAIN);
        p.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        JLabel title = new JLabel("TMP 广告软件");
        title.setFont(FONT_HEADER);
        title.setForeground(TEXT_PRIMARY);

        statusLabel = new JLabel("● 就绪");
        statusLabel.setFont(FONT_VALUE);
        statusLabel.setForeground(TEXT_MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));

        p.add(title, BorderLayout.WEST);
        p.add(statusLabel, BorderLayout.EAST);
        return p;
    }

    // ── 基本设置卡片 ──────────────────────────
    private JPanel buildSettingsCard() {
        JPanel card = createCard();

        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 4, 8);
        g.anchor = GridBagConstraints.WEST;

        // 快捷键
        g.gridx = 0; g.gridy = 0; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        card.add(makeLabel("发送快捷键", false), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        card.add(makeValueLabel("Y（固定）"), g);

        // 发送键
        g.gridx = 0; g.gridy = 1; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        card.add(makeLabel("发送按键", false), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        card.add(makeValueLabel("Enter（固定）"), g);

        // 倒计时
        g.gridx = 0; g.gridy = 2; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        card.add(makeLabel("倒计时（分钟）", false), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        this.countdownField = new JTextField(8);
        styleTextField(countdownField);
        card.add(countdownField, g);

        // 剩余时间
        g.gridx = 0; g.gridy = 3; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        card.add(makeLabel("剩余时间（秒）", false), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        this.remainingTimeLabel = new JLabel("0");
        this.remainingTimeLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 22));
        this.remainingTimeLabel.setForeground(ACCENT_BLUE);
        card.add(this.remainingTimeLabel, g);

        // 当前发送
        g.gridx = 0; g.gridy = 4; g.weightx = 0; g.fill = GridBagConstraints.NONE;
        card.add(makeLabel("当前发送", false), g);
        g.gridx = 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        this.currentMessageLabel = new JLabel("（未启动）");
        this.currentMessageLabel.setFont(FONT_BOLD);
        this.currentMessageLabel.setForeground(TEXT_MUTED);
        card.add(this.currentMessageLabel, g);

        return card;
    }

    // ── 消息卡片 ──────────────────────────────
    private JPanel buildMessagesCard() {
        JPanel card = createCard("广告消息");

        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 8, 3, 8);
        g.fill = GridBagConstraints.BOTH;
        g.gridwidth = 1;

        this.messageAreas = new JTextArea[MESSAGE_COUNT];
        this.msgDotLabels = new JLabel[MESSAGE_COUNT];

        for (int i = 0; i < MESSAGE_COUNT; i++) {
            // 左边：彩色圆点 + 标签
            JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            leftPanel.setOpaque(false);

            // 彩色圆点
            JLabel dot = new JLabel("●");
            dot.setFont(new Font("Dialog", Font.BOLD, 12));
            dot.setForeground(MSG_DOT_COLORS[i]);
            msgDotLabels[i] = dot;

            JLabel label = makeLabel("消息 " + (i + 1), false);
            leftPanel.add(dot);
            leftPanel.add(label);

            g.gridx = 0; g.gridy = i;
            g.weightx = 0; g.weighty = 0;
            g.fill = GridBagConstraints.NONE;
            g.anchor = GridBagConstraints.NORTHWEST;
            card.add(leftPanel, g);

            // 右边：输入框
            JTextArea area = new JTextArea(3, 30);
            area.setBackground(FIELD_BG);
            area.setFont(FONT_BODY);
            area.setBorder(new CompoundBorder(
                new RoundedLineBorder(BORDER_LIGHT, 1, 6),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
            ));
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            messageAreas[i] = area;

            g.gridx = 1; g.gridy = i;
            g.weightx = 1; g.weighty = 1;
            g.fill = GridBagConstraints.BOTH;
            card.add(new JScrollPane(area), g);
        }

        return card;
    }

    // ── 按钮栏 ────────────────────────────────
    private JPanel buildButtonBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        p.setOpaque(false);

        startButton = new RoundedButton("▶  开始", GREEN_START, GREEN_DARK, GREEN_START_PRESS, Color.WHITE);
        startButton.addActionListener(e -> start());
        p.add(startButton);

        stopButton = new RoundedButton("■  暂停", RED_STOP, RED_DARK, RED_STOP_PRESS, Color.WHITE);
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stop());
        p.add(stopButton);

        return p;
    }

    // ── 日志卡片 ──────────────────────────────
    private JPanel buildLogCard() {
        JPanel card = createCard("发送日志");

        card.setLayout(new BorderLayout());
        this.logArea = new JTextArea(5, 30);
        this.logArea.setEditable(false);
        this.logArea.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        this.logArea.setBackground(LOG_BG);
        this.logArea.setForeground(TEXT_MUTED);
        this.logArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        card.add(new JScrollPane(this.logArea), BorderLayout.CENTER);

        return card;
    }

    // ── 辅助：卡片容器 ─────────────────────────
    private JPanel createCard() {
        return createCard(null);
    }

    private JPanel createCard(String title) {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        if (title != null) {
            TitledBorder tb = BorderFactory.createTitledBorder(
                new RoundedLineBorder(BORDER_CARD, 1, 8),
                title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Microsoft YaHei UI", Font.BOLD, 13),
                TEXT_PRIMARY
            );
            card.setBorder(tb);
        } else {
            card.setBorder(new CompoundBorder(
                new RoundedLineBorder(BORDER_CARD, 1, 8),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));
        }
        return card;
    }

    // ── 辅助：标签 ─────────────────────────────
    private JLabel makeLabel(String text, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private JLabel makeValueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_VALUE);
        l.setForeground(TEXT_MUTED);
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }

    private void styleTextField(JTextField tf) {
        tf.setFont(FONT_BODY);
        tf.setBackground(FIELD_BG);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_BLUE);
        tf.setBorder(new CompoundBorder(
            new RoundedLineBorder(BORDER_LIGHT, 1, 6),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    // ════════════════════════════════════════════
    //  自定义圆角边框
    // ════════════════════════════════════════════
    private static class RoundedLineBorder extends AbstractBorder {
        private static final long serialVersionUID = 1L;
        private final Color color;
        private final int thickness;
        private final int radius;

        RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            int r = radius;
            g2.draw(new RoundRectangle2D.Float(
                x + thickness / 2f, y + thickness / 2f,
                w - thickness, h - thickness, r * 2, r * 2));
            g2.dispose();
        }

        @Override public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 2, thickness + 2, thickness + 2, thickness + 2);
        }
    }

    // ════════════════════════════════════════════
    //  业务方法
    // ════════════════════════════════════════════

    private void showUsageInstructions() {
        String instructions =
            "TMP广告软件 使用说明：\n\n" +
            "1. 发送快捷键：Y (固定)\n" +
            "2. 发送按键：Enter (固定)\n" +
            "3. 倒计时(分钟)：设置每次发送消息的间隔时间\n" +
            "4. 发送消息1-5：设置需要循环发送的广告消息\n" +
            "5. 当前发送：显示正在发送第几条消息（黄色高亮）\n" +
            "6. 发送日志：记录每次发送的时间和内容\n" +
            "7. 开始按钮：开始自动发送消息\n" +
            "8. 暂停按钮：停止自动发送消息\n\n" +
            "使用步骤：\n" +
            "1. 填写消息内容（需要发送的广告）\n" +
            "2. 设置倒计时时间（分钟）\n" +
            "3. 点击开始按钮，软件会按1、2、3、4、5、1的顺序循环发送消息\n" +
            "4. 点击暂停按钮停止发送\n\n" +
            "注意：请在安全的环境中使用本软件，遵守当地法律法规。\n" +
            "提示：关闭软件时自动保存消息和设置，下次启动自动恢复。";

        JOptionPane.showMessageDialog(this, instructions, "使用说明", JOptionPane.INFORMATION_MESSAGE);
    }

    // ---------- 持久化 ----------

    private void loadSettings() {
        String countdown = prefs.get("countdown", "");
        if (!countdown.isEmpty()) {
            countdownField.setText(countdown);
        }
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String msg = prefs.get("message" + i, "");
            if (!msg.isEmpty()) {
                messageAreas[i].setText(msg);
            }
        }
    }

    private void saveSettings() {
        String countdown = countdownField.getText().trim();
        if (!countdown.isEmpty()) {
            prefs.put("countdown", countdown);
        } else {
            prefs.remove("countdown");
        }
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String msg = messageAreas[i].getText().trim();
            if (!msg.isEmpty()) {
                prefs.put("message" + i, msg);
            } else {
                prefs.remove("message" + i);
            }
        }
    }

    // ---------- 高亮 ----------

    private void highlightMessage(int index) {
        for (int i = 0; i < messageAreas.length; i++) {
            messageAreas[i].setBackground(i == index ? HIGHLIGHT : FIELD_BG);
            msgDotLabels[i].setText(i == index ? "▶" : "●");
            msgDotLabels[i].setForeground(i == index ? ACCENT_BLUE : MSG_DOT_COLORS[i]);
        }
        currentMessageLabel.setText("消息 " + (index + 1));
        currentMessageLabel.setForeground(ACCENT_BLUE);
        statusLabel.setText("● 运行中");
        statusLabel.setForeground(GREEN_START);
    }

    private void clearHighlight() {
        for (int i = 0; i < messageAreas.length; i++) {
            messageAreas[i].setBackground(FIELD_BG);
            msgDotLabels[i].setText("●");
            msgDotLabels[i].setForeground(MSG_DOT_COLORS[i]);
        }
        currentMessageLabel.setText("（未启动）");
        currentMessageLabel.setForeground(TEXT_MUTED);
        statusLabel.setText("● 就绪");
        statusLabel.setForeground(TEXT_MUTED);
    }

    // ---------- 日志 ----------

    private void appendLog(String text) {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        logArea.append("[" + timestamp + "] " + text + "\n");
        // 防内存泄漏：超过最大行数时移除最早的记录
        try {
            int lineCount = logArea.getLineCount();
            if (lineCount > LOG_MAX_LINES) {
                int end = logArea.getLineStartOffset(lineCount - LOG_MAX_LINES);
                logArea.replaceRange("", 0, end);
            }
        } catch (javax.swing.text.BadLocationException e) {
            // 极少发生，忽略以保证稳定性
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ---------- 校验工具（可测试） ----------

    /**
     * 返回当前倒计时秒数。package-private 供测试使用。
     */
    int getCountdownSeconds() {
        return countdownSeconds;
    }

    /**
     * 校验倒计时输入。返回 null 表示通过，否则返回错误消息。
     */
    static String validateCountdown(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "请输入倒计时时间";
        }
        int minutes;
        try {
            minutes = Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return "请输入有效的整数";
        }
        if (minutes <= 0) {
            return "倒计时必须大于 0 分钟";
        }
        if (minutes > MAX_COUNTDOWN_MINUTES) {
            return "倒计时不能超过 " + MAX_COUNTDOWN_MINUTES + " 分钟（24 小时）";
        }
        return null;
    }

    /**
     * 将分钟转为秒。抛出 ArithmeticException 如果溢出。
     */
    static int parseCountdown(String input) {
        int minutes = Integer.parseInt(input.trim());
        return Math.multiplyExact(minutes, 60);
    }

    // ---------- 控制 ----------

    private void start() {
        String countdownText = this.countdownField.getText().trim();
        if (countdownText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "请先填写倒计时时间和至少一个消息",
                "提示",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean hasMessage = false;
        for (JTextArea area : messageAreas) {
            if (!area.getText().trim().isEmpty()) {
                hasMessage = true;
                break;
            }
        }

        if (!hasMessage) {
            JOptionPane.showMessageDialog(this,
                "请先填写倒计时时间和至少一个消息",
                "提示",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        String validationError = validateCountdown(countdownText);
        if (validationError != null) {
            JOptionPane.showMessageDialog(this, validationError, "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int intervalSeconds = -1;
        try {
            intervalSeconds = parseCountdown(countdownText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "请输入有效的倒计时时间",
                "提示",
                JOptionPane.WARNING_MESSAGE);
            return;
        } catch (ArithmeticException e) {
            JOptionPane.showMessageDialog(this,
                "倒计时数值过大，请减小",
                "提示",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.isRunning = true;
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(true);

        int intervalMinutes = intervalSeconds / 60;

        while (messageAreas[currentMessageIndex].getText().trim().isEmpty()) {
            currentMessageIndex = (currentMessageIndex + 1) % MESSAGE_COUNT;
        }
        highlightMessage(currentMessageIndex);
        appendLog("开始发送，间隔 " + intervalMinutes + " 分钟");

        // 初始化倒计时秒数（修复：v3.1 漏掉了此赋值，导致首次立即发送）
        this.countdownSeconds = intervalSeconds;
        final int initialIntervalSeconds = intervalSeconds; // effectively final copy for lambda

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    remainingTimeLabel.setText(String.valueOf(countdownSeconds));
                    if (countdownSeconds <= 0) {
                        executor.submit(() -> sendMessage());
                        countdownSeconds = initialIntervalSeconds;
                    }
                    countdownSeconds--;
                });
            }
        }, 0, 1000L);
    }

    private void stop() {
        this.isRunning = false;
        this.startButton.setEnabled(true);
        this.stopButton.setEnabled(false);

        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
        this.remainingTimeLabel.setText("0");
        clearHighlight();
        appendLog("已暂停");
    }

    private void releaseResources() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
        if (this.executor != null) {
            this.executor.shutdown();
            try {
                if (!this.executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        this.robot = null;
    }

    private void sendMessage() {
        // P0-2: 已暂停则放弃本次发送（stop() 可能在此任务排队期间触发）
        if (!isRunning) return;

        // P0-1: 在 EDT 快照消息文本，避免非 EDT 线程访问 Swing 组件
        final String[] messages = new String[MESSAGE_COUNT];
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (int i = 0; i < MESSAGE_COUNT; i++) {
                    messages[i] = messageAreas[i].getText().trim();
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            Thread.currentThread().interrupt();
            SwingUtilities.invokeLater(() -> appendLog("发送取消: 无法读取消息内容"));
            return;
        }

        // 再次检查运行状态，避免快照期间被停止
        if (!isRunning) return;

        String message = null;
        boolean found = false;
        int startIndex = this.currentMessageIndex;

        while (!found) {
            message = messages[currentMessageIndex];
            if (message != null && !message.isEmpty()) {
                found = true;
            } else {
                currentMessageIndex = (currentMessageIndex + 1) % MESSAGE_COUNT;
                if (currentMessageIndex == startIndex) {
                    break;
                }
            }
        }

        if (found && message != null && !message.isEmpty() && this.robot != null) {
            try {
                StringSelection ss = new StringSelection(message);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(ss, null);

                pressKey(this.robot, "Y");
                this.robot.delay(SEND_DELAY_MS);

                this.robot.keyPress(KeyEvent.VK_CONTROL);
                this.robot.keyPress(KeyEvent.VK_V);
                this.robot.keyRelease(KeyEvent.VK_V);
                this.robot.keyRelease(KeyEvent.VK_CONTROL);
                this.robot.delay(SEND_DELAY_MS);

                pressKey(this.robot, "ENTER");

                final int sentIndex = currentMessageIndex;
                final String sentMsg = message.length() > LOG_PREVIEW_LENGTH
                    ? message.substring(0, LOG_PREVIEW_LENGTH) + "..." : message;
                SwingUtilities.invokeLater(() -> {
                    appendLog("已发送 消息" + (sentIndex + 1) + ": " + sentMsg);
                });
            } catch (Exception e) {
                final String errMsg = "发送失败: " + e.getMessage();
                SwingUtilities.invokeLater(() -> appendLog(errMsg));
            }
        }

        currentMessageIndex = (currentMessageIndex + 1) % MESSAGE_COUNT;
        int checkStart = currentMessageIndex;
        while (messages[currentMessageIndex] == null || messages[currentMessageIndex].isEmpty()) {
            currentMessageIndex = (currentMessageIndex + 1) % MESSAGE_COUNT;
            if (currentMessageIndex == checkStart) break;
        }

        final int nextIndex = currentMessageIndex;
        SwingUtilities.invokeLater(() -> highlightMessage(nextIndex));
    }

    private void pressKey(Robot robot, String keyName) {
        if (keyName.equalsIgnoreCase("ENTER")) {
            robot.keyPress(KeyEvent.VK_ENTER);
            robot.keyRelease(KeyEvent.VK_ENTER);
        } else if (keyName.equalsIgnoreCase("SPACE")) {
            robot.keyPress(KeyEvent.VK_SPACE);
            robot.keyRelease(KeyEvent.VK_SPACE);
        } else if (keyName.equalsIgnoreCase("TAB")) {
            robot.keyPress(KeyEvent.VK_TAB);
            robot.keyRelease(KeyEvent.VK_TAB);
        } else if (keyName.equalsIgnoreCase("ESC")) {
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
        } else if (keyName.length() == 1) {
            char c = keyName.charAt(0);
            if (Character.isLetter(c)) {
                int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
                boolean needShift = Character.isUpperCase(c);
                try {
                    if (needShift) {
                        robot.keyPress(KeyEvent.VK_SHIFT);
                    }
                    robot.keyPress(keyCode);
                    robot.keyRelease(keyCode);
                } finally {
                    if (needShift) {
                        robot.keyRelease(KeyEvent.VK_SHIFT);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        // 设置系统原生外观，提升跨平台一致性
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        // 全局未捕获异常处理器，避免异常静默丢失
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            System.err.println("[" + t.getName() + "] 未捕获异常: " + e);
            e.printStackTrace();
        });
        SwingUtilities.invokeLater(() -> {
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
                System.err.println("[EDT] 未捕获异常: " + e);
                e.printStackTrace();
            });
            new TMPAdSoftware();
        });
    }
}
