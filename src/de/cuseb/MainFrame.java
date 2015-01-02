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

import de.cuseb.actions.ActionChangePlace;
import de.cuseb.actions.ActionDeleteBook;
import de.cuseb.actions.ActionDeleteCollection;
import de.cuseb.actions.ActionEditBook;
import de.cuseb.actions.ActionList;
import de.cuseb.actions.ActionNewCollection;
import de.cuseb.actions.ActionStatistic;
import de.cuseb.data.Book;
import de.cuseb.data.BookDatabase;
import de.cuseb.data.BookSelectionListener;
import de.cuseb.dialogs.BookEditDialog;
import de.cuseb.dialogs.ConfigDialog;
import de.cuseb.flow.BookFlowPanel;
import de.cuseb.palette.BookPalette;
import de.cuseb.statistics.StatisticStateModel;
import de.cuseb.table.BookTableAutoSizer;
import de.cuseb.table.BookTableCellRenderer;
import de.cuseb.table.BookTableColumn;
import de.cuseb.table.BookTableColumnListener;
import de.cuseb.table.BookTableModel;
import de.cuseb.table.BookTableModelCollections;
import de.cuseb.table.BookTableTransferHandler;
import de.cuseb.tree.BookTreeCellRenderer;
import de.cuseb.tree.BookTreeModel;
import de.cuseb.tree.BookTreeNode;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.regex.Pattern;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.TreePath;

