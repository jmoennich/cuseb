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

import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;

public class BookTableAutoSizer implements Runnable {

    private JTable table;

    public BookTableAutoSizer(JTable table) {
        this.table = table;
    }

    @Override
    public void run() {

        final ArrayList<Integer> widths = new ArrayList<Integer>();

        for (int column = 0; column < table.getColumnCount(); column++) {
            int maxWidth = 0;
            for (int i = 0; i < table.getRowCount(); i++) {
                TableCellRenderer renderer = table.getCellRenderer(i, column);
                if (renderer == null) {
                    return;
                }
                Component c = table.prepareRenderer(renderer, i, column);
                maxWidth = Math.max(
                        c.getPreferredSize().width + 25, maxWidth);
            }
            widths.add(maxWidth);
        }

        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                for (int i = 0; i < widths.size(); i++) {
                    table.getColumnModel().getColumn(i).setPreferredWidth(
                            widths.get(i));
                }
                table.doLayout();
            }
        });
    }
}
