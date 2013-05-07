package de.balpha.varsity;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ConfigForm {
    private JCheckBox cbPrimitive;
    private JPanel panel;
    private JTextField tfMinChars;

    public void setVerifier() {
        tfMinChars.setInputVerifier(new InputVerifier() {
            @Override
            public boolean verify(JComponent input) {
                try {
                    return Integer.parseInt(((JTextField)input).getText(), 10) >= 2;
                } catch (NumberFormatException nfe) {
                    return false;
                }
            }
        });
    }

    public JComponent getPanel() {
        return panel;
    }

    public boolean getFoldPrimitives() {
        return cbPrimitive.isSelected();
    }

    public void setFoldPrimitives(boolean yesno) {
        cbPrimitive.setSelected(yesno);
    }

    public int getMinChars() {
        String min = tfMinChars.getText();
        int result;
        try {
            result = Integer.parseInt(min, 10);
        } catch (NumberFormatException nfe) {
            result = 3;
        }
        if (result < 2)
            result = 2;
        return result;
    }

    public void setMinChars(int min) {
        if (min < 2)
            min = 2;
        tfMinChars.setText(min + "");
    }
}
