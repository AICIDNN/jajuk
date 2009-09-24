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

package org.jajuk.ui.views;

import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import net.miginfocom.swing.MigLayout;

import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.services.players.QueueModel;
import org.jajuk.ui.actions.ActionManager;
import org.jajuk.ui.actions.JajukAction;
import org.jajuk.ui.actions.JajukActions;
import org.jajuk.ui.widgets.JajukHtmlPanel;
import org.jajuk.ui.widgets.JajukJToolbar;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.IconLoader;
import org.jajuk.util.JajukIcons;
import org.jajuk.util.LocaleManager;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilFeatures;
import org.jajuk.util.log.Log;

/**
 * Wikipedia view
 */
public class WikipediaView extends ViewAdapter implements ActionListener {

  private static final long serialVersionUID = 1L;

  JLabel jlLanguage;

  JComboBox jcbLanguage;

  /** Cobra web browser */
  JajukHtmlPanel browser;

  JButton jbCopy;

  JButton jbLaunchInExternalBrowser;

  JToggleButton jbAuthorSearch;

  JToggleButton jbAlbumSearch;

  JToggleButton jbTrackSearch;

  /** Language index */
  int indexLang = 0;

  enum Type {
    AUTHOR, ALBUM, TRACK
  }

  Type type = Type.AUTHOR;

