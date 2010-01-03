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
package org.jajuk.base;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.services.bookmark.History;
import org.jajuk.services.core.ExitService;
import org.jajuk.services.core.SessionService;
import org.jajuk.services.tags.NoTagsTagImpl;
import org.jajuk.services.tags.Tag;
import org.jajuk.ui.helpers.ManualDirectoryRefreshReporter;
import org.jajuk.ui.helpers.RefreshReporter;
import org.jajuk.ui.widgets.InformationJPanel;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.ReadOnlyIterator;
import org.jajuk.util.UtilFeatures;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * A physical directory
 * <p>
 * Physical item.
 */
public class Directory extends PhysicalItem implements Comparable<Directory> {

  /** Parent directory ID*. */
  private final Directory dParent;

  /** Directory device. */
  private final Device device;

  /** IO file for optimizations*. */
  private java.io.File fio;

  /** DOCUMENT_ME. */
  private long discID = -1l;

  /**
   * Directory constructor.
   * 
   * @param sName DOCUMENT_ME
   * @param sId DOCUMENT_ME
   * @param dParent DOCUMENT_ME
   * @param device DOCUMENT_ME
   */
  public Directory(String sId, String sName, Directory dParent, Device device) {
    super(sId, sName);
    this.dParent = dParent;
    setProperty(Const.XML_DIRECTORY_PARENT, (dParent == null ? "-1" : dParent.getID()));
    this.device = device;
    setProperty(Const.XML_DEVICE, device.getID());
    this.fio = new File(device.getUrl() + getRelativePath());
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getIdentifier()
   */
  @Override
  public final String getLabel() {
    return XML_DIRECTORY;
  }

  /**
   * toString method.
   * 
   * @return the string
   */
  @Override
  public String toString() {
    return "Directory[ID=" + getID() + " Name={{" + getRelativePath() + "}} ParentID="
        + (dParent == null ? "null" : dParent.getID()) + " Device={{" + device.getName() + "}}]";
  }

  /**
   * Gets the absolute path.
   * 
   * @return the absolute path
   */
  public String getAbsolutePath() {
    StringBuilder sbOut = new StringBuilder(getDevice().getUrl()).append(getRelativePath());
    return sbOut.toString();
  }

  /**
   * Gets the device.
   * 
   * @return the device
   */
  public Device getDevice() {
    return device;
  }

  /**
   * Gets the parent directory.
   * 
   * @return the parent directory
   */
  public Directory getParentDirectory() {
    return dParent;
  }

  /**
   * Gets the directories.
   * 
   * @return all sub directories
   */
  public Set<Directory> getDirectories() {
    Set<Directory> out = new LinkedHashSet<Directory>(2);
    ReadOnlyIterator<Directory> it = DirectoryManager.getInstance().getDirectoriesIterator();
    while (it.hasNext()) {
      Directory directory = it.next();
      if (directory.getFio().getParentFile().equals(this.getFio())
      // check the device of the tested directory to handle directories
          // from cdrom
          && directory.getDevice().equals(getDevice())) {
        out.add(directory);
      }
    }
    return out;
  }

  /**
   * return child files.
   * 
   * @return child files
   */
  public Set<org.jajuk.base.File> getFiles() {
    Set<org.jajuk.base.File> out = new LinkedHashSet<org.jajuk.base.File>(2);
    ReadOnlyIterator<org.jajuk.base.File> it = FileManager.getInstance().getFilesIterator();
    while (it.hasNext()) {
      org.jajuk.base.File file = it.next();
      if (file.getFIO().getParentFile().equals(this.getFio())) {
        out.add(file);
      }
    }
    return out;
  }

  /**
   * return playlists.
   * 
   * @return playlists
   */
  public Set<Playlist> getPlaylistFiles() {
    Set<Playlist> out = new LinkedHashSet<Playlist>(2);
    ReadOnlyIterator<Playlist> it = PlaylistManager.getInstance().getPlaylistsIterator();
    while (it.hasNext()) {
      Playlist plf = it.next();
      if (plf.getFIO().getParentFile().equals(this.getFio())) {
        out.add(plf);
      }
    }
    return out;
  }

  /**
   * return ordered sibling files from the given file index.
   * 
   * @param fileStart DOCUMENT_ME
   * 
   * @return files or null if the given file is unknown
   */
  public List<org.jajuk.base.File> getFilesFromFile(org.jajuk.base.File fileStart) {
    Set<org.jajuk.base.File> files = getFiles();
    List<org.jajuk.base.File> alOut = new ArrayList<org.jajuk.base.File>(files);
    int indexOfStartingItem = alOut.indexOf(fileStart);
    if (indexOfStartingItem < 0) {
      return null;
    }
    return alOut.subList(indexOfStartingItem + 1, alOut.size());
  }

  /**
   * return ordered child files recursively.
   * 
   * @return child files recursively
   */
  public List<org.jajuk.base.File> getFilesRecursively() {
    List<org.jajuk.base.File> alFiles = new ArrayList<org.jajuk.base.File>(100);
    for (Item item : FileManager.getInstance().getFiles()) {
      org.jajuk.base.File file = (org.jajuk.base.File) item;
      if (file.hasAncestor(this)) {
        alFiles.add(file);
      }
    }
    return alFiles;
  }
  
   /**
   * return ordered child playlists recursively.
   * 
   * @return child playlists recursively
   */
  public List<Playlist> getPlaylistsRecursively() {
    List<Playlist> alPlaylists = new ArrayList<Playlist>(100);
    for (Item item : PlaylistManager.getInstance().getPlaylists()) {
      Playlist playlist = (Playlist) item;
      if (playlist.hasAncestor(this)) {
        alPlaylists.add(playlist);
      }
    }
    return alPlaylists;
  }

  /**
   * Scan all files in a directory.
   * 
   * @param bDeepScan :
   * force files tag read
   * @param reporter Refresh handler
   */
  public void scan(boolean bDeepScan, RefreshReporter reporter) {

    // Make sure to reset the disc ID
    this.discID = -1;
    java.io.File[] filelist = getFio().listFiles(UtilSystem.getFileFilter());
    if (filelist == null || filelist.length == 0) { // none file, leave
      return;
    }

    // Create a list of music files and playlist files to consider
    List<File> musicFiles = new ArrayList<File>(filelist.length);
    List<File> playlistFiles = new ArrayList<File>(filelist.length);
    List<Long> durations = new ArrayList<Long>(filelist.length);

    for (int i = 0; i < filelist.length; i++) {
      // Leave ASAP if exit request
      if (ExitService.isExiting()) {
        return;
      }
      // Check file name is correct (useful to fix name encoding
      // issues)
      if (!new File(filelist[i].getAbsolutePath()).exists()) {
        Log.warn("Cannot read file name (please rename it): {{" + filelist[i].getAbsolutePath()
            + "}}");
        continue;
      }

      // Ignore iTunes files
      if (filelist[i].getName().startsWith("._")) {
        continue;
      }

      // check if we recognize the file as music file
      String extension = UtilSystem.getExtension(filelist[i]);
      Type type = TypeManager.getInstance().getTypeByExtension(extension);

      // Now, compute disc ID and cache tags (only in deep mode because we don't
      // want to read tags in fast modes)
      if (bDeepScan && type.getTagImpl() != null) {
        try {
          Tag tag = Tag.getTagForFio(filelist[i], false);
          durations.add(tag.getLength());
        } catch (JajukException je) {
          Log.error(je);
        }
      }

      boolean bIsMusic = (Boolean) type.getValue(Const.XML_TYPE_IS_MUSIC);
      if (bIsMusic) {
        musicFiles.add(filelist[i]);
      } else { // playlist
        playlistFiles.add(filelist[i]);
      }
    }

    // Compute the disc id (deep mode only)
    if (bDeepScan) {
      this.discID = UtilFeatures.computeDiscID(durations);
    }

    // Perform actual scan and check errors for each file
    for (File musicfile : musicFiles) {
      try {
        scanMusic(musicfile, bDeepScan, reporter);
      } catch (Exception e) {
        Log.error(103, filelist.length > 0 ? "{{" + musicfile.toString() + "}}" : "", e);
      }
    }
    for (File playlistFile : playlistFiles) {
      try {
        scanPlaylist(playlistFile, bDeepScan, reporter);
      } catch (Exception e) {
        Log.error(103, filelist.length > 0 ? "{{" + playlistFile.toString() + "}}" : "", e);
      }
    }
    // Clear the tag cache so tags are actually read at next deep refresh
    Tag.clearCache();
  }

  /**
   * Scan music.
   * 
   * @param bDeepScan DOCUMENT_ME
   * @param reporter DOCUMENT_ME
   * @param music DOCUMENT_ME
   * 
   * @throws JajukException the jajuk exception
   */
  private void scanMusic(java.io.File music, boolean bDeepScan, RefreshReporter reporter)
      throws JajukException {
    String lName = music.getName();
    String sId = FileManager.createID(lName, this);
    // check the file is not already known in database
    org.jajuk.base.File fileRef = FileManager.getInstance().getFileByID(sId);
    // Set name again to make sure Windows users will see actual
    // name with right case
    if (UtilSystem.isUnderWindows() && fileRef != null) {
      fileRef.setName(lName);
    }

    // if known file and no deep scan, just leave
    if (fileRef != null && !bDeepScan) {
      return;
    }

    // If the audio file format doesn't support tagging (like wav) and the file
    // is already known, continue
    Type type = TypeManager.getInstance().getTypeByExtension(UtilSystem.getExtension(music));
    if ((type.getTaggerClass() == null || type.getTaggerClass().equals(NoTagsTagImpl.class))
        && fileRef != null) {
      return;
    }

    // Ignore tag error to make sure to get a
    // tag object in all cases
    Tag tag = Tag.getTagForFio(music, true);
    if (tag.isCorrupted()) {
      if (reporter != null) {
        reporter.notifyCorruptedFile();
      }
      // if an error occurs, just display a message but keep the track
      Log.error(103, "{{" + music.getAbsolutePath() + "}}", null);
    }

    String sTrackName = tag.getTrackName();
    String sAlbumName = tag.getAlbumName();
    String sAuthorName = tag.getAuthorName();
    String sStyle = tag.getStyleName();
    long length = tag.getLength(); // length in sec
    String sYear = tag.getYear();
    long lQuality = tag.getQuality();
    String sComment = tag.getComment();
    long lOrder = tag.getOrder();
    String sAlbumArtist = tag.getAlbumArtist();
    long discNumber = tag.getDiscNumber();

    if (fileRef == null && reporter != null) {
      // stats, do it here and not
      // before because we ignore the
      // file if we cannot read it
      reporter.notifyNewFile();
    }
    

    
    Track track = registerFile(music, sId, sTrackName, sAlbumName, sAuthorName, sStyle, length, sYear, lQuality,
        sComment, lOrder, sAlbumArtist, discID, discNumber);
    
    for(String s: TrackManager.getInstance().getActivatedExtraTags()){
      track.setProperty(s, tag.getTagField(s));
    }
  }

  /**
   * Register file.
   * 
   * @param music DOCUMENT_ME
   * @param sId DOCUMENT_ME
   * @param sTrackName DOCUMENT_ME
   * @param sAlbumName DOCUMENT_ME
   * @param sAuthorName DOCUMENT_ME
   * @param sStyle DOCUMENT_ME
   * @param length DOCUMENT_ME
   * @param sYear DOCUMENT_ME
   * @param lQuality DOCUMENT_ME
   * @param sComment DOCUMENT_ME
   * @param lOrder DOCUMENT_ME
   * @param sAlbumArtist DOCUMENT_ME
   * @param discID DOCUMENT_ME
   * @param discNumber DOCUMENT_ME
   */
  private Track registerFile(java.io.File music, String sId, String sTrackName, String sAlbumName,
      String sAuthorName, String sStyle, long length, String sYear, long lQuality, String sComment,
      long lOrder, String sAlbumArtist, long discID, long discNumber) {
    Album album = null;
    album = AlbumManager.getInstance().registerAlbum(sAlbumName, sAlbumArtist, discID);
    Style style = StyleManager.getInstance().registerStyle(sStyle);
    Year year = YearManager.getInstance().registerYear(sYear);
    Author author = AuthorManager.getInstance().registerAuthor(sAuthorName);
    Type type = TypeManager.getInstance().getTypeByExtension(UtilSystem.getExtension(music));
    // Store number of tracks in collection
    long trackNumber = TrackManager.getInstance().getElementCount();
    Track track = TrackManager.getInstance().registerTrack(sTrackName, album, style, author,
        length, year, lOrder, type, discNumber);

    // Note date for file date property. CAUTION: do not try to
    // check current date to accelerate refreshing if file has not
    // been modified since last refresh as user can rename a parent
    // directory and the files times under it are not modified
    long lastModified = music.lastModified();

    // Use file date if the "force file date" option is used
    if (Conf.getBoolean(Const.CONF_FORCE_FILE_DATE)) {
      track.setDiscoveryDate(new Date(lastModified));
    } else if (TrackManager.getInstance().getElementCount() > trackNumber) {
      // Update discovery date only if it is a new track

      // A new track has been created, we can safely update
      // the track date
      // We don't want to update date if the track is already
      // known, even if
      // it is a new file because a track can map several
      // files and discovery date
      // is a track attribute, not file one
      track.setDiscoveryDate(new Date());
    }

    org.jajuk.base.File file = FileManager.getInstance().registerFile(sId, music.getName(), this,
        track, music.length(), lQuality);
    // Set file date
    file.setProperty(Const.XML_FILE_DATE, new Date(lastModified));
    // Comment is at the track level, note that we take last
    // found file comment but we changing a comment, we will
    // apply to all files for a track
    track.setComment(sComment);
    // Make sure to refresh file size
    file.setProperty(Const.XML_SIZE, music.length());
    
    return track;
  }

  /**
   * Scan playlist.
   * DOCUMENT_ME
   * 
   * @param file DOCUMENT_ME
   * @param bDeepScan DOCUMENT_ME
   * @param reporter DOCUMENT_ME
   * 
   * @throws JajukException the jajuk exception
   */
  private void scanPlaylist(final java.io.File file, final boolean bDeepScan,
      final RefreshReporter reporter) throws JajukException {
    String sId = PlaylistManager.createID(file.getName(), this);
    Playlist plfRef = PlaylistManager.getInstance().getPlaylistByID(sId);
    // if known playlist and no deep scan, just leave
    if (plfRef != null && !bDeepScan) {
      return;
    }
    Playlist plFile = PlaylistManager.getInstance().registerPlaylistFile(file, this);
    plFile.forceRefresh(); // force refresh
    if (plfRef == null && reporter != null) {
      // stats, do it here and not
      // before because we ignore the
      // file if we cannot read it
      reporter.notifyNewFile();
    }
  }

  /**
   * Reset pre-calculated paths*.
   */
  protected void reset() {
    fio = null;
  }

  /**
   * Return full directory path name relative to device url.
   * 
   * @return String
   */
  public final String getRelativePath() {
    if (getName().equals("")) {
      // if this directory is a root device
      // directory
      return "";
    }
    StringBuilder sbOut = new StringBuilder().append(java.io.File.separatorChar).append(getName());
    boolean bTop = false;
    Directory dCurrent = this;
    while (!bTop && dCurrent != null) {
      dCurrent = dCurrent.getParentDirectory();
      if (dCurrent != null && !dCurrent.getName().equals("")) {
        // if it is the root directory, no parent
        sbOut.insert(0, java.io.File.separatorChar).insert(1, dCurrent.getName());
      } else {
        bTop = true;
      }
    }
    return sbOut.toString();
  }

  /**
   * Gets the fio.
   * 
   * @return Returns the IO file reference to this directory.
   */
  public File getFio() {
    if (fio == null) {
      fio = new java.io.File(getAbsolutePath());
    }
    return fio;
  }

  /**
   * Alphabetical comparator used to display ordered lists of directories
   * <p>
   * Sort ignoring cases
   * </p>.
   * 
   * @param otherDirectory DOCUMENT_ME
   * 
   * @return comparison result
   */
  public int compareTo(Directory otherDirectory) {
    if(otherDirectory == null) {
      return -1;
    }

    // Perf: leave if directories are equals
    if (otherDirectory.equals(this)) {
      return 0;
    }
    String abs = new StringBuilder(getDevice().getName()).append(getAbsolutePath()).toString();
    String otherAbs = new StringBuilder(otherDirectory.getDevice().getName()).append(
        otherDirectory.getAbsolutePath()).toString();
    // should ignore case to get a B c ... and not Bac
    // We must be consistent with equals, see
    // http://java.sun.com/javase/6/docs/api/java/lang/Comparable.html
    int comp = abs.compareToIgnoreCase(otherAbs);
    if (comp == 0) {
      return abs.compareTo(otherAbs);
    } else {
      return comp;
    }
  }

  /**
   * Return whether this item should be hidden with hide option.
   * 
   * @return whether this item should be hidden with hide option
   */
  public boolean shouldBeHidden() {
    if (getDevice().isMounted() || !Conf.getBoolean(Const.CONF_OPTIONS_HIDE_UNMOUNTED)) {
      return false;
    }
    return true;
  }

  /**
   * Get item description.
   * 
   * @return the desc
   */
  @Override
  public String getDesc() {
    String sName = null;
    if (getParentDirectory() == null) {
      sName = getDevice().getUrl();
    } else {
      sName = getFio().getAbsolutePath();
    }
    return Messages.getString("Item_Directory") + " : " + sName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getHumanValue(java.lang.String)
   */
  @Override
  public String getHumanValue(String sKey) {
    if (Const.XML_DIRECTORY_PARENT.equals(sKey)) {
      Directory parentdir = DirectoryManager.getInstance()
          .getDirectoryByID((String) getValue(sKey));
      if (parentdir == null) {
        return ""; // no parent directory
      } else {
        return parentdir.getFio().getAbsolutePath();
      }
    } else if (Const.XML_DEVICE.equals(sKey)) {
      Device dev = DeviceManager.getInstance().getDeviceByID((String) getValue(sKey));
      if(dev == null) {
        return "";
      }
      return dev.getName();
    }
    if (Const.XML_NAME.equals(sKey)) {
      if (dParent == null) { // if no parent, take device name
        return getDevice().getUrl();
      } else {
        return getName();
      }
    } else {
      // default
      return super.getHumanValue(sKey);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getIconRepresentation()
   */
  @Override
  public ImageIcon getIconRepresentation() {
    ImageIcon icon = null;
    // is this device synchronized?
    if (getBooleanValue(Const.XML_DIRECTORY_SYNCHRONIZED)) {
      icon = IconLoader.getIcon(JajukIcons.DIRECTORY_SYNCHRO);
    } else {
      icon = IconLoader.getIcon(JajukIcons.DIRECTORY_DESYNCHRO);
    }
    return icon;
  }

  /**
   * Set name (useful for Windows because same object can have different cases).
   * 
   * @param name Item name
   */
  protected void setName(String name) {
    setProperty(Const.XML_NAME, name);
    this.name = name;
  }

  /**
   * Refresh synchronously the directory recursively.
   * 
   * @param deep refresh ?
   * @param reporter DOCUMENT_ME
   * 
   * @throws JajukException the jajuk exception
   */
  public void refresh(boolean deep, RefreshReporter reporter) throws JajukException {
    scan(deep, reporter);
    final java.io.File[] lFiles = getFio().listFiles(UtilSystem.getDirFilter());
    if (lFiles != null) {
      for (final java.io.File element : lFiles) {
        final Directory subdir = DirectoryManager.getInstance().registerDirectory(
            element.getName(), this, getDevice());
        subdir.refresh(deep, reporter);
      }
    }
  }

  /**
   * Refresh : scan asynchronously the directory to find tracks.
   * 
   * @param bAsynchronous :
   * set asynchronous or synchronous mode
   * @param bAsk :
   * should we ask user if he wants to perform a deep or fast scan?
   * default=deep
   */
  public void manualRefresh(final boolean bAsynchronous, final boolean bAsk) {
    final RefreshReporter reporter = new ManualDirectoryRefreshReporter(getDevice());
    final Thread t = new Thread("Manual Refresh Thread") {
      @Override
      public void run() {
        try {
          int result = getDevice().prepareRefresh(bAsk);
          if (result == Device.OPTION_REFRESH_CANCEL) {
            return;
          }
          InformationJPanel.getInstance().setMessage(
              Messages.getString("ActionRefresh.1") + " : " + Directory.this.getName(), 1);
          boolean deep = (result == Device.OPTION_REFRESH_DEEP);
          // start counting
          reporter.startup();
          // Cleanup old files/directories/playlists
          cleanRemovedFiles();
          // Actual refresh
          refresh(deep, reporter);
          // cleanup logical items
          org.jajuk.base.Collection.cleanupLogical();
          ObservationManager.notify(new JajukEvent(JajukEvents.DEVICE_REFRESH));
          reporter.done();
          // commit collection at each refresh (can be useful if
          // application
          // is closed brutally with control-C or shutdown and that
          // exit hook
          // have no time to perform commit)
          try {
            org.jajuk.base.Collection.commit(SessionService
                .getConfFileByPath(Const.FILE_COLLECTION));
          } catch (final IOException e) {
            Log.error(e);
          }
        } catch (JajukException e) {
          Messages.showErrorMessage(e.getCode());
          Log.debug(e);
          return;
        }
      }
    };
    if (bAsynchronous) {
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();
    } else {
      // simply call the run method
      t.run();
    }
  }

  /**
   * Scan directory to cleanup removed files and playlists.
   * 
   * @return whether some items have been removed
   */
  public boolean cleanRemovedFiles() {
    boolean bChanges = false;
    // need to use a shallow copy to avoid concurrent exceptions
    final List<Directory> dirs = DirectoryManager.getInstance().getDirectories();
    // directories cleanup
    for (final Item item : dirs) {
      final Directory dir = (Directory) item;
      if (!ExitService.isExiting() && dir.getDevice().isMounted() && dir.isChildOf(this)
          && !dir.getFio().exists()) {
        // note that associated files are removed too
        DirectoryManager.getInstance().removeDirectory(dir.getID());
        Log.debug("Removed: " + dir);
        bChanges = true;
      }
    }
    // files cleanup
    final List<org.jajuk.base.File> lFiles = FileManager.getInstance().getFiles();
    for (final org.jajuk.base.File file : lFiles) {
      if (!ExitService.isExiting()
          // Only take into consideration files from this directory or
          // from
          // sub-directories
          && (file.getDirectory().equals(this) || file.getDirectory().isChildOf(this))
          && file.isReady() &&
          // Remove file if it doesn't exist any more or if it is a iTunes
          // file (useful for jajuk < 1.4)
          !file.getFIO().exists() || file.getName().startsWith("._")) {
        FileManager.getInstance().removeFile(file);
        Log.debug("Removed: " + file);
        bChanges = true;
      }
    }
    // Playlist cleanup
    final List<Playlist> plfiles = PlaylistManager.getInstance().getPlaylists();
    for (final Playlist plf : plfiles) {
      if (!ExitService.isExiting()
          // Only take into consideration files from this directory or
          // from
          // sub-directories
          && (plf.getDirectory().equals(this) || plf.getDirectory().isChildOf(this))
          && plf.isReady() && !plf.getFIO().exists()) {
        PlaylistManager.getInstance().removePlaylistFile(plf);
        Log.debug("Removed: " + plf);
        bChanges = true;
      }
    }
    // clear history to remove old files referenced in it
    if (Conf.getString(Const.CONF_HISTORY) != null) {
      History.getInstance().clear(Integer.parseInt(Conf.getString(Const.CONF_HISTORY)));
    }
    return bChanges;
  }

  /**
   * Return true is this is a child directory of the specified directory.
   * 
   * @param directory ancestor directory
   * 
   * @return true, if checks if is child of
   */
  public boolean isChildOf(Directory directory) {
    Directory dirTested = getParentDirectory();
    if (dirTested == null) {
      return false;
    }
    while (true) {
      if (dirTested.equals(directory)) {
        return true;
      } else {
        dirTested = dirTested.getParentDirectory();
        if (dirTested == null) {
          return false;
        }
      }
    }
  }
}
