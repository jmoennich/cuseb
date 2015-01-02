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
package de.cuseb.dialogs;

import de.cuseb.MainFrame;
import de.cuseb.Utils;
import de.cuseb.data.Book;
import de.cuseb.data.BookDatabase;
import de.cuseb.data.amazon.AmazonBookLookup;
import de.cuseb.data.amazon.AmazonBookLookupListener;
import de.cuseb.tree.BookTreeNode;
import java.awt.Cursor;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

public class BookEditDialog extends javax.swing.JDialog implements KeyListener,
        FocusListener, AmazonBookLookupListener {

    private Book book;
    private boolean continuous;
    private MainFrame mainFrame;
    private BufferedImage image;
    private AmazonBookLookup amazon;
    private static SimpleDateFormat format =
            new SimpleDateFormat("'geändert am' dd.MM.yyyy 'um' HH:mm");

    public BookEditDialog(MainFrame parent, Book book) {

        super(parent, true);
        this.book = book;
        this.mainFrame = parent;
        this.continuous = (book == null);
        amazon = new AmazonBookLookup();
        amazon.addAmazonBookLookupListener(this);
        initComponents();

        textAuflage.addKeyListener(this);
        textAutor.addKeyListener(this);
        textFormat.addKeyListener(this);
        textGenre.addKeyListener(this);
        textJahr.addKeyListener(this);
        textKommentar.addKeyListener(this);
        textLfdNr.addKeyListener(this);
        textOrt.addKeyListener(this);
        textPreis.addKeyListener(this);
        textReihe.addKeyListener(this);
        textTitelDeutsch.addKeyListener(this);
        textTitelFremd.addKeyListener(this);
        textVerlag.addKeyListener(this);
        textZustand.addKeyListener(this);
        textZyklus.addKeyListener(this);

        textAuflage.addFocusListener(this);
        textAutor.addFocusListener(this);
        textFormat.addFocusListener(this);
        textGenre.addFocusListener(this);
        textJahr.addFocusListener(this);
        textKommentar.addFocusListener(this);
        textLfdNr.addFocusListener(this);
        textOrt.addFocusListener(this);
        textPreis.addFocusListener(this);
        textReihe.addFocusListener(this);
        textTitelDeutsch.addFocusListener(this);
        textTitelFremd.addFocusListener(this);
        textVerlag.addFocusListener(this);
        textZustand.addFocusListener(this);
        textZyklus.addFocusListener(this);

        if (book != null) {
            setTitle("Eintrag bearbeiten");
            textAuflage.setText(Integer.toString(book.getAuflage()));
            textAutor.setText(book.getAutor());
            textFormat.setText(book.getFormat());
            textGenre.setText(book.getGenre());
            textJahr.setText(book.getJahr());
            textKommentar.setText(book.getKommentar());
            textLfdNr.setText(Long.toString(book.getLfdNr()));
            textOrt.setText(book.getOrt());
            textPreis.setText(Float.toString(book.getPreis()));
            textReihe.setText(book.getReihe());
            textTitelDeutsch.setText(book.getTitelDeutsch());
            textTitelFremd.setText(book.getTitelFremd());
            textVerlag.setText(book.getVerlag());
            textZustand.setText(book.getZustand());
            textZyklus.setText(book.getZyklus());
            Date change = book.getLaetzteAenderung();
            if (change != null) {
                labelChange.setText(format.format(change));
            }
            try {
                byte[] img = book.getTitelBild();
                if (img != null) {
                    image = ImageIO.read(new ByteArrayInputStream(img));
                    labelImage.setCursor(
                            Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    labelImage.setIcon(Utils.scaleAsIcon(
                            image, labelImage.getHeight()));
                    buttonScanDelete.setEnabled(true);
                }
            } catch (Exception e) {
                Utils.handleError(e);
            }
        } else {
            BookTreeNode nodeSammlung = mainFrame.getSelectedTreeNodeParent(
                    BookTreeNode.Type.SAMMLUNG);
            setTitle("Neuer Eintrag in Sammlung '" + nodeSammlung + "'");
            textAuflage.setText("1");
        }
        buttonSave.setEnabled(false);

        Utils.autoComplete(textVerlag, "verlag");
        Utils.autoComplete(textReihe, "reihe");
        Utils.autoComplete(textAutor, "autor");
        Utils.autoComplete(textOrt, "ort");
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        Object src = e.getSource();
        if (src instanceof JTextField) {
            JTextField field = (JTextField) e.getSource();
            field.setText(field.getText().trim());
            field.setSelectionStart(0);
            field.setSelectionEnd(0);
            field.repaint();
        }
        if (src == textGenre) {
            textGenre.setText(textGenre.getText().toUpperCase());
        } else if (src == textFormat) {
            textFormat.setText(textFormat.getText().toUpperCase());
        } else if (src == textAuflage) {
            textAuflage.setText(textAuflage.getText().replaceAll("[^0-9]+", ""));
        } else if (src == textPreis) {
            String preis = textPreis.getText();
            preis = preis.replaceAll(",", ".");
            preis = preis.replaceAll("[^0-9\\.]+", "");
            textPreis.setText(preis);
        } else if (src == textZustand) {
            textZustand.setText(textZustand.getText().replaceAll(" ", ""));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
        if (e.getSource() instanceof JTextField) {
            if(!e.isActionKey()) {
                labelStatus.setText(null);
                buttonSave.setEnabled(true);
            }
            JTextField field = (JTextField) e.getSource();
            if (e.getKeyCode() == 10) {
                if (field == textKommentar) {
                    buttonSave.requestFocus();
                } else {
                    field.transferFocus();
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    private void error(String text) {
        JOptionPane.showMessageDialog(this, text, "Fehler",
                JOptionPane.ERROR_MESSAGE);
    }

    private void focus(JTextField field) {
        field.requestFocus();
        int length = field.getText().length();
        if (length > 0) {
            length--;
        }
        field.setCaretPosition(length);
    }

    @Override
    public void amazonLookupFailed(Exception e) {
        Utils.handleError(e);
    }

    @Override
    public void amazonLookupSucceeded() {
        textAutor.setText(amazon.getAuthor());
        textPreis.setText(Float.toString(amazon.getListPrice()));
        textJahr.setText(Integer.toString(amazon.getPublicationYear()));
        textVerlag.setText(amazon.getPublisher());
        textTitelDeutsch.setText(amazon.getTitle());
        textZustand.setText("1");
        textAuflage.setText("1");
        try {
            image = amazon.getLargeImage();
            book.setTitelBild(Utils.toJPEG(image, 1.0f));
            labelImage.setIcon(Utils.scaleAsIcon(image,
                    labelImage.getHeight()));
        } catch (Exception e) {
            Utils.handleError(e);
        }
        if (amazon.getBinding().contains("Taschenbuch")) {
            textFormat.setText("TB");
        } else if (amazon.getBinding().contains("Broschiert")) {
            textFormat.setText("PB");
        } else if (amazon.getBinding().contains("Gebunden")) {
            textFormat.setText("HC");
        }
    }

    public void setVerlag(String verlag) {
        textVerlag.setText(verlag);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        labelVerlag = new javax.swing.JLabel();
        textVerlag = new javax.swing.JTextField();
        labelReihe = new javax.swing.JLabel();
        textReihe = new javax.swing.JTextField();
        labelLfdNr = new javax.swing.JLabel();
        textLfdNr = new javax.swing.JTextField();
        labelTitelDeutsch = new javax.swing.JLabel();
        textTitelDeutsch = new javax.swing.JTextField();
        labelTitelFremd = new javax.swing.JLabel();
        textTitelFremd = new javax.swing.JTextField();
        labelZyklus = new javax.swing.JLabel();
        textZyklus = new javax.swing.JTextField();
        labelAutor = new javax.swing.JLabel();
        textAutor = new javax.swing.JTextField();
        labelGenre = new javax.swing.JLabel();
        textGenre = new javax.swing.JTextField();
        labelFormat = new javax.swing.JLabel();
        textFormat = new javax.swing.JTextField();
        labelAuflage = new javax.swing.JLabel();
        textAuflage = new javax.swing.JTextField();
        Zustand = new javax.swing.JLabel();
        textZustand = new javax.swing.JTextField();
        labelPreis = new javax.swing.JLabel();
        textPreis = new javax.swing.JTextField();
        labelJahr = new javax.swing.JLabel();
        textJahr = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        buttonSave = new javax.swing.JButton();
        buttonScan = new javax.swing.JButton();
        buttonScanDelete = new javax.swing.JButton();
        labelStatus = new javax.swing.JLabel();
        textOrt = new javax.swing.JTextField();
        labelOrt = new javax.swing.JLabel();
        labelKommentar = new javax.swing.JLabel();
        textKommentar = new javax.swing.JTextField();
        labelImage = new javax.swing.JLabel();
        labelChange = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Neues Eintrag");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        labelVerlag.setText("Verlag");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(15, 10, 2, 5);
        getContentPane().add(labelVerlag, gridBagConstraints);

        textVerlag.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(15, 5, 1, 5);
        getContentPane().add(textVerlag, gridBagConstraints);

        labelReihe.setText("Reihe");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelReihe, gridBagConstraints);

        textReihe.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textReihe, gridBagConstraints);

        labelLfdNr.setText("Laufende Nr.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelLfdNr, gridBagConstraints);

        textLfdNr.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textLfdNr, gridBagConstraints);

        labelTitelDeutsch.setText("Titel Deutsch");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelTitelDeutsch, gridBagConstraints);

        textTitelDeutsch.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 10);
        getContentPane().add(textTitelDeutsch, gridBagConstraints);

        labelTitelFremd.setText("Titel Fremd");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelTitelFremd, gridBagConstraints);

        textTitelFremd.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 10);
        getContentPane().add(textTitelFremd, gridBagConstraints);

        labelZyklus.setText("Zyklus");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelZyklus, gridBagConstraints);

        textZyklus.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 10);
        getContentPane().add(textZyklus, gridBagConstraints);

        labelAutor.setText("Autor");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelAutor, gridBagConstraints);

        textAutor.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 10);
        getContentPane().add(textAutor, gridBagConstraints);

        labelGenre.setText("Genre");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelGenre, gridBagConstraints);

        textGenre.setColumns(2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textGenre, gridBagConstraints);

        labelFormat.setText("Format");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelFormat, gridBagConstraints);

        textFormat.setColumns(2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textFormat, gridBagConstraints);

        labelAuflage.setText("Auflage");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelAuflage, gridBagConstraints);

        textAuflage.setColumns(2);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textAuflage, gridBagConstraints);

        Zustand.setText("Zustand");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(Zustand, gridBagConstraints);

        textZustand.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textZustand, gridBagConstraints);

        labelPreis.setText("Preis");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelPreis, gridBagConstraints);

        textPreis.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textPreis, gridBagConstraints);

        labelJahr.setText("Jahr");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelJahr, gridBagConstraints);

        textJahr.setColumns(6);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textJahr, gridBagConstraints);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        buttonSave.setText("Speichern");
        buttonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSaveActionPerformed(evt);
            }
        });
        jPanel1.add(buttonSave);

        buttonScan.setText("Titelbild scannen");
        buttonScan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScanActionPerformed(evt);
            }
        });
        jPanel1.add(buttonScan);

        buttonScanDelete.setText("Titelbild löschen");
        buttonScanDelete.setEnabled(false);
        buttonScanDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonScanDeleteActionPerformed(evt);
            }
        });
        jPanel1.add(buttonScanDelete);

        labelStatus.setForeground(new java.awt.Color(0, 150, 0));
        labelStatus.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 0));
        jPanel1.add(labelStatus);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 15;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(10, 5, 5, 5);
        getContentPane().add(jPanel1, gridBagConstraints);

        textOrt.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textOrt, gridBagConstraints);

        labelOrt.setText("Aufbewahrungsort");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 13;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelOrt, gridBagConstraints);

        labelKommentar.setText("Kommentar");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 10, 2, 5);
        getContentPane().add(labelKommentar, gridBagConstraints);

        textKommentar.setColumns(30);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
        getContentPane().add(textKommentar, gridBagConstraints);

        labelImage.setBackground(java.awt.Color.white);
        labelImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelImage.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        labelImage.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        labelImage.setIconTextGap(0);
        labelImage.setOpaque(true);
        labelImage.setPreferredSize(new java.awt.Dimension(210, 0));
        labelImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                labelImageMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 14;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(15, 5, 1, 15);
        getContentPane().add(labelImage, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
        getContentPane().add(labelChange, gridBagConstraints);

        pack();
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        java.awt.Dimension dialogSize = getSize();
        setLocation((screenSize.width-dialogSize.width)/2,(screenSize.height-dialogSize.height)/2);
    }// </editor-fold>//GEN-END:initComponents

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSaveActionPerformed
        //if (textLfdNr.getText().trim().isEmpty()) {
        //    error("Laufende Nummer darf nicht leer sein!");
        //    focus(textLfdNr);
        //    return;
        //}
        if (textGenre.getText().trim().isEmpty()) {
            error("Genre darf nicht leer sein!");
            focus(textGenre);
            return;
        }
        if (textFormat.getText().trim().isEmpty()) {
            error("Format darf nicht leer sein!");
            focus(textFormat);
            return;
        }
        try {
            if (book == null) {
                book = new Book();
            }
            if (!textAuflage.getText().trim().isEmpty()) {
                book.setAuflage(Integer.parseInt(textAuflage.getText()));
            }
            book.setAutor(textAutor.getText());
            book.setFormat(textFormat.getText());
            book.setGenre(textGenre.getText());
            book.setJahr(textJahr.getText());
            book.setKommentar(textKommentar.getText());
            if (!textLfdNr.getText().trim().isEmpty()) {
                book.setLfdNr(Long.parseLong(textLfdNr.getText()));
            }
            book.setOrt(textOrt.getText());
            if (!textPreis.getText().trim().isEmpty()) {
                book.setPreis(Float.parseFloat(textPreis.getText()));
            }
            book.setReihe(textReihe.getText());
            BookTreeNode nodeSammlung = mainFrame.getSelectedTreeNodeParent(
                    BookTreeNode.Type.SAMMLUNG);
            book.setSammlung(nodeSammlung.getTitle());
            if (image != null) {
                book.setTitelBild(Utils.toJPEG(image, 1.0f));
            } else {
                book.setTitelBild(null);
            }
            book.setTitelDeutsch(textTitelDeutsch.getText());
            book.setTitelFremd(textTitelFremd.getText());
            book.setVerlag(textVerlag.getText());
            book.setZustand(textZustand.getText());
            book.setZyklus(textZyklus.getText());
            book.save();

            if (continuous) {
                textAuflage.setText("1");
                textAutor.setText(null);
                textFormat.setText(null);
                textGenre.setText(null);
                textJahr.setText(null);
                textKommentar.setText(null);
                if (!textLfdNr.getText().isEmpty()) {
                    try {
                        textLfdNr.setText(Long.toString(book.getLfdNr() + 1));
                    } catch (Exception e) {
                        Utils.handleError(e);
                    }
                }
                textOrt.setText(null);
                textPreis.setText(null);
                textReihe.setText(null);
                textTitelDeutsch.setText(null);
                textTitelFremd.setText(null);
                //textVerlag.setText(null);
                textZustand.setText(null);
                textZyklus.setText(null);
                textReihe.requestFocus();
                labelImage.setIcon(null);
                labelStatus.setText("Gespeichert, nächstes Buch eingeben!");
                //amazon = null;
                image = null;
                book = null;
            } else {
                dispose();
            }
        } catch (Exception e) {
            Utils.handleError(e);
        }
    }//GEN-LAST:event_buttonSaveActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        textVerlag.requestFocus();
    }//GEN-LAST:event_formWindowOpened

    private void buttonScanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScanActionPerformed
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            // temporärere Datei
            File scan = File.createTempFile("cuseb", ".tmp");
            scan.deleteOnExit();

            // Parameter zusammenbauen
            String cmd = BookDatabase.getInstance().getConfigValue(
                    BookDatabase.SCAN_COMMAND);
            ArrayList<String> args = new ArrayList<String>();
            StringTokenizer tokens = new StringTokenizer(cmd, " ");
            while (tokens.hasMoreTokens()) {
                args.add(tokens.nextToken());
            }
            args.add(scan.toString());

            // Scan starten
            Process process = new ProcessBuilder(args).start();
            process.waitFor();

            // Automatisch zuschneiden
            image = Utils.autoCrop(ImageIO.read(scan),
                    Float.parseFloat(
                    BookDatabase.getInstance().getConfigValue(
                    BookDatabase.SCAN_CROP_THRESHOLD)));
            labelImage.setIcon(Utils.scaleAsIcon(image,
                    labelImage.getHeight()));
            buttonSave.setEnabled(true);

        } catch (Exception e) {
            Utils.handleError(e);
        }
        setCursor(Cursor.getDefaultCursor());
    }//GEN-LAST:event_buttonScanActionPerformed

    private void buttonScanDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonScanDeleteActionPerformed
        image = null;
        labelImage.setIcon(null);
        buttonSave.setEnabled(true);
        buttonScanDelete.setEnabled(false);
    }//GEN-LAST:event_buttonScanDeleteActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        if (image != null) {
            labelImage.setIcon(Utils.scaleAsIcon(image,
                    labelImage.getHeight()));
        }
    }//GEN-LAST:event_formComponentResized

    private void labelImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_labelImageMouseClicked
        if (image == null) {
            return;
        }
        ImageEditDialog dialog = new ImageEditDialog(mainFrame, image);
        dialog.setVisible(true);
        if (!dialog.isCanceled() && dialog.isModified()) {
            try {
                image = dialog.getImage();
                if (book != null) {
                    book.setTitelBild(Utils.toJPEG(image, 1.0f));
                    book.save();
                }
                labelImage.setIcon(Utils.scaleAsIcon(image,
                        labelImage.getHeight()));
            } catch (Exception e) {
                Utils.handleError(e);
            }
        }
    }//GEN-LAST:event_labelImageMouseClicked
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel Zustand;
    private javax.swing.JButton buttonSave;
    private javax.swing.JButton buttonScan;
    private javax.swing.JButton buttonScanDelete;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel labelAuflage;
    private javax.swing.JLabel labelAutor;
    private javax.swing.JLabel labelChange;
    private javax.swing.JLabel labelFormat;
    private javax.swing.JLabel labelGenre;
    private javax.swing.JLabel labelImage;
    private javax.swing.JLabel labelJahr;
    private javax.swing.JLabel labelKommentar;
    private javax.swing.JLabel labelLfdNr;
    private javax.swing.JLabel labelOrt;
    private javax.swing.JLabel labelPreis;
    private javax.swing.JLabel labelReihe;
    private javax.swing.JLabel labelStatus;
    private javax.swing.JLabel labelTitelDeutsch;
    private javax.swing.JLabel labelTitelFremd;
    private javax.swing.JLabel labelVerlag;
    private javax.swing.JLabel labelZyklus;
    private javax.swing.JTextField textAuflage;
    private javax.swing.JTextField textAutor;
    private javax.swing.JTextField textFormat;
    private javax.swing.JTextField textGenre;
    private javax.swing.JTextField textJahr;
    private javax.swing.JTextField textKommentar;
    private javax.swing.JTextField textLfdNr;
    private javax.swing.JTextField textOrt;
    private javax.swing.JTextField textPreis;
    private javax.swing.JTextField textReihe;
    private javax.swing.JTextField textTitelDeutsch;
    private javax.swing.JTextField textTitelFremd;
    private javax.swing.JTextField textVerlag;
    private javax.swing.JTextField textZustand;
    private javax.swing.JTextField textZyklus;
    // End of variables declaration//GEN-END:variables
}
