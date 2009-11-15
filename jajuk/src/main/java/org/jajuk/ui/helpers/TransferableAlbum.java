/*
 *  Jajuk
 *  Copyright (C) 2003-2009 The Jajuk Team
 *  http://jajuk.info
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *  $Revision$
 */

package org.jajuk.ui.helpers;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Transferable album ( for DND ).
 */
public class TransferableAlbum extends DefaultMutableTreeNode implements Transferable {
  
  /** Generated serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** The Constant ALBUM_FLAVOR.  DOCUMENT_ME */
  public static final DataFlavor ALBUM_FLAVOR = new DataFlavor(
      DataFlavor.javaJVMLocalObjectMimeType, "Album");

  /** DOCUMENT_ME. */
  private final Object oData;

  /**
   * Instantiates a new transferable album.
   * 
   * @param oData DOCUMENT_ME
   */
  public TransferableAlbum(Object oData) {
    this.oData = oData;
  }

  /**
   * Gets the data.
   * 
   * @return the data
   */
  public Object getData() {
    return oData;
  }

  /** DOCUMENT_ME. */
  private final DataFlavor[] flavors = { ALBUM_FLAVOR };

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.datatransfer.Transferable#getTransferDataFlavors()
   */
  public DataFlavor[] getTransferDataFlavors() {
    return flavors;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.datatransfer.Transferable#isDataFlavorSupported(java.awt.datatransfer.DataFlavor)
   */
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return Arrays.asList(flavors).contains(flavor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.datatransfer.Transferable#getTransferData(java.awt.datatransfer.DataFlavor)
   */
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (flavor == ALBUM_FLAVOR) {
      return this;
    }
    throw new UnsupportedFlavorException(flavor);
  }
}
