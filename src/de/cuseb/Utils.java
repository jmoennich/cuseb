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

import de.cuseb.data.BookDatabase;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.CropImageFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.ShortLookupTable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.util.Properties;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class Utils {

    private static final SecureRandom random = new SecureRandom();

    public static void handleError(Exception ex) {

        ex.printStackTrace(System.err);

        Object[] options = {"Fehlerbericht senden", "Abbruch"};
        if (JOptionPane.showOptionDialog(null,
                "Es ist folgender Fehler aufgetreten:\n" + ex.getMessage(),
                "Fehler",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                options,
                options[0]) != JOptionPane.OK_OPTION) {
            return;
        }

        final BookDatabase db = BookDatabase.getInstance();
        final String to = db.getConfigValue(BookDatabase.MAIL_SMTP_TO);
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host",
                db.getConfigValue(BookDatabase.MAIL_SMTP_HOST));

        Session mailSession = Session.getDefaultInstance(props,
                new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(
                        to, db.getConfigValue(
                        BookDatabase.MAIL_SMTP_PASSWORD));
            }
        });

        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ex.printStackTrace(new PrintStream(buffer));
            MimeMessage message = new MimeMessage(mailSession);
            message.setSubject(ex.getMessage());
            message.setContent(buffer.toString(), "text/plain");
            message.setFrom(new InternetAddress(to));
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(to));
            Transport transport = mailSession.getTransport();
            transport.connect();
            transport.sendMessage(message,
                    message.getRecipients(Message.RecipientType.TO));
            transport.close();
            JOptionPane.showMessageDialog(
                    null,
                    "Der Fehlerbericht wurde erfolgreich gesendet",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // TODO: move to ImageUtils
    public static BufferedImage autoCrop(BufferedImage source, float threshold) {

        int rgb;
        int backlo;
        int backhi;
        int width = source.getWidth();
        int height = source.getHeight();
        int startx = width;
        int starty = height;
        int destx = 0;
        int desty = 0;

        rgb = source.getRGB(source.getWidth() - 1, source.getHeight() - 1);
        backlo = ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + ((rgb) & 255) / 3;
        backlo = (int) (backlo - (backlo * threshold));
        if (backlo < 0) {
            backlo = 0;
        }
        backhi = ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + ((rgb) & 255) / 3;
        backhi = (int) (backhi + (backhi * threshold));

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rgb = source.getRGB(x, y);
                int sum = ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + ((rgb) & 255) / 3;
                if (sum < backlo || sum > backhi) {
                    if (y < starty) {
                        starty = y;
                    }
                    if (x < startx) {
                        startx = x;
                    }
                    if (y > desty) {
                        desty = y;
                    }
                    if (x > destx) {
                        destx = x;
                    }
                }
            }
        }
        System.out.println("crop: ["
                + startx + ", " + starty + ", "
                + destx + ", " + desty + "]");

        BufferedImage result = new BufferedImage(
                destx - startx, desty - starty,
                source.getType());
        result.getGraphics().drawImage(
                Toolkit.getDefaultToolkit().createImage(
                new FilteredImageSource(source.getSource(),
                new CropImageFilter(startx, starty, destx, desty))),
                0, 0, null);
        return result;
    }

    // TODO: move to ImageUtils
    public static byte[] toJPEG(BufferedImage source, float quality) throws Exception {
        ImageWriter iw = ImageIO.getImageWritersByFormatName("JPEG").next();
        ImageWriteParam iwp = iw.getDefaultWriteParam();
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(quality);
        ByteArrayOutputStream jpg = new ByteArrayOutputStream();
        IIOImage image = new IIOImage(source, null, null);
        iw.setOutput(new MemoryCacheImageOutputStream(jpg));
        iw.write(null, image, iwp);
        iw.dispose();
        byte[] bytes = jpg.toByteArray();
        System.out.println("[toJPEG: " + bytes.length + " bytes]");
        return bytes;
    }

    // TODO: move to ImageUtils
    public static BufferedImage brighten(BufferedImage image) {
        short brighten[] = new short[256];
        for (int i = 0; i < 256; i++) {
            short pixelValue = (short) (Math.sqrt((double) i * 255.0));
            if (pixelValue > 255) {
                pixelValue = 255;
            } else if (pixelValue < 0) {
                pixelValue = 0;
            }
            brighten[i] = pixelValue;
        }
        LookupTable lookupTable = new ShortLookupTable(0, brighten);
        LookupOp lop = new LookupOp(lookupTable, null);
        return lop.filter(image, image);
    }

    // TODO: move to ImageUtils
    public static ImageIcon scaleAsIcon(Image img, int height) {
        if (img == null) {
            return null;
        }
        return new ImageIcon(img.getScaledInstance(
                -1, height, Image.SCALE_AREA_AVERAGING));
    }

    // TODO: move to ImageUtils
    public static void createImageDirs(File base) {
        String dirs[] = new String[]{
            "0", "1", "2", "3", "4", "5", "6", "7",
            "8", "9", "a", "b", "c", "d", "e", "f"};
        for (String dir : dirs) {
            File subdir = new File(base, dir);
            if (!subdir.exists()) {
                subdir.mkdir();
            }
        }
    }

    public static String randomHexString() {
        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        StringBuilder result = new StringBuilder(40);
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // TODO: move to FileUtils
    public static byte[] bytesFromFile(File file) throws Exception {
        if (!file.exists()) {
            return null;
        }
        FileImageInputStream in = null;
        try {
            byte bytes[] = new byte[(int) file.length()];
            in = new FileImageInputStream(file);
            in.read(bytes);
            return bytes;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    // TODO: move to FileUtils
    public static void bytesToFile(byte[] bytes, File file) throws Exception {
        FileOutputStream out = null;
        if (bytes != null) {
            try {
                out = new FileOutputStream(file);
                out.write(bytes);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } else {
            file.delete();
        }
    }

    // TODO: move to GuiUtils
    public static void autoComplete(final JTextField field, final String column) {

        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {

                String low = field.getText().toLowerCase();
                if (low.length() == 0
                        || e.isActionKey()
                        || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    return;
                }

                ResultSet res = null;
                String sql = "SELECT DISTINCT " + column
                        + " FROM buch WHERE LOWER("
                        + column + ") LIKE '" + low + "%' LIMIT 1";
                try {
                    res = BookDatabase.getInstance().query(sql);
                    if (res.next()) {
                        int pos = field.getCaretPosition();
                        field.setText(res.getString(1));
                        field.setSelectionStart(pos);
                        field.setSelectionEnd(field.getText().length());
                    }
                } catch (Exception ex) {
                    // bewusst ignoriert
                } finally {
                    try {
                        if (res != null && res.getStatement() != null) {
                            res.getStatement().close();
                        }
                    } catch (Exception ex2) {
                        // bewusst ignoriert
                    }
                }
            }
        });
    }
}
