/*  
 * Copyright 2012 Jan Mönnich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.cuseb;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class SearchField extends javax.swing.JToolBar {

    private Thread delayThread;
    private JTextField textFilter;
    private JButton buttonMagnifier;
    private ArrayList<SearchFieldListener> listeners;

    /** Creates new form SearchField */
    public SearchField() {
        initComponents();
        setBorder(new EmptyBorder(3, 3, 3, 3));
        textFilter = new JTextField();
        buttonMagnifier = new JButton(new ImageIcon(getClass().getResource(
                "/de/cuseb/images/system-search.png")));
        if (System.getProperty("os.name").contains("Mac")) {
            textFilter.putClientProperty("JTextField.variant", "search");
        } else {
            //textFilter.setLayout(new BorderLayout());
            //buttonMagnifier.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            buttonMagnifier.setContentAreaFilled(false);
            buttonMagnifier.setBorderPainted(false);
            buttonMagnifier.setFocusPainted(false);
            buttonMagnifier.setBounds(4, 4, 16, 17);
            textFilter.add(buttonMagnifier);
        }
        textFilter.setColumns(12);
        textFilter.setPreferredSize(new Dimension(150, 24));
        textFilter.setMargin(new Insets(0, 22, 0, 0));
        textFilter.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {
                if (delayThread != null && delayThread.isAlive()) {
                    delayThread.interrupt();
                }
                final char keyChar = e.getKeyChar();
                delayThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            if (keyChar != 10) {
                                Thread.sleep(500);
                            }
                            SwingUtilities.invokeLater(new Runnable() {

                                @Override
                                public void run() {
                                    fireSearchValueChanged();
                                }
                            });
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                });
                delayThread.start();
            }
        });
        listeners = new ArrayList<SearchFieldListener>();
        add(textFilter);
    }

    public void addSearchFieldListener(SearchFieldListener listener) {
        listeners.add(listener);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        buttonMagnifier.setEnabled(enabled);
        textFilter.setEditable(enabled);
        textFilter.setText("");
        fireSearchValueChanged();
    }

    public void setSearchColumns(Collection<String> columns) {
        popupSearch.removeAll();
        if (columns == null) {
            return;
        }
        for (String column : columns) {
            JMenuItem item = new JCheckBoxMenuItem(column);
            item.setSelected(true);
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    fireSearchValueChanged();
                }
            });
            popupSearch.add(item);
        }
    }

    public void clear() {
        textFilter.setText("");
        //fireSearchValueChanged();
    }

    private void fireSearchValueChanged() {
        int checkNr = 0;
        int result[] = new int[0];
        Component items[] = popupSearch.getComponents();
        for (Component item : items) {
            if (item instanceof JCheckBoxMenuItem) {
                JCheckBoxMenuItem check = (JCheckBoxMenuItem) item;
                if (check.isSelected()) {
                    result = Arrays.copyOf(result, result.length + 1);
                    result[result.length - 1] = checkNr;
                }
                checkNr++;
            }
        }
        for (SearchFieldListener listener : listeners) {
            listener.searchValueChanged(textFilter.getText());
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popupSearch = new javax.swing.JPopupMenu();

        setFloatable(false);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPopupMenu popupSearch;
    // End of variables declaration//GEN-END:variables
}
