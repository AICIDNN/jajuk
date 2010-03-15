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
package org.jajuk.util;

import org.jajuk.JajukTestCase;

/**
 * 
 */
public class TestUpgradeManager extends JajukTestCase {

  String v1 = "1.8.4";
  String v2 = "1.9.1";
  String v3 = "1.9.2RC3";
  String v4 = "1.8.5";

  /*
   * (non-Javadoc)
   * 
   * @see junit.framework.TestCase#setUp()
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  /**
   * Test method for
   * {@link org.jajuk.util.UpgradeManager#getNumberRelease(String)}.
   */
  public void testGetNumberRelease() throws Exception {
    assertEquals(10804, UpgradeManager.getNumberRelease(v1));
    assertEquals(10901, UpgradeManager.getNumberRelease(v2));
    assertEquals(10902, UpgradeManager.getNumberRelease(v3));
  }

  /**
  * Test method for
  * {@link org.jajuk.util.UpgradeManager#isMajorMigration(String,String)}.
  */
  public void testIsMajorMigration() throws Exception {
    assertEquals(true, UpgradeManager.isMajorMigration(v1, v2));
    assertEquals(false, UpgradeManager.isMajorMigration(v1, v4));
    assertEquals(false, UpgradeManager.isMajorMigration(v2, v3));
  }

  /**
  * Test method for
  * {@link org.jajuk.util.UpgradeManager#isNewer(String,String)}.
  */
  public void testIsNewer() throws Exception {
    assertEquals(true, UpgradeManager.isNewer(v1, v2));
    assertEquals(true, UpgradeManager.isNewer(v1, v4));
    assertEquals(true, UpgradeManager.isNewer(v2, v3));
    assertEquals(false, UpgradeManager.isNewer(v2, v1));
  }

}
