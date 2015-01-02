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
import de.cuseb.data.BookDatabase;
import de.cuseb.tree.BookTreeNode;
import java.awt.event.ActionEvent;
import java.sql.PreparedStatement;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

public class ActionDeleteCollection extends AbstractAction {

    private MainFrame mainFrame;

    public ActionDeleteCollection(MainFrame mainFrame) {
        super("Sammlung löschen", null);
        putValue(SHORT_DESCRIPTION, "Sammlung löschen");
        this.mainFrame = mainFrame;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        String sammlung = mainFrame.getSelectedTreeNodeParent(
                BookTreeNode.Type.SAMMLUNG).getTitle();
        if (sammlung == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(mainFrame, "Soll die Sammlung '"
                + sammlung
                + "' wirklich gelöscht werden?\n"
                + "ACHTUNG: Alle Bücher in dieser Sammlung "
                + "werden dabei gelöscht!")
                != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            // Sammlung löschen
            PreparedStatement st = BookDatabase.getInstance().prepare(
                    "DELETE FROM sammlung WHERE name=?");
            st.setString(1, sammlung);
            st.executeUpdate();
            st.close();
            
            // Alle Bücher in dieser Sammlung löschen
            st = BookDatabase.getInstance().prepare(
                    "DELETE FROM buch WHERE sammlung=?");
            st.setString(1, sammlung);
            int booksDeleted = st.executeUpdate();
            st.close();
            mainFrame.updateTree();
            JOptionPane.showMessageDialog(mainFrame, "Sammlung '"
                    + sammlung + "' mit " + booksDeleted
                    + " Einträgen gelöscht");
            
        } catch (Exception ex) {
            Utils.handleError(ex);
        }
    }
}
