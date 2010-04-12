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

package org.jajuk.ui.wizard;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;

import org.jajuk.base.Device;
import org.jajuk.base.DeviceManager;
import org.jajuk.services.core.SessionService;
import org.jajuk.services.startup.StartupCollectionService;
import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.ui.widgets.JajukFileChooser;
import org.jajuk.ui.widgets.JajukJDialog;
import org.jajuk.ui.widgets.OKCancelPanel;
import org.jajuk.ui.widgets.PathSelector;
import org.jajuk.ui.widgets.ToggleLink;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukFileFilter;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilGUI;
import org.jajuk.util.UtilSystem;
import org.jajuk.util.filters.DirectoryFilter;
import org.jajuk.util.log.Log;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.VerticalLayout;

/**
 * First time Wizard.
 */
public class FirstTimeWizard extends JajukJDialog implements ActionListener, PropertyChangeListener {
  
  /** Generated serialVersionUID. */
  private static final long serialVersionUID = 1L;

  /** DOCUMENT_ME. */
  private JLabel jlLeftIcon;

  /** DOCUMENT_ME. */
  private JPanel jpRightPanel;

  /** DOCUMENT_ME. */
  private JButton jbFileSelection;

  /** DOCUMENT_ME. */
  private JLabel jlSelectedFile;

  /** DOCUMENT_ME. */
  private PathSelector workspacePath;

  /** DOCUMENT_ME. */
  private JLabel jlRefreshTime;

  /** DOCUMENT_ME. */
  private JTextField jtfRefreshTime;

  /** DOCUMENT_ME. */
  private JCheckBox jcbHelp;

  /** DOCUMENT_ME. */
  private JXCollapsiblePane advanced;

  /** DOCUMENT_ME. */
  private JButton jbOk;

  /** DOCUMENT_ME. */
  private JButton jbCancel;

  /** DOCUMENT_ME. */
  private JPanel jpMain;

  /** Selected directory. */
  private File fDir;

  /**
   * First time wizard.
   */
  public FirstTimeWizard() {
    super();

    initUI();
    pack();
    final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    setLocation(((int) dim.getWidth() / 3), ((int) dim.getHeight() / 3));
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    setVisible(true);
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  public void actionPerformed(final ActionEvent e) {
    if (e.getSource() == jbCancel) {
      dispose(); // close window
      // alert Main to continue startup
      SessionService.notifyFirstTimeWizardClosed();
    } else if (e.getSource() == jbFileSelection) {
      final JajukFileChooser jfc = new JajukFileChooser(new JajukFileFilter(DirectoryFilter
          .getInstance()));
      jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      jfc.setDialogTitle(Messages.getString("FirstTimeWizard.5"));
      jfc.setMultiSelectionEnabled(false);
      final int returnVal = jfc.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        fDir = jfc.getSelectedFile();
        // First, check device availability
        int code = DeviceManager.getInstance().checkDeviceAvailablity(fDir.getName(), 0,
            fDir.getAbsolutePath(), true);
        if (code != 0) {
          Messages.showErrorMessage(code);
          jbOk.setEnabled(false);
          return;
        }
        jbOk.setEnabled(true);
        jbOk.grabFocus();

        jlSelectedFile.setText(fDir.getAbsolutePath());
        pack(); // repack as size of dialog can be exceeded now
      }
    } else if (e.getSource() == jbOk) {
      final boolean bShowHelp = jcbHelp.isSelected();
      final String sPATH = workspacePath.getUrl().trim();
      // Check workspace directory
      if ((!sPATH.isEmpty()) && (!new File(sPATH).canRead())) {
        Messages.showErrorMessage(165);
        return;
      }
      // Set Workspace directory
      try {
        final java.io.File bootstrap = new java.io.File(SessionService.getBootstrapPath());
        final Writer bw = new BufferedWriter(new FileWriter(bootstrap));
        try {
          bw.write(sPATH);
          bw.flush();
        } finally {
          bw.close();
        }
        // Store the workspace PATH
        SessionService.setWorkspace(sPATH);
      } catch (final IOException ex) {
        Messages.showErrorMessage(24);
        Log.debug("Cannot write bootstrap file");
      }
      // Close window
      dispose();
      // Notify Main to continue startup
      SessionService.notifyFirstTimeWizardClosed();
      new Thread("First Time Wizard Action Thread") {
        @Override
        public void run() {
          // Wait for context loading (default configuration...)
          StartupCollectionService.waitForLaunchRefresh();

          // Create a directory device
          final Device device = DeviceManager.getInstance().registerDevice(fDir.getName(), 0,
              fDir.getAbsolutePath());
          device.setProperty(Const.XML_DEVICE_AUTO_MOUNT, true);
          // Set refresh time
          double dRefreshTime = 5d;
          try {
            dRefreshTime = Double.parseDouble(jtfRefreshTime.getText());
            if (dRefreshTime < 0) {
              dRefreshTime = 0;
            }
          } catch (final NumberFormatException e1) {
            dRefreshTime = 0;
          }
          device.setProperty(Const.XML_DEVICE_AUTO_REFRESH, dRefreshTime);
          try {
            // Refresh device synchronously
            device.refresh(false, false, false);
          } catch (final Exception e2) {
            Log.error(112, device.getName(), e2);
            Messages.showErrorMessage(112, device.getName());
          }
          // Show Help window if required
          if (bShowHelp) {
            // Display help window
            try {
              ActionManager.getAction(JajukActions.HELP_REQUIRED).perform(null);
            } catch (Exception e1) {
              Log.error(e1);
            }
          }
        }
      }.start();
    }
  }

