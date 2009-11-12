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
 *  $$Revision$$
 */
package org.jajuk.ui.wizard;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.miginfocom.swing.MigLayout;

import org.jajuk.base.Style;
import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.services.dj.Ambience;
import org.jajuk.services.dj.AmbienceDigitalDJ;
import org.jajuk.services.dj.AmbienceManager;
import org.jajuk.ui.helpers.FontManager;
import org.jajuk.ui.helpers.FontManager.JajukFont;
import org.jajuk.ui.windows.JajukMainWindow;
import org.jajuk.util.Conf;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.LocaleManager;
import org.jajuk.util.Messages;
import org.jajuk.util.log.Log;
import org.qdwizard.Screen;
import org.qdwizard.Wizard;

/**
 * Ambiences management wizard
 */
public class AmbienceWizard extends Wizard {

  public static class AmbiencePanel extends Screen implements ActionListener {

    private static final long serialVersionUID = 1L;

    /** All dynamic widgets */
    JComponent[][] widgets;

    JButton jbNew;

    JButton jbDelete;

    JButton jbDefaults;

    /** DJ* */
    AmbienceDigitalDJ dj = null;

    /** Selected ambience index */
    int ambienceIndex = 0;

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(final ActionEvent ae) {
      if (ae.getSource() == jbNew) {
        // create a void ambience
        AmbienceWizard.ambiences.add(new Ambience(Long.toString(System.currentTimeMillis()), ""));
        Collections.sort(AmbienceWizard.ambiences);
        // refresh screen
        refreshScreen();
        // select new row
        final JRadioButton jrb = (JRadioButton) widgets[AmbienceWizard.ambiences.size() - 1][0];
        jrb.setSelected(true);
        ambienceIndex = AmbienceWizard.ambiences.size() - 1;
        setProblem(Messages.getString("DigitalDJWizard.39"));
        jbNew.setEnabled(false);
        jbDelete.setEnabled(true);
        final JTextField jtf = (JTextField) widgets[ambienceIndex][1];
        jtf.requestFocusInWindow();
      } else if (ae.getSource() == jbDelete) {
        final Ambience ambience = AmbienceWizard.ambiences.get(ambienceIndex);
        AmbienceWizard.ambiences.remove(ambience);
        AmbienceManager.getInstance().removeAmbience(ambience.getID());
        if (AmbienceManager.getInstance().getAmbiences().size() == 0) {
          jbDelete.setEnabled(false);
        }
        if (ambienceIndex > 0) {
          ambienceIndex--;
          final JRadioButton jrb = (JRadioButton) widgets[ambienceIndex][0];
          jrb.setSelected(true);
        }
        // refresh screen
        refreshScreen();
      } else if (ae.getSource() == jbDefaults) {
        AmbienceManager.getInstance().createDefaultAmbiences();
        AmbienceWizard.ambiences = new ArrayList<Ambience>(AmbienceManager.getInstance()
            .getAmbiences());
        Collections.sort(AmbienceWizard.ambiences);
        // refresh screen
        refreshScreen();
      }
      // in all cases, notify command panel
      ObservationManager.notify(new JajukEvent(JajukEvents.AMBIENCES_CHANGE));
    }

    /**
     * Add a style to a proportion
     * 
     * @param row
     *          row
     */
    private void addStyle(final int row) {
      final Ambience ambience = AmbienceWizard.ambiences.get(row);
      // create list of styles used in current selection
      final StylesSelectionDialog dialog = new StylesSelectionDialog(null);
      dialog.setSelection(ambience.getStyles());
      dialog.setVisible(true);
      final Set<Style> styles = dialog.getSelectedStyles();
      // check if at least one style has been selected
      if (styles.size() == 0) {
        return;
      }
      StringBuilder sText = new StringBuilder();
      // reset old styles
      ambience.setStyles(new HashSet<Style>(10));
      for (final Style style : styles) {
        ambience.addStyle(style);
        sText.append(style.getName2()).append(',');
      }
      sText.deleteCharAt(sText.length() - 1);
      // Set button text
      ((JButton) widgets[row][2]).setText(sText.toString());
      // if we have ambience name and some styles, register the
      // ambience
      if ((ambience.getName().length() > 0) && (ambience.getStyles().size() > 0)) {
        // no more error message if at least one ambience
        setProblem(null);
        jbNew.setEnabled(true);
      }
    }

