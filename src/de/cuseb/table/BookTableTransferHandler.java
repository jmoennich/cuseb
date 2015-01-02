/*  
 * Copyright 2012-2014 Jan Mönnich
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

import de.cuseb.MainFrame;
import de.cuseb.data.Book;
import de.cuseb.tree.BookTreeNode;
import java.awt.datatransfer.Transferable;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

/**
 *
 * @author jan
 */
public class BookTableTransferHandler extends TransferHandler {

    JTable table;

    public BookTableTransferHandler(JTable table) {
        this.table = table;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @Override
    public Transferable createTransferable(JComponent c) {
        BookTableModel model = (BookTableModel) table.getModel();
        BookTableTransferBooks books = new BookTableTransferBooks();
        for (int row : table.getSelectedRows()) {
            books.add(model.getBookAtRow(table.convertRowIndexToModel(row)));
        }
        return books;
    }

    @Override
    public void exportDone(JComponent c, Transferable t, int action) {
    }

    @Override
    public boolean canImport(TransferSupport support) {

        // Nur BookTableTransferBooks annehmen
        if (!support.isDataFlavorSupported(support.getDataFlavors()[0])) {
            return false;
        }

        // Nur im Baum annehmen
        DropLocation location = support.getDropLocation();
        if (!(location instanceof JTree.DropLocation)) {
            return false;
        }

        // Nur annehmen über einem Knoten (nicht der Wurzel)        
        JTree.DropLocation treeLocation = (JTree.DropLocation) location;
        TreePath path = treeLocation.getPath();
        if (path == null) {
            return false;
        }

        BookTreeNode node = (BookTreeNode) path.getLastPathComponent();
        return node.getType() == BookTreeNode.Type.SAMMLUNG;
    }

    @Override
    public boolean importData(TransferSupport support) {

        JTree.DropLocation location = (JTree.DropLocation) support.getDropLocation();
        BookTreeNode node = (BookTreeNode) location.getPath().getLastPathComponent();

        LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
        values.put("sammlung", node.getTitle());

        BookTableModel model = (BookTableModel) table.getModel();
        List<Book> selected = model.getSelectedBooks(table);
        model.updateBookProperties(selected, values);
        model.removeBooks(selected);
        MainFrame.getInstance().updateTree();

        return true;
    }
}
