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
 *  $$Revision$$
 */

package org.jajuk.ui.views;

import javax.swing.JMenuItem;

import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.ui.helpers.FilesTableModel;
import org.jajuk.ui.helpers.JajukTableModel;
import org.jajuk.ui.helpers.TwoStepsDisplayable;
import org.jajuk.ui.widgets.JajukTable;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilGUI;

/**
 * Logical table view
 */
public class FilesTableView extends AbstractTableView implements TwoStepsDisplayable {

  private static final long serialVersionUID = 1L;

  private JMenuItem jmiFilePlayDirectory;

  public FilesTableView() {
    super();
    columnsConf = CONF_FILES_TABLE_COLUMNS;
    editableConf = CONF_FILES_TABLE_EDITION;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.IView#getDesc()
   */
  public String getDesc() {
    return Messages.getString("FilesTableView.0");
  }

  public void initUI() {
    UtilGUI.populate(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.helpers.TwoStepsDisplayable#shortCall(java.lang.Object)
   */
  @Override
  public void shortCall(Object in) {
    jtable = new JajukTable(model, true, columnsConf);
    super.shortCall(null);
    // File menu
    jmiFilePlayDirectory = new JMenuItem(ActionManager
        .getAction(JajukActions.PLAY_DIRECTORY_SELECTION));
    jmiFilePlayDirectory.putClientProperty(Const.DETAIL_SELECTION, jtable.getSelection());
    jtable.getMenu().add(jmiFilePlayDirectory, 4);
    // Add this generic menu item manually to ensure it's the last one in
    // the list for GUI reasons
    jtable.getMenu().addSeparator();
    jtable.getMenu().add(pjmTracks);
    jtable.getMenu().add(jmiBookmark);
    jtable.getMenu().addSeparator();
    jtable.getMenu().add(jmiProperties);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.helpers.TwoStepsDisplayable#longCall()
   */
  @Override
  public Object longCall() {
    super.longCall();
    return null;
  }

  /** populate the table */
  @Override
  public synchronized JajukTableModel populateTable() {
    // model creation
    return new FilesTableModel();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.views.AbstractTableView#initTable()
   */
  @Override
  void initTable() {
    boolean bEditable = Conf.getBoolean(Const.CONF_FILES_TABLE_EDITION);
    jtbEditable.setSelected(bEditable);
  }

}
