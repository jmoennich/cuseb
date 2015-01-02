/*  
 * Copyright 2012-2014 Jan Mönnich
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

import de.cuseb.data.Book;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;

/**
 *
 * @author jan
 */
public class BookTableTransferBooks extends ArrayList<Book> implements Transferable {

    private DataFlavor flavour;

    public BookTableTransferBooks() {
        try {
            flavour = new DataFlavor(
                    DataFlavor.javaJVMLocalObjectMimeType
                    + ";class=de.cuseb.table.BookTableTransferBooks");
        } catch (ClassNotFoundException cnfe) {
            // ignored
        }        
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        return this;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{flavour};
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.getRepresentationClass() == getClass();
    }
}