public class MainFrame extends javax.swing.JFrame implements
        SearchFieldListener, BookSelectionListener {
    
    public static final String VERSION = "10.04.2013";
    // Singleton
    private static MainFrame instance;
    private SearchField searchField;
    private BookPalette palette;
    private BookFlowPanel flowPanel;
    private BookTreeModel treeModel;
    private BookTableModel tableModel;
    private BookTableAutoSizer tableAutoSizer;
    private BookTableCellRenderer tableCellRenderer;
    private BookTableModelCollections tableModelCollections;
    private ActionDeleteBook actionDeleteBook;
    private ActionDeleteCollection actionDeleteCollection;
    private ActionEditBook actionEditBook;
    private ActionList actionList;
    private ActionNewCollection actionNewCollection;
    private ActionStatistic actionStatistic;
    private ActionChangePlace actionChangePlace;
    private HashMap<Action, Boolean> actionStates;
    private JToggleButton buttonColumns;
    private JPopupMenu popupColumns;
    private int origIncrement;
    
    public MainFrame() {
        
        initComponents();
        actionDeleteCollection = new ActionDeleteCollection(this);
        actionDeleteCollection.setEnabled(false);
        actionNewCollection = new ActionNewCollection(this);
        actionChangePlace = new ActionChangePlace(this);
        actionDeleteBook = new ActionDeleteBook(this);
        actionStatistic = new ActionStatistic(this);
        actionEditBook = new ActionEditBook(this);
        actionList = new ActionList(this);

        // OS X
        getRootPane().putClientProperty(
                "apple.awt.brushMetalLook", Boolean.TRUE);
        statusLabel.setOpaque(true);

        // Knöpfe
        //addButton(actionNewCollection, "first");
        addButton(actionEditBook, "first");
        addButton(actionDeleteBook, "last");
        toolbar.addSeparator();
        //addButton(actionShowFlow, "first");
        buttonColumns = new JToggleButton("Spalten");
        buttonColumns.setBorder(new EmptyBorder(3, 3, 3, 3));
        buttonColumns.setFocusable(false);
        buttonColumns.setIcon(new ImageIcon(getClass().getResource(
                "/de/cuseb/images/table.png")));
        buttonColumns.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupColumns.show(toolbar, buttonColumns.getX(),
                        buttonColumns.getY() + buttonColumns.getHeight());
                popupColumns.setVisible(buttonColumns.isSelected());
            }
        });
        popupColumns = new JPopupMenu();
        popupColumns.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
            
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                buttonColumns.setSelected(false);
            }
            
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        
        toolbar.add(buttonColumns);
        addButton(actionList, "middle");
        //addButton(actionStatistic, "last");
        //toolbar.addSeparator();
        //addButton(actionConfiguration, "first");
        //addButton(actionInfo, "last");

        // Menüs
        itemDelteCollection.setAction(actionDeleteCollection);
        itemNewCollection.setAction(actionNewCollection);
        itemChangePlace.setAction(actionChangePlace);
        itemDeleteBook.setAction(actionDeleteBook);
        itemNewBook.setAction(actionEditBook);
        itemPrint.setAction(actionList);

        // Suchfeld
        searchField = new SearchField();
        searchField.addSearchFieldListener(this);
        panelTop.add(searchField, BorderLayout.EAST);
        actionStates = new HashMap<Action, Boolean>();

        // Drag & Drop aus Liste
        BookTableTransferHandler transfer = new BookTableTransferHandler(table);

        // Baum
        treeModel = new BookTreeModel();
        tree.setCellRenderer(new BookTreeCellRenderer());
        tree.setTransferHandler(transfer);
        tree.setModel(treeModel);
        try {
            treeModel.update();
        } catch (Exception e) {
            Utils.handleError(e);
        }

        // Tabelle
        tableCellRenderer = new BookTableCellRenderer();
        table.setDefaultRenderer(String.class, tableCellRenderer);
        table.setDefaultRenderer(Float.class, tableCellRenderer);
        table.setDefaultRenderer(Long.class, tableCellRenderer);
        table.setTransferHandler(transfer);
        table.getColumnModel().addColumnModelListener(
                new BookTableColumnListener(table));
        table.getSelectionModel().addListSelectionListener(
                new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                tableSelectionChanged(e);
            }
        });
        
        tableModel = new BookTableModel();
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                tableValueChanged(e);
            }
        });
        table.setModel(tableModel);
        for (final BookTableColumn column : BookTableModel.availableColumns) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(column.getTitle());
            item.setSelected(tableModel.getColumns().contains(column));
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tableModel.toggleColumn(column);
                }
            });
            popupColumns.add(item);
        }
        tableAutoSizer = new BookTableAutoSizer(table);
        tableModelCollections = new BookTableModelCollections();
        tableModelCollections.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                tableCollectionsValueChanged(e);
            }
        });

        // Titelbilder
        flowPanel = new BookFlowPanel();
        flowPanel.addBookSelectionListener(this);
        palette = new BookPalette();
        palette.addBookSelectionListener(this);

        // Statusanzeige
        statusLabel.setText(String.format(
                "CuseB Version %s, Datenbankgröße: %.1f MB",
                VERSION, BookDatabase.getInstance().getSizeMB()));
    }
    
    public static MainFrame getInstance() {
        return instance;
    }
    
    private void addButton(Action action, String position) {
        JButton button = new JButton(action);
        //button.putClientProperty("JButton.buttonType", "segmentedTextured");
        //button.putClientProperty("JButton.segmentPosition", position);
        button.setFocusable(false);
        button.setText(null);
        button.setBorder(new EmptyBorder(3, 3, 3, 3));
        //button.setOpaque(true);
        button.setText(action.getValue(Action.NAME).toString());
        // button.setVerticalTextPosition(JButton.BOTTOM);
        // button.setHorizontalAlignment(JButton.LEFT);
        toolbar.add(button);
    }

    // Suchfeld
    @Override
    public void searchValueChanged(String value) {
        TableRowSorter sorter = (TableRowSorter) table.getRowSorter();
        if (!value.isEmpty()) {
            sorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + Pattern.quote(value)));
        } else {
            sorter.setRowFilter(null);
        }
        if (itemViewFlow.isSelected()) {
            flowPanel.update(table);
        } else if (itemViewPalette.isSelected()) {
            palette.update(table);
        }
    }

    // Statuszeile
    public void showStatus(String text) {
        statusLabel.setText(text);
    }

    // Table
    public JTable getTable() {
        return table;
    }
    
    public BookTreeNode getSelectedTreeNodeParent(BookTreeNode.Type type) {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        BookTreeNode current = (BookTreeNode) path.getLastPathComponent();
        while (current != null && current.getType() != type) {
            current = (BookTreeNode) current.getParent();
        }
        return current;
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupView = new javax.swing.ButtonGroup();
        split = new javax.swing.JSplitPane();
        treeScroll = new javax.swing.JScrollPane();
        tree = new javax.swing.JTree();
        scroll = new javax.swing.JScrollPane();
        table = new javax.swing.JTable();
        statusPanel = new javax.swing.JPanel();
        statusLabel = new javax.swing.JLabel();
        panelTop = new javax.swing.JPanel();
        toolbar = new javax.swing.JToolBar();
        menu = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        itemNewCollection = new javax.swing.JMenuItem();
        itemDelteCollection = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        itemNewBook = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        itemPrint = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        itemBackupDB = new javax.swing.JMenuItem();
        menuEdit = new javax.swing.JMenu();
        itemDeleteBook = new javax.swing.JMenuItem();
        itemChangePlace = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        itemConfig = new javax.swing.JMenuItem();
        menuView = new javax.swing.JMenu();
        itemViewTable = new javax.swing.JRadioButtonMenuItem();
        itemViewFlow = new javax.swing.JRadioButtonMenuItem();
        itemViewPalette = new javax.swing.JRadioButtonMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        itemZustand = new javax.swing.JCheckBoxMenuItem();
        menuStatistics = new javax.swing.JMenu();
        itemStatisticStates = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("CuseB");

        split.setBorder(null);
        split.setDividerLocation(250);

        tree.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        tree.setAutoscrolls(true);
        tree.setDropMode(javax.swing.DropMode.ON);
        tree.setRowHeight(0);
        tree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                treeValueChanged(evt);
            }
        });
        treeScroll.setViewportView(tree);

        split.setLeftComponent(treeScroll);

        scroll.setBorder(null);

        table.setAutoCreateRowSorter(true);
        table.setDragEnabled(true);
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new java.awt.Dimension(0, 1));
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tableMouseClicked(evt);
            }
        });
        scroll.setViewportView(table);

        split.setRightComponent(scroll);

        getContentPane().add(split, java.awt.BorderLayout.CENTER);

        statusPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
        statusPanel.add(statusLabel);

        getContentPane().add(statusPanel, java.awt.BorderLayout.SOUTH);

        panelTop.setLayout(new java.awt.BorderLayout());

        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setFocusable(false);
        panelTop.add(toolbar, java.awt.BorderLayout.CENTER);

        getContentPane().add(panelTop, java.awt.BorderLayout.NORTH);

        menuFile.setText("Datei");

        itemNewCollection.setText("Sammlung anlegen...");
        menuFile.add(itemNewCollection);

        itemDelteCollection.setText("Sammlung löschen");
        itemDelteCollection.setEnabled(false);
        menuFile.add(itemDelteCollection);
        menuFile.add(jSeparator1);

        itemNewBook.setText("Neuer Eintrag");
        menuFile.add(itemNewBook);
        menuFile.add(jSeparator2);

        itemPrint.setText("Liste drucken...");
        menuFile.add(itemPrint);
        menuFile.add(jSeparator3);

        itemBackupDB.setText("Datenbank sichern...");
        itemBackupDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemBackupDBActionPerformed(evt);
            }
        });
        menuFile.add(itemBackupDB);

        menu.add(menuFile);

        menuEdit.setText("Bearbeiten");

        itemDeleteBook.setText("Eintrag löschen");
        menuEdit.add(itemDeleteBook);

        itemChangePlace.setText("Aufbewahrungsort ändern...");
        menuEdit.add(itemChangePlace);
        menuEdit.add(jSeparator5);

        itemConfig.setText("Einstellungen...");
        itemConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemConfigActionPerformed(evt);
            }
        });
        menuEdit.add(itemConfig);

        menu.add(menuEdit);

        menuView.setText("Ansicht");

        buttonGroupView.add(itemViewTable);
        itemViewTable.setSelected(true);
        itemViewTable.setText("Tabelle");
        itemViewTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemViewTableActionPerformed(evt);
            }
        });
        menuView.add(itemViewTable);

        buttonGroupView.add(itemViewFlow);
        itemViewFlow.setText("Titelbilder - Regal");
        itemViewFlow.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemViewFlowActionPerformed(evt);
            }
        });
        menuView.add(itemViewFlow);

        buttonGroupView.add(itemViewPalette);
        itemViewPalette.setText("Titelbilder - Palette");
        itemViewPalette.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemViewPaletteActionPerformed(evt);
            }
        });
        menuView.add(itemViewPalette);
        menuView.add(jSeparator4);

        itemZustand.setText("Zustand farblich hervorheben");
        itemZustand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemZustandActionPerformed(evt);
            }
        });
        menuView.add(itemZustand);

        menu.add(menuView);

        menuStatistics.setText("Statistiken");

        itemStatisticStates.setText("Reihen mit Zustand (aktuelle Sammlung)");
        itemStatisticStates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itemStatisticStatesActionPerformed(evt);
            }
        });
        menuStatistics.add(itemStatisticStates);

        menu.add(menuStatistics);

        setJMenuBar(menu);

        setSize(new java.awt.Dimension(1120, 700));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    // Tree --------------------------------------------------------------------
    public BookTreeModel getTreeModel() {
        return treeModel;
    }
    
    public void updateTree() {
        TreePath selection = tree.getSelectionPath();
        Enumeration<TreePath> expanded =
                tree.getExpandedDescendants(new TreePath(treeModel.getRoot()));
        try {
            treeModel.update();
            if (expanded != null) {
                while (expanded.hasMoreElements()) {
                    tree.expandPath(expanded.nextElement());
                }
            }
            if (selection != null) {
                tree.setSelectionPath(selection);
            }
        } catch (Exception e) {
            Utils.handleError(e);
        }
    }
    
    private void treeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_treeValueChanged
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            actionEditBook.setEnabled(false);
            actionDeleteCollection.setEnabled(false);
            return;
        }
        if (path.getLastPathComponent() == treeModel.getRoot()) {
            try {
                table.setModel(tableModelCollections);
                tableModelCollections.update();
                buttonColumns.setEnabled(false);
                actionEditBook.setEnabled(false);
            } catch (Exception e) {
                Utils.handleError(e);
            }
        } else {
            if (table.getModel() != tableModel) {
                table.setModel(tableModel);
                buttonColumns.setEnabled(true);
            }
            BookTreeNode node = (BookTreeNode) path.getLastPathComponent();
            BookTreeNode nodeSammlung = (BookTreeNode) getSelectedTreeNodeParent(
                    BookTreeNode.Type.SAMMLUNG);
            searchField.clear();
            actionDeleteCollection.setEnabled(
                    node.getType() == BookTreeNode.Type.SAMMLUNG);
            actionEditBook.setEnabled(nodeSammlung != null);
            scroll.getViewport().setViewPosition(new Point(0, 0));
            scroll.getViewport().updateUI();
            tableModel.update(node);
        }
    }//GEN-LAST:event_treeValueChanged

    // Table -------------------------------------------------------------------
    public BookTableModel getTableModel() {
        return tableModel;
    }
    
    public Book getBookAtViewRow(int row) {
        try {
            return tableModel.getBookAtRow(
                    table.getRowSorter().convertRowIndexToModel(row));
        } catch (Exception e) {
            Utils.handleError(e);
            return null;
        }
    }
    
    private void tableSelectionChanged(ListSelectionEvent e) {
        actionDeleteBook.setEnabled(table.getSelectedRowCount() > 0);
        actionChangePlace.setEnabled(table.getSelectedRowCount() > 0);
    }
    
    private void tableValueChanged(TableModelEvent e) {
        if (tableModel.isTransferring()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            new Thread(tableAutoSizer).start();
            showStatus(table.getRowCount() + " Bücher, Gesamtwert "
                    + NumberFormat.getCurrencyInstance().format(
                    tableModel.getTotalPrice()));
            actionList.setEnabled(table.getRowCount() > 0);
            setCursor(Cursor.getDefaultCursor());
            if (itemViewFlow.isSelected()) {
                flowPanel.update(table);
            } else if (itemViewPalette.isSelected()) {
                palette.update(table);
            }
        }
    }
    
    private void tableCollectionsValueChanged(TableModelEvent e) {
        if (tableModelCollections.isTransferring()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            if (!itemViewTable.isSelected()) {
                itemViewTable.setSelected(true);
                switchView();
            }
            new Thread(tableAutoSizer).start();
            showStatus(table.getRowCount()
                    + " Sammlungen, "
                    + tableModelCollections.getTotalCount()
                    + " Bücher, Gesamtwert "
                    + NumberFormat.getCurrencyInstance().format(
                    tableModelCollections.getTotalPrice()));
            actionList.setEnabled(false);
            setCursor(Cursor.getDefaultCursor());
        }
    }
    
    public void switchView() {
        if (itemViewFlow.isSelected()) {
            actionDeleteBook.setEnabled(false);
            actionChangePlace.setEnabled(false);
            buttonColumns.setEnabled(false);
            scroll.setViewportView(flowPanel);
            flowPanel.update(table);
        } else if (itemViewPalette.isSelected()) {
            scroll.setViewportView(palette);
            scroll.getVerticalScrollBar().setUnitIncrement(15);
            scroll.getViewport().setViewPosition(new Point(0, 0));
            palette.update(table);
        } else {
            actionDeleteBook.setEnabled(table.getSelectedRowCount() > 0);
            actionChangePlace.setEnabled(table.getSelectedRowCount() > 0);
            buttonColumns.setEnabled(true);
            scroll.setViewportView(table);
        }
    }
    
    @Override
    public void bookSelected(Book book) {
        BookEditDialog dialog = new BookEditDialog(this, book);
        dialog.setVisible(true);
    }
    
    private void tableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tableMouseClicked
        if (table.getModel() != tableModel) {
            return;
        }
        if (evt.getClickCount() == 2) {
            int row = table.getSelectedRow();
            if (row < 0) {
                return;
            }
            Book book = tableModel.getBookAtRow(
                    table.getRowSorter().convertRowIndexToModel(row));
            bookSelected(book);
            table.repaint();
        }
        
    }//GEN-LAST:event_tableMouseClicked
    
    private void itemViewTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemViewTableActionPerformed
        switchView();
    }//GEN-LAST:event_itemViewTableActionPerformed
    
    private void itemViewFlowActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemViewFlowActionPerformed
        switchView();
    }//GEN-LAST:event_itemViewFlowActionPerformed
    
    private void itemConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemConfigActionPerformed
        ConfigDialog dialog = new ConfigDialog(this, true);
        dialog.setVisible(true);
    }//GEN-LAST:event_itemConfigActionPerformed
    
    private void itemBackupDBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemBackupDBActionPerformed
        try {
            BookDatabase.getInstance().backup();
        } catch (Exception e) {
            Utils.handleError(e);
        }
    }//GEN-LAST:event_itemBackupDBActionPerformed
    
    private void itemZustandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemZustandActionPerformed
        tableCellRenderer.setColoredZustand(itemZustand.isSelected());
        table.repaint();
    }//GEN-LAST:event_itemZustandActionPerformed
    
    private void itemViewPaletteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemViewPaletteActionPerformed
        switchView();
    }//GEN-LAST:event_itemViewPaletteActionPerformed
    
    private void itemStatisticStatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itemStatisticStatesActionPerformed
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            buttonColumns.setEnabled(false);
            actionList.setEnabled(true);
            table.setModel(new StatisticStateModel());
            tree.clearSelection();
        } catch (Exception ex) {
            Utils.handleError(ex);
        } finally {
            setCursor(Cursor.getDefaultCursor());
        }
    }//GEN-LAST:event_itemStatisticStatesActionPerformed

    //--------------------------------------------------------------------------
    public static void main(String args[]) {
        
        if (instance != null) {
            System.exit(0);
            return;
        }
        
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            BookDatabase.getInstance();
        } catch (Exception e) {
            Utils.handleError(e);
        }
        
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                instance = new MainFrame();
                instance.setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupView;
    private javax.swing.JMenuItem itemBackupDB;
    private javax.swing.JMenuItem itemChangePlace;
    private javax.swing.JMenuItem itemConfig;
    private javax.swing.JMenuItem itemDeleteBook;
    private javax.swing.JMenuItem itemDelteCollection;
    private javax.swing.JMenuItem itemNewBook;
    private javax.swing.JMenuItem itemNewCollection;
    private javax.swing.JMenuItem itemPrint;
    private javax.swing.JMenuItem itemStatisticStates;
    private javax.swing.JRadioButtonMenuItem itemViewFlow;
    private javax.swing.JRadioButtonMenuItem itemViewPalette;
    private javax.swing.JRadioButtonMenuItem itemViewTable;
    private javax.swing.JCheckBoxMenuItem itemZustand;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JMenuBar menu;
    private javax.swing.JMenu menuEdit;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenu menuStatistics;
    private javax.swing.JMenu menuView;
    private javax.swing.JPanel panelTop;
    private javax.swing.JScrollPane scroll;
    private javax.swing.JSplitPane split;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JTable table;
    private javax.swing.JToolBar toolbar;
    private javax.swing.JTree tree;
    private javax.swing.JScrollPane treeScroll;
    // End of variables declaration//GEN-END:variables
}
