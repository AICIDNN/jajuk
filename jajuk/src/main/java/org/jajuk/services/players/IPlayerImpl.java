/*
 *  Jajuk
 *  Copyright (C) 2003 The Jajuk Team
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
package org.jajuk.services.players;

import org.jajuk.base.File;
import org.jajuk.services.webradio.WebRadio;

/**
 * Minimum methods required for all Player implementations
 */
public interface IPlayerImpl {

  /**
   * Launches player
   * 
   * @param file :
   *          jajuk file to be played
   * @param fPosition
   *          position in % of the file
   * @param length
   *          length to play in ms or TO_THE_END of you want to play to the end
   *          of the current file
   * @param bMuted
   *          mute state
   * @param fVolume
   *          volume
   * @throws Exception
   */
  void play(File file, float fPosition, long length, float fVolume) throws Exception;

  /**
   * Play a web radio stream
   * 
   * @param radio
   * @param fVolume
   * @throws Exception
   */
  void play(WebRadio radio, float fVolume) throws Exception;

  /**
   * Stop current player
   * 
   * @throws Exception
   */
  void stop() throws Exception;

  /**
   * Set the gain
   * 
   * @param fVolume :
   *          gain from 0 to 1
   * @throws Exception
   */
  void setVolume(float fVolume) throws Exception;

  /**
   * @return elapsed time (ms) for this player
   */
  long getElapsedTime();

  /** Pause the player */
  void pause() throws Exception;

  /** Resume the player */
  void resume() throws Exception;

  /** Seek to a given position in %. ex : 0.2 for 20% */
  void seek(float fPosition);

  /** Return track LENGTH in */
  float getCurrentPosition();

  /** Return track position in ms */
  long getCurrentLength();

  /** Return volume in % */
  float getCurrentVolume();

  /** Return player state */
  int getState();

}
