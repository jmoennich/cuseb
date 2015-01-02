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

import java.awt.Component;
import javax.swing.GrayFilter;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;

public class BookTreeCellRenderer implements TreeCellRenderer {
    
    private TreeCellRenderer origTreeRenderer;
    private ImageIcon iconBooks;
    private ImageIcon iconBooksDisabled;
    private ImageIcon iconFolder;
    private ImageIcon iconFolderDisabled;
    private ImageIcon iconFolderOpen;
    private ImageIcon iconFolderOpenDisabled;
    
    public BookTreeCellRenderer() {
        String path = "/de/cuseb/images/";
        iconBooks = new ImageIcon(getClass().getResource(
                path + "books.png"));
        iconBooksDisabled = new ImageIcon(GrayFilter.createDisabledImage(
                iconBooks.getImage()));
        iconFolder = new ImageIcon(getClass().getResource(
                path + "folder.png"));
        iconFolderDisabled = new ImageIcon(GrayFilter.createDisabledImage(
                iconFolder.getImage()));
        iconFolderOpen = new ImageIcon(getClass().getResource(
                path + "folder-open.png"));
        iconFolderOpenDisabled = new ImageIcon(GrayFilter.createDisabledImage(
                iconFolderOpen.getImage()));
        origTreeRenderer = new DefaultTreeCellRenderer();
    }
    
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean selected, boolean expanded, boolean leaf, int row,
            boolean hasFocus) {
        
        JLabel label = (JLabel) origTreeRenderer.getTreeCellRendererComponent(
                tree, value, selected, expanded, leaf, row, hasFocus);
        label.setBorder(new EmptyBorder(1, 1, 1, 1));
        
        if (value instanceof BookTreeNode) {
            BookTreeNode node = (BookTreeNode) value;
            if (node.getType() == BookTreeNode.Type.SAMMLUNG) {
                label.setIcon(iconBooks);
                label.setDisabledIcon(iconBooksDisabled);
            } else {
                label.setIcon(iconFolder);
                label.setDisabledIcon(iconFolderDisabled);
            }
        } else {
            label.setIcon(null);
            label.setDisabledIcon(null);
        }
        return label;
    }
}
