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

import java.awt.HeadlessException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jajuk.ConstTest;
import org.jajuk.JUnitHelpers;
import org.jajuk.JajukTestCase;
import org.jajuk.services.bookmark.Bookmarks;
import org.jajuk.services.players.QueueModel;
import org.jajuk.services.players.StackItem;
import org.jajuk.services.startup.StartupCollectionService;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * 
 */
public class TestPlaylist extends JajukTestCase {

  /**
   * Test method for {@link org.jajuk.base.Playlist#hashCode()}.
   */
  public final void testHashCode() {
    Playlist play = new Playlist("1", "name", null);
    Playlist equ = new Playlist("1", "name", null);

    JUnitHelpers.HashCodeTest(play, equ);
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getDesc()}.
   */
  public final void testGetDesc() {
    Playlist play = new Playlist("1", "name", null);
    assertFalse(StringUtils.isBlank(play.getDesc()));

  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#equals(java.lang.Object)}.
   */
  public final void testEqualsObject() {
    Playlist play = new Playlist(Playlist.Type.NORMAL, "1", "name", null);
    Playlist equ = new Playlist(Playlist.Type.NORMAL, "1", "name", null);

    // equals looks at id and type
    Playlist nonequ1 = new Playlist(Playlist.Type.NORMAL, "2", "name", null);
    Playlist nonequ2 = new Playlist(Playlist.Type.NORMAL, "2", "name2", null);
    Playlist nonequ3 = new Playlist(Playlist.Type.NORMAL, "2", "name3", JUnitHelpers.getDirectory());

    JUnitHelpers.EqualsTest(play, equ, nonequ1);
    JUnitHelpers.EqualsTest(play, equ, nonequ2);
    JUnitHelpers.EqualsTest(play, equ, nonequ3);
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getLabel()}.
   */
  public final void testGetLabel() {
    Playlist play = new Playlist("1", "name", null);
    assertTrue(StringUtils.isNotBlank(play.getLabel()));
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#getHumanValue(java.lang.String)}.
   * 
   * @throws Exception
   */
  public final void testGetHumanValue() throws Exception {
    Playlist play = createPlaylist();

    DirectoryManager.getInstance().registerDirectory(play.getDirectory().getDevice());

    // this is what we read here...
    play.setProperty(Const.XML_DIRECTORY, play.getDirectory().getDevice().getID());

    String str1 = ConstTest.PATH_DEVICE;
    String str2 = play.getHumanValue(Const.XML_DIRECTORY);
    str1 = StringUtils.stripEnd(str1, java.io.File.separator);
    str2 = StringUtils.stripEnd(str2, java.io.File.separator);
    assertEquals(str1, str2);

    assertEquals("", play.getHumanValue("notexist"));

    // define property
    StartupCollectionService.registerItemManagers();
    ItemManager.getItemManager(Playlist.class).registerProperty(
        new PropertyMetaInformation("testkey", true, true, true, true, true, String.class,
            "defaultval"));

    play.setProperty("testkey", "testval");

    assertEquals("testval", play.getHumanValue("testkey"));

    play.removeProperty("testkey");

    assertEquals("defaultval", play.getHumanValue("testkey"));
  }

  /**
   * @return
   * @throws Exception
   */
  private Playlist createPlaylist() throws Exception {
    Device device = JUnitHelpers.getDevice();
    Directory topdir = JUnitHelpers.getDirectory();
    try {
      topdir.getDevice().mount(true);
    } catch (Exception e) {
      Log.error(e);
    }
    Directory dir = JUnitHelpers.getDirectory("testdir", topdir, device);
    // cleanup
    dir.getFio().delete();
    dir.getFio().mkdirs();

    java.io.File fioPlaylist = new java.io.File(dir.getAbsolutePath() + "/playlist.m3u");
    Playlist play = PlaylistManager.getInstance().registerPlaylistFile(fioPlaylist, dir);

    List<org.jajuk.base.File> list = new ArrayList<org.jajuk.base.File>();
    list.add(JUnitHelpers.getFile("file1", false));
    list.add(JUnitHelpers.getFile("file1", false));

    play.setFiles(list);
    try {
      play.commit();
    } catch (JajukException e) {
      Log.error(e);
    }
    return play;
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getIconRepresentation()}.
   */
  public final void testGetIconRepresentation() {
    Playlist play = new Playlist("1", "name", null);
    assertNotNull(play.getIconRepresentation());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getRate()}.
   * 
   * @throws Exception
   */
  public final void testGetRate() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    assertEquals(0, play.getRate());

    File file = JUnitHelpers.getFile("file1", false);
    file.getTrack().setRate(2);
    play.addFile(file);

    // we use 2 above
    assertEquals(2, play.getRate());

    // multiple files round the rate
    file = JUnitHelpers.getFile("file2", false);
    file.getTrack().setRate(4);
    play.addFile(file);
    assertEquals(3, play.getRate());

    play.addFile(JUnitHelpers.getFile("file3", false));
    play.getFiles().get(2).getTrack().setRate(3);

    assertEquals(3, play.getRate());
  }

  public final void testGetRateNull() throws Exception {
    Playlist play = createPlaylist();
    play.setFiles(null);

    assertEquals(0, play.getRate());
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#Playlist(org.jajuk.base.Playlist.Type, java.lang.String, java.lang.String, org.jajuk.base.Directory)}
   * .
   */
  public final void testPlaylistTypeStringStringDirectory() {
    new SmartPlaylist(Playlist.Type.BESTOF, "1", "name", null);
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#Playlist(java.lang.String, java.lang.String, org.jajuk.base.Directory)}
   * .
   */
  public final void testPlaylistStringStringDirectory() {
    new Playlist("1", "name", null);
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#addFile(org.jajuk.base.File)}.
   * 
   * @throws Exception
   */
  public final void testAddFileFile() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    play.addFile(JUnitHelpers.getFile("file1", false));
    assertEquals(1, play.getFiles().size());
  }

  public final void testAddFileQueue() throws Exception {
    Playlist play = getPlaylistQueue();

    File file = JUnitHelpers.getFile("file1", false);
    file.getDirectory().getDevice().mount(true);
    play.addFile(file);

    // wait a bit to let the "push" be done in a separate thread
    JUnitHelpers.waitForThreadToFinish("Queue Push Thread");

    assertEquals(1, QueueModel.getQueueSize());

    file = JUnitHelpers.getFile("file1", false);
    play.addFile(1, file);

    // wait a bit to let the "push" be done in a separate thread
    JUnitHelpers.waitForThreadToFinish("Queue Push Thread");

    assertEquals(2, QueueModel.getQueueSize());
    assertEquals(2, play.getFiles().size());

    // test with repeat as well to see if we get repeat set for the new track as
    // well
    QueueModel.getItem(0).setRepeat(true);

    file = JUnitHelpers.getFile("file1", false);
    play.addFile(1, file);

    // wait a bit to let the "push" be done in a separate thread
    JUnitHelpers.waitForThreadToFinish("Queue Push Thread");

    assertEquals(3, QueueModel.getQueueSize());
    assertEquals(3, play.getFiles().size());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getType()}.
   */
  public final void testGetType() {
    Playlist play = getPlaylistBookmark();
    assertEquals(Playlist.Type.BOOKMARK, play.getType());
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#addFile(int, org.jajuk.base.File)}.
   * 
   * @throws Exception
   */
  public final void testAddFileIntFile() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    play.addFile(JUnitHelpers.getFile("test.tst", false));
    assertEquals(1, play.getFiles().size());

    File file = JUnitHelpers.getFile("othername", false);
    file.setName("othername");
    play.addFile(1, file);

    // this should now be at pos 1
    assertEquals("test.tst", play.getFiles().get(0).getName());
    assertEquals("othername", play.getFiles().get(1).getName());

    file = JUnitHelpers.getFile("file3", false);
    file.setName("yetanother");
    play.addFile(1, file);

    assertEquals("test.tst", play.getFiles().get(0).getName());
    assertEquals("yetanother", play.getFiles().get(1).getName());
    assertEquals("othername", play.getFiles().get(2).getName());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#addFiles(java.util.List)}.
   * 
   * @throws Exception
   */
  public final void testAddFiles() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    List<File> files = new ArrayList<File>();

    // empty add does not do anything
    play.addFiles(files);

    assertEquals(0, play.getFiles().size());

    // add some files
    files.add(JUnitHelpers.getFile("file1", false));
    files.add(JUnitHelpers.getFile("file1", false));
    files.add(JUnitHelpers.getFile("file1", false));
    files.add(JUnitHelpers.getFile("file1", false));

    assertEquals(0, play.getFiles().size());
    play.addFiles(files);
    assertEquals(4, play.getFiles().size());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#clear()}.
   * 
   * @throws Exception
   */
  public final void testClear() throws Exception {
    Playlist play = createPlaylist();

    // nothing happens without content
    play.clear();

    play.addFile(JUnitHelpers.getFile("file1", false));
    play.addFile(JUnitHelpers.getFile("file1", false));
    play.addFile(JUnitHelpers.getFile("file1", false));
    play.addFile(JUnitHelpers.getFile("file1", false));
    assertEquals(4, play.getFiles().size());

    // now clear clears out the class
    play.clear();
    assertEquals(0, play.getFiles().size());
  }

  public final void testClearEmptyList() throws Exception {
    Device device = JUnitHelpers.getDevice();
    device.mount(true);
    Playlist play = new Playlist(Playlist.Type.NORMAL, "1", "playlist.m3u", JUnitHelpers
        .getDirectory());
    play.clear();
  }

  public final void testClearQueue() {
    Playlist play = getPlaylistQueue();
    play.clear();
  }

  public final void testClearBookmark() {
    Playlist play = getPlaylistBookmark();
    play.clear();
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#commit()}.
   * 
   * @throws Exception
   */
  public final void testCommit() throws Exception {
    Playlist playlist = createPlaylist();
    playlist.commit();
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#compareTo(org.jajuk.base.Playlist)}.
   */
  public final void testCompareTo() {
    Playlist play = new Playlist("1", "name", null);

    Playlist equ = new Playlist("1", "name", null);
    Playlist equ2 = new Playlist("4", "name", null); // different id still
    // compares as we just look
    // at name and directory...

    Playlist nonequ1 = new Playlist("2", "name3", null);
    Playlist nonequ2 = new Playlist("5", "name2", null);
    Playlist nonequ3 = new Playlist("2", "name", JUnitHelpers.getDirectory());

    JUnitHelpers.CompareToTest(play, equ, nonequ1);
    JUnitHelpers.CompareToTest(play, equ, nonequ2);
    JUnitHelpers.CompareToTest(play, equ, nonequ3);

    JUnitHelpers.CompareToTest(play, equ2, nonequ1);
    JUnitHelpers.CompareToTest(play, equ2, nonequ2);
    JUnitHelpers.CompareToTest(play, equ2, nonequ3);
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#containsExtFiles()}.
   */
  public final void testContainsExtFiles() {
    Playlist play = new Playlist("1", "name", null);

    // false usually
    assertFalse(play.containsExtFiles());

    // TODO: add test that loads a playlist with unavailable files so that this
    // is set to true...
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#down(int)}.
   * 
   * @throws Exception
   */
  public final void testDown() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    File file = JUnitHelpers.getFile("file1", false);
    file.setName("name1");
    play.addFile(file);

    file = JUnitHelpers.getFile("file2", false);
    file.setName("name2");
    play.addFile(file);

    file = JUnitHelpers.getFile("file3", false);
    file.setName("name3");
    play.addFile(file);

    file = JUnitHelpers.getFile("file4", false);
    file.setName("name4");
    play.addFile(file);

    assertEquals(4, play.getFiles().size());

    play.down(0);
    assertEquals("name2", play.getFiles().get(0).getName());
    assertEquals("name1", play.getFiles().get(1).getName());
    assertEquals("name3", play.getFiles().get(2).getName());
    assertEquals("name4", play.getFiles().get(3).getName());

    play.down(2);
    assertEquals("name2", play.getFiles().get(0).getName());
    assertEquals("name1", play.getFiles().get(1).getName());
    assertEquals("name4", play.getFiles().get(2).getName());
    assertEquals("name3", play.getFiles().get(3).getName());

    play.up(1);
    assertEquals("name1", play.getFiles().get(0).getName());
    assertEquals("name2", play.getFiles().get(1).getName());
    assertEquals("name4", play.getFiles().get(2).getName());
    assertEquals("name3", play.getFiles().get(3).getName());

    play.up(3);
    assertEquals("name1", play.getFiles().get(0).getName());
    assertEquals("name2", play.getFiles().get(1).getName());
    assertEquals("name3", play.getFiles().get(2).getName());
    assertEquals("name4", play.getFiles().get(3).getName());
  }

  public final void testDownBookmark() {
    Playlist play = getPlaylistBookmark();
    play.down(0);
    play.up(0);
  }

  /**
   * @return
   */
  private Playlist getPlaylistBookmark() {
    return new Playlist(Playlist.Type.BOOKMARK, "1", "name", null);
  }

  public final void testDownQueue() {
    Playlist play = getPlaylistQueue();
    play.down(-1);
    play.up(0);
  }

  /**
   * @return
   */
  private Playlist getPlaylistQueue() {
    // make sure the Queue is empty before creating a playlist on it
    QueueModel.clear();

    return new Playlist(Playlist.Type.QUEUE, "1", "name", null);
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#forceRefresh()}.
   * 
   * @throws Exception
   */
  public final void testForceRefresh() throws Exception {
    // make sure we have a playlist stored before
    Playlist play = createPlaylist();
    play.forceRefresh();
  }

  /**
  * Test method for {@link org.jajuk.base.Playlist#getAbsolutePath()}.
  * 
  * @throws Exception
  */
  public final void testGetAbsolutePath() throws Exception {
    Playlist play = createPlaylist();
    assertEquals(ConstTest.PATH_DEVICE + java.io.File.separator + "dir" + java.io.File.separator
        + "testdir" + java.io.File.separator + "playlist.m3u", play.getAbsolutePath());

    // call it a second time to use the cached version
    assertEquals(ConstTest.PATH_DEVICE + java.io.File.separator + "dir" + java.io.File.separator
        + "testdir" + java.io.File.separator + "playlist.m3u", play.getAbsolutePath());
  }

  public final void testGetAbsolutePathNotNormal() {
    Playlist play = new Playlist(Playlist.Type.BESTOF, "1", "name", null);
    assertTrue(StringUtils.isBlank(play.getAbsolutePath()));

    play.setFIO(new java.io.File("testfile"));
    assertTrue(StringUtils.isNotBlank(play.getAbsolutePath()));
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getDirectory()}.
   */
  public final void testGetDirectory() {
    Playlist play = new Playlist("1", "name", null);
    assertNull(play.getDirectory());

    play = new Playlist(Playlist.Type.NORMAL, "1", "playlist.m3u", JUnitHelpers.getDirectory());
    assertNotNull(play.getDirectory());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getFiles()}.
   * 
   * @throws Exception
   */
  public final void testGetFiles() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    assertEquals(0, play.getFiles().size());

    play.addFile(JUnitHelpers.getFile("file1", false));
    assertEquals(1, play.getFiles().size());
  }

  public final void testGetFilesNull() throws Exception {
    Playlist play = createPlaylist();
    play.setFiles(null); // null as list!
    assertEquals(2, play.getFiles().size());
  }

  public final void testGetFilesNovelities() throws Exception {
    Device device = JUnitHelpers.getDevice();
    Directory dir = JUnitHelpers.getDirectory();
    device.mount(true);

    Playlist play = new SmartPlaylist(Playlist.Type.NOVELTIES, "1", "playlist.m3u", dir);

    assertNotNull(play.getFiles());
  }

  public final void testGetFilesBestOf() throws Exception {
    Playlist play = new SmartPlaylist(Playlist.Type.BESTOF, "1", "playlist.m3u", JUnitHelpers
        .getDirectory());

    assertNotNull(play.getFiles());
  }

  public final void testGetFilesNew() throws Exception {
    Playlist play = new SmartPlaylist(Playlist.Type.NEW, "1", "playlist.m3u", JUnitHelpers
        .getDirectory());

    assertNotNull(play.getFiles());
    assertEquals(0, play.getFiles().size());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getFIO()}.
   */
  public final void testGetAndSetFIO() {
    Playlist play = new SmartPlaylist(Playlist.Type.BESTOF, "1", "name", null);
    assertNotNull(play.getFIO());

    play.setFIO(null);
    assertNotNull(play.getFIO()); // recreated...

    play.setFIO(new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator
        + "testfio"));
    assertNotNull(play.getFIO());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#isReady()}.
   * 
   * @throws Exception
   */
  public final void testIsReady() throws Exception {
    Playlist play = createPlaylist();

    // mounted initially
    assertTrue(play.isReady());

    play.getDirectory().getDevice().unmount();

    assertFalse(play.isReady());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#load()}.
   * 
   * @throws Exception
   */
  public final void testLoad() throws Exception {
    // first commit a playlist
    {
      Playlist play = createPlaylist();

      play.addFile(JUnitHelpers.getFile("file1", false));
      new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator + "testdir")
          .mkdir();

      play.setFIO(new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator
          + "testdir" + java.io.File.separator + "playlist.m3u"));

      play.commit();
    }

    Playlist play = createPlaylist();
    List<File> list = play.load();
    assertNotNull(list);

    assertEquals(2, list.size());

    assertEquals("file1", list.get(0).getName());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#play()}.
   * 
   * @throws Exception
   */
  public final void testPlay() throws Exception {
    Playlist play = createPlaylist();

    // some error without files
    play.play();

    play.addFile(JUnitHelpers.getFile("file1", false));

    // try again with files
    play.play();
  }

  public final void testPlayNull() throws Exception {
    Playlist play = createPlaylist();

    // some error without files
    play.setFiles(null);
    try {
      play.play();
    } catch (HeadlessException e) {
      // this tries to open a FileChooser...
    }
    play.setFiles(new ArrayList<File>());
    try {
      play.play();
    } catch (HeadlessException e) {
      // this tries to open a FileChooser...
    }
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#remove(int)}.
   * 
   * @throws Exception
   */
  public final void testRemove() throws Exception {
    Playlist play = createPlaylist();
    play.addFile(JUnitHelpers.getFile("file1", false));
    play.remove(0);
  }

  public final void testRemoveBookmark() throws Exception {
    Bookmarks.getInstance().addFile(JUnitHelpers.getFile("file1", false));

    Playlist play = getPlaylistBookmark();
    play.remove(0);
  }

  public final void testRemoveQueue() {
    Playlist play = getPlaylistQueue();
    play.remove(0);
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#replaceFile(org.jajuk.base.File, org.jajuk.base.File)}
   * .
   * 
   * @throws Exception
   */
  public final void testReplaceFile() throws Exception {
    Playlist play = createPlaylist();

    play.addFile(JUnitHelpers.getFile("file1", false));

    File file = JUnitHelpers.getFile("file1", false);
  
    play.setFIO(new java.io.File(System.getProperty("java.io.tmpdir") + java.io.File.separator
        + "testdir" + java.io.File.separator + "playlist.m3u"));

    play.replaceFile(play.getFiles().get(0), file);
  }

  public final void testReplaceFileBookmark() throws Exception {
    Playlist play = getPlaylistBookmark();

    play.addFile(JUnitHelpers.getFile("file1", false));

    // wait for the thread to finish before doing this
    JUnitHelpers.waitForThreadToFinish("Queue Push Thread");

    play.replaceFile(play.getFiles().get(0), JUnitHelpers.getFile("file1", false));
  }

  public final void testReplaceFileQueue() throws Exception {
    // make sure Queue is empty
    QueueModel.clear();

    Playlist play = getPlaylistQueue();

    // for type Queue, we need to push to the Queue
    File file = JUnitHelpers.getFile("file1", false);
    file.getDirectory().getDevice().mount(true);
    QueueModel.insert(new StackItem(file), 0);

    assertEquals(1, play.getFiles().size());
    assertNotNull(play.getFiles().get(0));

    play.replaceFile(play.getFiles().get(0), JUnitHelpers.getFile("file1", false));
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#reset()}.
   */
  public final void testReset() {
    Playlist play = new Playlist("1", "name", null);

    Directory dir = JUnitHelpers.getDirectory();
    play.setParentDirectory(dir);

    play.setFIO(new java.io.File("testfile"));
    play.reset();
    assertNotNull(play.getFIO()); // recreated again...
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#saveAs()}.
   * 
   * @throws Exception
   */
  public final void testSaveAs() throws Exception {
    Playlist play = createPlaylist();

    try {
      play.saveAs();
    } catch (InvocationTargetException e) {
      // this tries to open a FileChooser...
    } catch (HeadlessException e) {
      // this tries to open a FileChooser...
    }
  }

  public final void testSaveAsBestOf() throws Exception {
    Directory dir = JUnitHelpers.getDirectory();

    Playlist play = new Playlist(Playlist.Type.BESTOF, "1", "playlist.m3u", dir);

    List<File> list = new ArrayList<File>();
    list.add(JUnitHelpers.getFile("file1", false));
    list.add(JUnitHelpers.getFile("file1", false));

    play.setFiles(list);

    try {
      play.saveAs();
    } catch (InvocationTargetException e) {
      // this tries to open a FileChooser...
    } catch (HeadlessException e) {
      // this tries to open a FileChooser...
    }
  }

  public final void testSaveAsBookmark() throws Exception {
    Playlist play = getPlaylistBookmark();

    try {
      play.saveAs();
    } catch (InvocationTargetException e) {
      // this tries to open a FileChooser...
    } catch (HeadlessException e) {
      // this tries to open a FileChooser...
    }
  }

  public final void testSaveAsNovelities() throws Exception {
    Directory dir = JUnitHelpers.getDirectory();

    Playlist play = new Playlist(Playlist.Type.NOVELTIES, "1", "playlist.m3u", dir);

    List<File> list = new ArrayList<File>();
    list.add(JUnitHelpers.getFile("file1", false));
    list.add(JUnitHelpers.getFile("file1", false));

    play.setFiles(list);

    try {
      play.saveAs();
    } catch (InvocationTargetException e) {
      // this tries to open a FileChooser...
    } catch (HeadlessException e) {
      // this tries to open a FileChooser...
    }
  }

  public final void testSaveAsQueue() throws Exception {
    Playlist play = getPlaylistQueue();

    try {
      play.saveAs();
    } catch (InvocationTargetException e) {
      // this tries to open a FileChooser...
    } catch (HeadlessException e) {
      // this tries to open a FileChooser...
    }
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#setFiles(java.util.List)}.
   * 
   * @throws Exception
   */
  public final void testSetFiles() throws Exception {
    Playlist play = createPlaylist();

    List<File> list = new ArrayList<File>();
    list.add(JUnitHelpers.getFile("file1", false));

    play.setFiles(list);

    assertEquals(1, play.getFiles().size());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#setFIO(java.io.File)}.
   */
  public final void testSetFIO() {
    // tested above in getFIO();
  }

  /**
   * Test method for
   * {@link org.jajuk.base.Playlist#setParentDirectory(org.jajuk.base.Directory)}
   * .
   */
  public final void testSetParentDirectory() {
    Playlist play = new Playlist("1", "name", null);
    assertNull(play.getDirectory());

    Directory dir = JUnitHelpers.getDirectory();

    play.setParentDirectory(dir);

    assertNotNull(play.getDirectory());

    // also try setting it to null
    play.setParentDirectory(null);
    assertNull(play.getDirectory());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#shouldBeHidden()}.
   * 
   * @throws Exception
   */
  public final void testShouldBeHidden() throws Exception {

    Directory dir = JUnitHelpers.getDirectory();

    Playlist play = new Playlist("1", "name", dir);

    // related configuration
    Conf.setProperty(Const.CONF_OPTIONS_HIDE_UNMOUNTED, "false");

    // always false as long as conf is set to "false"
    assertFalse(play.shouldBeHidden());

    // related configuration
    Conf.setProperty(Const.CONF_OPTIONS_HIDE_UNMOUNTED, "true");

    // now "true" because device is not mounted
    assertTrue(play.shouldBeHidden());

    // now mount the device
    dir.getDevice().mount(true);

    // now "false" again, as we have the device mounted
    assertFalse(play.shouldBeHidden());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#toString()}.
   */
  public final void testToString() {
    Playlist play = new Playlist("1", "name", null);

    // first test without directory
    JUnitHelpers.ToStringTest(play);

    Directory dir = JUnitHelpers.getDirectory();

    // then with a directory
    play = new Playlist("1", "name", dir);
    JUnitHelpers.ToStringTest(play);
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#up(int)}.
   */
  public final void testUp() {
    // tested as part of testDown()
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getHits()}.
   * 
   * @throws Exception
   */
  public final void testGetHits() throws Exception {
    FileManager.getInstance().clear();

    Playlist play = createPlaylist();

    // first without files
    assertEquals(0, play.getHits());

    // then with some files
    play.addFile(JUnitHelpers.getFile("file11", false));

    // still zero as file has no hits set
    assertEquals(0, play.getHits());

    // now add a file with hit-count set
    File file = JUnitHelpers.getFile("file12", false);
    file.getTrack().setHits(3);
    play.addFile(file);

    // now hits are set
    assertEquals(3, play.getHits());

    // add another file with different hit-count
    file = JUnitHelpers.getFile("file14", false);
    file.getTrack().setHits(11);
    play.addFile(file);

    // now hits accumulate
    assertEquals(14, play.getHits());
  }

  public final void testGetHitsNull() throws Exception {
    Playlist play = createPlaylist();
    play.setFiles(null);

    // first without files
    assertEquals(0, play.getHits());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getDuration()}.
   * 
   * @throws Exception
   */
  public final void testGetDuration() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    // at first no duration at all
    assertEquals(0, play.getDuration());

    // when we add tracks, duration accumulates
    play.addFile(JUnitHelpers.getFile("file1", false));

    // we use 120 seconds as length in "JUnitHelpers.getFile("file1", false)"
    assertEquals(120, play.getDuration());

    // another file
    play.addFile(JUnitHelpers.getFile("file1", false));

    // sums up two times 120
    assertEquals(240, play.getDuration());
  }

  public final void testGetDurationNull() throws Exception {
    Playlist play = createPlaylist();
    play.setFiles(null);

    // at first no duration at all
    assertEquals(0, play.getDuration());
  }

  /**
   * Test method for {@link org.jajuk.base.Playlist#getNbOfTracks()}.
   * 
   * @throws Exception
   */
  public final void testGetNbOfTracks() throws Exception {
    Playlist play = createPlaylist();
    play.remove(0);
    play.remove(0);

    assertEquals(0, play.getNbOfTracks());

    play.addFile(JUnitHelpers.getFile("file1", false));
    assertEquals(1, play.getNbOfTracks());

    // another file
    play.addFile(JUnitHelpers.getFile("file1", false));

    assertEquals(2, play.getNbOfTracks());
  }

  public final void testGetNbOfTracksNull() throws Exception {
    Playlist play = createPlaylist();
    play.setFiles(null);

    assertEquals(0, play.getNbOfTracks());
  }
}
