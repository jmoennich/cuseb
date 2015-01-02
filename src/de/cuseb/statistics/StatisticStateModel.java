package de.cuseb.statistics;

import de.cuseb.MainFrame;
import de.cuseb.data.BookDatabase;
import de.cuseb.tree.BookTreeNode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author moennich
 */
public class StatisticStateModel extends DefaultTableModel {

    private static Class[] columnClasses = new Class[]{
        String.class, Long.class, Float.class, Float.class
    };
    private static String[] columnTitles = new String[]{
        "Reihe", "Anzahl", "Durchs. Zustand", "Preis (Summe)"
    };

    public StatisticStateModel() throws Exception {

        super(columnTitles, 0);

        BookTreeNode node = MainFrame.getInstance().getSelectedTreeNodeParent(
                BookTreeNode.Type.SAMMLUNG);

        if(node == null) {
            throw new Exception("Keine Sammlung selektiert!");
        }
        
        PreparedStatement st = BookDatabase.getInstance().prepare(
                "SELECT reihe,count(*)," + ""
                + "round(AVG(replace(replace(replace(replace(replace("
                + "zustand,'4-5','4.5'),'3-4','3.5'),'2-3','2.5'),'1-2','1.5'),'0-1','0.5')),2),"
                + "round(SUM(preis), 2)"
                + " FROM buch WHERE sammlung=?"
                + " GROUP BY reihe ORDER BY sammlung,reihe;");
        st.setString(1, node.getTitle());

        ResultSet res = st.executeQuery();
        while (res.next()) {
            Vector row = new Vector();
            row.add(res.getString(1));
            row.add(res.getLong(2));
            row.add(res.getString(3));
            row.add(res.getFloat(4));
            dataVector.add(row);
        }
        res.getStatement().close();

        fireTableDataChanged();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    @Override
    public Class<?> getColumnClass(int i) {
        return columnClasses[i];
    }
}
