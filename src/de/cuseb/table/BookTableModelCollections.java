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
import java.sql.ResultSet;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class BookTableModelCollections extends DefaultTableModel
        implements Runnable {

    private boolean transferring;
    private float totalPrice;
    private long totalCount;
    private Thread thread;

    public BookTableModelCollections() {
        super(new String[]{"Sammlung", "Einträge", "Preis (Summe)"}, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int i) {
        if (i == 0) {
            return String.class;
        } else if (i == 1) {
            return Long.class;
        } else if (i == 2) {
            return Float.class;
        }
        return super.getColumnClass(i);
    }

    public long getTotalCount() {
        return totalCount;
    }

    public float getTotalPrice() {
        return totalPrice;
    }

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

    @Override
    public void run() {
        totalCount = 0;
        totalPrice = 0;
        dataVector.clear();
        transferring = true;
        fireTableDataChanged();
        try {
            ResultSet res = BookDatabase.getInstance().query(
                    "SELECT sammlung,COUNT(*),SUM(preis) FROM buch "
                    + "GROUP BY sammlung ORDER BY sammlung");
            while (res.next()) {
                Vector row = new Vector();
                row.add(res.getString(1));
                row.add(res.getLong(2));
                row.add(res.getFloat(3));
                dataVector.add(row);
                totalCount += res.getLong(2);
                totalPrice += res.getFloat(3);
            }
            res.getStatement().close();
            fireTableDataChanged();
        } catch (Exception e) {
            Utils.handleError(e);
        }
        transferring = false;
        fireTableDataChanged();
    }
}
