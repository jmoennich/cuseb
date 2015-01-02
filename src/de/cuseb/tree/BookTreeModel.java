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
package de.cuseb.tree;

import de.cuseb.data.BookDatabase;
import java.sql.ResultSet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public class BookTreeModel extends DefaultTreeModel {

    private int sammlungCount;

    public BookTreeModel() {
        super(new BookTreeNode("Sammlungen", null));
    }

    public int getSammlungCount() {
        return sammlungCount;
    }

    public synchronized void update() throws Exception {

        String oldSammlung = "";
        String oldVerlag = "";
        String oldReihe = "";
        BookTreeNode nodeSammlung = null;
        BookTreeNode nodeVerlag = null;
        BookTreeNode nodeReihe = null;

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) root;
        rootNode.removeAllChildren();
        sammlungCount = 0;
        ResultSet res = BookDatabase.getInstance().query(
                "SELECT name,verlag,reihe FROM sammlung "
                + "LEFT JOIN buch ON name=sammlung "
                + "GROUP BY name,verlag,reihe "
                + "ORDER BY name,verlag,reihe");

        while (res.next()) {
            String sammlung = res.getString(1);
            String verlag = res.getString(2);
            String reihe = res.getString(3);
            if (!oldSammlung.equals(sammlung)) {
                oldSammlung = sammlung;
                nodeSammlung = new BookTreeNode(sammlung,
                        BookTreeNode.Type.SAMMLUNG);
                sammlungCount++;
                rootNode.add(nodeSammlung);
                oldVerlag = "";
                oldReihe = "";
            }
            if (verlag != null && !oldVerlag.equals(verlag)) {
                oldVerlag = verlag;
                nodeVerlag = new BookTreeNode(verlag,
                        BookTreeNode.Type.VERLAG);
                nodeSammlung.add(nodeVerlag);
                oldReihe = "";
            }
            if (reihe != null && !oldReihe.equals(reihe)) {
                oldReihe = reihe;
                nodeReihe = new BookTreeNode(reihe,
                        BookTreeNode.Type.REIHE);
                nodeVerlag.add(nodeReihe);
            }
        }
        nodeStructureChanged(rootNode);
        res.getStatement().close();
    }
}
