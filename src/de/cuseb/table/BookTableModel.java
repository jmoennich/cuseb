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

import de.cuseb.Utils;
import de.cuseb.data.BookDatabase;
import de.cuseb.data.Book;
import de.cuseb.tree.BookTreeNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class BookTableModel implements TableModel, Runnable {

    private float totalPrice;
    private String sammlung;
    private String verlag;
    private String reihe;
    private Thread thread;
    private boolean transferring;
    private ArrayList<Book> books;
    private ArrayList<BookTableColumn> columns;
    private Set<TableModelListener> listeners;
    public static BookTableColumn[] availableColumns = new BookTableColumn[]{
        new BookTableColumn(Long.class, "Auflage", "Auf", 0.02f) {
            @Override
            public Object getValue(Book book) {
                return book.getAuflage();
            }

            @Override
            public String getCatalogText(Book book) {
                return "Auflage: " + book.getAuflage();
            }
        },
        new BookTableColumn(String.class, "Autor", "Autor", 0.1f) {
            @Override
            public Object getValue(Book book) {
                return book.getAutor();
            }
        },
        new BookTableColumn(String.class, "Format", "Fmt", 0.02f) {
            @Override
            public Object getValue(Book book) {
                return book.getFormat();
            }

            @Override
            public String getCatalogText(Book book) {
                return "Format: " + book.getFormat();
            }
        },
        new BookTableColumn(String.class, "Genre", "Gen", 0.02f) {
            @Override
            public Object getValue(Book book) {
                return book.getGenre();
            }

            @Override
            public String getCatalogText(Book book) {
                return "Genre: " + book.getGenre();
            }
        },
        new BookTableColumn(Long.class, "Lfd Nr.", "Lfd Nr.", 0.07f) {
            @Override
            public Object getValue(Book book) {
                return book.getLfdNr();
            }

            @Override
            public String getCatalogText(Book book) {
                return "Nr. " + book.getLfdNr();
            }
        },
        new BookTableColumn(Long.class, "Ort", "Ort", 0.1f) {
            @Override
            public Object getValue(Book book) {
                return book.getOrt();
            }
        },
        new BookTableColumn(Float.class, "Preis", "Prs", 0.03f) {
            @Override
            public Object getValue(Book book) {
                return book.getPreis();
            }

            @Override
            public String getCatalogText(Book book) {
                return NumberFormat.getCurrencyInstance().format(
                        book.getPreis());
            }
        },
        new BookTableColumn(String.class, "Reihe", "Reihe", 0.1f) {
            @Override
            public Object getValue(Book book) {
                return book.getReihe();
            }
        },
        new BookTableColumn(String.class, "Titel", "Titel", 0.2f) {
            @Override
            public Object getValue(Book book) {
                return book.getTitelDeutsch();
            }
        },
        new BookTableColumn(String.class, "Titel (fremd)", "Titel(f)", 0.2f) {
            @Override
            public Object getValue(Book book) {
                return book.getTitelFremd();
            }
        },
        new BookTableColumn(String.class, "Verlag", "Verlag", 0.15f) {
            @Override
            public Object getValue(Book book) {
                return book.getVerlag();
            }
        },
        new BookTableColumn(String.class, "Zustand", "Zst", 0.02f) {
            @Override
            public Object getValue(Book book) {
                return book.getZustand();
            }

            @Override
            public String getCatalogText(Book book) {
                return "Zustand: " + book.getZustand();
            }
        },
        new BookTableColumn(String.class, "Zyklus", "Zyklus", 0.15f) {
            @Override
            public Object getValue(Book book) {
                return book.getZyklus();
            }
        }
    };

    public BookTableModel() {
        books = new ArrayList<Book>();
        columns = new ArrayList<BookTableColumn>();
        listeners = new HashSet<TableModelListener>();
        // Spalten aus Einstellungen holen
        String cols = BookDatabase.getInstance().getConfigValue(
                BookDatabase.DEFAULT_COLUMNS);
        StringTokenizer tokenizer = new StringTokenizer(cols, ",");
        while (tokenizer.hasMoreElements()) {
            BookTableColumn column = getBookTableColumnByName(
                    tokenizer.nextToken());
            if (column != null) {
                columns.add(column);
            }
        }
    }

    //--------------------------------------------------------------------------
    public void setSammlung(String sammlung) {
        this.sammlung = sammlung;
    }

    public void setVerlag(String verlag) {
        this.verlag = verlag;
    }

    public ArrayList<BookTableColumn> getColumns() {
        return columns;
    }

    public BookTableColumn getBookTableColumnByName(String name) {
        for (BookTableColumn column : availableColumns) {
            if (column.getTitle().equals(name)) {
                return column;
            }
        }
        return null;
    }

    public List<BookTableColumn> getBookTableColumns(JTable table) {
        List<BookTableColumn> result = new ArrayList<BookTableColumn>();
        for(int i = 0; i < table.getColumnCount(); i++) {
            result.add(getBookTableColumnByName(table.getColumnName(i)));
        }
        return result;
    }
    
    public void toggleColumn(BookTableColumn column) {
        if (columns.contains(column)) {
            columns.remove(column);
        } else {
            columns.add(column);
        }
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this,
                    TableModelEvent.HEADER_ROW));
        }
    }

    public Book getBookAtRow(int row) {
        return books.get(row);
    }

    public List<Book> getSelectedBooks(JTable table) {
        ArrayList<Book> result = new ArrayList<Book>(table.getRowCount());
        for (int row : table.getSelectedRows()) {
            result.add(books.get(table.convertRowIndexToModel(row)));
        }
        return result;
    }

    public List<Book> getAllBooks(JTable table) {
        ArrayList<Book> result = new ArrayList<Book>(table.getRowCount());
        for (int row = 0; row < table.getRowCount(); row++) {
            result.add(books.get(table.convertRowIndexToModel(row)));
        }
        return result;
    }

    public void deleteBook(Book book) throws Exception {
        int index = books.indexOf(book);
        if (index == -1) {
            return;
        }
        book.delete();
        books.remove(index);
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this, index, index,
                    TableModelEvent.ALL_COLUMNS,
                    TableModelEvent.DELETE));
        }
    }

    public void removeBooks(List<Book> books) {
        for (Book book : books) {
            int index = books.indexOf(book);
            if (index > -1) {
                for (TableModelListener listener : listeners) {
                    listener.tableChanged(new TableModelEvent(this, index, index,
                            TableModelEvent.ALL_COLUMNS,
                            TableModelEvent.DELETE));
                }
            }
        }
    }

    public int updateBookProperties(
            List<Book> books,
            LinkedHashMap<String, String> values) {

        PreparedStatement st = null;
        try {
            BookDatabase db = BookDatabase.getInstance();
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE buch SET ");

            // Spalten
            Iterator<String> keyIterator = values.keySet().iterator();
            while (keyIterator.hasNext()) {
                sql.append(keyIterator.next());
                sql.append("=?");
                if (keyIterator.hasNext()) {
                    sql.append(",");
                }
            }

            // IDs
            sql.append(" WHERE id IN (");
            Iterator<Book> iterator = books.iterator();
            while (iterator.hasNext()) {
                Book book = iterator.next();
                sql.append(book.getId());
                if (iterator.hasNext()) {
                    sql.append(",");
                }
            }
            sql.append(")");

            st = db.prepare(sql.toString());

            // Werte 
            int pnr = 1;
            Iterator<String> valueIterator = values.values().iterator();
            while (valueIterator.hasNext()) {
                st.setString(pnr++, valueIterator.next());
            }

            // Update
            return st.executeUpdate();

        } catch (SQLException se) {
            Utils.handleError(se);

        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (Exception e) {
                    Utils.handleError(e);
                }
            }
        }
        return 0;
    }

    //--------------------------------------------------------------------------
    public boolean isTransferring() {
        return transferring;
    }

    @SuppressWarnings("deprecation")
    public synchronized void update() {
        if (thread != null && thread.isAlive()) {
            thread.stop();
        }
        thread = new Thread(this);
        thread.start();
    }

    public synchronized void update(BookTreeNode node) {
        if (node.getParent() == null) {
            return;
        }
        if (node.getType() == BookTreeNode.Type.REIHE) {
            reihe = node.getTitle();
            verlag = ((BookTreeNode) node.getParent()).getTitle();
            sammlung = ((BookTreeNode) node.getParent().getParent()).getTitle();

        } else if (node.getType() == BookTreeNode.Type.VERLAG) {
            reihe = null;
            verlag = node.getTitle();
            sammlung = ((BookTreeNode) node.getParent()).getTitle();

        } else if (node.getType() == BookTreeNode.Type.SAMMLUNG) {
            reihe = null;
            verlag = null;
            sammlung = node.getTitle();
        }
        update();
    }

    @Override
    public void run() {
        books.clear();
        transferring = true;
        for (TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this));
        }
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(Book.SQL_FIELDS);
        sql.append(" FROM buch WHERE sammlung=?");
        if (verlag != null) {
            sql.append(" AND verlag=?");
        }
        if (reihe != null) {
            sql.append(" AND reihe=?");
        }
        sql.append(" ORDER BY reihe,titel_deutsch");
        PreparedStatement st = null;
        try {
            st = BookDatabase.getInstance().prepare(sql.toString());
            st.setString(1, sammlung);
            if (verlag != null) {
                st.setString(2, verlag);
            }
            if (reihe != null) {
                st.setString(3, reihe);
            }
            totalPrice = 0.0f;
            ResultSet res = st.executeQuery();
            while (res.next()) {
                Book book = Book.getInstance(res);
                books.add(book);
                totalPrice += book.getPreis();
            }
        } catch (Exception e) {
            Utils.handleError(e);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
            } catch (Exception e) {
                Utils.handleError(e);
            }
        }
        transferring = false;
        for (final TableModelListener listener : listeners) {
            listener.tableChanged(new TableModelEvent(this));
        }
    }

    public float getTotalPrice() {
        return totalPrice;
    }

    //--------------------------------------------------------------------------
    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columns.get(columnIndex).getClazz();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex).getTitle();
    }

    @Override
    public int getRowCount() {
        return books.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < books.size()) {
            return columns.get(columnIndex).getValue(books.get(rowIndex));
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }
}
