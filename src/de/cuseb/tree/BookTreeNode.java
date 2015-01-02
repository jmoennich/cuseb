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

import javax.swing.tree.DefaultMutableTreeNode;

public class BookTreeNode extends DefaultMutableTreeNode {

    private String title;
    private Type type;

    public static enum Type {

        SAMMLUNG, VERLAG, REIHE
    };

    public BookTreeNode(String title, Type type) {
        super(title);
        this.title = title;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof BookTreeNode)) {
            return false;
        }
        BookTreeNode node = (BookTreeNode) o;
        return node.getType() == type && node.getTitle().equals(title);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (this.title != null ? this.title.hashCode() : 0);
        hash = 37 * hash + (this.type != null ? this.type.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return title;
    }
}
