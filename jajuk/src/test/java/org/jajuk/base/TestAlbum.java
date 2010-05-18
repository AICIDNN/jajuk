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
 *  $Revision: 3132 $
 */
package org.jajuk.base;

import java.util.Date;

import org.jajuk.JUnitHelpers;
import org.jajuk.JajukTestCase;
import org.jajuk.services.startup.StartupCollectionService;
import org.jajuk.util.Const;
import org.jajuk.util.Messages;

/**
 * 
 */
public class TestAlbum extends JajukTestCase {

  /**
   * Test method for {@link org.jajuk.base.Album#getDesc()}.
   */
  public final void testGetDesc() {
    Album album = new Album("1", "name", 123);
    assertNotNull(album.getDesc());
    assertFalse(album.getDesc().isEmpty());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getAny()}.
   */
  public final void testGetAny() {
    // need item managers to do this step
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);
    /*
     * album.getTracksCache().add(getTrack(album));
     * 
     * album.setProperty(Const.XML_TRACK_DISCOVERY_DATE, System.) String str = album.getAny();
     * assertFalse(str.isEmpty());
     */

    // add a genre and year and check again
    album.getTracksCache().add(getTrack(album));
    assertFalse(album.getAny().isEmpty());
    /*
     * assertFalse("getAny() should return differently as soon as we have genre and year" ,
     * str.equals(album.getAny()));
     */
  }

  public final void testGetAnyAlbumArtist() {
    // need item managers to do this step
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);
    /*
     * album.getTracksCache().add(getTrack(album));
     * 
     * album.setProperty(Const.XML_TRACK_DISCOVERY_DATE, System.) String str = album.getAny();
     * assertFalse(str.isEmpty());
     */

    // add a genre and year and check again
    Track track = getTrack(album);
    track.setAlbumArtist(new AlbumArtist("4", "artist"));
    album.getTracksCache().add(track);
    assertFalse(album.getAny().isEmpty());
    /*
     * assertFalse("getAny() should return differently as soon as we have genre and year" ,
     * str.equals(album.getAny()));
     */
  }

  private Track getTrack(Album album) {
    return new Track("1", "trackname", album, getGenre(), getArtist(), 123, getYear(), 1, new Type(
        "3", "typename", "ext", null, null), 1);
  }

  private Artist getArtist() {
    return new Artist("1", "artistname");
  }

  private Genre getGenre() {
    return new Genre("1", "genrename");
  }

  private Year getYear() {
    return new Year("1", "yearname");
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getLabel()}.
   */
  public final void testGetLabel() {
    Album album = new Album("1", "name", 123);
    assertFalse(album.getLabel().isEmpty());
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Album#getHumanValue(java.lang.String)}.
   */
  public final void testGetHumanValue() {
    // some of the lines below can require the ItemManagers to be registered
    // correctly
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);
    assertFalse(album.getHumanValue(Const.XML_ALBUM).isEmpty());

    // things are empty before adding a track...
    assertTrue(album.getHumanValue(Const.XML_ARTIST).isEmpty());
    assertTrue(album.getHumanValue(Const.XML_GENRE).isEmpty());
    assertTrue(album.getHumanValue(Const.XML_YEAR).isEmpty());

    // add a genre and year
    Track track = getTrack(album);
    track.setProperty(Const.XML_TRACK_DISCOVERY_DATE, new Date());
    album.getTracksCache().add(track);
    assertFalse(album.getHumanValue(Const.XML_GENRE).isEmpty());

    assertFalse(album.getHumanValue(Const.XML_ARTIST).isEmpty());
    assertFalse(album.getHumanValue(Const.XML_YEAR).isEmpty());
    assertFalse(album.getHumanValue(Const.XML_TRACK_RATE).isEmpty());
    assertFalse(album.getHumanValue(Const.XML_TRACK_LENGTH).isEmpty());
    assertFalse(album.getHumanValue(Const.XML_TRACKS).isEmpty());
    assertFalse(album.getHumanValue(Const.XML_TRACK_DISCOVERY_DATE).isEmpty());
    assertEquals("Value: " + album.getHumanValue(Const.XML_TRACK_HITS), "0", album
        .getHumanValue(Const.XML_TRACK_HITS));
    assertFalse(album.getHumanValue(Const.XML_ANY).isEmpty());
    assertTrue(album.getHumanValue(Const.XML_ALBUM_ARTIST).isEmpty());
    assertTrue(album.getHumanValue(Const.XML_ALBUM_COVER).isEmpty());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getIconRepresentation()}.
   */
  public final void testGetIconRepresentation() {
    Album album = new Album("1", "name", 123);
    assertNotNull(album.getIconRepresentation());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getRate()}.
   */
  public final void testGetRate() {
    Album album = new Album("1", "name", 123);
    assertEquals(0, album.getRate());

    // add track to have some useful rate
    Track track = getTrack(album);
    track.setRate(3);
    album.getTracksCache().add(track);
    assertEquals(3, album.getRate());
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Album#Album(java.lang.String, java.lang.String, java.lang.String, long)}
   * .
   */
  public final void testAlbum() {
    new Album("1", "name", 123);
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getDiscID()}.
   */
  public final void testGetDiscID() {
    Album album = new Album("1", "name", 123);
    assertEquals(123, album.getDiscID());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getName2()}.
   */
  public final void testGetName2() {
    Album album = new Album("1", "name", 123);
    assertEquals("name", album.getName2());

    album = new Album("1", Const.UNKNOWN_ALBUM, 123);
    assertEquals(Messages.getString(Const.UNKNOWN_ALBUM), album.getName2());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#toString()}.
   */
  public final void testToString() {
    Album album = new Album("1", "name", 123);
    JUnitHelpers.ToStringTest(album);
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Album#compareTo(org.jajuk.base.Album)}.
   */
  public final void testCompareTo() {
    Album album = new Album("1", "name", 123);
    Album equal = new Album("1", "name", 123);
    Album nonequal = new Album("2", "name", 123);

    JUnitHelpers.CompareToTest(album, equal, nonequal);
  }

  /**
   * Test method for {@link org.jajuk.base.Album#isUnknown()}.
   */
  public final void testIsUnknown() {
    Album album = new Album("1", "name", 123);
    assertFalse(album.isUnknown());

    album = new Album("1", Const.UNKNOWN_ALBUM, 123);
    assertTrue(album.isUnknown());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#findCoverFile()}.
   * 
   * @throws Exception
   */
  public final void testGetCoverFile() throws Exception {
    Album album = new Album("1", "name", 123);

    // no file at first
    assertNull(album.findCoverFile());

    // none
    album.setProperty(Const.XML_ALBUM_COVER, Const.COVER_NONE);
    assertNull(album.findCoverFile());

    // set a cover file
    album.setProperty(Const.XML_ALBUM_COVER, System.getProperty("java.io.tmpdir")
        + java.io.File.separator + "cover.tst");
    assertNotNull(album.findCoverFile());

    // try with a track and no cover file set
    album.removeProperty(Const.XML_ALBUM_COVER);
    Track track = getTrack(album);
    track.addFile(getFile(7, track));
    track.addFile(getFile(8, track));
    album.getTracksCache().add(track);
    assertNull(album.findCoverFile());

    // TODO: some code is still not covered here, need to find out how to do
    // that...
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getThumbnail(int)}.
   * 
   * @throws Exception
   */
  public final void testGetThumbnail() throws Exception {
    JUnitHelpers.createSessionDirectory();

    Album album = new Album("1", "name", 123);
    assertNotNull(album.getThumbnail(100));

    // TODO: actual code is not well covered right now, need to add some more
    // test-code here...
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getGenre()}.
   */
  public final void testGetGenre() {
    Album album = new Album("1", "name", 123);

    // now genre without track
    assertNull(album.getGenre());

    // genre with at least one track
    album.getTracksCache().add(getTrack(album));
    assertNotNull(album.getGenre());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getArtist()}.
   */
  public final void testGetArtist() {
    Album album = new Album("1", "name", 123);

    // no artist without track
    assertNull(album.getArtist());

    album.getTracksCache().add(getTrack(album));
    assertNotNull(album.getGenre());

    // add a second track with different artist
    Track track = new Track("2", "trackname2", album, getGenre(), new Artist("2", "artistname2"),
        123, getYear(), 1, new Type("4", "typename2", "ext", null, null), 1);
    album.getTracksCache().add(track);

    // now null again as multiple different artists are in the list
    assertNull(album.getArtist());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getYear()}.
   */
  public final void testGetYear() {
    Album album = new Album("1", "name", 123);

    // no artist without track
    assertNull(album.getYear());

    album.getTracksCache().add(getTrack(album));
    assertNotNull(album.getYear());

    // add a second track with different year
    Track track = new Track("2", "trackname2", album, getGenre(), getArtist(), 123, new Year("2",
        "yearname2"), 1, new Type("4", "typename2", "ext", null, null), 1);
    album.getTracksCache().add(track);

    // now null again as multiple different artists are in the list
    assertNull(album.getYear());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getDuration()}.
   */
  public final void testGetDuration() {
    Album album = new Album("1", "name", 123);

    // zero without any track
    assertEquals(0, album.getDuration());

    // add a track with duration 123
    album.getTracksCache().add(getTrack(album));
    assertEquals(123, album.getDuration());

    // another one, this is summed up
    album.getTracksCache().add(getTrack(album));
    assertEquals(246, album.getDuration());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getNbOfTracks()}.
   */
  public final void testGetNbOfTracks() {
    Album album = new Album("1", "name", 123);

    // zero without any track
    assertEquals(0, album.getNbOfTracks());

    // add a track with duration 123
    album.getTracksCache().add(getTrack(album));
    assertEquals(1, album.getNbOfTracks());

    // another one, this is summed up
    album.getTracksCache().add(getTrack(album));
    assertEquals(2, album.getNbOfTracks());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getHits()}.
   */
  public final void testGetHits() {
    Album album = new Album("1", "name", 123);

    // zero without any track
    assertEquals(0, album.getHits());

    // still zero as tracks have zero hits usually
    album.getTracksCache().add(getTrack(album));
    assertEquals(0, album.getHits());

    // another one, this has some hits
    Track track = getTrack(album);
    track.setHits(3);
    album.getTracksCache().add(track);
    assertEquals(3, album.getHits());

    // and another one, now it sums up
    track = getTrack(album);
    track.setHits(5);
    album.getTracksCache().add(track);
    assertEquals(8, album.getHits());
  }

  private File getFile(int i, Track track) throws Exception {
    Device device = JUnitHelpers.getDevice();
    if (!device.isMounted()) {
      device.mount(true);
    }
    Directory dir = new Directory(Integer.valueOf(i).toString(), "", null, device);

    return new org.jajuk.base.File(Integer.valueOf(i).toString(), "test.tst", dir, track, 120, 70);
  }

  /**
   * Test method for {@link org.jajuk.base.Album#containsReadyFiles()}.
   * 
   * @throws Exception
   */
  public final void testContainsReadyFiles() throws Exception {
    Album album = new Album("1", "name", 123);

    // no files
    assertFalse(album.containsReadyFiles());

    // add a track/file
    Track track = getTrack(album);
    track.addFile(getFile(6, track));
    album.getTracksCache().add(track);
    assertTrue(album.containsReadyFiles());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getDiscoveryDate()}.
   */
  public final void testGetDiscoveryDate() {
    Album album = new Album("1", "name", 123);
    assertNull(album.getDiscoveryDate());

    Track track = getTrack(album);
    track.setDiscoveryDate(new Date());
    album.getTracksCache().add(track);
    assertNotNull(album.getDiscoveryDate());
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Album#matches(java.lang.String, java.lang.String)}.
   */
  public final void testMatches() {
    Album album = new Album("1", "myname", 123);

    // true if either of both is null !?
    assertTrue(album.matches(null, null));
    assertTrue(album.matches(Const.XML_ALBUM, null));
    assertTrue(album.matches(null, ".*art.*"));

    // false when not "ALBUM" or "GENRE"
    assertFalse(album.matches(Const.XML_ALBUM_ARTIST, ".*art.*"));

    // useful match?
    assertTrue(album.matches(Const.XML_ALBUM, "my"));
    assertTrue(album.matches(Const.XML_ALBUM, "name"));
    assertFalse(album.matches(Const.XML_ALBUM, "notexist"));

    // false without Genre
    assertFalse(album.matches(Const.XML_GENRE, "."));

    Track track = getTrack(album);
    album.getTracksCache().add(track);

    // now the genre should be found as well
    assertTrue(album.matches(Const.XML_GENRE, "genrename"));
  }

  /**
   * Test method for {@link org.jajuk.base.Album#resetTracks()}.
   */
  public final void testResetTracks() {
    Album album = new Album("1", "name", 123);

    // nothing happens without tracks
    album.resetTracks();

    // add tracks
    Track track = getTrack(album);
    album.getTracksCache().add(track);

    assertEquals(1, album.getTracksCache().size());

    // reset purges the tracks
    album.resetTracks();
    assertEquals(0, album.getTracksCache().size());
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getTracksCache()}.
   */
  public final void testGetTracksCache() {
    // tested in the other tests
  }

  /**
   * Test method for {@link org.jajuk.base.Album#getAnyTrack()}.
   */
  public final void testGetAnyTrack() {
    Album album = new Album("1", "name", 123);

    // nothing to return without tracks
    assertNull(album.getAnyTrack());

    // add tracks
    Track track = getTrack(album);
    album.getTracksCache().add(track);

    // now we get back the first track
    assertNotNull(album.getAnyTrack());
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Album#setAvailableThumb(int, boolean)}.
   */
  public final void testSetAndIsAvailableThumb() {
    Album album = new Album("1", "name", 123);
    assertFalse(album.isThumbAvailable(50));
    album.setAvailableThumb(50, true);
    assertTrue(album.isThumbAvailable(50));

    // test once more with a new album to create the thumbs-array there as well
    album = new Album("1", "name", 123);
    album.setAvailableThumb(100, false);
  }

  public final void testGetArtistOrAlbumArtist_Unknown() {
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);

    // unknown artist is returned without a track
    assertEquals(Const.UNKNOWN_ARTIST, album.getArtistOrALbumArtist());
  }

  public final void testGetArtistOrAlbumArtist_AlbumArtist() {
    // need item managers to do this step
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);

    // add a genre and year and check again
    Track track = getTrack(album);
    track.setAlbumArtist(new AlbumArtist("4", "albumartist"));

    album.getTracksCache().add(track);
    album.getTracksCache().add(
        new Track("1", "trackname", album, getGenre(), new Artist("2", "artistname2"), 123,
            getYear(), 1, new Type("3", "typename", "ext", null, null), 1));

    // here we should get the album artist from the Track because we have two tracks with different
    // artists
    assertEquals("albumartist", album.getArtistOrALbumArtist());
  }

  public final void testGetArtistOrAlbumArtist_TrackArtist() {
    // need item managers to do this step
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);

    // add a genre and year and check again
    Track track = getTrack(album);
    album.getTracksCache().add(track);

    // here we should get the artist from the Track as no album artist is set
    assertEquals("artistname", album.getArtistOrALbumArtist());
  }

  public final void testGetArtistOrAlbumArtist_TrackArtist2() {
    // need item managers to do this step
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);

    // add a genre and year and check again
    Track track = getTrack(album);
    track.setAlbumArtist(new AlbumArtist("4", "albumartist"));
    album.getTracksCache().add(track);

    // here we should get the artist from the Track as all tracks have the same artist
    assertEquals("artistname", album.getArtistOrALbumArtist());
  }

  public final void testGetArtistOrAlbumArtist_AlbumArtistUnknown() {
    // need item managers to do this step
    StartupCollectionService.registerItemManagers();

    Album album = new Album("1", "name", 123);

    // add a genre and year and check again
    Track track = getTrack(album);
    track.setAlbumArtist(new AlbumArtist("4", Const.UNKNOWN_ARTIST));

    album.getTracksCache().add(track);
    album.getTracksCache().add(
        new Track("1", "trackname", album, getGenre(), new Artist("2", "artistname2"), 123,
            getYear(), 1, new Type("3", "typename", "ext", null, null), 1));

    // here we should get the artist from the first Track as the album artist is "unknown"
    assertEquals("artistname", album.getArtistOrALbumArtist());
  }
}
