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
package de.cuseb.flow;

import de.cuseb.data.Book;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

public class BookFlowItem implements Runnable {

    private boolean imageLoaded = false;
    private BufferedImage image = null;
    private Thread thread;
    private Book book;

    public BookFlowItem(Book book) {
        this.image = BookFlowImageFactory.getInstance().getDefaultImageReflected();
        this.book = book;
    }

    public synchronized Image getImage() {
        if (!imageLoaded && thread == null) {
            thread = new Thread(this);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.start();
        }
        return image;
    }

    @Override
    public void run() {
        try {
            byte img[] = null;
            synchronized (this) {
                img = book.getTitelBild();
            }
            if (img != null) {
                BookFlowImageFactory fx = BookFlowImageFactory.getInstance();
                image = fx.createReflectedPicture(
                        ImageIO.read(new ByteArrayInputStream(img)));
            }
            imageLoaded = true;
        } catch (Exception e) {
            image = BookFlowImageFactory.getInstance().getDefaultImageReflected();
        }
    }

    public Book getBook() {
        return book;
    }

    public String getLabel() {
        return book.getTitelDeutsch();
    }
}
