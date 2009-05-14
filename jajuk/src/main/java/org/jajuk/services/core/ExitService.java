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
package org.jajuk.services.core;

import java.io.File;

import org.jajuk.Main;
import org.jajuk.base.Collection;
import org.jajuk.base.DeviceManager;
import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.services.bookmark.History;
import org.jajuk.services.dj.AmbienceManager;
import org.jajuk.services.players.QueueModel;
import org.jajuk.services.players.Player;
import org.jajuk.services.webradio.WebRadioManager;
import org.jajuk.ui.actions.JajukAction;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.log.Log;

/**
 * All code related to jajuk exit
 */
public class ExitService extends Thread {

  /** Exit code */
  private static int iExitCode = 0;

  /** Exiting flag */
  private static boolean bExiting = false;

  public ExitService() {
    super("Exit hook thread");
  }

  @Override
  public void run() {
    Log.debug("Exit Hook begin");

    // stop sound ASAP
    Player.stop(true);

    ObservationManager.notifySync(new JajukEvent(JajukEvents.EXITING));

    try {
      if (iExitCode == 0) {
        // Store current FIFO for next session
        QueueModel.commit();
        // commit only if exit is safe (to avoid
        // commiting
        // empty collection) commit ambiences
        AmbienceManager.getInstance().commit();
        // Commit webradios
        WebRadioManager.getInstance().commit();
        // Store webradio state
        Conf.setProperty(Const.CONF_WEBRADIO_WAS_PLAYING, Boolean.toString(QueueModel.isPlayingRadio()));

        // commit configuration
        org.jajuk.util.Conf.commit();
        // commit history
        History.commit();
        // Wait few secs if some devices are still refreshing, a kill signal has
        // been sent to them
        if (DeviceManager.getInstance().isAnyDeviceRefreshing()) {
          for (int i = 0; i < 10; i++) {
            if (DeviceManager.getInstance().isAnyDeviceRefreshing()) {
              Thread.sleep(1000);
              Log.debug("Exiting waiting for refresh process end...");
            } else {
              continue;
            }
          }
        }
        // Commit collection if not still refreshing
        if (!DeviceManager.getInstance().isAnyDeviceRefreshing()) {
          Collection.commit(UtilSystem.getConfFileByPath(Const.FILE_COLLECTION));
        }
        /* release keystrokes resources */
        JajukAction.cleanup();

        // Remove localhost_<user> session files
        // (can occur when network is not available)
        File[] files = UtilSystem.getConfFileByPath(Const.FILE_SESSIONS).listFiles();
        if (files != null) {
          for (File element : files) {
            if (element.getName().indexOf("localhost") != -1) {
              if (!element.exists()) {
                Log.info("Session file: " + element.getAbsolutePath() + " does not exist.");
              } else if (element.delete()) {
                Log.warn("Deleted session file: " + element.getAbsolutePath());
              } else {
                Log.warn("Could not delete file: " + element.getAbsolutePath());
              }
            }
          }
        }

        // Remove session flag.
        File file = Main.getSessionIdFile();
        if (!file.exists()) {
          Log.info("Cannot delete file, file: " + file.toString()
              + " does not exist or workspace move.");
        } else if (!file.delete()) {
          Log.warn("Could not delete file: " + file.toString());
        }

      }
    } catch (Exception e) {
      Log.error(e);
    } finally {
      Log.debug("Exit Hook end");
    }
  }

  /**
   * Exit code, then system will execute the exit hook
   * 
   * @param iExitCode
   *          exit code
   *          <p>
   *          0 : normal exit
   *          <p>
   *          1: unexpected error
   */
  public static void exit(final int iExitCode) {
    // set exiting flag
    bExiting = true;
    // store exit code to be read by the system hook
    ExitService.iExitCode = iExitCode;
    // display a message
    Log.debug("Exit with code: " + iExitCode);
    System.exit(iExitCode);
  }

  /**
   * @return Returns whether jajuk is in exiting state
   */
  public static boolean isExiting() {
    return bExiting;
  }

}
