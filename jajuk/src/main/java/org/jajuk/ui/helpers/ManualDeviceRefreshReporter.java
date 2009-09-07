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
package org.jajuk.ui.helpers;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jajuk.base.Device;
import org.jajuk.base.Directory;
import org.jajuk.ui.wizard.RefreshDialog;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.log.Log;

/**
 * Refresh reporter with GUI special operations
 */
public class ManualDeviceRefreshReporter extends RefreshReporter {

  // Refresh dialog
  private RefreshDialog rdialog;

  private int progress;

  public ManualDeviceRefreshReporter(Device device) {
    super(device);
  }

  @Override
  public void startup() {
    super.startup();
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        // if <0 directories count -> the progress bar is in indeterminate state
        rdialog = new RefreshDialog((dirTotal < 0));
        rdialog.setTitle(Messages.getString("RefreshDialog.2") + " " + device.getName());
        rdialog.setAction(Messages.getString("RefreshDialog.3"), IconLoader
            .getIcon(JajukIcons.INFO));
      }
    });
  }

  @Override
  public void reset() {
    super.reset();
    this.progress = 0;

  }

  @Override
  public void cleanupDone() {
    // We estimate that cleanup represents about 20% of the total workload
    rdialog.setProgress(20);
    rdialog
        .setAction(Messages.getString("RefreshDialog.1"), IconLoader.getIcon(JajukIcons.REFRESH));
    // Update counter only if final directory count is known
    if (dirTotal > 0) {
      updateDialogTitle.start();
    }
  }

  @Override
  public void updateState(Directory dir) {
    if (rdialog != null) {
      rdialog.setRefreshing(new StringBuilder(Messages.getString("Device.44")).append(' ').append(
          dir.getRelativePath()).toString());
      progress = 30 + (int) (70 * (float) dirCount / dirTotal);
      if (progress > 100) {
        progress = 100;
      }
      rdialog.setProgress(progress);
    }
    dirCount++;
  }

  @Override
  public void done() {
    long refreshTime = System.currentTimeMillis() - lRefreshDateStart;
    String message = buildFinalMessage(refreshTime);
    Log.debug(message);
    reset();
    // Close refresh dialog
    rdialog.dispose();
    // Close title timer
    updateDialogTitle.stop();
    // Display end of refresh message with stats
    Messages.showInfoMessage(message);
  }

  /**
   * This timer limit dialog title changes (this can have side effect on
   * performances or other in some window managers. Too many window title change
   * causes others menu bar items freezes under KDE for ie)
   */
  Timer updateDialogTitle = new Timer(1000, new ActionListener() {

    public void actionPerformed(ActionEvent e) {
      String sTitle = Messages.getString("RefreshDialog.2") + " " + device.getName() + " ("
          + progress + " %)";
      if (!sTitle.equals(rdialog.getTitle())) {
        rdialog.setTitle(sTitle);
      }
    }

  });

}
