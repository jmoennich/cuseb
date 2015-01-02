/*  
 * Thanks to Romain Guy and Kevin Long who wrote/modified the original code
 * that can be found here:
 * http://www.curious-creature.org/2005/07/09/a-music-shelf-in-java2d/
 * 
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
package de.cuseb.flow;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class BookFlowImageFactory {

    public static final int WIDTH = 262;
    public static final int HEIGHT = 233;
    private static BookFlowImageFactory instance = null;
    private BufferedImage defaultImage;
    private BufferedImage defaultImageReflected;
    private BufferedImage mask;

    public static BookFlowImageFactory getInstance() {
        if (instance == null) {
            instance = new BookFlowImageFactory();
        }
        return instance;
    }

    private BookFlowImageFactory() {

        // Alpha-Verlauf für Spiegelungen
        mask = new BufferedImage(WIDTH, HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = mask.createGraphics();
        g2.setPaint(new GradientPaint(0.0f, 0.0f,
                new Color(1.0f, 1.0f, 1.0f, 0.5f), 0.0f, HEIGHT / 2.0f,
                new Color(1.0f, 1.0f, 1.0f, 1.0f)));
        g2.fill(new Rectangle2D.Double(0, 0, WIDTH, HEIGHT));
        g2.dispose();

        // Standard-Bild
        defaultImage = new BufferedImage(WIDTH, HEIGHT,
                BufferedImage.TYPE_INT_ARGB);
        int sx = 50;
        int ex = WIDTH - 50;
        Graphics2D g2d = (Graphics2D) defaultImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        GradientPaint gradient =
                new GradientPaint(0, 0,
                new Color(150, 150, 150), ex, HEIGHT,
                new Color(80, 80, 80));
        g2d.setPaint(gradient);
        g2d.fill(new Rectangle2D.Float(
                (float) sx, (float) 0,
                (float) ex - sx, (float) HEIGHT));
        g2d.setColor(Color.black);
        g2d.setFont(new Font(Font.DIALOG, Font.PLAIN, 50));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawString("?",
                (WIDTH - g2d.getFontMetrics().stringWidth("?")) / 2,
                HEIGHT / 2);
        g2d.dispose();
        defaultImageReflected = createReflectedPicture(defaultImage);
    }

    public BufferedImage getDefaultImage() {
        return defaultImage;
    }

    public BufferedImage getDefaultImageReflected() {
        return defaultImageReflected;
    }

    public final BufferedImage createReflectedPicture(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        float scale;
        if (width > height) {
            scale = (float) WIDTH / (float) width;
        } else {
            scale = (float) HEIGHT / (float) height;
        }
        int scaledWidth = (int) ((float) width * scale);
        int scaledHeight = (int) ((float) height * scale);
        int x = (WIDTH - scaledWidth) / 2;
        int y = (HEIGHT - scaledHeight) / 2;
        BufferedImage buffer = new BufferedImage(WIDTH, HEIGHT << 1,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffer.createGraphics();
        AffineTransform orig = g2.getTransform();
        g2.drawImage(source, x, y, scaledWidth, scaledHeight, null);
        g2.translate(0, scaledHeight << 1);
        g2.transform(AffineTransform.getScaleInstance(1.0, -1.0));
        g2.drawImage(source, x, y, scaledWidth, scaledHeight, null);
        g2.setTransform(orig);
        g2.setComposite(AlphaComposite.DstOut);
        g2.drawImage(mask, x, scaledHeight, null);
        g2.dispose();
        return buffer;
    }
}
