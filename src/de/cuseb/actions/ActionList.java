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
package de.cuseb.actions;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfBoolean;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import de.cuseb.dialogs.ActionListDialog;
import de.cuseb.MainFrame;
import de.cuseb.Utils;
import de.cuseb.data.Book;
import de.cuseb.flow.BookFlowImageFactory;
import de.cuseb.table.BookTableColumn;
import de.cuseb.table.BookTableModel;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JTable;

/**
 *
 * @author moennich
 */
public class ActionList extends AbstractAction implements Runnable {

    private MainFrame mainFrame;
    private ActionListDialog dialog;

    private ActionList() {
    }

    public ActionList(MainFrame mainFrame) {
        super("Liste anzeigen/drucken", new ImageIcon(mainFrame.getClass().getResource(
                "/de/cuseb/images/printer.png")));
        putValue(SHORT_DESCRIPTION, "Liste anzeigen/drucken");
        this.mainFrame = mainFrame;
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dialog = new ActionListDialog(mainFrame, true);
        dialog.setVisible(true);
        if (dialog.isCanceled()) {
            return;
        }
        mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(this).start();
    }

    @Override
    public synchronized void run() {
        try {
            boolean landscape = false;
            Rectangle format = null;
            if (dialog.getAusrichtung().equals("Hochformat")) {
                format = PageSize.A4;
            } else {
                format = PageSize.A4.rotate();
                landscape = true;
            }
            //Document document = new Document(PageSize.A4.rotate());
            Document document = new Document(format);
            document.addCreationDate();
            document.addProducer();

            // Globale Schrift (Tabelle und Fußzeile)
            Font font = new Font(BaseFont.createFont(
                    BaseFont.HELVETICA,
                    BaseFont.WINANSI, true),
                    8);
            final Font fontBold = new Font(BaseFont.createFont(
                    BaseFont.HELVETICA_BOLD,
                    BaseFont.WINANSI, true),
                    8);

            ByteArrayOutputStream out = new ByteArrayOutputStream(10240);
            PdfWriter writer = PdfWriter.getInstance(document, out);

            // Querformat richtig drucken
            if (landscape) {
                writer.addViewerPreference(
                        PdfName.PICKTRAYBYPDFSIZE,
                        PdfBoolean.PDFTRUE);
            }

            // Fußzeile mit Seitennummer
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    Rectangle rect = document.getPageSize();
                    ColumnText.showTextAligned(
                            writer.getDirectContent(),
                            Element.ALIGN_RIGHT,
                            new Phrase("Seite " + writer.getPageNumber(), fontBold),
                            document.getPageSize().getRight()
                            - document.rightMargin(),
                            document.bottomMargin(), 0);
                }
            });

            boolean selonly = dialog.getSelektion().equals("Selektierte Bücher");
            JTable table = mainFrame.getTable();
            BookTableModel model = (BookTableModel) table.getModel();
            PdfPTable ptable = null;

            document.open();

            if (dialog.getFormat().equals("Liste")) {

                float widths[] = new float[table.getColumnCount()];
                ptable = new PdfPTable(table.getColumnCount());
                adjustTable(ptable);

                for (int i = 0; i < widths.length; i++) {
                    if (model instanceof BookTableModel) {
                        BookTableColumn column = model.getBookTableColumnByName(
                                table.getColumnName(i));
                        widths[i] = column.getWidth();
                        ptable.addCell(new Phrase(column.getTitleShort(), fontBold));
                    } else {
                        widths[i] = 1.0f / table.getColumnCount();
                        ptable.addCell(new Phrase(table
                                .getTableHeader()
                                .getColumnModel()
                                .getColumn(i)
                                .getHeaderValue()
                                .toString(), fontBold));
                    }
                }
                ptable.setHeaderRows(1);
                ptable.setWidths(widths);

                boolean odd = true;
                for (int row = 0; row < table.getRowCount(); row++) {
                    if (selonly && !table.isRowSelected(row)) {
                        continue;
                    }
                    ptable.getDefaultCell().setGrayFill(odd ? 0.95f : 1f);
                    odd = !odd;
                    for (int col = 0; col < table.getColumnCount(); col++) {
                        Object value = table.getValueAt(row, col);
                        if (value == null) {
                            value = "";
                        }
                        ptable.addCell(new Phrase(value.toString(), font));
                    }
                }

            } else if (dialog.getFormat().equals("Katalog")) {

                ptable = new PdfPTable(4);
                ptable.setWidths(new float[]{0.25f, 0.25f, 0.25f, 0.25f});
                adjustTable(ptable);

                List<Book> books = selonly ? model.getSelectedBooks(table) : model.getAllBooks(table);
                List<BookTableColumn> columns = model.getBookTableColumns(table);
                
                for (int row = 0; row < books.size(); row += 4) {
                    for (int colnr = 0; colnr < 4; colnr++) {
                        if (row + colnr > books.size() - 1) {
                            break;
                        }
                        
                        Book book = books.get(row + colnr);
                        
                        byte buf[] = book.getTitelBild();
                        Image img = Image.getInstance(
                                writer,
                                buf == null
                                ? BookFlowImageFactory.getInstance().getDefaultImage()
                                : ImageIO.read(new ByteArrayInputStream(buf)),
                                0.5f);

                        img.scaleToFit((document.getPageSize().getWidth() - document.getPageSize().getBorderWidthLeft() - document.getPageSize().getBorderWidthRight()) / 5f, 1000.0f);

                        PdfPCell imgcell = new PdfPCell(img);
                        imgcell.disableBorderSide(PdfPCell.LEFT);
                        imgcell.disableBorderSide(PdfPCell.TOP);
                        imgcell.disableBorderSide(PdfPCell.RIGHT);
                        imgcell.disableBorderSide(PdfPCell.BOTTOM);
                        ptable.addCell(imgcell);
                    }
                    for (int colnr = 0; colnr < 4; colnr++) {
                        if (row + colnr > books.size() - 1) {
                            break;
                        }
                        Book book = books.get(row + colnr);
                        Paragraph paragraph = new Paragraph();
                        for (int col = 0; col < table.getColumnCount(); col++) {
                            BookTableColumn column = columns.get(col);
                            Object value = column.getValue(book);
                            if (value != null) {
                                paragraph.add(new Chunk(column.getCatalogText(book), font));
                                paragraph.add(Chunk.NEWLINE);
                            }
                        }
                        PdfPCell cell = new PdfPCell(paragraph);
                        cell.setBorder(0);
                        cell.setPaddingBottom(10.0f);
                        ptable.addCell(cell);
                    }
                }
            }
            document.add(ptable);
            document.close();

            File pdf = File.createTempFile("cuseb", ".pdf");
            pdf.deleteOnExit();
            FileOutputStream pdfout = new FileOutputStream(pdf);
            pdfout.write(out.toByteArray());
            pdfout.close();
            Desktop.getDesktop().open(pdf);

        } catch (Exception ex) {
            Utils.handleError(ex);
        }
        mainFrame.setCursor(Cursor.getDefaultCursor());
    }

    private void adjustTable(PdfPTable table) {
        //table.setWidths(headerwidths);
        table.setWidthPercentage(100);
        table.getDefaultCell().disableBorderSide(PdfPCell.LEFT);
        table.getDefaultCell().disableBorderSide(PdfPCell.TOP);
        table.getDefaultCell().disableBorderSide(PdfPCell.RIGHT);
        table.getDefaultCell().disableBorderSide(PdfPCell.BOTTOM);
        table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
        table.getDefaultCell().setVerticalAlignment(Element.ALIGN_TOP);
        table.getDefaultCell().setPaddingBottom(3);
        table.getDefaultCell().setPaddingTop(0);
    }
}
