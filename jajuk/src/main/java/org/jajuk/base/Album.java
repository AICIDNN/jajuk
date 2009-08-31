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

import java.awt.Container;
import java.awt.MediaTracker;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.commons.lang.StringUtils;
import org.jajuk.ui.thumbnails.ThumbnailManager;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilFeatures;
import org.jajuk.util.UtilString;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.log.Log;

/**
 * An Album *
 * <p>
 * Logical item
 */
public class Album extends LogicalItem implements Comparable<Album> {

  /**
   * For perfs, we cache the associated tracks. This cache is filled by the
   * TrackManager using the getTracksCache() method
   */
  private final List<Track> cache = new ArrayList<Track>(15);

  /**
   * This array stores thumbnail presence for all the available size
   * (performance) By default all booleans are false
   */
  private boolean[] availableTumbs;

  /**
   * Album constructor
   * 
   * @param id
   * @param sName
   */
  public Album(String sId, String sName, String sAlbumArtist, long discID) {
    super(sId, sName);
    setProperty(Const.XML_ALBUM_ARTIST, sAlbumArtist);
    setProperty(Const.XML_ALBUM_DISC_ID, discID);
  }

  /**
   * @return the discID
   */
  public long getDiscID() {
    return getLongValue(Const.XML_ALBUM_DISC_ID);
  }

  /**
   * @return the albumArtiest
   */
  public String getAlbumArtist() {
    return getStringValue(Const.XML_ALBUM_ARTIST);
  }

  /**
   * @return the albumArtist or author if album artist not available
   *         <p>
   *         If this is various, the album artist is tried to be defined by the
   *         track artists of this album
   *         </p>
   */
  public String getAlbumArtistOrArtist() {
    // If the album artist tag is provided, perfect, let's use it !
    String albumArtist = getAlbumArtist();
    if (StringUtils.isNotBlank(albumArtist) && !(Const.UNKNOWN_AUTHOR.equals(albumArtist))) {
      return albumArtist;
    }
    // various artist? check if all authors are the same
    Author author = getAuthor();
    if (author == null) {
      // Several different author, return translated "various"
      return Messages.getString(Const.VARIOUS_ARTIST);
    } else {
      // single artist, return it
      return author.getName2();
    }
  }

  /**
   * Return album name, dealing with unknown for any language
   * 
   * @return album name
   */
  public String getName2() {
    String sOut = getName();
    if (sOut.equals(UNKNOWN_ALBUM)) {
      sOut = Messages.getString(UNKNOWN_ALBUM);
    }
    return sOut;
  }

  /**
   * Return album-artist name, dealing with unknown for any language
   * 
   * @return album-artist name
   */
  public String getAlbumArtist2() {
    String sOut = getAlbumArtist();
    if (sOut.equals(UNKNOWN_AUTHOR)) {
      sOut = Messages.getString(UNKNOWN_AUTHOR);
    }
    return sOut;
  }

  /**
   * toString method
   */
  @Override
  public String toString() {
    return "Album[ID=" + getID() + " Name={{" + getName() + "}}" + " Album Artist={{"
        + getAlbumArtist() + "}}" + " disk ID={{" + getDiscID() + "}}]";
  }

  /**
   * Alphabetical comparator on the name
   * <p>
   * Used to display ordered lists
   * 
   * @param other
   *          item to be compared
   * @return comparison result
   */
  public int compareTo(Album otherAlbum) {
    if (otherAlbum == null) {
      return -1;
    }

    // compare using name and id to differentiate unknown items
    StringBuilder current = new StringBuilder(getName2());
    current.append(getID());
    StringBuilder other = new StringBuilder(otherAlbum.getName2());
    other.append(otherAlbum.getID());
    return current.toString().compareToIgnoreCase(other.toString());
  }

