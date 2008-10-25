/*
 *  Jajuk
 *  Copyright (C) 2005 The Jajuk Team
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
 *  $$Revision: 4113 $$
 */
package org.jajuk.ui.actions;

import java.awt.event.ActionEvent;

import org.jajuk.base.File;
import org.jajuk.base.Track;
import org.jajuk.services.players.FIFO;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;

public class BanCurrentAction extends SelectionAction {

  private static final long serialVersionUID = 1L;

  /**
   * Ban current track. The Ban action is used to ban a track so it is never
   * selected
   */
  BanCurrentAction() {
    super(Messages.getString("BanSelectionAction.0"), IconLoader.getIcon(JajukIcons.BAN), true);
    setShortDescription(Messages.getString("BanSelectionAction.1"));
  }

  @Override
  public void perform(ActionEvent e) throws Exception {
    File current = FIFO.getCurrentFile();
    if (current != null) {
      Track track = current.getTrack();
      track.setProperty(Const.XML_TRACK_BANNED, true);
    }
  }
}