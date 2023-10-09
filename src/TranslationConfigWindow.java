import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

public class TranslationConfigWindow extends JFrame {
    private final JTextField vvmPathField;
    private final JComboBox<String> languageComboBox;


    public TranslationConfigWindow() {
        // 设置窗口标题
        setTitle("VVM-翻译助手配置");

        // 创建组件
        vvmPathField = new JTextField(20);
        JButton browseButton = new JButton("浏览");
        JButton translateButton = new JButton("开始翻译");
        JButton backupButton = new JButton("备份");
        JButton rollbackButton = new JButton("恢复备份");

        languageComboBox = new JComboBox<>(); // 可添加更多语言选项
        try {
            Translator.getAllLanguageFiles().forEach(s -> languageComboBox.addItem(s));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 设置布局
        setLayout(new FlowLayout());

        // 添加组件到窗口
        add(new JLabel("VisualVM路径:"));
        add(vvmPathField);
        add(browseButton);
        add(new JLabel("选择语言:"));
        add(languageComboBox);
        add(translateButton);
        add(backupButton);
        add(rollbackButton);

        // 设置事件监听器
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                vvmPathField.setText(fileChooser.getSelectedFile().toString());
            }
        });

        translateButton.addActionListener(e -> {
            try {
                Translator.setVvmPath(Path.of(vvmPathField.getText()));
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "发生错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }

            String selectedLanguage = (String) languageComboBox.getSelectedItem();

            try {
                Translator.useAllLanguages(selectedLanguage);
                // 弹出提示框示例
                JOptionPane.showMessageDialog(null, "翻译完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "发生错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        backupButton.addActionListener(e -> {
            try {
                Translator.setVvmPath(Path.of(vvmPathField.getText()));
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "发生错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            try {
                Translator.createBackup();
                JOptionPane.showMessageDialog(null, "备份完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "发生错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        rollbackButton.addActionListener(e -> {
            try {
                Translator.setVvmPath(Path.of(vvmPathField.getText()));
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "发生错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
            try {
                Translator.rollbackFromBackup();
                JOptionPane.showMessageDialog(null, "恢复备份完成！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "发生错误：" + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 设置窗口属性
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 120);
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TranslationConfigWindow().setVisible(true));
    }
}
