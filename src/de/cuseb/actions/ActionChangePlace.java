/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cuseb.actions;

import de.cuseb.MainFrame;
import de.cuseb.data.Book;
import de.cuseb.dialogs.ActionChangePlaceDialog;
import de.cuseb.table.BookTableModel;
import java.awt.event.ActionEvent;
import java.util.LinkedHashMap;
import java.util.List;
import javax.swing.AbstractAction;
import static javax.swing.Action.SHORT_DESCRIPTION;
import javax.swing.JOptionPane;

/**
 *
 * @author jan
 */
public class ActionChangePlace extends AbstractAction {

    private MainFrame mainFrame;

    public ActionChangePlace(MainFrame mainFrame) {
        super("Aufbewahrungsort ändern...", null);
        putValue(SHORT_DESCRIPTION, "Aufbewahrungsort ändern...");
        this.mainFrame = mainFrame;
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        ActionChangePlaceDialog dialog = new ActionChangePlaceDialog(mainFrame);
        dialog.setVisible(true);

        if (dialog.isSaved()) {

            // Selektierte Bücher
            BookTableModel model = mainFrame.getTableModel();
            List<Book> books = model.getSelectedBooks(mainFrame.getTable());

            // Ort aus Dialog übernehmen
            String ort = dialog.getOrt();
            LinkedHashMap values = new LinkedHashMap();
            values.put("ort", ort);

            // Ort in DB und in Instanzen ändern
            int rows = model.updateBookProperties(books, values);
            for(Book book : books) {
                book.setOrt(ort);
            }
            
            JOptionPane.showMessageDialog(mainFrame,
                    "Aufbewahrungsort von " + rows + " Büchern geändert",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
