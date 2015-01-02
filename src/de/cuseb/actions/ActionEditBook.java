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

import de.cuseb.dialogs.BookEditDialog;
import de.cuseb.MainFrame;
import de.cuseb.Utils;
import de.cuseb.tree.BookTreeNode;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

public class ActionEditBook extends AbstractAction {

    private MainFrame mainFrame;

    public ActionEditBook(MainFrame mainFrame) {
        super("Neuer Eintrag", new ImageIcon(mainFrame.getClass().getResource(
                "/de/cuseb/images/add.png")));
        putValue(SHORT_DESCRIPTION, "Neuer Eintrag");
        this.mainFrame = mainFrame;
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        BookEditDialog dialog = new BookEditDialog(mainFrame, null);
        BookTreeNode nodeVerlag = mainFrame.getSelectedTreeNodeParent(
                BookTreeNode.Type.VERLAG);

        if (nodeVerlag != null) {
            dialog.setVerlag(nodeVerlag.getTitle());
        }
        dialog.setVisible(true);

        try {
            mainFrame.updateTree();
            mainFrame.getTableModel().update();
        } catch (Exception ex) {
            Utils.handleError(ex);
        }
    }
}