  /**
   * @return whether the album is Unknown or not
   */
  public boolean isUnknown() {
    return this.getName().equals(UNKNOWN_ALBUM);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getIdentifier()
   */
  @Override
  public final String getLabel() {
    return XML_ALBUM;
  }

  /**
   * Get item description
   */
  @Override
  public String getDesc() {
    return Messages.getString("Item_Album") + " : " + getName2();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getHumanValue(java.lang.String)
   */
  @Override
  public String getHumanValue(String sKey) {
    // We compute here all pseudo keys (non album real attributes) that can be
    // required on an album
    if (Const.XML_AUTHOR.equals(sKey)) {
      return handleAuthor();
    } else if (Const.XML_ALBUM.equals(sKey)) {
      return getName2();
    } else if (Const.XML_STYLE.equals(sKey)) {
      return handleStyle();
    } else if (Const.XML_YEAR.equals(sKey)) {
      return handleYear();
    } else if (Const.XML_TRACK_RATE.equals(sKey)) {
      return Long.toString(getRate());
    } else if (Const.XML_TRACK_LENGTH.equals(sKey)) {
      return Long.toString(getDuration());
    } else if (Const.XML_TRACKS.equals(sKey)) {
      return Integer.toString(getNbOfTracks());
    } else if (Const.XML_TRACK_DISCOVERY_DATE.equals(sKey)) {
      return UtilString.getLocaleDateFormatter().format(getDiscoveryDate());
    } else if (Const.XML_TRACK_HITS.equals(sKey)) {
      return Long.toString(getHits());
    } else if (Const.XML_ANY.equals(sKey)) {
      return getAny();
    } else if (Const.XML_ALBUM_ARTIST.equals(sKey)) {
      return getAlbumArtist2();
    }
    // default
    return super.getHumanValue(sKey);
  }

  /**
   * @return
   */
  private String handleAuthor() {
    Author author = getAuthor();
    if (author != null) {
      return author.getName2();
    } else {
      // More than one author, display void string
      return "";
    }
  }

  /**
   * @return
   */
  private String handleStyle() {
    Style style = getStyle();
    if (style != null) {
      return style.getName2();
    } else {
      // More than one style, display void string
      return "";
    }
  }

  /**
   * @return
   */
  private String handleYear() {
    Year year = getYear();
    if (year != null) {
      return Long.toString(year.getValue());
    } else {
      return "";
    }
  }

  /**
   * @return a human representation of all concatenated properties
   */
  @Override
  public String getAny() {
    // rebuild any
    StringBuilder sb = new StringBuilder(100);
    sb.append(super.getAny()); // add all album-based properties
    // now add others properties
    Author author = getAuthor();
    if (author != null) {
      sb.append(author.getName2());
    }
    Style style = getStyle();
    if (style != null) {
      sb.append(style.getName2());
    }
    Year year = getYear();
    if (year != null) {
      sb.append(getHumanValue(Const.XML_YEAR));
    }
    sb.append(getHumanValue(Const.XML_TRACK_RATE));
    sb.append(getHumanValue(Const.XML_TRACK_LENGTH));
    sb.append(getHumanValue(Const.XML_TRACKS));
    sb.append(getHumanValue(Const.XML_TRACK_DISCOVERY_DATE));
    sb.append(getHumanValue(Const.XML_TRACK_HITS));
    sb.append(getHumanValue(Const.XML_ALBUM_ARTIST));
    return sb.toString();
  }

  /**
   * @return associated best cover file available or null if none. The returned
   *         file can not be readable, so use a try/catch around a future access
   */
  public File getCoverFile() {
    String cachedCoverPath = getStringValue(XML_ALBUM_COVER);
    // If none cover is found, we save this information to save discovery time
    // afterwards (performance factor x2 or x3 in catalog view)
    if ("none".equals(cachedCoverPath)) {
      return null;
    } else if (!StringUtils.isBlank(cachedCoverPath)) {
      return new File(cachedCoverPath);
    }
    File fDir = null; // analyzed directory
    // search for local covers in all directories mapping the current track
    // to reach other devices covers and display them together
    List<Track> lTracks = TrackManager.getInstance().getAssociatedTracks(this, false);
    if (lTracks.size() == 0) {
      setProperty(XML_ALBUM_COVER, "none");
      return null;
    }
    // List if directories we have to look in
    Set<Directory> dirs = new HashSet<Directory>(2);
    for (Track track : lTracks) {
      for (org.jajuk.base.File file : track.getFiles()) {
        if (file.isReady()) {
          // note that hashset ensures directory unicity
          dirs.add(file.getDirectory());
        }
      }
    }
    // If none available dir, we can't search for cover for now (may be better
    // next time when at least one device will be mounted)
    if (dirs.size() == 0) {
      return null;
    }

    // look for absolute cover in collection
    for (Directory dir : dirs) {
      String sAbsolut = dir.getStringValue(Const.XML_DIRECTORY_DEFAULT_COVER);
      if (!StringUtils.isBlank(sAbsolut.trim())) {
        File fAbsoluteDefault = new File(dir.getAbsolutePath() + '/' + sAbsolut);
        if (fAbsoluteDefault.exists()) {
          // Test the image is not corrupted
          try {
            MediaTracker mediaTracker = new MediaTracker(new Container());
            ImageIcon ii = new ImageIcon(fAbsoluteDefault.getAbsolutePath());
            mediaTracker.addImage(ii.getImage(), 0);
            mediaTracker.waitForID(0); // wait for image
            if (!mediaTracker.isErrorAny()) {
              setProperty(XML_ALBUM_COVER, fAbsoluteDefault.getAbsolutePath());
              return fAbsoluteDefault;
            }
          } catch (Exception e) {
            Log.error(e);
          }
        }
      }
    }

    // look for standard cover in collection
    for (Directory dir : dirs) {
      fDir = dir.getFio(); // store this dir
      java.io.File[] files = fDir.listFiles();// null if none file
      // found
      for (int i = 0; files != null && i < files.length; i++) {
        // test file exists, do not use the File.canRead() method: it can be
        // very costly when using a NAS under Windows
        if (files[i].exists() && files[i].length() < MAX_COVER_SIZE * 1024) {
          // check size to avoid out of memory errors
          String sExt = UtilSystem.getExtension(files[i]);
          if ((sExt.equalsIgnoreCase("jpg") || sExt.equalsIgnoreCase("png") || sExt
              .equalsIgnoreCase("gif"))
              && (UtilFeatures.isStandardCover(files[i]))) {
            // Test the image is not corrupted
            try {
              MediaTracker mediaTracker = new MediaTracker(new Container());
              ImageIcon ii = new ImageIcon(files[i].getAbsolutePath());
              mediaTracker.addImage(ii.getImage(), 0);
              mediaTracker.waitForID(0); // wait for image
              if (!mediaTracker.isErrorAny()) {
                setProperty(XML_ALBUM_COVER, files[i].getAbsolutePath());
                return files[i];
              }
            } catch (Exception e) {
              Log.error(e);
            }
          }
        }
      }
    }
    // none ? OK, return first cover file we find
    for (Directory dir : dirs) {
      fDir = dir.getFio(); // store this dir
      java.io.File[] files = fDir.listFiles();// null if none file
      // found
      for (int i = 0; files != null && i < files.length; i++) {
        // test file exists, do not use the File.canRead() method: it can be
        // very costly when using a NAS under Windows
        if (files[i].exists() && files[i].length() < MAX_COVER_SIZE * 1024) {
          // check size to avoid out of memory errors
          String sExt = UtilSystem.getExtension(files[i]);
          if (sExt.equalsIgnoreCase("jpg") || sExt.equalsIgnoreCase("png")
              || sExt.equalsIgnoreCase("gif")) {
            // Test the image is not corrupted
            try {
              MediaTracker mediaTracker = new MediaTracker(new Container());
              ImageIcon ii = new ImageIcon(files[i].getAbsolutePath());
              mediaTracker.addImage(ii.getImage(), 0);
              mediaTracker.waitForID(0); // wait for image
              if (!mediaTracker.isErrorAny()) {
                setProperty(XML_ALBUM_COVER, files[i].getAbsolutePath());
                return files[i];
              }
            } catch (Exception e) {
              Log.error(e);
            }
          }
        }
      }
    }
    setProperty(XML_ALBUM_COVER, "none");
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.base.Item#getIconRepresentation()
   */
  @Override
  public ImageIcon getIconRepresentation() {
    return IconLoader.getIcon(JajukIcons.ALBUM);
  }

  /**
   * @return album rating
   */
  @Override
  public long getRate() {
    long rate = 0;
    for (Track track : cache) {
      rate += track.getRate();
    }
    return rate;
  }

  /**
   * 
   * @param size
   *          size using format width x height
   * @return album thumb for given size
   */
  public ImageIcon getThumbnail(int size) {
    File fCover = ThumbnailManager.getThumbBySize(this, size);
    // Check if thumb already exists
    if (!fCover.exists() || fCover.length() == 0) {
      return IconLoader.getNoCoverIcon(size);
    }
    BufferedImage img = null;
    try {
      img = ImageIO.read(new File(fCover.getAbsolutePath()));
    } catch (IOException e) {
      Log.error(e);
    }
    // can be null now if an error occurred, we reported a error to the log
    // already...
    if (img == null) {
      return null;
    }

    ImageIcon icon = new ImageIcon(img);
    // Free thumb memory (DO IT AFTER FULL ImageIcon loading)
    img.flush();

    // TODO: is this really useful? It just frees a local variable that is
    // going out of scope quickly anyway?
    // accelerate GC cleanup
    img = null;

    return icon;
  }

  /**
   * 
   * @return style for the album. Return null if the album contains tracks with
   *         different styles
   */
  public Style getStyle() {
    Set<Style> styles = new HashSet<Style>(1);
    for (Track track : cache) {
      styles.add(track.getStyle());
    }
    // If different styles, the album style is null
    if (styles.size() == 1) {
      return styles.iterator().next();
    } else {
      return null;
    }
  }

  /**
   * 
   * @return author for the album. <br>
   *         Return null if the album contains tracks with different authors
   */
  public Author getAuthor() {
    if (cache.size() == 0) {
      return null;
    }
    Author first = cache.get(0).getAuthor();
    for (Track track : cache) {
      if (!track.getAuthor().equals(first)) {
        return null;
      }
    }
    return first;
  }

  /**
   * 
   * @return year for the album. Return null if the album contains tracks with
   *         different years
   */
  public Year getYear() {
    Set<Year> years = new HashSet<Year>(1);
    for (Track track : cache) {
      years.add(track.getYear());
    }
    // If different Authors, the album Author is null
    if (years.size() == 1) {
      return years.iterator().next();
    } else {
      return null;
    }
  }

  /**
   * Return full album length in secs
   */
  public long getDuration() {
    long length = 0;
    for (Track track : cache) {
      length += track.getDuration();
    }
    return length;
  }

  /**
   * @return album nb of tracks
   */
  public int getNbOfTracks() {
    return cache.size();
  }

  /**
   * @return album total nb of hits
   */
  public long getHits() {
    int hits = 0;
    for (Track track : cache) {
      hits += track.getHits();
    }
    return hits;
  }

  /**
   * @return whether the album contains a least one available track
   */
  public boolean containsReadyFiles() {
    for (Track track : cache) {
      if (track.getReadyFiles().size() > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return First found track discovery date
   */
  public Date getDiscoveryDate() {
    if (cache.size() > 0) {
      return cache.get(0).getDiscoveryDate();
    } else {
      return null;
    }
  }

  /**
   * Returns true, if the pattern matches the specified property.
   * 
   * Currently only Const.XML_ALBUM and Const.XML_STYLE are supported
   * properties. The pattern is used for a case-insensitive sub-string match, 
   * no regular expression is used!
   * 
   * @param property The property to use for the match, currently either Cosnt.XML_ALBUM 
   *                  or Const.XML_STYLE
   * @param pattern The string to search for as case-insensitive sub-string
   * 
   * @return true if either parameter is null or if the pattern matches, false otherwise.
   */
  public boolean matches(String property, String pattern) {
    if (StringUtils.isBlank(property) || StringUtils.isBlank(pattern)) {
      return true;
    }
    
    String sValue = null;
    if (Const.XML_ALBUM.equals(property)) {
      sValue = getName2();
    } else if (Const.XML_STYLE.equals(property)) {
      Style style = getStyle();
      if (style == null) {
        return false;
      }
      sValue = style.getName2();
    }
    if (sValue == null) {
      return false;
    }

    // do not use regexp matches(<string>) because the string may contain
    // characters to be escaped
    return (sValue.toLowerCase(Locale.getDefault()).indexOf(
        pattern.toLowerCase(Locale.getDefault())) != -1);
  }

  /**
   * Reset tracks cache
   */
  protected void resetTracks() {
    cache.clear();
  }

  /**
   * 
   * @return ordered tracks cache for this album (perf)
   */
  public List<Track> getTracksCache() {
    return this.cache;
  }

  /**
   * 
   * @return a track from this album
   */
  public Track getAnyTrack() {
    if (cache.size() == 0) {
      return null;
    } else {
      return cache.get(0);
    }
  }

  /**
   * Set that the thumb for given size is available
   * 
   * @param size
   *          (thumb size like 50)
   * @param available
   */
  public void setAvailableThumb(int size, boolean available) {
    if (availableTumbs == null) {
      availableTumbs = new boolean[6];
    }
    availableTumbs[size / 50 - 1] = available;
  }

  /**
   * Return whether a thumb is available for given size
   * 
   * @param size
   *          (thumb size like 50)
   * @return whether a thumb is available for given size
   */
  public boolean isThumbAvailable(int size) {
    // Lazy loading of thumb availability (for all sizes)
    if (availableTumbs == null) {
      availableTumbs = new boolean[6];
      for (int i = 50; i <= 300; i += 50) {
        File fThumb = ThumbnailManager.getThumbBySize(this, i);
        setAvailableThumb(i, fThumb.exists() && fThumb.length() > 0);
      }
    }
    return availableTumbs[size / 50 - 1];
  }

}