    @Override
    public String getDescription() {
      return Messages.getString("DigitalDJWizard.47");
    }

    @Override
    public String getName() {
      return Messages.getString("DigitalDJWizard.57");
    }

    /**
     * 
     * @return a panel containing all items
     */
    private JScrollPane getPanel() {
      widgets = new JComponent[AmbienceWizard.ambiences.size()][3];
      final JPanel out = new JPanel();
      // Delete|Style name|styles list
      final ButtonGroup group = new ButtonGroup();
      // now add all ambiences
      for (int index = 0; index < AmbienceWizard.ambiences.size(); index++) {
        // Ambience name
        final JTextField jtfName = new JTextField();
        jtfName.setText(AmbienceWizard.ambiences.get(index).getName());
        jtfName.addCaretListener(new CaretListener() {
          public void caretUpdate(final CaretEvent arg0) {
            final int index = AmbienceWizard.getWidgetIndex(widgets, (JComponent) arg0.getSource());
            final String s = jtfName.getText();
            // Check this name is not already token
            for (int i = 0; i < widgets.length; i++) {
              if (i == index) {
                continue;
              }
              final JTextField jtf = (JTextField) widgets[i][1];
              if (jtf.getText().equals(s)) {
                setProblem(Messages.getString("DigitalDJWizard.60"));
                return;
              }
            }
            // reset previous problems
            if ((s.length() == 0) || (((JButton) widgets[index][2]).getText().length() == 0)) {
              setProblem(Messages.getString("DigitalDJWizard.39"));
            } else {
              setProblem(null);
            }
            final JButton jb = (JButton) widgets[index][2];
            final Ambience ambience = AmbienceWizard.ambiences.get(index);
            ambience.setName(s);
            jb.setEnabled(s.length() > 0);
          }
        });
        jtfName.setToolTipText(Messages.getString("DigitalDJWizard.36"));
        widgets[index][1] = jtfName;
        // radio button
        final JRadioButton jrbAmbience = new JRadioButton();
        group.add(jrbAmbience);
        jrbAmbience.addActionListener(new ActionListener() {
          public void actionPerformed(final ActionEvent ae) {
            ((JTextField) widgets[AmbienceWizard.getWidgetIndex(widgets, jrbAmbience)][1])
                .getText();
            ambienceIndex = AmbienceWizard.getWidgetIndex(widgets, jrbAmbience);
          }
        });
        widgets[index][0] = jrbAmbience;
        if (index == ambienceIndex) {
          jrbAmbience.setSelected(true);
        }
        final Ambience ambience = AmbienceWizard.ambiences.get(index);
        // style list
        final JButton jbStyle = new JButton(IconLoader.getIcon(JajukIcons.STYLE));
        if (ambience.getName().length() == 0) {
          jbStyle.setEnabled(false);
        }
        if ((ambience.getStyles() != null) && (ambience.getStyles().size() > 0)) {
          jbStyle.setText(ambience.getStylesDesc());
          jbStyle.setToolTipText(ambience.getStylesDesc());
        }
        jbStyle.addActionListener(new ActionListener() {
          public void actionPerformed(final ActionEvent ae) {
            final int row = AmbienceWizard.getWidgetIndex(widgets, (JComponent) ae.getSource());
            addStyle(row);
            // refresh ambience (force an action event)
            final JRadioButton jrb = (JRadioButton) widgets[row][0];
            jrb.doClick();
          }
        });
        jbStyle.setToolTipText(Messages.getString("DigitalDJWizard.27"));
        widgets[index][2] = jbStyle;
      }
      // Create layout
      out.setLayout(new MigLayout("insets 5,gapx 5", "[][][grow]"));
      // Create header
      final JLabel jlHeader1 = new JLabel(Messages.getString("DigitalDJWizard.37"));
      jlHeader1.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
      final JLabel jlHeader2 = new JLabel(Messages.getString("DigitalDJWizard.27"));
      jlHeader2.setFont(FontManager.getInstance().getFont(JajukFont.BOLD));
      out.add(jlHeader1, "center,span 2");
      out.add(jlHeader2, "center,wrap");
      // Add widgets
      for (int i = 0; i < widgets.length; i++) {
        out.add(widgets[i][0], "grow,center,width 25!");
        out.add(widgets[i][1], "grow,center,width 120!");
        out.add(widgets[i][2], "center,grow,wrap,width 270:270");
      }
      final JScrollPane jsp = new JScrollPane(out);
      // select first ambiance found
      if (AmbienceWizard.ambiences.size() > 0) {
        final JRadioButton jrb = (JRadioButton) widgets[0][0];
        jrb.doClick();
      }
      return jsp;
    }

