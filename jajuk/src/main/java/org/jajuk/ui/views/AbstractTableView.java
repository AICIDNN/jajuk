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

import ext.AutoCompleteDecorator;
import ext.SwingWorker;
import info.clearthought.layout.TableLayout;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;

import org.jajuk.base.AuthorManager;
import org.jajuk.base.File;
import org.jajuk.base.Item;
import org.jajuk.base.ItemManager;
import org.jajuk.base.StyleManager;
import org.jajuk.events.Event;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.events.Observer;
import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.ui.helpers.JajukTableModel;
import org.jajuk.ui.helpers.TableTransferHandler;
import org.jajuk.ui.widgets.InformationJPanel;
import org.jajuk.ui.widgets.JajukTable;
import org.jajuk.ui.widgets.JajukToggleButton;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.error.CannotRenameException;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.error.NoneAccessibleFileException;
import org.jajuk.util.log.Log;
import org.jdesktop.swingx.autocomplete.ComboBoxCellEditor;
import org.jdesktop.swingx.decorator.SortOrder;
import org.jdesktop.swingx.table.DefaultTableColumnModelExt;
import org.jdesktop.swingx.table.TableColumnExt;

/**
 * Abstract table view : common implementation for both files and tracks table
 * views
 */
public abstract class AbstractTableView extends ViewAdapter implements ActionListener,
    ItemListener, TableModelListener, Const, Observer {

  JajukTable jtable;

  JPanel jpControl;

  JajukToggleButton jtbEditable;

  JLabel jlFilter;

  JComboBox jcbProperty;

  JLabel jlEquals;

  JTextField jtfValue;

  /** Table model */
  JajukTableModel model;

  /** Currently applied filter */
  String sAppliedFilter = "";

  /** Currently applied criteria */
  String sAppliedCriteria;

  /** Do search panel need a search */
  private boolean bNeedSearch = false;

  /** Default time in ms before launching a search automatically */
  private static final int WAIT_TIME = 300;

  /** Date last key pressed */
  private long lDateTyped;

  /** Editable table configuration name, must be overwritten by child classes */
  String editableConf;

  /**
   * Columns to show table configuration name, must be overwritten by child
   * classes
   */
  String columnsConf;

  JMenuItem jmiPlay;
  JMenuItem jmiPush;
  JMenuItem jmiDelete;
  JMenuItem jmiPlayRepeat;
  JMenuItem jmiPlayShuffle;
  JMenuItem jmiBookmark;
  JMenuItem jmiProperties;

  /**
   * Launches a thread used to perform dynamic filtering when user is typing
   */
  Thread filteringThread = new Thread("Dynamic user input filtering thread") {
    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(200);
        } catch (InterruptedException ie) {
          Log.error(ie);
        }
        if (bNeedSearch && (System.currentTimeMillis() - lDateTyped >= WAIT_TIME)) {
          sAppliedFilter = jtfValue.getText();
          sAppliedCriteria = getApplyCriteria();
          applyFilter(sAppliedCriteria, sAppliedFilter);
          bNeedSearch = false;
        }
      }
    }
  };

  /**
   * 
   * @return Applied criteria
   */
  private String getApplyCriteria() {
    int indexCombo = jcbProperty.getSelectedIndex();
    if (indexCombo == 0) { // first criteria is special: any
      sAppliedCriteria = XML_ANY;
    } else { // otherwise, take criteria from model
      sAppliedCriteria = model.getIdentifier(indexCombo);
    }
    return sAppliedCriteria;
  }

  /**
   * Code used in child class SwingWorker for long delay computations (used in
   * initUI())
   * 
   * @return
   */
  public Object construct() {
    model = populateTable();
    jtable = new JajukTable(model, true, columnsConf);

    // Add generic menus
    jmiPlay = new JMenuItem(ActionManager.getAction(JajukActions.PLAY_SELECTION));
    jmiPlay.putClientProperty(DETAIL_SELECTION, jtable.getSelection());
    jtable.getMenu().add(jmiPlay);

    jmiPush = new JMenuItem(ActionManager.getAction(JajukActions.PUSH_SELECTION));
    jmiPush.putClientProperty(DETAIL_SELECTION, jtable.getSelection());
    jtable.getMenu().add(jmiPush);

    jmiDelete = new JMenuItem(ActionManager.getAction(JajukActions.DELETE));
    jmiDelete.putClientProperty(DETAIL_SELECTION, jtable.getSelection());
    jtable.getMenu().add(jmiDelete);

    jmiPlayRepeat = new JMenuItem(ActionManager.getAction(JajukActions.PLAY_REPEAT_SELECTION));
    jmiPlayRepeat.putClientProperty(DETAIL_SELECTION, jtable.getSelection());
    jtable.getMenu().add(jmiPlayRepeat);

    jmiPlayShuffle = new JMenuItem(ActionManager.getAction(JajukActions.PLAY_SHUFFLE_SELECTION));
    jmiPlayShuffle.putClientProperty(DETAIL_SELECTION, jtable.getSelection());
    jtable.getMenu().add(jmiPlayShuffle);

    jmiBookmark = new JMenuItem(ActionManager.getAction(JajukActions.BOOKMARK_SELECTION));
    jmiBookmark.putClientProperty(DETAIL_SELECTION, jtable.getSelection());

    jmiProperties = new JMenuItem(ActionManager.getAction(JajukActions.SHOW_PROPERTIES));
    jmiProperties.putClientProperty(DETAIL_SELECTION, jtable.getSelection());
    return null;
  }

  /**
   * Code used in child class SwingWorker for display computations (used in
   * initUI())
   * 
   * @return
   */
  public void finished() {
    // Control panel
    jpControl = new JPanel();
    jpControl.setBorder(BorderFactory.createEtchedBorder());
    jtbEditable = new JajukToggleButton(IconLoader.ICON_EDIT);
    jtbEditable.setToolTipText(Messages.getString("AbstractTableView.11"));
    jtbEditable.addActionListener(this);
    jlFilter = new JLabel(Messages.getString("AbstractTableView.0"));
    // properties combo box, fill with columns names expect ID
    jcbProperty = new JComboBox();
    // "any" criteria
    jcbProperty.addItem(Messages.getString("AbstractTableView.8"));
    for (int i = 1; i < model.getColumnCount(); i++) {
      // Others columns except ID
      jcbProperty.addItem(model.getColumnName(i));
    }
    jcbProperty.setToolTipText(Messages.getString("AbstractTableView.1"));
    jcbProperty.addItemListener(this);
    jlEquals = new JLabel(Messages.getString("AbstractTableView.7"));
    jtfValue = new JTextField();
    jtfValue.setBorder(BorderFactory.createLineBorder(Color.BLUE));
    jtfValue.addKeyListener(new KeyAdapter() {
      @Override
      public void keyReleased(KeyEvent e) {
        bNeedSearch = true;
        lDateTyped = System.currentTimeMillis();
      }
    });
    jtfValue.setToolTipText(Messages.getString("AbstractTableView.3"));
    int iXspace = 5;
    double sizeControl[][] = {
        { iXspace, 20, 3 * iXspace, TableLayout.FILL, iXspace, 0.3, TableLayout.FILL,
            TableLayout.FILL, iXspace, 0.3, 2 }, { 5, 25, 5 } };
    TableLayout layout = new TableLayout(sizeControl);
    jpControl.setLayout(layout);
    jpControl.add(jtbEditable, "1,1");
    jpControl.add(jlFilter, "3,1");
    jpControl.add(jcbProperty, "5,1");
    jpControl.add(jlEquals, "7,1");
    jpControl.add(jtfValue, "9,1");
    double size[][] = { { 0.99 }, { TableLayout.PREFERRED, 0.99 } };
    setLayout(new TableLayout(size));
    add(jpControl, "0,0");
    setCellEditors();
    JScrollPane jsp = new JScrollPane(jtable);
    jsp.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 0));
    add(jsp, "0,1");
    jtable.setDragEnabled(true);
    jtable.setTransferHandler(new TableTransferHandler(jtable));
    jtable.showColumns(jtable.getColumnsConf());
    applyFilter(null, null);
    jtable.setSortOrder(1, SortOrder.ASCENDING);
    jtable.setHighlighters(UtilGUI.getAlternateHighlighter());
    jtable.packTable(5);
    // Register on the list for subject we are interested in
    ObservationManager.register(this);
    // refresh columns conf in case of some attributes been removed
    // or added before view instantiation
    Properties properties = ObservationManager
        .getDetailsLastOccurence(JajukEvents.EVENT_CUSTOM_PROPERTIES_ADD);
    Event event = new Event(JajukEvents.EVENT_CUSTOM_PROPERTIES_ADD, properties);
    update(event);
    initTable(); // perform type-specific init
    // Start filtering thread
    filteringThread.start();
    //Register keystrokes
    setKeystrokes();
  }

  public Set<JajukEvents> getRegistrationKeys() {
    Set<JajukEvents> eventSubjectSet = new HashSet<JajukEvents>();
    eventSubjectSet.add(JajukEvents.EVENT_DEVICE_MOUNT);
    eventSubjectSet.add(JajukEvents.EVENT_DEVICE_UNMOUNT);
    eventSubjectSet.add(JajukEvents.EVENT_DEVICE_REFRESH);
    eventSubjectSet.add(JajukEvents.EVENT_SYNC_TREE_TABLE);
    eventSubjectSet.add(JajukEvents.EVENT_CUSTOM_PROPERTIES_ADD);
    eventSubjectSet.add(JajukEvents.EVENT_CUSTOM_PROPERTIES_REMOVE);
    eventSubjectSet.add(JajukEvents.EVENT_RATE_CHANGED);
    eventSubjectSet.add(JajukEvents.EVENT_TABLE_CLEAR_SELECTION);
    eventSubjectSet.add(JajukEvents.EVENT_PARAMETERS_CHANGE);
    eventSubjectSet.add(JajukEvents.EVENT_VIEW_REFRESH_REQUEST);
    return eventSubjectSet;
  }

  /**
   * Apply a filter, to be implemented by files and tracks tables, alter the
   * model
   */
  public void applyFilter(final String sPropertyName, final String sPropertyValue) {
    SwingWorker sw = new SwingWorker() {
      @Override
      public Object construct() {
        model.removeTableModelListener(AbstractTableView.this);
        model.populateModel(sPropertyName, sPropertyValue, jtable.getColumnsConf());
        model.addTableModelListener(AbstractTableView.this);
        model.fireTableDataChanged();
        return null;
      }

      @Override
      public void finished() {
        // Force table repaint (for instance for rating stars update)
        jtable.revalidate();
        jtable.repaint();
        UtilGUI.stopWaiting();
      }
    };
    UtilGUI.waiting();
    sw.start();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.Observer#update(java.lang.String)
   */
  public void update(final Event event) {
    SwingUtilities.invokeLater(new Runnable() {
      @SuppressWarnings("unchecked")
      public void run() {
        try {
          jtable.setAcceptColumnsEvents(false); // flag reloading to avoid wrong
          // column
          // events
          JajukEvents subject = event.getSubject();
          if (JajukEvents.EVENT_TABLE_CLEAR_SELECTION.equals(subject)) {
            jtable.clearSelection();
          }
          if (JajukEvents.EVENT_DEVICE_MOUNT.equals(subject)
              || JajukEvents.EVENT_DEVICE_UNMOUNT.equals(subject)) {
            jtable.clearSelection();
            // force filter to refresh
            applyFilter(sAppliedCriteria, sAppliedFilter);
          } else if (JajukEvents.EVENT_SYNC_TREE_TABLE.equals(subject)) {
            // Consume only events from the same perspective for
            // table/tree synchronization
            if (event.getDetails() != null
                && !(event.getDetails().getProperty(DETAIL_ORIGIN).equals(getPerspective().getID()))) {
              return;
            }
            // Update model tree selection
            model.setTreeSelection((Set<Item>) event.getDetails().get(DETAIL_SELECTION));
            // force redisplay to apply the filter
            jtable.clearSelection();
            // force filter to refresh
            applyFilter(sAppliedCriteria, sAppliedFilter);
          } else if (JajukEvents.EVENT_PARAMETERS_CHANGE.equals(subject)) {
            // force redisplay to apply the filter
            jtable.clearSelection();
            // force filter to refresh
            applyFilter(sAppliedCriteria, sAppliedFilter);
          } else if (JajukEvents.EVENT_DEVICE_REFRESH.equals(subject)) {
            // force filter to refresh
            applyFilter(sAppliedCriteria, sAppliedFilter);
          } else if (JajukEvents.EVENT_VIEW_REFRESH_REQUEST.equals(subject)) {
            // force filter to refresh if the events has been triggered by the
            // table itself after a column change
            JTable table = (JTable) event.getDetails().get(DETAIL_CONTENT);
            if (table.equals(jtable)) {
              applyFilter(sAppliedCriteria, sAppliedFilter);
            }
          } else if (JajukEvents.EVENT_RATE_CHANGED.equals(subject)) {
            // Ignore the refresh if the event comes from the table itself
            Properties properties = event.getDetails();
            if (properties != null && AbstractTableView.this.equals(properties.get(DETAIL_ORIGIN))) {
              return;
            }
            // Keep current selection and nb of rows
            int[] selection = jtable.getSelectedRows();
            // force filter to refresh
            applyFilter(sAppliedCriteria, sAppliedFilter);
            jtable.setSelectedRows(selection);
          } else if (JajukEvents.EVENT_CUSTOM_PROPERTIES_ADD.equals(subject)) {
            Properties properties = event.getDetails();
            if (properties == null) {
              // can be null at view populate
              return;
            }
            model = populateTable();
            model.addTableModelListener(AbstractTableView.this);
            jtable.setModel(model);
            setCellEditors();
            // add new item in configuration cols
            jtable.addColumnIntoConf((String) properties.get(DETAIL_CONTENT));
            jtable.showColumns(jtable.getColumnsConf());
            applyFilter(sAppliedCriteria, sAppliedFilter);
            jcbProperty.addItem(properties.get(DETAIL_CONTENT));
          } else if (JajukEvents.EVENT_CUSTOM_PROPERTIES_REMOVE.equals(subject)) {
            Properties properties = event.getDetails();
            if (properties == null) { // can be null at view
              // populate
              return;
            }
            // remove item from configuration cols
            model = populateTable();// create a new model
            model.addTableModelListener(AbstractTableView.this);
            jtable.setModel(model);
            setCellEditors();
            jtable.addColumnIntoConf((String) properties.get(DETAIL_CONTENT));
            jtable.showColumns(jtable.getColumnsConf());
            applyFilter(sAppliedCriteria, sAppliedFilter);
            jcbProperty.removeItem(properties.get(DETAIL_CONTENT));
          }
        } catch (Exception e) {
          Log.error(e);
        } finally {
          // for unknown reason, we need to repaint the view to refresh table
          // after first time wizard
          revalidate();
          repaint();
          jtable.setAcceptColumnsEvents(true); // make sure to remove this flag
        }
      }
    });
  }
  
   /**
   * Add keystroke support on the tree
   */
  private void setKeystrokes() {
    jtable.putClientProperty(DETAIL_SELECTION, jtable.getSelection());
    InputMap inputMap = jtable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    ActionMap actionMap = jtable.getActionMap();

    // Delete
    Action action = ActionManager.getAction(JajukActions.DELETE);
    inputMap.put(KeyStroke.getKeyStroke("DELETE"), "delete");
    actionMap.put("delete", action);
    
    // Properties ALT/ENTER
    action = ActionManager.getAction(JajukActions.SHOW_PROPERTIES);
    inputMap.put(KeyStroke.getKeyStroke("alt ENTER"), "properties");
    actionMap.put("properties", action);
  }

  /** Fill the table */
  abstract JajukTableModel populateTable();

  private void setCellEditors() {
    for (TableColumn tc : ((DefaultTableColumnModelExt) jtable.getColumnModel()).getColumns(true)) {
      TableColumnExt col = (TableColumnExt) tc;
      String sIdentifier = model.getIdentifier(col.getModelIndex());
      // create a combo box for styles, note that we can't add new
      // styles dynamically
      if (XML_STYLE.equals(sIdentifier)) {
        JComboBox jcb = new JComboBox(StyleManager.getInstance().getStylesList());
        jcb.setEditable(true);
        AutoCompleteDecorator.decorate(jcb);
        col.setCellEditor(new ComboBoxCellEditor(jcb));
        col.setSortable(true);
      }
      // create a combo box for authors, note that we can't add new
      // authors dynamically
      if (XML_AUTHOR.equals(sIdentifier)) {
        JComboBox jcb = new JComboBox(AuthorManager.getAuthorsList());
        jcb.setEditable(true);
        AutoCompleteDecorator.decorate(jcb);
        col.setCellEditor(new ComboBoxCellEditor(jcb));
      }
      // create a button for playing
      else if (XML_PLAY.equals(sIdentifier)) {
        col.setMinWidth(PLAY_COLUMN_SIZE);
        col.setMaxWidth(PLAY_COLUMN_SIZE);
      } else if (XML_TRACK_RATE.equals(sIdentifier)) {
        col.setMinWidth(RATE_COLUMN_SIZE);
        col.setMaxWidth(RATE_COLUMN_SIZE);
      }
    }
  }

  /**
   * Detect property change
   */
  public void itemStateChanged(ItemEvent ie) {
    if (ie.getSource() == jcbProperty) {
      sAppliedFilter = jtfValue.getText();
      sAppliedCriteria = getApplyCriteria();
      applyFilter(sAppliedCriteria, sAppliedFilter);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
   */
  public void tableChanged(TableModelEvent e) {
    // Check the table change event has not been generated by a
    // fireModelDataChange call
    if (e.getColumn() < 0) {
      return;
    }
    String sKey = model.getIdentifier(e.getColumn());
    Object oValue = model.getValueAt(e.getFirstRow(), e.getColumn());
    /* can be Boolean or String */
    Item item = model.getItemAt(e.getFirstRow());
    try {
      // file filter used by physical table view to change only the
      // file, not all files associated with the track
      Set<File> filter = null;
      if (item instanceof File) {
        filter = new HashSet<File>();
        filter.add((File)item);
      }
      Item itemNew = ItemManager.changeItem(item, sKey, oValue, filter);
      model.setItemAt(e.getFirstRow(), itemNew); // update model
      // user message
      InformationJPanel.getInstance().setMessage(
          Messages.getString("PropertiesWizard.8") + ": " + ItemManager.getHumanType(sKey),
          InformationJPanel.INFORMATIVE);
      // Require refresh of all tables
      Properties properties = new Properties();
      properties.put(DETAIL_ORIGIN, AbstractTableView.this);
      ObservationManager.notify(new Event(JajukEvents.EVENT_RATE_CHANGED, properties));

    } catch (NoneAccessibleFileException none) {
      Messages.showErrorMessage(none.getCode());
      ((JajukTableModel) jtable.getModel()).undo(e.getFirstRow(), e.getColumn());
    } catch (CannotRenameException cre) {
      Messages.showErrorMessage(cre.getCode());
      ((JajukTableModel) jtable.getModel()).undo(e.getFirstRow(), e.getColumn());
    } catch (JajukException je) {
      Log.error("104", je);
      Messages.showErrorMessage(104, je.getMessage());
      ((JajukTableModel) jtable.getModel()).undo(e.getFirstRow(), e.getColumn());
    }
  }

  /**
   * Table initialization after table display
   * 
   */
  abstract void initTable();

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(final ActionEvent e) {
    // Editable state
    if (e.getSource() == jtbEditable) {
      Conf.setProperty(editableConf, Boolean.toString(jtbEditable.isSelected()));
      model.setEditable(jtbEditable.isSelected());
      return;
    }

  }

}
