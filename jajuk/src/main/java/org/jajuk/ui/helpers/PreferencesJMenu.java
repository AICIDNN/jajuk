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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jajuk.base.Item;
import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.ui.helpers.FontManager.JajukFont;
import org.jajuk.util.Const;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilFeatures;

/**
 * Preference menu item including ban/unban and preference choice
 */
public class PreferencesJMenu extends JMenu {

  private static final long serialVersionUID = -4041513018474249903L;

  private JMenuItem jmiBan;
  private JMenuItem jmiUnBan;
  private JMenuItem jmiAdore;
  private JMenuItem jmiLove;
  private JMenuItem jmiLike;
  private JMenuItem jmiUnset;
  private JMenuItem jmiAverage;
  private JMenuItem jmiPoor;
  private JMenuItem jmiHate;

  /**
   * Constructs a Preference menu
   * 
   * @param item
   */
  public PreferencesJMenu(Item item) {
    super(Messages.getString("Preference.7"));
    List<Item> selection = new ArrayList<Item>(1);
    selection.add(item);
    initUI(selection);
  }

  /**
   * Constructs a Preference menu
   * 
   * @param selection
   *          selection list bound to the actions
   */
  public PreferencesJMenu(List<? extends Item> selection) {
    super(Messages.getString("Preference.7"));
    initUI(selection);
  }

  /**
   * initUI
   * 
   * @param selection
   */
  private void initUI(List<? extends Item> selection) {
    // We compute preference of first item in selection to set right
    // item bold font
    long selectionPreference = UtilFeatures.getPreferenceForSelection(selection);

    jmiBan = new JMenuItem(ActionManager.getAction(JajukActions.BAN_SELECTION));
    jmiBan.putClientProperty(Const.DETAIL_SELECTION, selection);

    jmiUnBan = new JMenuItem(ActionManager.getAction(JajukActions.UN_BAN_SELECTION));
    jmiUnBan.putClientProperty(Const.DETAIL_SELECTION, selection);

    jmiAdore = new JMenuItem(ActionManager.getAction(JajukActions.PREFERENCE_ADORE));
    jmiAdore.putClientProperty(Const.DETAIL_SELECTION, selection);
    if (selectionPreference == Const.PREFERENCE_ADORE) {
      jmiAdore.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
    }

    jmiLove = new JMenuItem(ActionManager.getAction(JajukActions.PREFERENCE_LOVE));
    jmiLove.putClientProperty(Const.DETAIL_SELECTION, selection);
    if (selectionPreference == Const.PREFERENCE_LOVE) {
      jmiLove.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
    }

    jmiLike = new JMenuItem(ActionManager.getAction(JajukActions.PREFERENCE_LIKE));
    jmiLike.putClientProperty(Const.DETAIL_SELECTION, selection);
    if (selectionPreference == Const.PREFERENCE_LIKE) {
      jmiLike.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
    }

    jmiUnset = new JMenuItem(ActionManager.getAction(JajukActions.PREFERENCE_UNSET));
    jmiUnset.putClientProperty(Const.DETAIL_SELECTION, selection);
    if (selectionPreference == Const.PREFERENCE_UNSET) {
      jmiUnset.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
    }

    jmiAverage = new JMenuItem(ActionManager.getAction(JajukActions.PREFERENCE_AVERAGE));
    jmiAverage.putClientProperty(Const.DETAIL_SELECTION, selection);
    if (selectionPreference == Const.PREFERENCE_AVERAGE) {
      jmiAverage.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
    }

    jmiPoor = new JMenuItem(ActionManager.getAction(JajukActions.PREFERENCE_POOR));
    jmiPoor.putClientProperty(Const.DETAIL_SELECTION, selection);
    if (selectionPreference == Const.PREFERENCE_POOR) {
      jmiPoor.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
    }

    jmiHate = new JMenuItem(ActionManager.getAction(JajukActions.PREFERENCE_HATE));
    jmiHate.putClientProperty(Const.DETAIL_SELECTION, selection);
    if (selectionPreference == Const.PREFERENCE_HATE) {
      jmiHate.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
    }

    add(jmiBan);
    add(jmiUnBan);
    addSeparator();
    add(jmiAdore);
    add(jmiLove);
    add(jmiLike);
    add(jmiUnset);
    add(jmiAverage);
    add(jmiPoor);
    add(jmiHate);
  }

  /**
   * Repaint the preference popup menu. This is a workaround for the listener
   * concurrency issue between mouse and selection listener in tables. When 
   * a table selection change, the view that contains the table calls this method
   * in valueChanged() method as we can't build a popup menu in mouse adapter methods
   * because the selection is not always yet set 
   * 
   * @param selection
   */
  public synchronized void resetUI(List<? extends Item> selection) {
    removeAll();
    initUI(selection);
  }

}
