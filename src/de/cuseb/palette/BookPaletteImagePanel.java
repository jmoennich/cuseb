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
import de.cuseb.flow.BookFlowImageFactory;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class BookPaletteImagePanel extends JPanel implements Runnable {

    private Book book;
    private int titlex;
    private String title;
    private Thread thread;
    private BufferedImage image;
    private boolean imageLoading;
    private boolean imageLoaded;
    private static float alpha;

    public BookPaletteImagePanel(Book book) {

        this.book = book;
        this.title = book.getTitelDeutsch();
        if (title == null) {
            title = "";
        }

        this.image = BookFlowImageFactory.getInstance().getDefaultImage();
        setBackground(Color.BLACK);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Textposition ausrechnen
        FontMetrics metrics = getFontMetrics(getFont());
        int width = metrics.stringWidth(title);
        while (width > BookFlowImageFactory.WIDTH && title.length() > 5) {
            title = title.substring(0, title.length() - 5) + "...";
            width = metrics.stringWidth(title);
        }
        titlex = (BookFlowImageFactory.WIDTH / 2) - (width / 2);
    }

    public boolean isImageLoaded() {
        return imageLoaded;
    }

    public void clearImage() {
        if (thread != null && thread.isAlive()) {
            thread.stop();
        }
        image = BookFlowImageFactory.getInstance().getDefaultImage();
        imageLoading = false;
        imageLoaded = false;
    }

    public static void setAlpha(float alpha) {
        BookPaletteImagePanel.alpha = alpha;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(image.getWidth(), image.getHeight() + 30);
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        // Bild asynchron laden
        if (!imageLoaded && !imageLoading) {
            thread = new Thread(this);
            thread.setPriority(Thread.MAX_PRIORITY);
            thread.start();
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(image, null, 0, 0);
        g2.setColor(Color.WHITE);
        g2.drawString(title, titlex, BookFlowImageFactory.HEIGHT + 25);
    }

    @Override
    public void run() {

        imageLoading = true;

        // Titelbild
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(
                    book.getTitelBild()));
            int width = source.getWidth();
            int height = source.getHeight();
            float scale;
            if (width > height) {
                scale = (float) BookFlowImageFactory.WIDTH / (float) width;
            } else {
                scale = (float) BookFlowImageFactory.HEIGHT / (float) height;
            }
            int scaledWidth = (int) ((float) width * scale);
            int scaledHeight = (int) ((float) height * scale);
            int x = (BookFlowImageFactory.WIDTH - scaledWidth) / 2;
            int y = (BookFlowImageFactory.HEIGHT - scaledHeight) / 2;
            image = new BufferedImage(
                    BookFlowImageFactory.WIDTH,
                    BookFlowImageFactory.HEIGHT,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g2.drawImage(source, x, y, scaledWidth, scaledHeight, null);
            g2.dispose();
        } catch (Exception e) {
            image = BookFlowImageFactory.getInstance().getDefaultImage();
        }
        // Bild auf null setzen, um Speicherfreigabe zu erlauben
        book.setTitelBild(null);
        imageLoading = false;
        imageLoaded = true;

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                invalidate();
                repaint();
            }
        });
    }
}
