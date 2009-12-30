/*
 *  Jajuk
 *  Copyright (C) 2003-2008 The Jajuk Team
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
package org.jajuk.services.lyrics;

import ext.services.network.NetworkUtils;
import ext.services.xml.XMLUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jajuk.JUnitHelpers;
import org.jajuk.JajukTestCase;
import org.jajuk.base.Album;
import org.jajuk.base.Author;
import org.jajuk.base.Device;
import org.jajuk.base.Directory;
import org.jajuk.base.Style;
import org.jajuk.base.Track;
import org.jajuk.base.Type;
import org.jajuk.base.Year;
import org.jajuk.services.core.SessionService;
import org.jajuk.services.lyrics.providers.FlyWebLyricsProvider;
import org.jajuk.services.lyrics.providers.GenericWebLyricsProvider;
import org.jajuk.services.lyrics.providers.ILyricsProvider;
import org.jajuk.services.lyrics.providers.LyrcWebLyricsProvider;
import org.jajuk.services.lyrics.providers.LyricWikiWebLyricsProvider;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.DownloadManager;
import org.jajuk.util.log.Log;
import org.w3c.dom.Document;

/**
 * Lyrics unit tests
 */
public class TestLyrics extends JajukTestCase {

  private final File tmp = new File("test.tmp");
  private static final String ARTIST = "Massive Attack";
  private static final String TITLE = "Dissolved Girl";
  private static final String TESTED_WORD = "Day, yesterday";

  // LyricsFly put a delay of 1500 ms before we are allowed to query again, we
  // need to take that into account for some of the tests
  private static final long FLY_DELAY = 1500 + 200;

  // helper method to emma-coverage of the unused constructor
  public void testPrivateConstructor() throws Exception {
    // For EMMA code-coverage tests
    JUnitHelpers.executePrivateConstructor(LyricsService.class);
  }

  /**
   * Test setup
   */
  @Override
  public void setUp() throws IOException {
    if (tmp.exists()) {
      tmp.delete();
    }

    JUnitHelpers.createSessionDirectory();

    // to first cover this method while no providers are loaded yet
    LyricsService.getProviders();
  }

  /**
   * Test provider loading
   */
  public void testProvidersLoading() {
    LyricsService.loadProviders();
    List<ILyricsProvider> providers = LyricsService.getProviders();
    assertNotNull(providers);
    assertFalse(providers.size() == 0);
  }

  /**
   * Test provider response to get lyrics (shared code)
   */
  private void testWebService(GenericWebLyricsProvider provider) {
    String lyrics = provider.getLyrics(ARTIST, TITLE);
    Log.debug("Resulting Lyrics(" + provider.getProviderHostname() + "): " + lyrics);
    assertTrue("Lyrics(" + provider.getProviderHostname() + "): " + lyrics, StringUtils
        .isNotBlank(lyrics));
    assertTrue("Lyrics(" + provider.getProviderHostname() + "): " + lyrics, lyrics
        .indexOf(TESTED_WORD) != -1);
  }

  /**
   * Test provider web site url (shared code)
   */
  private void testWeb(GenericWebLyricsProvider provider) throws IOException {
    URL url = provider.getWebURL(ARTIST, TITLE);
    assertNotNull(url);
    DownloadManager.download(url, tmp);

    assertTrue(tmp.exists());
    assertTrue(tmp.length() > 0);
  }

  /**
   * Test Fly provider response to get lyrics
   * 
   * @throws Exception
   */
  // TODO: re-enable after we added a new userid
  public void ntestFlyService() throws Exception {
    GenericWebLyricsProvider provider = new FlyWebLyricsProvider();
    testWebService(provider);

    // delay a bit as LyricsFly puts a min. delay before the next request is
    // allowed
    Thread.sleep(FLY_DELAY);
  }