    /**
     * Create panel UI
     * 
     */
    @Override
    public void initUI() {
      AmbienceWizard.ambiences = new ArrayList<Ambience>(AmbienceManager.getInstance()
          .getAmbiences());
      Collections.sort(AmbienceWizard.ambiences);
      setCanFinish(true);
      jbNew = new JButton(Messages.getString("DigitalDJWizard.32"), IconLoader
          .getIcon(JajukIcons.NEW));
      jbNew.addActionListener(this);
      jbNew.setToolTipText(Messages.getString("DigitalDJWizard.33"));
      jbDelete = new JButton(Messages.getString("DigitalDJWizard.34"), IconLoader
          .getIcon(JajukIcons.DELETE));
      jbDelete.addActionListener(this);
      jbDelete.setToolTipText(Messages.getString("DigitalDJWizard.35"));
      jbDefaults = new JButton(Messages.getString("DigitalDJWizard.62"), IconLoader
          .getIcon(JajukIcons.DEFAULTS));
      jbDefaults.addActionListener(this);
      jbDefaults.setToolTipText(Messages.getString("DigitalDJWizard.63"));

      // Add items
      refreshScreen();
    }

    /**
     * Refresh panel
     */
    private void refreshScreen() {
      setLayout(new MigLayout("insets 5,gapy 15", "[center,grow]"));
      removeAll();
      // refresh panel
      add(getPanel(), "grow,wrap");
      add(jbNew, "split 3");
      add(jbDelete);
      add(jbDefaults);
      revalidate();
      repaint();
    }
  }

  /** Ambiences* */
  static List<Ambience> ambiences;

  /**
   * 
   * @param widget
   * @return index of a given widget row in the widget table
   */
  private static int getWidgetIndex(final JComponent[][] widgets, final JComponent widget) {
    for (int row = 0; row < widgets.length; row++) {
      for (int col = 0; col < widgets[0].length; col++) {
        if (widget.equals(widgets[row][col])) {
          return row;
        }
      }
    }
    return -1;
  }

  public AmbienceWizard() {
    super(Messages.getString("DigitalDJWizard.56"), AmbiencePanel.class, null, JajukMainWindow
        .getInstance(), LocaleManager.getLocale(), 500, 600);
    setHeaderIcon(IconLoader.getIcon(JajukIcons.AMBIENCE));
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.wizard.Wizard#finish()
   */
  @Override
  public void finish() {
    for (final Ambience ambience : AmbienceWizard.ambiences) {
      AmbienceManager.getInstance().registerAmbience(ambience);
    }
    // commit it to avoid it is lost before the app close
    AmbienceManager.getInstance().commit();
    try {
      Conf.commit();
    } catch (final Exception e) {
      Log.error(113, e);
      Messages.showErrorMessage(113);
    }
    // Refresh UI
    ObservationManager.notify(new JajukEvent(JajukEvents.AMBIENCES_CHANGE));

  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.wizard.Wizard#getNextScreen(java.lang.Class)
   */
  @Override
  public Class<? extends org.qdwizard.Screen> getNextScreen(
      final Class<? extends org.qdwizard.Screen> screen) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.wizard.Wizard#getPreviousScreen(java.lang.Class)
   */
  @Override
  public Class<? extends org.qdwizard.Screen> getPreviousScreen(
      final Class<? extends org.qdwizard.Screen> screen) {
    return null;
  }

}
