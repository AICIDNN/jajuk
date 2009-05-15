/*
 *  Jajuk
 *  Copyright (C) 2007 The Jajuk Team
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

package org.jajuk.ui.thumbnails;

import ext.ProcessLauncher;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Appender;
import org.jajuk.Main;
import org.jajuk.base.Album;
import org.jajuk.base.AlbumManager;
import org.jajuk.base.Collection;
import org.jajuk.base.DeviceManager;
import org.jajuk.services.core.SessionService;
import org.jajuk.util.Const;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.filters.JarFilter;
import org.jajuk.util.log.Log;

/**
 * Creating thumbs use a large amount of RAM. This executable class is intended
 * to build all thumbs in a separated java process to free memory when the
 * process is done.
 * 
 */
public final class ThumbnailsMaker {

  private static boolean bAlreadyRunning = false;

  /**
   * 
   * @return separator between a '-cp' argument
   */
  private static char getJarSeparator() {
    if (UtilSystem.isUnderWindows()) {
      return ';';
    } else {
      return ':';
    }
  }

  /**
   * Convenient method to launch all thumb makers, one for each size
   * 
   * @param bSynchronous
   *          do you have to wait all process done ?
   */
  public static void launchAllSizes(final boolean bSynchronous) {
    // We need this mutex to make sure auto-refresh cannot launch several times
    // the full thumbs rebuild:
    // autorefresh at time t launch this method asynchronously, then autorefresh
    // at t+n relaunch it..
    if (bAlreadyRunning) {
      Log.debug("Thumb maker already running, leaving");
      return;
    } else {
      bAlreadyRunning = true;
    }
    final Thread t = new Thread() {
      @Override
      public void run() {
        try {
          for (int i = 50; i <= 300; i += 50) {
            ThumbnailsMaker.launchProcessus(i);
          }
        } catch (Exception e) { // TODO Auto-generated
          e.printStackTrace();
        } finally {
          bAlreadyRunning = false;
        }
      }
    };
    // Set min prority to avoid using too much CPU
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
    if (bSynchronous) {
      try {
        t.join();
      } catch (final InterruptedException e) { // TODO Auto-generated catch
        e.printStackTrace();
      }

    }
  }

  /**
   * Convenient method to launch the thumb creation in another JVM
   * 
   * @param size
   * @return status : 0 if OK, 1 ok error
   * @throws URISyntaxException 
   * @throws IOException 
   */
  public static int launchProcessus(final int size) throws URISyntaxException, IOException {
    final String jvmPath = System.getProperty("java.home") + File.separatorChar + "bin"
        + File.separatorChar + "java";
    // @TODO Under JNLP mode, following line throw an URI malformed exception
    // (contains %20 for ie). We
    // don't figured out so far how to
    // build proper JAR path. We keep things like it for the moment, pictures
    // are built in the main JVM instead of separated ones
    final String jarPath = new File(UtilSystem.getJarLocation(Main.class).toURI())
        .getAbsolutePath()
        + File.separator + "jajuk.jar";
    final List<String> commands = new ArrayList<String>(10);
    commands.add(jvmPath);
    commands.add("-Xms50M");
    commands.add("-Xmx600M");
    commands.add("-cp");
    // Add the bin directory that contains classes
    String cp = new File(UtilSystem.getJarLocation(Main.class).toURI()).getAbsolutePath();
    cp += ThumbnailsMaker.getJarSeparator();
    final File libDir = new File(UtilSystem.getJarLocation(Appender.class).toURI()).getParentFile();
    final File[] files = libDir.listFiles(JarFilter.getInstance());
    for (final File element : files) {
      cp += element.getAbsolutePath() + ThumbnailsMaker.getJarSeparator();
    }
    // If we are not in dev, add jajuk jar itself to the classpath
    if (!SessionService.isIdeMode()) {
      cp += ThumbnailsMaker.getJarSeparator() + jarPath;
    }
    // remove last separator
    cp = cp.substring(0, cp.length() - 1);
    commands.add(cp);

    // Add this class self name (with package)
    commands.add(ThumbnailsMaker.class.getCanonicalName());
    // Add size as a program argument
    commands.add(Integer.toString(size));
    // Add the test status
    commands.add(Boolean.toString(SessionService.isTestMode()));
    // Add the workspace value
    commands.add(SessionService.getWorkspace());
    // Add the session Id path
    commands.add(SessionService.getSessionIdFile().getAbsolutePath());
    Log.debug("Use command:" + commands);
    // We use this class and not ProcessBuilder as it hangs under windows
    // probably because of stream reading
    final ProcessLauncher launcher = new ProcessLauncher();
    return launcher.exec(commands.toArray(new String[commands.size()]));
  }

  /**
   * @param args :
   *          size: thumb size like 100, or 300 boolean; test mode ? workspace
   *          session id full path
   * 
   */
  public static void main(final String[] args) {
    new ThumbnailsMaker(Integer.parseInt(args[0]), Boolean.parseBoolean(args[1]), args[2], args[3]);
  }

  private int size = 0;

  private int stat = 0;

  private boolean bTest = false;

  private final String workspace;

  private final File sessionId;

  /**
   * No instances
   */
  private ThumbnailsMaker(final int pSize, final boolean pTest, final String pWorkspace,
      final String pSessionIdFile) {
    size = pSize;
    bTest = pTest;
    workspace = pWorkspace;
    sessionId = new File(pSessionIdFile);
    try {
      buildThumbs();
    } catch (final Exception e) {
      e.printStackTrace();
      // Leave in error
      System.exit(1);
    }
    // Leave successfully
    System.exit(0);
  }

  /**
   * Build thumbs for given parameters We make a minimal jajuk startup here to
   * make the process possible
   * 
   * @throws Exception
   */
  private void buildThumbs() throws Exception {
    final long lTime = System.currentTimeMillis();
    Main.initializeFromThumbnailsMaker(bTest, workspace);
    // log startup depends on : setExecLocation, initialCheckups
    Log.getInstance();
    Log.setVerbosity(Log.FATAL);
    Main.initialCheckups();
    Main.registerItemManagers();
    // Register device types
    for (final String deviceTypeId : Main.DEVICE_TYPES) {
      DeviceManager.getInstance().registerDeviceType(Messages.getString(deviceTypeId));
    }
    // registers supported audio supports and default properties
    Main.registerTypes();
    // load collection
    Collection.load(SessionService.getConfFileByPath(Const.FILE_COLLECTION));
    // Mount devices
    Main.autoMount();
    final List<Album> albums = AlbumManager.getInstance().getAlbums();
    // For each album, create the associated thumb
    for (final Album album : albums) {
      // Leave if jajuk leaved
      if (!sessionId.exists()) {
        Log.debug("Parent Jajuk closed, leaving now...");
        return;
      }
      if (ThumbnailManager.refreshThumbnail(album, size)) {
        stat++;
      }
      // Call GC to avoid increasing too much memory
      if ((stat > 0) && (stat % 30 == 0)) {
        System.gc();
      }
    }
    Log.setVerbosity(Log.DEBUG);
    Log.debug("[Thumb maker] " + stat + " thumbs created for size: " + size + " in "
        + (System.currentTimeMillis() - lTime) + " ms");
  }

}
