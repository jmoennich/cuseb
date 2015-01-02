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
import java.awt.event.ActionEvent;
import java.sql.PreparedStatement;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 *
 * @author moennich
 */
public class ActionNewCollection extends AbstractAction {

    private MainFrame mainFrame;

    private ActionNewCollection() {
    }

    public ActionNewCollection(MainFrame mainFrame) {
        super("Neue Sammlung", null);
        putValue(SHORT_DESCRIPTION, "Neue Sammlung");
        this.mainFrame = mainFrame;
    }

    public void actionPerformed(ActionEvent e) {
        
        String sammlung =
                JOptionPane.showInputDialog(mainFrame, "Neue Sammlung:");
        
        if(sammlung == null) {
            return;
        }
        try {
            PreparedStatement st =
                    BookDatabase.getInstance().prepare(
                    "INSERT INTO sammlung(name) VALUES(?)");
            st.setString(1, sammlung);
            st.executeUpdate();
            st.close();
            mainFrame.updateTree();
            
        } catch (Exception ex) {
            Utils.handleError(ex);
        }
    }
}
