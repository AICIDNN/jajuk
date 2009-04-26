/*
 *  Jajuk
 *  Copyright (C) 2003-2009 The Jajuk Team
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
package org.jajuk.dbus;

/**
 * Provides implementation of the D-Bus interface and implementation of the D-Bus support code for connecting to D-Bus
 */
import static org.jajuk.ui.actions.JajukActions.DECREASE_VOLUME;
import static org.jajuk.ui.actions.JajukActions.EXIT;
import static org.jajuk.ui.actions.JajukActions.FORWARD_TRACK;
import static org.jajuk.ui.actions.JajukActions.INCREASE_VOLUME;
import static org.jajuk.ui.actions.JajukActions.NEXT_ALBUM;
import static org.jajuk.ui.actions.JajukActions.NEXT_TRACK;
import static org.jajuk.ui.actions.JajukActions.PAUSE_RESUME_TRACK;
import static org.jajuk.ui.actions.JajukActions.PREVIOUS_ALBUM;
import static org.jajuk.ui.actions.JajukActions.PREVIOUS_TRACK;
import static org.jajuk.ui.actions.JajukActions.REWIND_TRACK;
import static org.jajuk.ui.actions.JajukActions.SHUFFLE_GLOBAL;
import static org.jajuk.ui.actions.JajukActions.STOP_TRACK;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.util.log.Log;

/**
 * 
 */
public class DBusSupportImpl implements DBusSupport {

  /**
   * The D-Bus Path that is used
   */
  private static final String PATH = "/JajukDBus";

  /**
   * The D-Bus name of the Bus that we request
   */
  private static final String BUS = "org.jajuk.dbus.DBusSupport";
  DBusConnection conn;

  /**
   * Set up the D-Bus connection and export an object to allow other applications to control Jajuk via D-Bus. 
   * 
   * This will catch errors and report them to the logfile.
   * 
   */
  public void connect() {
    Log.info("Trying to start support for D-Bus on Linux with Bus: ");
    
    try {
      conn = DBusConnection.getConnection(DBusConnection.SESSION);
      conn.requestBusName(BUS);

      conn.exportObject(PATH, this);
    } catch (DBusException e) {
      Log.error(e);
    }

  }

  /**
   * Disconnects from D-Bus.
   */
  public void disconnect() {
    Log.info("Disconnecting from D-Bus");
    if (conn != null) {
      conn.disconnect();
    }
  }

  
  /**
   * Interface methods to react on D-Bus signals
   * 
   * These methods are invoked via D-Bus and trigger the corresponding action in Jajuk 
   * 
   */
 

  @Override
  public void forward() throws Exception {
    Log.info("Invoking D-Bus action for 'forward'");
    ActionManager.getAction(FORWARD_TRACK).perform(null);
  }

  @Override
  public void next() throws Exception {
    Log.info("Invoking D-Bus action for 'next'");
    ActionManager.getAction(NEXT_TRACK).perform(null);
  }

  @Override
  public void playPause() throws Exception {
    Log.info("Invoking D-Bus action for 'play/pause'");
    ActionManager.getAction(PAUSE_RESUME_TRACK).perform(null);
  }

  @Override
  public void previous() throws Exception {
    Log.info("Invoking D-Bus action for 'previous'");
    ActionManager.getAction(PREVIOUS_TRACK).perform(null);
  }

  @Override
  public void rewind() throws Exception {
    Log.info("Invoking D-Bus action for 'rewind'");
    ActionManager.getAction(REWIND_TRACK).perform(null);
  }

  @Override
  public void stop() throws Exception {
    Log.info("Invoking D-Bus action for 'stop'");
    ActionManager.getAction(STOP_TRACK).perform(null);
  }

  @Override
  public void decreaseVolume() throws Exception {
    Log.info("Invoking D-Bus action for 'decreaseVolume'");
    ActionManager.getAction(DECREASE_VOLUME).perform(null);
  }

  @Override
  public void exit() throws Exception {
    Log.info("Invoking D-Bus action for 'exit'");
    ActionManager.getAction(EXIT).perform(null);
  }

  @Override
  public void increaseVolume() throws Exception {
    Log.info("Invoking D-Bus action for 'increaseVolume'");
    ActionManager.getAction(INCREASE_VOLUME).perform(null);
  }

  @Override
  public void nextAlbum() throws Exception {
    Log.info("Invoking D-Bus action for 'nextAlbum'");
    ActionManager.getAction(NEXT_ALBUM).perform(null);
  }

  @Override
  public void previousAlbum() throws Exception {
    Log.info("Invoking D-Bus action for 'previousAlbum'");
    ActionManager.getAction(PREVIOUS_ALBUM).perform(null);
  }

  @Override
  public void shuffleGlobal() throws Exception {
    Log.info("Invoking D-Bus action for 'shuffleGlobal'");
    ActionManager.getAction(SHUFFLE_GLOBAL).perform(null);
  }

  @Override
  public void mute() throws Exception {
    Log.info("Invoking D-Bus action for 'mute'");
    ActionManager.getAction(JajukActions.MUTE_STATE).perform(null);
  }

  /**
   * Required method for DBusInterface 
   */
  @Override
  public boolean isRemote() {
    return false;
  }
}
