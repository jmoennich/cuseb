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

import de.cuseb.data.BookDatabase;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class ConfigDialogTableModel implements TableModel {

    private ArrayList<TableModelListener> listeners;
    private ArrayList<String> keys;
    private ArrayList<String> values;

    public ConfigDialogTableModel() {
        listeners = new ArrayList<TableModelListener>();
        TreeMap<String, String> map = new TreeMap<String, String>(
                BookDatabase.getInstance().getConfigValues());
        keys = new ArrayList<String>(map.keySet());
        values = new ArrayList<String>(map.values());
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "Schlüssel";
        }
        return "Wert";
    }

    @Override
    public int getRowCount() {
        return keys.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return keys.get(rowIndex);
        } else {
            return values.get(rowIndex);
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        values.set(rowIndex, aValue.toString());
    }

    public void store() throws Exception {
        HashMap<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), values.get(i));
        }
        BookDatabase.getInstance().setConfigValues(map);
    }
}