  // TODO: re-enable after we added a new userid
  public void ntestFlyServiceSonar() throws Exception {
    // ensure that this is not configured somehow
    assertFalse(Conf.getBoolean(Const.CONF_NETWORK_NONE_INTERNET_ACCESS));

    // do some in-depth test here to find out why this fails in Sonar

    Field urlField = FlyWebLyricsProvider.class.getDeclaredField("URL");
    urlField.setAccessible(true);
    String queryString = (String) (urlField.get(null));

    queryString = queryString.replace(Const.PATTERN_AUTHOR, (ARTIST != null) ? NetworkUtils
        .encodeString(ARTIST) : "");

    queryString = queryString.replace(Const.PATTERN_TRACKNAME, (TITLE != null) ? NetworkUtils
        .encodeString(TITLE) : "");

    URL url = new URL(queryString);

    Log.info("Downloading: " + url);

    String xml = null;

    // xml = DownloadManager.getTextFromCachedFile(url, "UTF-8");
    // Drop the query if user required "none Internet access from jajuk".
    // This method shouldn't be called anyway because we views have to deal with
    // this option at their level, this is a additional control.
    assertFalse(Conf.getBoolean(Const.CONF_NETWORK_NONE_INTERNET_ACCESS));

    // make sure that we remove any existing cache for this file before we run
    // this test
    {
      File file = SessionService.getCachePath(url);
      assertNotNull(file);
      if (file.exists()) {
        Log.info("Removing existing cache file: " + file);
        assertTrue(file.toString(), file.delete());
      }
    }

    File file = DownloadManager.downloadToCache(url);
    assertNotNull(file);

    // make sure the file is available correctly
    assertTrue(file.toString(), file.exists());
    assertTrue(file.toString(), file.canRead());
    assertTrue(file.toString(), file.isFile());
    assertFalse(file.toString(), file.isHidden());
    assertTrue(file.toString(), file.length() > 0);

    StringBuilder builder = new StringBuilder();
    InputStream input = new BufferedInputStream(new FileInputStream(file));
    boolean bRead = false;
    try {
      byte[] array = new byte[1024];
      int read;
      while ((read = input.read(array)) > 0) {
        builder.append(new String(array, 0, read, "UTF-8"));
        bRead = true;
      }
    } finally {
      input.close();
    }

    // make sure we read at least some bytes/chars
    assertTrue(file.toString(), bRead);

    xml = builder.toString();
    assertTrue(xml, StringUtils.isNotBlank(xml));

    // FlyProvider.getLyrics()
    Document document = XMLUtils.getDocument(xml);
    assertNotNull(document);

    String lyrics = null;
    lyrics = XMLUtils.getChildElementContent(document.getDocumentElement(), "tx");
    lyrics = lyrics.replace("[br]", "");

    assertTrue(xml, StringUtils.isNotBlank(lyrics));

    // delay a bit as LyricsFly puts a min. delay before the next request is
    // allowed
    Thread.sleep(FLY_DELAY);
  }

  /**
   * Test Fly web url availability
   */
  public void testFlyWeb() throws Exception {
    GenericWebLyricsProvider provider = new FlyWebLyricsProvider();
    testWeb(provider);

    // delay a bit as LyricsFly puts a min. delay before the next request is
    // allowed
    Thread.sleep(FLY_DELAY);
  }

  /**
   * Test LyricWiki provider response to get lyrics
   */
  public void testLyricWikiService() {
    GenericWebLyricsProvider provider = new LyricWikiWebLyricsProvider();
    testWebService(provider);
  }

  /**
   * Test LyricWiki web url availability
   */
  public void testLyricWikiWeb() throws Exception {
    GenericWebLyricsProvider provider = new LyricWikiWebLyricsProvider();
    testWeb(provider);
  }

  /**
   * Test providers order For each provider, we test the class and then we
   * remove it from the providers list to allow the others to run
   * 
   * @throws Exception
   */
  public void testWebProvidersOrder() throws Exception {
    // Removing TagProvider and TxtProvider
    LyricsService.getProviders().remove(0);
    LyricsService.getProviders().remove(0);

    org.jajuk.base.File dummyFile = new org.jajuk.base.File("1", "test", new Directory("1", "dir", null, new Device("1", "test")), new Track("1",
        TITLE, new Album("1", "Album", "artist", 1), new Style("1", "style"), new Author("1",
            ARTIST), 0, new Year("1", "100"), 0, new Type("1", "name", "ext", null, null), 0),
        120l, 70l);
    LyricsService.getLyrics(dummyFile);
    assertTrue("Instance: " + LyricsService.getCurrentProvider().getClass()
        + " but expected LyricWikiProvider",
        LyricsService.getCurrentProvider() instanceof LyricWikiWebLyricsProvider);

    LyricsService.getProviders().remove(0);
    LyricsService.getLyrics(dummyFile);
// TODO: enable after flylyrics works again...
//    assertTrue("Instance: " + LyricsService.getCurrentProvider().getClass()
//        + " but expected FlyProvider",
//        LyricsService.getCurrentProvider() instanceof FlyWebLyricsProvider);

    LyricsService.getProviders().remove(0);
    LyricsService.getLyrics(dummyFile);
    assertTrue("Instance: " + LyricsService.getCurrentProvider().getClass()
        + " but expected LyrcProvider",
        LyricsService.getCurrentProvider() instanceof LyrcWebLyricsProvider);

    // delay a bit as LyricsFly puts a min. delay before the next request is
    // allowed
    Thread.sleep(FLY_DELAY);
  }

}
