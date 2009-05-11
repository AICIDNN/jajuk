/*
 *  Jajuk
 *  Copyright (C) 2003 The Jajuk Team
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

package org.jajuk.base;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.Observer;
import org.jajuk.util.Const;
import org.jajuk.util.MD5Processor;
import org.jajuk.util.Messages;
import org.jajuk.util.ReadOnlyIterator;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * Convenient class to manage playlists
 */
public final class PlaylistManager extends ItemManager implements Observer {
  /** Self instance */
  private static PlaylistManager singleton;

  /**
   * No constructor available, only static access
   */
  private PlaylistManager() {
    super();
    // ---register properties---
    // ID
    registerProperty(new PropertyMetaInformation(Const.XML_ID, false, true, false, false, false,
        String.class, null));
    // Name
    registerProperty(new PropertyMetaInformation(Const.XML_NAME, false, true, true, true, false,
        String.class, null));
    // Directory
    registerProperty(new PropertyMetaInformation(Const.XML_DIRECTORY, false, true, true, false,
        false, String.class, null));
  }

  /**
   * @return singleton
   */
  public static PlaylistManager getInstance() {
    if (singleton == null) {
      singleton = new PlaylistManager();
    }
    return singleton;
  }

  /**
   * Register an Playlist with a known id
   * 
   * @param fio
   * @param dParentDirectory
   */
  public synchronized Playlist registerPlaylistFile(java.io.File fio, Directory dParentDirectory) {
    String sId = createID(fio.getName(), dParentDirectory);
    return registerPlaylistFile(sId, fio.getName(), dParentDirectory);
  }

  /**
   * @param sName
   * @param dParentDirectory
   * @return ItemManager ID
   */
  protected static String createID(String sName, Directory dParentDirectory) {
    return MD5Processor.hash(new StringBuilder(dParentDirectory.getDevice().getName()).append(
        dParentDirectory.getRelativePath()).append(sName).toString());
  }

  /**
   * Delete a playlist
   * 
   */
  public synchronized void removePlaylistFile(Playlist plf) {
    String sFileToDelete = plf.getDirectory().getFio().getAbsoluteFile().toString()
        + java.io.File.separatorChar + plf.getName();
    java.io.File fileToDelete = new java.io.File(sFileToDelete);
    if (fileToDelete.exists()) {
      if (!fileToDelete.delete()) {
        Log.warn("Could not delete file: " + fileToDelete.toString());
      }
      // check that file has been really deleted (sometimes, we get no
      // exception)
      if (fileToDelete.exists()) {
        Log.error("131", new JajukException(131));
        Messages.showErrorMessage(131);
        return;
      }
    }
    // remove playlist
    removeItem(plf);
  }

  /**
   * Register an Playlist with a known id
   * 
   * @param sName
   */
  public synchronized Playlist registerPlaylistFile(String sId, String sName,
      Directory dParentDirectory) {
    Playlist playlistFile = getPlaylistByID(sId);
    if (playlistFile != null) {
      return playlistFile;
    }
    playlistFile = new Playlist(sId, sName, dParentDirectory);
    registerItem(playlistFile);
    if (dParentDirectory.getDevice().isRefreshing()) {
      Log.debug("Registered new playlist: " + playlistFile);
    }
    return playlistFile;
  }

  /**
   * Clean all references for the given device
   * 
   * @param sId
   *          : Device id
   */
  public synchronized void cleanDevice(String sId) {
    for (Playlist plf : getPlaylists()) {
      if (plf.getDirectory() == null || plf.getDirectory().getDevice().getID().equals(sId)) {
        removeItem(plf);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.ItemManager#getIdentifier()
   */
  @Override
  public String getLabel() {
    return Const.XML_PLAYLIST_FILES;
  }

  /**
   * Change a playlist name
   * 
   * @param plfOld
   * @param sNewName
   * @return new playlist
   */
  public synchronized Playlist changePlaylistFileName(Playlist plfOld, String sNewName)
      throws JajukException {
    // check given name is different
    if (plfOld.getName().equals(sNewName)) {
      return plfOld;
    }
    // check if this file still exists
    if (!plfOld.getFIO().exists()) {
      throw new JajukException(135);
    }
    java.io.File ioNew = new java.io.File(plfOld.getFIO().getParentFile().getAbsolutePath()
        + java.io.File.separator + sNewName);
    // recalculate file ID
    plfOld.getDirectory();
    String sNewId = PlaylistManager.createID(sNewName, plfOld.getDirectory());
    // create a new playlist (with own fio and sAbs)
    Playlist plfNew = new Playlist(sNewId, sNewName, plfOld.getDirectory());
    // Transfer all properties (id and name)
    // We use a shallow copy of properties to avoid any properties share between
    // two items
    plfNew.setProperties(plfOld.getShallowProperties());
    plfNew.setProperty(Const.XML_ID, sNewId); // reset new id and name
    plfNew.setProperty(Const.XML_NAME, sNewName); // reset new id and name
    // check file name and extension
    if (plfNew.getName().lastIndexOf('.') != plfNew.getName().indexOf('.')// just
        // one
        // '.'
        || !(UtilSystem.getExtension(ioNew).equals(Const.EXT_PLAYLIST))) { // check
      // extension
      Messages.showErrorMessage(134);
      throw new JajukException(134);
    }
    // check if future file exists (under windows, file.exists
    // return true even with different case so we test file name is
    // different)
    if (!ioNew.getName().equalsIgnoreCase(plfOld.getName()) && ioNew.exists()) {
      throw new JajukException(134);
    }
    // try to rename file on disk
    try {
      plfOld.getFIO().renameTo(ioNew);
    } catch (Exception e) {
      throw new JajukException(134, e);
    }
    // OK, remove old file and register this new file
    removeItem(plfOld);
    registerItem(plfNew);
    return plfNew;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Observer#update(org.jajuk.base.Event)
   */
  public void update(JajukEvent event) {
    JajukEvents subject = event.getSubject();
    if (JajukEvents.FILE_NAME_CHANGED.equals(subject)) {
      Properties properties = event.getDetails();
      File fNew = (File) properties.get(Const.DETAIL_NEW);
      File fileOld = (File) properties.get(Const.DETAIL_OLD);
      // search references in playlists
      ReadOnlyIterator<Playlist> it = getPlaylistsIterator();
      while (it.hasNext()) {
        Playlist plf = it.next();
        if (plf.isReady()) { // check only in mounted
          // playlists, note that we can't
          // change unmounted playlists
          try {
            if (plf.getFiles().contains(fileOld)) {
              plf.replaceFile(fileOld, fNew);
            }
          } catch (Exception e) {
            Log.error(17, e);
          }
        }
      }
    }
  }

  public Set<JajukEvents> getRegistrationKeys() {
    Set<JajukEvents> eventSubjectSet = new HashSet<JajukEvents>();
    eventSubjectSet.add(JajukEvents.FILE_NAME_CHANGED);
    return eventSubjectSet;
  }

  /**
   * @param sID
   *          Item ID
   * @return item
   */
  public Playlist getPlaylistByID(String sID) {
    return (Playlist) getItemByID(sID);
  }

  /**
   * 
   * @return ordered playlists list
   */
  @SuppressWarnings("unchecked")
  public synchronized List<Playlist> getPlaylists() {
    return (List<Playlist>) getItems();
  }

  /**
   * 
   * @return playlists iterator
   */
  @SuppressWarnings("unchecked")
  public synchronized ReadOnlyIterator<Playlist> getPlaylistsIterator() {
    return new ReadOnlyIterator<Playlist>((Iterator<Playlist>) getItemsIterator());
  }
}
