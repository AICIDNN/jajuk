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

import com.jgoodies.animation.Animation;
import com.jgoodies.animation.Animations;
import com.jgoodies.animation.Animator;
import com.jgoodies.animation.animations.BasicTextAnimation;
import com.jgoodies.animation.components.BasicTextLabel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.jajuk.base.File;
import org.jajuk.events.JajukEvent;
import org.jajuk.events.JajukEvents;
import org.jajuk.events.ObservationManager;
import org.jajuk.events.Observer;
import org.jajuk.services.players.QueueModel;
import org.jajuk.services.webradio.WebRadio;
import org.jajuk.ui.windows.JajukMainWindow;
import org.jajuk.util.Conf;
import org.jajuk.util.Const;
import org.jajuk.util.Messages;
import org.jajuk.util.UtilFeatures;
import org.jajuk.util.UtilString;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * Animation-based view
 */
public class AnimationView extends ViewAdapter implements ComponentListener {

  private static final long serialVersionUID = 1L;

  private static final int DEFAULT_FRAME_RATE = 15;

  private static final int DEFAULT_DURATION = 5000;

  private static final int DEFAULT_PAUSE = 500;

  /** Current panel width* */
  private int iSize;

  private BasicTextLabel btl1;

  private Animator animator;

  public AnimationView() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.views.IView#getDesc()
   */
  public String getDesc() {
    return Messages.getString("AnimationView.0");
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.views.IView#populate()
   */
  public void initUI() {
    setLayout(new BorderLayout());
    addComponentListener(this);
    btl1 = new BasicTextLabel(" ");
    add(btl1);
    // Force initial message refresh
    UtilFeatures.updateStatus(this);

    ObservationManager.register(this);

  }

  public Set<JajukEvents> getRegistrationKeys() {
    Set<JajukEvents> eventSubjectSet = new HashSet<JajukEvents>();
    eventSubjectSet.add(JajukEvents.FILE_LAUNCHED);
    eventSubjectSet.add(JajukEvents.WEBRADIO_LAUNCHED);
    eventSubjectSet.add(JajukEvents.ZERO);
    eventSubjectSet.add(JajukEvents.PLAYER_STOP);
    return eventSubjectSet;
  }

  /** Set the text to be displayed* */
  public void setText(final String sText) {
    SwingUtilities.invokeLater(new Runnable() {
      // this is mandatory to
      // get actual getWitdth
      public void run() {
        iSize = AnimationView.this.getWidth();
        // current width. Must be called inside an invoke and wait,
        // otherwise, returns zero
        Font font = null;
        boolean bOk = false;
        int i = 40;
        while (!bOk) {
          font = new Font("verdana", Font.BOLD, i);
          FontMetrics fontMetrics = JajukMainWindow.getInstance().getFontMetrics(font);
          int iFontSize = SwingUtilities.computeStringWidth(fontMetrics, sText);
          if (iFontSize <= iSize - 150) {
            bOk = true;
          } else {
            i--;
          }
        }
        btl1.setFont(font);
        if (animator != null) {
          animator.stop();
        }
        Animation animPause = Animations.pause(DEFAULT_PAUSE);
        Animation anim = null;
        // select a random animation
        int iShuffle = (int) (Math.random() * 3);
        switch (iShuffle) {
        case 0:
          anim = BasicTextAnimation.defaultScale(btl1, DEFAULT_DURATION, sText, Color.darkGray);
          break;
        case 1:
          anim = BasicTextAnimation.defaultSpace(btl1, DEFAULT_DURATION, sText, Color.darkGray);
          break;
        case 2:
          anim = BasicTextAnimation.defaultFade(btl1, DEFAULT_DURATION, sText, Color.darkGray);
          break;
        }
        Animation animAll = Animations.sequential(anim, animPause);
        anim = Animations.repeat(Float.POSITIVE_INFINITY, animAll);
        animator = new Animator(anim, DEFAULT_FRAME_RATE);
        animator.start();
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.jajuk.ui.Observer#update(java.lang.String)
   */
  public void update(JajukEvent event) {
    JajukEvents subject = event.getSubject();
    if (subject.equals(JajukEvents.FILE_LAUNCHED)) {
      File file = QueueModel.getPlayingFile();
      if (file != null) {
        String s = "";
        try {
          s = UtilString.applyPattern(file, Conf.getString(Const.CONF_ANIMATION_PATTERN), false,
              false);
        } catch (JajukException e) {
          Log.error(e);
        }
        setText(s);
      }
    } else if (subject.equals(JajukEvents.ZERO) || subject.equals(JajukEvents.PLAYER_STOP)) {
      setText(Messages.getString("JajukWindow.18"));
    } else if (subject.equals(JajukEvents.WEBRADIO_LAUNCHED)) {
      WebRadio radio = (WebRadio) event.getDetails().get(Const.DETAIL_CONTENT);
      if (radio != null) {
        setText(radio.getName());
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
   */
  @Override
  public void componentResized(ComponentEvent e) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        iSize = SwingUtilities.getRootPane(AnimationView.this).getWidth();
        UtilFeatures.updateStatus(AnimationView.this);
      }
    });
  }
}
