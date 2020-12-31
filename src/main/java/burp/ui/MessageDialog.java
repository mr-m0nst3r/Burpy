package burp.ui;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author ViCrack
 *
 * 显示文本的对话框, 支持缩放拉伸, 鼠标右键菜单显示
 *
 * TODO 鼠标滚轮可缩放字体, 支持切换编辑器语法高亮, 切换换行
 */
public class MessageDialog extends JDialog {

    private final RSyntaxTextArea syntaxTextArea = new RSyntaxTextArea();
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
        RTextScrollPane scrollPane = new RTextScrollPane(syntaxTextArea);

        syntaxTextArea.setText(msg);
        syntaxTextArea.setLineWrap(false);
        syntaxTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        syntaxTextArea.setCaretPosition(0);
        // 设置语法高亮
        //syntaxTextArea.setSyntaxEditingStyle();

        JPanel southPanel = new JPanel();
        JButton btnClose = new JButton();
        btnClose.setText("Close");
        btnClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        FlowLayout flowLayout1 = new FlowLayout();
        southPanel.setLayout(flowLayout1);
        flowLayout1.setAlignment(FlowLayout.RIGHT);
        getContentPane().add(southPanel, BorderLayout.SOUTH);
        southPanel.add(btnClose);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
        scrollPane.setViewportView(syntaxTextArea);

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