  /** Current search */
  String search = null;

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.views.IView#getDesc()
   */
  public String getDesc() {
    return Messages.getString("WikipediaView.0");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.views.IView#populate()
   */
  public void initUI() {
    jlLanguage = new JLabel(Messages.getString("WikipediaView.1"));
    jcbLanguage = new JComboBox();
    for (String sDesc : LocaleManager.getLocalesDescs()) {
      jcbLanguage.addItem(sDesc);
    }
    // get stored language
    jcbLanguage.setSelectedItem(LocaleManager.getDescForLocale(Conf
        .getString(Const.CONF_WIKIPEDIA_LANGUAGE)));
    jcbLanguage.addActionListener(this);
    // Buttons
    JajukAction aCopy = ActionManager.getAction(JajukActions.COPY_TO_CLIPBOARD);
    jbCopy = new JButton(aCopy);
    if (Desktop.isDesktopSupported()) {
      jbLaunchInExternalBrowser = new JButton(ActionManager
          .getAction(JajukActions.LAUNCH_IN_BROWSER));
      // Remove text inside the buttons
      jbLaunchInExternalBrowser.setText(null);
    }
    jbCopy.setText(null);
    ButtonGroup bg = new ButtonGroup();
    jbAuthorSearch = new JToggleButton(IconLoader.getIcon(JajukIcons.AUTHOR), false);
    jbAuthorSearch.setToolTipText(Messages.getString("WikipediaView.5"));
    // Select author search (default)
    jbAuthorSearch.setSelected(true);
    jbAuthorSearch.addActionListener(this);
    jbAlbumSearch = new JToggleButton(IconLoader.getIcon(JajukIcons.ALBUM), true);
    jbAlbumSearch.setToolTipText(Messages.getString("WikipediaView.6"));
    jbAlbumSearch.addActionListener(this);
    jbTrackSearch = new JToggleButton(IconLoader.getIcon(JajukIcons.TRACK), false);
    jbTrackSearch.setToolTipText(Messages.getString("WikipediaView.7"));
    jbTrackSearch.addActionListener(this);
    // Group this three mutual exclusive buttons
    bg.add(jbAuthorSearch);
    bg.add(jbAlbumSearch);
    bg.add(jbTrackSearch);

    JToolBar jtb = new JajukJToolbar();
    jtb.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
    // Add items
    jtb.add(jbAuthorSearch);
    jtb.add(jbAlbumSearch);
    jtb.add(jbTrackSearch);
    jtb.addSeparator();
    jtb.add(jbCopy);
    if (Desktop.isDesktopSupported()) {
      jtb.add(jbLaunchInExternalBrowser);
      jtb.addSeparator();
    }
    jtb.add(jcbLanguage);

    JPanel jpCommand = new JPanel();
    jpCommand.setBorder(BorderFactory.createEtchedBorder());
    jpCommand.setLayout(new FlowLayout(FlowLayout.LEFT));
    jpCommand.add(jtb);

    // global layout
    setLayout(new MigLayout("ins 0", "[grow]", "[][grow]"));
    browser = new JajukHtmlPanel();
    add(jpCommand, "growx,wrap");
    add(browser, "grow");

    // Display default page at startup is none track launch
    // avoid to launch this if a track is playing
    // to avoid thread concurrency
    if (QueueModel.getPlayingFile() == null) {
      reset();
    }

    // subscriptions to events
    ObservationManager.register(WikipediaView.this);

    // Force initial message refresh
    UtilFeatures.updateStatus(this);
  }

  public Set<JajukEvents> getRegistrationKeys() {
    Set<JajukEvents> eventSubjectSet = new HashSet<JajukEvents>();
    eventSubjectSet.add(JajukEvents.FILE_LAUNCHED);
    eventSubjectSet.add(JajukEvents.ZERO);
    eventSubjectSet.add(JajukEvents.AUTHOR_CHANGED);
    eventSubjectSet.add(JajukEvents.ALBUM_CHANGED);
    eventSubjectSet.add(JajukEvents.TRACK_CHANGED);
    eventSubjectSet.add(JajukEvents.PERSPECTIVE_CHANGED);
    return eventSubjectSet;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.Observer#update(java.lang.String)
   */
  public void update(JajukEvent event) {
    JajukEvents subject = event.getSubject();
    // Make a search after a stop period
    if (subject.equals(JajukEvents.FILE_LAUNCHED)
        || subject.equals(JajukEvents.PERSPECTIVE_CHANGED)) {
      // If current state is stopped, reset page
      if (QueueModel.getPlayingFile() == null) {
        reset();
        return;
      }
      // Launch search
      launchSearch(false);
    }
    // Reset the page when stopping
    else if (subject.equals(JajukEvents.ZERO)) {
      reset();
    }
    // User changed current track tags, so we have to reload
    // new author wikipedia page
    else if (subject.equals(JajukEvents.AUTHOR_CHANGED)
        || subject.equals(JajukEvents.ALBUM_CHANGED) || subject.equals(JajukEvents.TRACK_CHANGED)) {
      update(new JajukEvent(JajukEvents.FILE_LAUNCHED));
    }
  }

  /**
   * Perform wikipedia search
   * 
   * @param bForceReload
   *          force the page display
   */
  private void launchSearch(final boolean bForceReload) {
    Thread t = new Thread("Wikipedia Search Thread") {
      @Override
      public void run() {
        try {
          String lSearch = null;
          if (QueueModel.getPlayingFile() != null) {
            if (type == Type.AUTHOR) {
              lSearch = QueueModel.getPlayingFile().getTrack().getAuthor().getName2();
              // don't display page if item is unknown
              if (Messages.getString(UNKNOWN_AUTHOR).equals(lSearch)) {
                lSearch = null;
              }
            } else if (type == Type.ALBUM) {
              lSearch = QueueModel.getPlayingFile().getTrack().getAlbum().getName2();
              // don't display page if item is unknown
              if (Messages.getString(UNKNOWN_ALBUM).equals(lSearch)) {
                lSearch = null;
              }
            } else if (type == Type.TRACK) {
              lSearch = QueueModel.getPlayingFile().getTrack().getName();
            }
          }
          // If search is still null, display an nothing found page
          if (lSearch == null) {
            browser.setUnknown();
            return;
          }
          // Avoid reloading an existing page
          if (!bForceReload && lSearch.equals(WikipediaView.this.search)) {
            return;
          }
          // Store the search to avoid future identical searches
          WikipediaView.this.search = lSearch;

          URL url = new URL(("http://"
              + LocaleManager.getLocaleForDesc((String) jcbLanguage.getSelectedItem())
              + ".wikipedia.org/wiki/" + lSearch).replaceAll(" ", "_"));
          Log.debug("Wikipedia search: " + url);
          jbCopy.putClientProperty(Const.DETAIL_CONTENT, url.toExternalForm());
          if (Desktop.isDesktopSupported()) {
            jbLaunchInExternalBrowser.putClientProperty(Const.DETAIL_CONTENT, url.toExternalForm());
          }
          browser.setURL(url, LocaleManager.getLocaleForDesc((String) jcbLanguage.getSelectedItem()).toString());
        } catch (FileNotFoundException e) {
          // only report a warning for FileNotFoundException and do not show a
          // stacktrace in the logfile as it is expected in many cases where the
          // name is not found on Wikipedia
          Log.warn("Could not load URL, no content found at specified address: " + e.getMessage());
        } catch (Exception e) {
          Log.error(e);
        }

      }
    };
    t.setPriority(Thread.MIN_PRIORITY);
    t.start();
  }

  /*
   * Reset view
   */
  private void reset() {
    // Reset current search
    this.search = null;
    // Display jajuk page (in a thread to avoid freezing UI)
    new Thread("Wikipedia Reset Thread") {
      @Override
      public void run() {
        if (browser != null) {
          try {
            browser.clearDocument();
          } catch (Exception e) {
            Log.error(e);
          }
        }
      }
    }.start();
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
   */
  public void actionPerformed(ActionEvent arg0) {
    if (arg0.getSource() == jcbLanguage) {
      // update index
      Locale locale = LocaleManager.getLocaleForDesc((String) jcbLanguage.getSelectedItem());
      Conf.setProperty(Const.CONF_WIKIPEDIA_LANGUAGE, locale.getLanguage());
      // force launch wikipedia search for this language
      launchSearch(true);
    } else if (arg0.getSource() == jbAlbumSearch) {
      type = Type.ALBUM;
      // force event
      launchSearch(true);
    } else if (arg0.getSource() == jbAuthorSearch) {
      type = Type.AUTHOR;
      // force event
      launchSearch(true);
    } else if (arg0.getSource() == jbTrackSearch) {
      type = Type.TRACK;
      // force event
      launchSearch(true);
    }
  }
}
