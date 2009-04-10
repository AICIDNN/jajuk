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

import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import org.jajuk.services.players.FIFO;
import org.jajuk.ui.thumbnails.ThumbnailManager;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilString;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.log.Log;

/**
 * A music file to be played
 * <p>
 * Physical item
 */
public class File extends PhysicalItem implements Comparable<File>, Const {

  /** Parent directory */
  protected final Directory directory;

  /** Associated track */
  protected Track track;

  /** IO file associated with this file */
  private java.io.File fio;

  /**
   * File instanciation
   * 
   * @param sId
   * @param sName
   * @param directory
   * @param track
   * @param lSize
   * @param sQuality
   */
  public File(String sId, String sName, Directory directory, Track track, long lSize, long lQuality) {
    super(sId, sName);
    this.directory = directory;
    setProperty(Const.XML_DIRECTORY, directory.getID());
    this.track = track;
    setProperty(Const.XML_TRACK, track.getID());
    setProperty(Const.XML_SIZE, lSize);
    setProperty(Const.XML_QUALITY, lQuality);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getIdentifier()
   */
  @Override
  public final String getLabel() {
    return XML_FILE;
  }

  /**
   * toString method
   */
  @Override
  public String toString() {
    return "File[ID=" + getID() + " Name={{" + getName() + "}} Dir=" + directory + " Size="
        + getSize() + " Quality=" + getQuality() + "]";
  }

  /**
   * String representation as displayed in a search result
   */
  public String toStringSearch() {
    StringBuilder sb = new StringBuilder(track.getStyle().getName2()).append('/').append(
        track.getAuthor().getName2()).append('/').append(track.getAlbum().getName2()).append('/')
        .append(track.getName()).append(" [").append(directory.getName()).append('/').append(
            getName()).append(']');
    return sb.toString();
  }

  /**
   * Return true is the specified directory is an ancestor for this file
   * 
   * @param directory
   * @return
   */
  public boolean hasAncestor(Directory directory) {
    Directory dirTested = getDirectory();
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

  /**
   * @return
   */
  public long getSize() {
    return getLongValue(Const.XML_SIZE);
  }

  /**
   * @return
   */
  public Directory getDirectory() {
    return directory;
  }

  /**
   * @return associated device
   */
  public Device getDevice() {
    return directory.getDevice();
  }

  /**
   * @return associated type
   */
  public Type getType() {
    String extension = UtilSystem.getExtension(this.getName());
    if (extension != null) {
      return TypeManager.getInstance().getTypeByExtension(extension);
    }
    return null;
  }

  /**
   * @return
   */
  public long getQuality() {
    return getLongValue(Const.XML_QUALITY);
  }

  /**
   * @return
   */
  public Track getTrack() {
    return track;
  }

  /**
   * Return absolute file path name
   * 
   * @return String
   */
  public String getAbsolutePath() {
    StringBuilder sbOut = new StringBuilder(getDevice().getUrl()).append(
        getDirectory().getRelativePath()).append(java.io.File.separatorChar).append(this.getName());
    return sbOut.toString();
  }

  /**
   * Alphabetical comparator used to display ordered lists of files
   * <p>
   * Sort ignoring cases
   * </p>
   * 
   * @param other
   *          file to be compared
   * @return comparison result
   */
  public int compareTo(File otherFile) {
    // Begin by comparing file parent directory for performances
    if (directory.equals(otherFile.getDirectory())) {
      // If both files are in the same directory, sort by track order

      int iOrder = (int) getTrack().getOrder();
      int iOrderOther = (int) otherFile.getTrack().getOrder();
      if (iOrder != iOrderOther) {
        return iOrder - iOrderOther;
      }
      // if same order too, simply compare file names
      String abs = getName();
      String otherAbs = otherFile.getName();
      // We must be consistent with equals, see
      // http://java.sun.com/javase/6/docs/api/java/lang/Comparable.html
      int comp = abs.compareToIgnoreCase(otherAbs);
      if (comp == 0) {
        return abs.compareTo(otherAbs);
      } else {
        return comp;
      }
    } else {
      // Files are in different directories, sort by parent directory
      return this.getDirectory().compareTo(otherFile.getDirectory());
    }
  }

  /**
   * Return true if the file can be accessed right now
   * 
   * @return true the file can be accessed right now
   */
  public boolean isReady() {
    return getDirectory().getDevice().isMounted();
  }

  /**
   * Return true if the file is currently refreshed or synchronized
   * 
   * @return true if the file is currently refreshed or synchronized
   */
  public boolean isScanned() {
    if (getDirectory().getDevice().isRefreshing() || getDirectory().getDevice().isSynchronizing()) {
      return true;
    }
    return false;
  }

  /**
   * Return Io file associated with this file
   * 
   * @return
   */
  public java.io.File getFIO() {
    if (fio == null) {
      fio = new java.io.File(getAbsolutePath());
    }
    return fio;
  }

  /**
   * Return whether this item should be hidden with hide option
   * 
   * @return whether this item should be hidden with hide option
   */
  public boolean shouldBeHidden() {
    if (getDirectory().getDevice().isMounted()
        || !Conf.getBoolean(Const.CONF_OPTIONS_HIDE_UNMOUNTED)) {
      return false;
    }
    return true;
  }

  /**
   * @param track
   *          The track to set.
   */
  public void setTrack(Track track) {
    // We remove previous track so it will be cleanup (if it maps no more
    // files). This allow cleaning old tracks after a change of tags from others
    // programs than jajuk
    if (!this.track.equals(track)) {
      this.track.removeFile(this);
      this.track = track;
      setProperty(Const.XML_TRACK, track.getID());
    }
  }

  /**
   * Get item description
   */
  @Override
  public String getDesc() {
    return Messages.getString("Item_File") + " : " + getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getHumanValue(java.lang.String)
   */
  @Override
  public String getHumanValue(String sKey) {
    if (Const.XML_DIRECTORY.equals(sKey)) {
      Directory dParent = DirectoryManager.getInstance().getDirectoryByID(getStringValue(sKey));
      return dParent.getFio().getAbsolutePath();
    } else if (Const.XML_TRACK.equals(sKey)) {
      return getTrack().getName();
    } else if (Const.XML_SIZE.equals(sKey)) {
      return (Math.round(getSize() / 10485.76) / 100f) + Messages.getString("FilesTreeView.54");
    } else if (Const.XML_QUALITY.equals(sKey)) {
      return getQuality() + Messages.getString("FIFO.13");
    } else if (Const.XML_ALBUM.equals(sKey)) {
      return getTrack().getAlbum().getName2();
    } else if (Const.XML_STYLE.equals(sKey)) {
      return getTrack().getStyle().getName2();
    } else if (Const.XML_AUTHOR.equals(sKey)) {
      return getTrack().getAuthor().getName2();
    } else if (Const.XML_TRACK_LENGTH.equals(sKey)) {
      return UtilString.formatTimeBySec(getTrack().getDuration());
    } else if (Const.XML_TRACK_RATE.equals(sKey)) {
      return Long.toString(getTrack().getRate());
    } else if (Const.XML_DEVICE.equals(sKey)) {
      return getDirectory().getDevice().getName();
    } else if (Const.XML_ANY.equals(sKey)) {
      return getAny();
    } else {// default
      return super.getHumanValue(sKey);
    }
  }

  /**
   * @return a human representation of all concatenated properties
   */
  @Override
  public String getAny() {
    // rebuild any
    StringBuilder sb = new StringBuilder(100);
    File file = this;
    Track lTrack = file.getTrack();
    sb.append(super.getAny()); // add all files-based properties
    // now add others properties
    sb.append(file.getDirectory().getDevice().getName());
    sb.append(lTrack.getName());
    sb.append(lTrack.getStyle().getName2());
    sb.append(lTrack.getAuthor().getName2());
    sb.append(lTrack.getAlbum().getName2());
    sb.append(lTrack.getDuration());
    sb.append(lTrack.getRate());
    sb.append(lTrack.getValue(Const.XML_TRACK_COMMENT));// custom properties now
    sb.append(lTrack.getValue(Const.XML_TRACK_ORDER));// custom properties now
    return sb.toString();
  }

  /** Reset pre-calculated paths* */
  protected void reset() {
    // sAbs = null;
    fio = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getIconRepresentation()
   */
  @Override
  public ImageIcon getIconRepresentation() {
    ImageIcon icon = null;
    String ext = UtilSystem.getExtension(getName()); // getName() is better
    // here as
    // it will do less and not
    // create java.io.File in File
    Type type = TypeManager.getInstance().getTypeByExtension(ext);
    // Find associated icon with this type
    URL iconUrl = null;
    String sIcon;
    if (type != null) {
      sIcon = (String) type.getProperties().get(Const.XML_TYPE_ICON);
      try {
        iconUrl = new URL(sIcon);
      } catch (MalformedURLException e) {
        Log.error(e);
      }
    }
    if (iconUrl == null) {
      icon = IconLoader.getIcon(JajukIcons.TYPE_WAV);
    } else {
      icon = new ImageIcon(iconUrl);
    }
    return icon;
  }

  /**
   * Set name (useful for Windows because same object can have different cases)
   * 
   * @param name
   *          Item name
   */
  protected void setName(String name) {
    setProperty(Const.XML_NAME, name);
    this.name = name;
  }

  /**
   * 
   * @return text to be displayed in the tray balloon and tooltip with HTML
   *         formating that is used correctly under Linux
   */
  public String getHTMLFormatText() {
    String sOut = "";
    sOut += "<HTML><br>";
    int size = 100;
    int maxSize = 30;
    ThumbnailManager.refreshThumbnail(FIFO.getPlayingFile().getTrack().getAlbum(), size);
    java.io.File cover = ThumbnailManager.getThumbBySize(FIFO.getPlayingFile().getTrack()
        .getAlbum(), size);
    if (cover.canRead()) {
      sOut += "<p ALIGN=center><img src='file:" + cover.getAbsolutePath() + "'/></p><br>";
    }
    sOut += "<p><b>" + UtilString.getLimitedString(getTrack().getName(), maxSize)
        + "</b></font></p>";
    String sAuthor = UtilString.getLimitedString(getTrack().getAuthor().getName(), maxSize);
    if (!sAuthor.equals(UNKNOWN_AUTHOR)) {
      sOut += "<p>" + sAuthor + "</font></p>";
    }
    String sAlbum = UtilString.getLimitedString(getTrack().getAlbum().getName(), maxSize);
    if (!sAlbum.equals(UNKNOWN_ALBUM)) {
      sOut += "<p>" + sAlbum + "</font></p>";
    }
    sOut += "</HTML>";
    return sOut;
  }

  /**
   * 
   * @return Text to be displayed in the tootip and baloon under windows.
   * 
   */
  public final String getBasicFormatText() {
    String sOut = "";
    sOut = "";
    String sAuthor = getTrack().getAuthor().getName();
    if (!sAuthor.equals(UNKNOWN_AUTHOR)) {
      sOut += sAuthor + " / ";
    }
    String sAlbum = getTrack().getAlbum().getName();
    if (!sAlbum.equals(UNKNOWN_ALBUM)) {
      sOut += sAlbum + " / ";
    }
    sOut += getTrack().getName();
    return sOut;
  }

}
