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
package de.cuseb.palette;

import de.cuseb.data.Book;
import de.cuseb.data.BookSelectionListener;
import de.cuseb.table.BookTableModel;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.Timer;

public class BookPalette extends JPanel implements Runnable {

    private Timer fader;
    private float alpha;
    private final Thread thread;
    private ArrayList<BookSelectionListener> listeners;

    public BookPalette() {
        setBackground(Color.BLACK);
        setLayout(new GridBagLayout());
        listeners = new ArrayList<BookSelectionListener>();
        thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(ComponentEvent ce) {
                synchronized (thread) {
                    thread.notify();
                }
            }
        });
    }

    public void addBookSelectionListener(BookSelectionListener listener) {
        listeners.add(listener);
    }

    @Override
    public void run() {
        while (true) {
            try {
                Rectangle visible = getVisibleRect();
                for (Component child : getComponents()) {
                    BookPaletteImagePanel panel = (BookPaletteImagePanel) child;

                    // Bild geladen aber nicht im Viewport sichtbar?
                    if (panel.isImageLoaded()
                            && !panel.getBounds().intersects(visible)) {
                        panel.clearImage();
                    }
                }
                synchronized (thread) {
                    thread.wait();
                }
            } catch (Exception e) {
                // Ignorant!
            }
        }
    }

    public void update(JTable table) {

        // Einblenden
        alpha = 0f;
        BookPaletteImagePanel.setAlpha(alpha);
        fader = new Timer(10, new FaderTimer());
        fader.start();

        removeAll();
        BookTableModel model = (BookTableModel) table.getModel();
        int x = 0;
        int y = 0;
        GridBagConstraints grid = new GridBagConstraints();
        Insets insets = new Insets(10, 5, 10, 5);
        grid.weightx = 0.33d;
        grid.insets = insets;

        for (int i = 0; i < table.getRowCount(); i++) {
            final Book book = model.getBookAtRow(
                    table.getRowSorter().convertRowIndexToModel(i));
            BookPaletteImagePanel panel = new BookPaletteImagePanel(book);
            panel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1
                            && e.getClickCount() == 2) {
                        for (BookSelectionListener listener : listeners) {
                            listener.bookSelected(book);
                        }
                    }
                }
            });
            grid.gridx = x++;
            grid.gridy = y;
            add(panel, grid);
            if (x > 2) {
                x = 0;
                y++;
            }
        }
        updateUI();
    }

    private class FaderTimer implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            alpha += 0.025f;
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                fader.stop();
            }
            BookPaletteImagePanel.setAlpha(alpha);
            repaint();
        }
    }
}
