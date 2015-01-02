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
import java.util.Enumeration;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;

public class BookTableColumnListener implements TableColumnModelListener {

    private JTable table;

    public BookTableColumnListener(JTable table) {
        this.table = table;
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
        update();
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
        update();
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
        update();
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
    }

    private void update() {
        if (!(table.getModel() instanceof BookTableModel)) {
            return;
        }
        StringBuilder tableColumnString = new StringBuilder();
        Enumeration<TableColumn> cols = table.getColumnModel().getColumns();
        while (cols.hasMoreElements()) {
            TableColumn col = cols.nextElement();
            tableColumnString.append(col.getHeaderValue().toString());
            if (cols.hasMoreElements()) {
                tableColumnString.append(",");
            }
        }
        try {
            BookDatabase.getInstance().setConfigValue(
                    BookDatabase.DEFAULT_COLUMNS, tableColumnString.toString());
        } catch (Exception e) {
            Utils.handleError(e);
        }
    }
}
