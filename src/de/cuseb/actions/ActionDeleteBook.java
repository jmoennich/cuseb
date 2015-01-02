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

import de.cuseb.MainFrame;
import de.cuseb.Utils;
import de.cuseb.table.BookTableModel;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public class ActionDeleteBook extends AbstractAction {

    private MainFrame mainFrame;

    public ActionDeleteBook(MainFrame mainFrame) {
        super("Löschen", new ImageIcon(mainFrame.getClass().getResource(
                "/de/cuseb/images/remove.png")));
        putValue(SHORT_DESCRIPTION, "Buch löschen");
        this.mainFrame = mainFrame;
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (JOptionPane.showConfirmDialog(mainFrame,
                mainFrame.getTable().getSelectedRowCount()
                + " Bücher wirklich löschen?", "Bestätigung",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        for (int row : mainFrame.getTable().getSelectedRows()) {
            try {
                BookTableModel model = mainFrame.getTableModel();
                model.deleteBook(mainFrame.getBookAtViewRow(row));
                mainFrame.updateTree();
            } catch (Exception ex) {
                Utils.handleError(ex);
            }
        }
    }
}