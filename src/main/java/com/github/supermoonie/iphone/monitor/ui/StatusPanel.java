package com.github.supermoonie.iphone.monitor.ui;

import javax.swing.*;
import java.awt.*;

/**
 * @author super_w
 * @since 2021/6/15
 */
public class StatusPanel extends JPanel {

    private final JLabel statusField;

    public StatusPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        add(Box.createHorizontalStrut(5));
        add(Box.createHorizontalStrut(5));

        statusField = new JLabel("");
        statusField.setAlignmentX(LEFT_ALIGNMENT);
        add(statusField);
        add(Box.createHorizontalStrut(5));
        add(Box.createVerticalStrut(21));
        setBackground(Color.WHITE);
    }

    public void setStatusText(String text) {
        statusField.setText(text);
    }
}