  /**
   * Inits the ui.
   * DOCUMENT_ME
   */
  private void initUI() {
    setTitle(Messages.getString("FirstTimeWizard.0"));
    jlLeftIcon = new JLabel(UtilGUI.getImage(Const.IMAGE_SEARCH));
    jlLeftIcon.setBorder(new EmptyBorder(0, 20, 0, 0));
    jpRightPanel = new JPanel();
    JLabel jlWelcome = new JLabel(Messages.getString("FirstTimeWizard.1"));
    JLabel jlFileSelection = new JLabel(Messages.getString("FirstTimeWizard.2"));
    jbFileSelection = new JButton(IconLoader.getIcon(JajukIcons.OPEN_DIR));
    jbFileSelection.addActionListener(this);

    JLabel jlSelectedFileText = new JLabel(Messages.getString("FirstTimeWizard.8"));
    jlSelectedFile = new JLabel(Messages.getString("FirstTimeWizard.9"));
    jlSelectedFile.setBorder(new BevelBorder(BevelBorder.LOWERED));

    final JLabel jlWorkspace = new JLabel(Messages.getString("FirstTimeWizard.7"));
    jlWorkspace.setToolTipText(Messages.getString("FirstTimeWizard.7"));
    workspacePath = new PathSelector(UtilSystem.getUserHome());
    workspacePath.setToolTipText(Messages.getString("FirstTimeWizard.7"));

    jcbHelp = new JCheckBox(Messages.getString("FirstTimeWizard.4"));
    // Refresh time
    jlRefreshTime = new JLabel(Messages.getString("DeviceWizard.53"));
    jtfRefreshTime = new JTextField("5");// 5 mins by default
    JLabel jlMins = new JLabel(Messages.getString("DeviceWizard.54"));

    // buttons
    OKCancelPanel okp = new OKCancelPanel(this);
    jbOk = okp.getOKButton();
    jbCancel = okp.getCancelButton();
    jbCancel.setText(Messages.getString("Later"));
    jbOk.setEnabled(false);
    advanced = new JXCollapsiblePane();

    // we need to listen for the animation state property in order to allow to
    // resize the dialog after the advanced-panel is expanded/collapsed
    // see http://forums.java.net/jive/thread.jspa?threadID=67800&tstart=0 for some related discussion
    // why we need to listen on "animationState" to know when the expanding/collapsing is finished
    advanced.addPropertyChangeListener("animationState", this); 

    // Build the toggle link used to expand / collapse the panel
    final ToggleLink toggle = new ToggleLink(Messages.getString("FirstTimeWizard.6"), advanced);

    // Advanced collapsible panel
    advanced.setLayout(new VerticalLayout(10));
    advanced.setCollapsed(true);
    advanced.add(jlWorkspace);
    advanced.add(workspacePath);
    advanced.add(jcbHelp);

    // Add items
    setLayout(new MigLayout("insets 10,gapx 10,gapy 15", "[][grow]", "[][][][][][]"));
    add(jlLeftIcon, "cell 0 0 1 6,top");
    add(jlWelcome, "cell 1 0");
    add(jlFileSelection, "split 2,cell 1 1");
    add(jbFileSelection, "cell 1 1");
    add(jlSelectedFileText, "split 2,cell 1 2");
    add(jlSelectedFile, "cell 1 2, grow");
    add(jlRefreshTime, "split 3,cell 1 3");
    add(jtfRefreshTime, "cell 1 3, grow,width ::50");
    add(jlMins, "cell 1 3");
    add(toggle, "cell 1 4,grow");
    add(advanced, "cell 1 5,grow");
    add(okp, "right,span,cell 1 6");

    getRootPane().setDefaultButton(jbOk);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent
   * )
   */
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    // if the property changes to "collapsed" or "expanded" the change of the panel is
    // finished and we should re-pack() the dialog in order to make space for the panel in the dialog correctly
    if (evt.getNewValue().equals("collapsed") || evt.getNewValue().equals("expanded")) {
      pack(); // repack as size of dialog can be exceeded now
    }
  }
}
