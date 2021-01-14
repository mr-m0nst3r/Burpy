package burp.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * @author ViCrack
 *
 * 显示文本的对话框, 支持缩放拉伸, 鼠标右键菜单显示
 *
 * TODO 鼠标滚轮可缩放字体, 支持切换编辑器语法高亮, 切换换行
 */
public class MessageDialog extends JDialog {

    private final JTextArea textArea = new JTextArea();
    private final String title;
    private final String msg;

    public MessageDialog(String title, String msg) {
        this.title = title;
        this.msg = msg;
        init();
    }

    private void init() {
        setTitle(title);
        getContentPane().setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textArea);

        textArea.setText(msg);
        textArea.setLineWrap(true);
        textArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        textArea.setCaretPosition(0);
        // 设置语法高亮
        //syntaxTextArea.setSyntaxEditingStyle();

        JPanel southPanel = new JPanel();
        JPanel southLeftPanel = new JPanel();
        JPanel southRightPanel = new JPanel();

        southPanel.setLayout(new BorderLayout());
        southPanel.add(southLeftPanel, BorderLayout.WEST);
        southPanel.add(southRightPanel, BorderLayout.EAST);

        FlowLayout flowLayout1 = new FlowLayout();
        flowLayout1.setAlignment(FlowLayout.LEFT);
        southLeftPanel.setLayout(flowLayout1);
        FlowLayout flowLayout2 = new FlowLayout();
        flowLayout2.setAlignment(FlowLayout.RIGHT);
        southRightPanel.setLayout(flowLayout2);

        getContentPane().add(southPanel, BorderLayout.SOUTH);
        JCheckBox chkLineWrap = new JCheckBox("LineWrap");
        chkLineWrap.setSelected(true);
        chkLineWrap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.setLineWrap(chkLineWrap.isSelected());
            }
        });
        southLeftPanel.add(chkLineWrap);
        JButton btnClose = new JButton();
        btnClose.setText("Close");
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        southRightPanel.add(btnClose);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(textArea);

        Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int)screensize.getWidth();
        int height = (int)screensize.getHeight();
        setSize(width / 3,  height / 3);

        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setAlwaysOnTop(true);
        setVisible(true);

    }

    /**
     * 显示文本窗口
     * @param title 对话框标题
     * @param msg   对话框的内容
     */
    public static void show(String title, String msg){
        new MessageDialog(title, msg);
    }

}
