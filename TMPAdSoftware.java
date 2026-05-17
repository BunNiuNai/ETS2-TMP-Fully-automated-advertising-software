import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

public class TMPAdSoftware extends JFrame {
    private int currentMessageIndex;
    private boolean isRunning;
    private int countdownSeconds;
    private Timer timer;
    private Robot robot;
    private JTextField countdownField;
    private JLabel remainingTimeLabel;
    private JTextArea messageArea1;
    private JTextArea messageArea2;
    private JTextArea messageArea3;
    private JTextArea messageArea4;
    private JTextArea messageArea5;
    private JButton startButton;
    private JButton stopButton;

    public TMPAdSoftware() {
        super("TMP广告软件");
        this.currentMessageIndex = 0;
        this.isRunning = false;

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                releaseResources();
                dispose();
                System.exit(0);
            }
        });

        try {
            this.robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                "无法初始化Robot，自动发送功能可能无法正常使用",
                "错误",
                JOptionPane.ERROR_MESSAGE);
        }

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: 发送快捷键 + Y (固定)
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
        mainPanel.add(new JLabel("发送快捷键:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.weighty = 0;
        JLabel keyLabel = new JLabel("Y (固定)");
        keyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        mainPanel.add(keyLabel, gbc);

        // Row 1: 发送按键 + Enter (固定)
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
        mainPanel.add(new JLabel("发送按键:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.weighty = 0;
        JLabel enterLabel = new JLabel("Enter (固定)");
        enterLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        mainPanel.add(enterLabel, gbc);

        // Row 2: 倒计时(分钟)
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
        mainPanel.add(new JLabel("倒计时(分钟):"), gbc);

        gbc.gridx = 1; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.weighty = 0;
        this.countdownField = new JTextField(10);
        this.countdownField.setBackground(Color.WHITE);
        this.countdownField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        this.countdownField.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        mainPanel.add(this.countdownField, gbc);

        // Row 3: 剩余时间(秒)
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
        mainPanel.add(new JLabel("剩余时间(秒):"), gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1; gbc.weighty = 0;
        this.remainingTimeLabel = new JLabel("0");
        this.remainingTimeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        this.remainingTimeLabel.setForeground(Color.RED);
        mainPanel.add(this.remainingTimeLabel, gbc);

        // Row 4-8: 发送消息 1-5
        JTextArea[] areas = new JTextArea[5];
        for (int i = 0; i < 5; i++) {
            gbc.gridx = 0; gbc.gridy = 4 + i;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0; gbc.weighty = 0;
            mainPanel.add(new JLabel("发送消息" + (i + 1) + ":"), gbc);

            gbc.gridx = 1; gbc.gridy = 4 + i;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weightx = 1; gbc.weighty = 1;
            JTextArea area = new JTextArea(3, 30);
            area.setBackground(Color.WHITE);
            area.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            area.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            mainPanel.add(new JScrollPane(area), gbc);
            areas[i] = area;
        }
        this.messageArea1 = areas[0];
        this.messageArea2 = areas[1];
        this.messageArea3 = areas[2];
        this.messageArea4 = areas[3];
        this.messageArea5 = areas[4];

        // Row 9: 开始 + 暂停 buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        this.startButton = new JButton("开始");
        this.startButton.setBackground(new Color(70, 130, 180));
        this.startButton.setForeground(Color.WHITE);
        this.startButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        this.startButton.addActionListener(e -> start());
        buttonPanel.add(this.startButton);

        this.stopButton = new JButton("暂停");
        this.stopButton.setBackground(new Color(70, 130, 180));
        this.stopButton.setForeground(Color.WHITE);
        this.stopButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        this.stopButton.setEnabled(false);
        this.stopButton.addActionListener(e -> stop());
        buttonPanel.add(this.stopButton);

        gbc.gridx = 0; gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0; gbc.weighty = 0;
        mainPanel.add(buttonPanel, gbc);

        setContentPane(mainPanel);
        setSize(500, 650);
        setVisible(true);

        showUsageInstructions();
    }

    private void showUsageInstructions() {
        String instructions =
            "TMP广告软件 使用说明：\n\n" +
            "1. 发送快捷键：Y (固定)\n" +
            "2. 发送按键：Enter (固定)\n" +
            "3. 倒计时(分钟)：设置每次发送消息的间隔时间\n" +
            "4. 发送消息1-5：设置需要循环发送的广告消息\n" +
            "5. 开始按钮：开始自动发送消息\n" +
            "6. 暂停按钮：停止自动发送消息\n\n" +
            "使用步骤：\n" +
            "1. 填写消息内容（需要发送的广告）\n" +
            "2. 设置倒计时时间（分钟）\n" +
            "3. 点击开始按钮，软件会按1、2、3、4、5、1的顺序循环发送消息\n" +
            "4. 点击暂停按钮停止发送\n\n" +
            "注意：请在安全的环境中使用本软件，遵守当地法律法规。\n" +
            "提示：如果消息为空，将自动跳过该消息。";

        JOptionPane.showMessageDialog(this, instructions, "使用说明", JOptionPane.INFORMATION_MESSAGE);
    }

    private void start() {
        String countdownText = this.countdownField.getText().trim();
        if (countdownText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "请先填写倒计时时间或至少一个消息",
                "提示",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean hasMessage = !this.messageArea1.getText().trim().isEmpty()
            || !this.messageArea2.getText().trim().isEmpty()
            || !this.messageArea3.getText().trim().isEmpty()
            || !this.messageArea4.getText().trim().isEmpty()
            || !this.messageArea5.getText().trim().isEmpty();

        if (!hasMessage) {
            JOptionPane.showMessageDialog(this,
                "请先填写倒计时时间或至少一个消息",
                "提示",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            this.countdownSeconds = Integer.parseInt(countdownText) * 60;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                "请输入有效的倒计时时间",
                "提示",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.isRunning = true;
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(true);

        int intervalSeconds = Integer.parseInt(countdownText) * 60;

        this.timer = new Timer();
        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    remainingTimeLabel.setText(String.valueOf(countdownSeconds));
                    if (countdownSeconds <= 0) {
                        sendMessage();
                        countdownSeconds = intervalSeconds;
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
    }

    private void releaseResources() {
        if (this.timer != null) {
            this.timer.cancel();
            this.timer = null;
        }
        this.robot = null;
        this.countdownField = null;
        this.remainingTimeLabel = null;
        this.messageArea1 = null;
        this.messageArea2 = null;
        this.messageArea3 = null;
        this.messageArea4 = null;
        this.messageArea5 = null;
        this.startButton = null;
        this.stopButton = null;
    }

    private void sendMessage() {
        String message = null;
        boolean found = false;
        int startIndex = this.currentMessageIndex;

        while (!found) {
            switch (this.currentMessageIndex) {
                case 0:
                    message = this.messageArea1.getText().trim();
                    break;
                case 1:
                    message = this.messageArea2.getText().trim();
                    break;
                case 2:
                    message = this.messageArea3.getText().trim();
                    break;
                case 3:
                    message = this.messageArea4.getText().trim();
                    break;
                case 4:
                    message = this.messageArea5.getText().trim();
                    break;
            }

            if (message != null && !message.isEmpty()) {
                found = true;
            } else {
                this.currentMessageIndex = (this.currentMessageIndex + 1) % 5;
                if (this.currentMessageIndex == startIndex) {
                    break;
                }
            }
        }

        if (found && message != null && !message.isEmpty() && this.robot != null) {
            try {
                StringSelection stringSelection = new StringSelection(message);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);

                pressKey(this.robot, "Y");
                this.robot.delay(500);

                this.robot.keyPress(KeyEvent.VK_CONTROL);
                this.robot.keyPress(KeyEvent.VK_V);
                this.robot.keyRelease(KeyEvent.VK_V);
                this.robot.keyRelease(KeyEvent.VK_CONTROL);
                this.robot.delay(500);

                pressKey(this.robot, "ENTER");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.currentMessageIndex = (this.currentMessageIndex + 1) % 5;
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
                if (Character.isUpperCase(c)) {
                    robot.keyPress(KeyEvent.VK_SHIFT);
                }
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
                if (Character.isUpperCase(c)) {
                    robot.keyRelease(KeyEvent.VK_SHIFT);
                }
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TMPAdSoftware::new);
    }
}
