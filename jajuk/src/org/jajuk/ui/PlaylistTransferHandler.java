/*
 *  Jajuk
 *  Copyright (C) 2003 bflorat
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

package org.jajuk.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.JPanel;

import org.jajuk.base.Album;
import org.jajuk.base.Author;
import org.jajuk.base.BasicFile;
import org.jajuk.base.Bookmarks;
import org.jajuk.base.Directory;
import org.jajuk.base.FIFO;
import org.jajuk.base.File;
import org.jajuk.base.Style;
import org.jajuk.base.Track;
import org.jajuk.util.log.Log;

/**
 *  Dnd support for playlists
 *
 * @author     bflorat
 * @created    13 f�vr. 2004
 */
/**
 * 
 *  DND handler for playlists
 *
 * @author     bflorat
 * @created    13 f�vr. 2004
 */
public class PlaylistTransferHandler implements DropTargetListener {
	
	private JPanel jpanel;
	private DropTarget dropTarget; //droptarget
	
	public PlaylistTransferHandler(JPanel jpanel, int action) {
		this.jpanel = jpanel;
		dropTarget = new DropTarget(jpanel, action, this);
	}
	
	public final void dragExit(DragSourceEvent dse) {
		dse.getDragSourceContext().setCursor(DragSource.DefaultMoveNoDrop);
	}	
	
	/* Methods for DropTargetListener */
	
	public final void dragEnter(DropTargetDragEvent dtde) {
	}
	
	public final void dragExit(DropTargetEvent dte) {
	}
	
	public final void dragOver(DropTargetDragEvent dtde) {
		PlaylistFileItem plfi = (PlaylistFileItem)(((DropTarget)dtde.getSource()).getComponent());
		if ( plfi.getType() == PlaylistFileItem.PLAYLIST_TYPE_BESTOF){ //no dnd to best of playlist
				dtde.rejectDrag();
		}	
	}
	
	public final void dropActionChanged(DropTargetDragEvent dtde) {
		int action = dtde.getDropAction();
		dtde.acceptDrag(action);			
	}
	
	public final void drop(DropTargetDropEvent dtde) {
		try {
			int action = dtde.getDropAction();
			Transferable transferable = dtde.getTransferable();
			PlaylistFileItem plfi = (PlaylistFileItem)(((DropTarget)dtde.getSource()).getComponent());
			Object oData = null;
			if (transferable.isDataFlavorSupported(TransferableTreeNode.NODE_FLAVOR) || transferable.isDataFlavorSupported(TransferableTableRow.ROW_FLAVOR)) {
				String sFlavor  = ((DataFlavor)Arrays.asList(transferable.getTransferDataFlavors()).get(0)).getHumanPresentableName(); 
				if ( sFlavor.equals("Node")){ //$NON-NLS-1$
					dtde.acceptDrop(action);				
					dtde.dropComplete(true);
					TransferableTreeNode ttn = (TransferableTreeNode)transferable.getTransferData(TransferableTreeNode.NODE_FLAVOR);
					oData = ttn.getData();	
				}
				else if ( sFlavor.equals("Row") ){ //$NON-NLS-1$
					dtde.acceptDrop(action);				
					dtde.dropComplete(true);
					TransferableTableRow ttr = (TransferableTableRow)transferable.getTransferData(TransferableTableRow.ROW_FLAVOR);
					oData = ttr.getData();
				} 
				else{
					dtde.rejectDrop();
					dtde.dropComplete(false);
				}
			}
			//computes logical selection if any
			ArrayList alLogicalTracks = null;
			ArrayList alLogicalFiles = null;
			if(oData instanceof Style || oData instanceof Author || oData instanceof Album || oData instanceof Track){
				if( oData instanceof Style ){
					alLogicalTracks = ((Style)oData).getTracks();
				}
				else if( oData instanceof Author ){
					alLogicalTracks = ((Author)oData).getTracks();
				}
				else if(  oData instanceof Album ){
					alLogicalTracks = ((Album)oData).getTracks();
				}
				else if( oData instanceof Track){
					alLogicalTracks = new ArrayList(100);
					alLogicalTracks.add(oData);
				}
				//prepare files
				if ( alLogicalTracks != null && alLogicalTracks.size() > 0){
					alLogicalFiles = new ArrayList(alLogicalTracks.size());
					Iterator it = alLogicalTracks.iterator();
					while ( it.hasNext()){
						Track track = (Track)it.next();
						File file = track.getPlayeableFile();
						if ( file == null){ //none mounted file for this track
							continue;
						}
						alLogicalFiles.add(track.getPlayeableFile());
					}
				}
			}
			//queue case
			if ( plfi.getType() == PlaylistFileItem.PLAYLIST_TYPE_QUEUE){
				if (oData instanceof File){
					FIFO.getInstance().push((File)oData,true);
				}
				else if(oData instanceof Directory){
					FIFO.getInstance().push(((Directory)oData).getFilesRecursively(),true);
				}
				else if(oData instanceof Style || oData instanceof Author || oData instanceof Album || oData instanceof Track){
					if ( alLogicalFiles != null){
						FIFO.getInstance().push(alLogicalFiles,true);
					}
				}		
			}
			//bookmark case
			else if ( plfi.getType() == PlaylistFileItem.PLAYLIST_TYPE_BOOKMARK){
				if (oData instanceof File){
					Bookmarks.getInstance().addFile((File)oData);
				}
				else if(oData instanceof Directory){
					Iterator it = ((Directory)oData).getFilesRecursively().iterator();
					while (it.hasNext()){
						File file = (File)it.next();
						Bookmarks.getInstance().addFile(file);	
					}
				}
				else if(oData instanceof Style || oData instanceof Author || oData instanceof Album || oData instanceof Track){
					if ( alLogicalFiles != null){
						Iterator it = alLogicalFiles.iterator();
						while (it.hasNext()){
							File file = (File)it.next();
							Bookmarks.getInstance().addFile(file);	
						}
					}
				}
			}
			//normal or new playlist case
			else if ( plfi.getType() == PlaylistFileItem.PLAYLIST_TYPE_NORMAL || plfi.getType() == PlaylistFileItem.PLAYLIST_TYPE_NEW){
				if (oData instanceof File){
					plfi.getPlaylistFile().addBasicFile(new BasicFile((File)oData));
				}
				else if(oData instanceof Directory){
					Iterator it = ((Directory)oData).getFilesRecursively().iterator();
					while (it.hasNext()){
						File file = (File)it.next();
						plfi.getPlaylistFile().addBasicFile(new BasicFile(file));	
					}
				}
				else if(oData instanceof Style || oData instanceof Author || oData instanceof Album || oData instanceof Track){
					if ( alLogicalFiles != null){
						Iterator it = alLogicalFiles.iterator();
						while (it.hasNext()){
							File file = (File)it.next();
							plfi.getPlaylistFile().addBasicFile(new BasicFile(file));	
						}
					}
				}
			}
		}		
		catch (Exception e) {	
			Log.error(e);
			dtde.rejectDrop();
			dtde.dropComplete(false);
		}
	}
}
