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
package de.cuseb.table;

import de.cuseb.data.Book;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class BookTableCellRenderer implements TableCellRenderer {

    private TableCellRenderer origRenderer;
    private SimpleDateFormat dateFormat;
    private boolean coloredZustand;
    private Border emptyBorder;
    private Color colorZebra;
    private Color colorGood;
    private Color colorOK;
    private Color colorBad;

    public BookTableCellRenderer() {
        colorZebra = Color.decode("#eeeeee");
        colorGood = Color.decode("#ddffdd");
        colorBad = Color.decode("#ffdddd");
        colorOK = Color.decode("#ffffdd");
        dateFormat = new SimpleDateFormat("dd.MM.yy");
        origRenderer = new DefaultTableCellRenderer();
        emptyBorder = new EmptyBorder(1, 3, 1, 3);
        coloredZustand = false;
    }

    public void setColoredZustand(boolean zustand) {
        this.coloredZustand = zustand;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {

        JLabel label = (JLabel) origRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        label.setBorder(emptyBorder);
        if (value instanceof Date) {
            label.setText(dateFormat.format(value));
        }
        if (value instanceof Float) {
            label.setHorizontalAlignment(JLabel.RIGHT);
            label.setText(NumberFormat.getCurrencyInstance().format(
                    (Float) value));
        } else {
            label.setHorizontalAlignment(JLabel.LEFT);
        }
        if (!isSelected) {
            if (coloredZustand) {
                BookTableModel model = (BookTableModel) table.getModel();
                Book book = model.getBookAtRow(
                        table.getRowSorter().convertRowIndexToModel(row));
                if (book.getZustand() == null) {
                    label.setBackground(Color.WHITE);
                } else {
                    String zustand = book.getZustand();
                    if (zustand != null) {
                        if (zustand.startsWith("0") || zustand.startsWith("1")) {
                            label.setBackground(colorGood);
                        } else if (zustand.startsWith("2")) {
                            label.setBackground(colorOK);
                        } else if (zustand.startsWith("3")) {
                            label.setBackground(colorBad);
                        }
                    } else {
                        label.setBackground(Color.WHITE);
                    }
                }
            } else {
                label.setBackground(row % 2 == 0 ? Color.WHITE : colorZebra);
            }
        }
        return label;
    }
}
