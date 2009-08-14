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
 *  $Revision: 3132 $
 */
package org.jajuk.events;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import org.jajuk.JUnitHelpers;

import junit.framework.TestCase;

/**
 * 
 */
public class TestObservationManager extends TestCase {
  AtomicInteger called = new AtomicInteger(0);
  
  /* (non-Javadoc)
   * @see junit.framework.TestCase#tearDown()
   */
  @Override
  protected void tearDown() throws Exception {
    // sleep a bit to let async threads finish before we run next test...
    Thread.sleep(100);

    super.tearDown();
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#register(org.jajuk.events.Observer)}.
   */
  public void testRegister() {
    ObservationManager.register(new TestObserverRegistry.LocalObserver(called));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#unregister(org.jajuk.events.JajukEvents, org.jajuk.events.Observer)}.
   */
  public void testUnregister() {
    ObservationManager.unregister(new TestObserverRegistry.LocalObserver(called));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#notify(org.jajuk.events.JajukEvent)}.
   */
  public void testNotifyJajukEvent() {
    ObservationManager.notify(new JajukEvent(JajukEvents.VOLUME_CHANGED));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#notifySync(org.jajuk.events.JajukEvent)}.
   */
  public void testNotifySync() {
    ObservationManager.notifySync(new JajukEvent(JajukEvents.PLAY_ERROR));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#containsEvent(org.jajuk.events.JajukEvents)}.
   */
  public void testContainsEvent() {
    assertFalse(ObservationManager.containsEvent(JajukEvents.CDDB_WIZARD));
    ObservationManager.notifySync(new JajukEvent(JajukEvents.CDDB_WIZARD));
    assertTrue(ObservationManager.containsEvent(JajukEvents.CDDB_WIZARD));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#notify(org.jajuk.events.JajukEvent, boolean)}.
   */
  public void testNotifyJajukEventBoolean() {
    ObservationManager.notify(new JajukEvent(JajukEvents.VOLUME_CHANGED));
    ObservationManager.notifySync(new JajukEvent(JajukEvents.VOLUME_CHANGED));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#getDetailLastOccurence(org.jajuk.events.JajukEvents, java.lang.String)}.
   */
  public void testGetDetailLastOccurence() throws Exception {
    assertNull(ObservationManager.getDetailLastOccurence(JajukEvents.ALARMS_CHANGE, "notexists"));
    Properties prop = new Properties();
    prop.setProperty("test", "value");
    ObservationManager.notifySync(new JajukEvent(JajukEvents.VOLUME_CHANGED, prop));
    assertEquals("value", ObservationManager.getDetailLastOccurence(JajukEvents.VOLUME_CHANGED, "test"));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#getDetail(org.jajuk.events.JajukEvent, java.lang.String)}.
   */
  public void testGetDetail() {
    Properties prop = new Properties();
    prop.setProperty("test", "value");
    assertEquals("value", ObservationManager.getDetail(new JajukEvent(JajukEvents.VOLUME_CHANGED, prop), "test"));
  }

  public void testGetDetailNull() {
    Properties prop = new Properties();
    prop.setProperty("test", "value");
    assertNull(ObservationManager.getDetail(new JajukEvent(JajukEvents.VOLUME_CHANGED, prop), "notexisting"));
    
    assertNull(ObservationManager.getDetail(new JajukEvent(JajukEvents.VOLUME_CHANGED), "notexisting"));
  }

  /**
   * Test method for {@link org.jajuk.events.ObservationManager#getDetailsLastOccurence(org.jajuk.events.JajukEvents)}.
   */
  public void testGetDetailsLastOccurence() throws Exception {
    assertNull(ObservationManager.getDetailLastOccurence(JajukEvents.ALARMS_CHANGE, "notexists"));
    Properties prop = new Properties();
    prop.setProperty("test", "value");
    ObservationManager.notifySync(new JajukEvent(JajukEvents.VOLUME_CHANGED, prop));
    assertNotNull(ObservationManager.getDetailsLastOccurence(JajukEvents.VOLUME_CHANGED));
    assertEquals("value", ObservationManager.getDetailsLastOccurence(JajukEvents.VOLUME_CHANGED).get("test"));
  }

  // helper method to emma-coverage of the unused constructor
  public void testPrivateConstructor() throws Exception {
    // For EMMA code-coverage tests
    JUnitHelpers.executePrivateConstructor(ObservationManager.class);
  }

  public void testException() throws Exception {
    Observer observer = new TestObserverRegistry.LocalObserver(true, called);
    ObservationManager.register(observer);
    ObservationManager.notifySync(new JajukEvent(JajukEvents.ALBUM_CHANGED));
    ObservationManager.unregister(observer);
  }
}
