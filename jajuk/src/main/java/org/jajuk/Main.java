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
package org.jajuk;

import ext.JVM;

import java.util.Locale;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jajuk.base.Collection;
import org.jajuk.services.bookmark.History;
import org.jajuk.services.core.ExitService;
import org.jajuk.services.core.SessionService;
import org.jajuk.services.dj.AmbienceManager;
import org.jajuk.services.dj.DigitalDJManager;
import org.jajuk.services.lastfm.LastFmManager;
import org.jajuk.services.startup.StartupAsyncService;
import org.jajuk.services.startup.StartupCollectionService;
import org.jajuk.services.startup.StartupControlsService;
import org.jajuk.services.startup.StartupEngineService;
import org.jajuk.services.startup.StartupGUIService;
import org.jajuk.ui.helpers.FontManager;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.DownloadManager;
import org.jajuk.util.LocaleManager;
import org.jajuk.util.UpgradeManager;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.UtilString;
import org.jajuk.util.log.Log;
import org.jvnet.substance.skin.SubstanceBusinessLookAndFeel;

/**
 * Jajuk launching class.
 */
public final class Main {

  /**
   * private constructor to avoid instantiating utility class.
   */
  private Main() {
  }

  /**
   * Main entry.
   * 
   * @param args
   *          DOCUMENT_ME
   */
  public static void main(final String[] args) {
    // non ui init
    try {
      // check JVM version
      if (!JVM.current().isOrLater(JVM.JDK1_6)) {
        System.out.println("Java Runtime Environment 1.6 minimum required." + " You use a JVM "
            + JVM.current());
        System.exit(2); // error code 2 : wrong JVM
      }
      // set flags from command line options
      SessionService.handleCommandline(args);

      // set flags from system properties
      SessionService.handleSystemProperties();

      // Set substance theme (for raw error windows displayed by initial
      // checkups only)
      // (must be done in the EDT)
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
            UIManager.setLookAndFeel(new SubstanceBusinessLookAndFeel());
          } catch (UnsupportedLookAndFeelException e) {
            // No Log here, logs are not yet initialized
            e.printStackTrace();
          }
        }
      });

      // perform initial checkups and create needed files
      StartupControlsService.initialCheckups();

      // log startup depends on : initialCheckups
      Log.getInstance();
      Log.setVerbosity(Log.DEBUG);

      // Load user configuration. Depends on: initialCheckups
      Conf.load();

      Log.debug("----------------------------------------------------------------------------");
      Log.debug("Starting Jajuk" + Const.JAJUK_VERSION + " <" + Const.JAJUK_CODENAME + ">" + " "
          + Const.JAJUK_VERSION_DATE);

      // Full substance configuration now
      // (must be done in the EDT)
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          UtilGUI.setupSubstanceLookAndFeel(Conf.getString(Const.CONF_OPTIONS_LNF));
        }
      });

      // Set default fonts
      FontManager.getInstance().setDefaultFont();

      // Detect current release
      UpgradeManager.detectRelease();

      // Set actual log verbosity. Depends on:
      // Conf.load
      if (!SessionService.isTestMode()) {
        // test mode is always in debug mode
        Log.setVerbosity(Integer.parseInt(Conf.getString(Const.CONF_OPTIONS_LOG_LEVEL)));
      }
      // Set locale. setSystemLocal
      LocaleManager.setLocale(new Locale(Conf.getString(Const.CONF_OPTIONS_LANGUAGE)));

      // Display the splash screen through a invokeAndWait
      StartupGUIService.launchSplashScreen();

      // Apply any proxy (requires load conf)
      DownloadManager.setDefaultProxySettings();

      // Registers ItemManager managers
      StartupCollectionService.registerItemManagers();

      // Upgrade configuration from previous releases
      UpgradeManager.upgradeStep1();

      // Display user system configuration
      Log.debug("Workspace used: " + SessionService.getWorkspace());
      Log.debug(UtilString.getAnonymizedSystemProperties().toString());

      // Display user Jajuk configuration
      Log.debug(UtilString.getAnonymizedJajukProperties().toString());

      // check for another session (needs setLocal)
      SessionService.checkOtherSession();

      // Create a session file
      SessionService.createSessionFile();

      // Register device types
      StartupCollectionService.registerDevicesTypes();

      // registers supported audio supports and default properties. Display a
      // "Downloading mplayer" message by default in the splash screen in case
      // of it is downloaded
      StartupGUIService.fireStepOneOver();
      StartupCollectionService.registerTypes();

      // Display progress
      StartupGUIService.fireStepTwoOver();

      // Load collection
      StartupCollectionService.loadCollection();

      // Upgrade step2 (after collection load)
      UpgradeManager.upgradeStep2();

      // Clean the collection up
      Collection.cleanupLogical();

      // Display progress
      StartupGUIService.fireStepThreeOver();

      // Load history
      History.load();

      // Load ambiences
      AmbienceManager.getInstance().load();

      // Start LastFM support
      LastFmManager.getInstance();

      // Load djs
      DigitalDJManager.getInstance().loadAllDJs();

      // Various asynchronous startup actions that needs collection load
      boolean bCollectionLoadRecover = StartupCollectionService.isCollectionLoadRecover();
      StartupAsyncService.startupAsyncAfterCollectionLoad(bCollectionLoadRecover);

      // Auto mount devices, freeze for SMB drives
      // if network is not reachable
      // Do not start this if first session, it is causes concurrency with
      // first refresh thread
      if (!UpgradeManager.isFirstSession()) {
        StartupEngineService.autoMount();
      }

      // Launch startup track if any (but don't start it if first session
      // because the first refresh is probably still running)
      if (!UpgradeManager.isFirstSession()) {
        StartupEngineService.launchInitialTrack();
      }

      // Launch the right jajuk window
      StartupGUIService.launchUI();

      // Late collection upgrade actions
      UpgradeManager.upgradeStep3();

    } catch (final Exception e) { // last chance to catch any error for
      // logging
      // purpose
      e.printStackTrace();
      Log.error(106, e);
      ExitService.exit(1);
    } catch (final Error error) {
      // last chance to catch any error for logging purpose
      error.printStackTrace();
      Log.error(106, error);
      ExitService.exit(1);
    } finally { // make sure to close splash screen in all cases
      // (i.e. if UI is not started)
      StartupGUIService.startupOver();
    }
  }

  /**
   * Called when starting Jajuk from the thumb maker process, set jajuk in light
   * mode.
   * 
   * @param bTest
   *          DOCUMENT_ME
   * @param workspace
   *          DOCUMENT_ME
   */
  public static void initializeFromThumbnailsMaker(final boolean bTest, final String workspace) {
    SessionService.setTestMode(bTest);
    SessionService.setWorkspace(workspace);
    SessionService.setInThumbMaker(true);
  }

}
