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
package org.jajuk.services.covers;

import java.awt.HeadlessException;
import java.io.File;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jajuk.JUnitHelpers;
import org.jajuk.JajukTestCase;
import org.jajuk.services.covers.Cover.CoverType;

/**
 * 
 */
public class TestCover extends JajukTestCase {

  /**
   * Test method for {@link org.jajuk.services.covers.Cover#hashCode()}.
   * 
   * @throws Exception
   */
  public final void testHashCode() throws Exception {
    Cover cover = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    Cover equal = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    JUnitHelpers.HashCodeTest(cover, equal);
  }

  /**
   * Test method for
   * {@link org.jajuk.services.covers.Cover#Cover(java.net.URL, org.jajuk.services.covers.Cover.CoverType)}
   * .
   * 
   * @throws Exception
   */
  public final void testCoverURLCoverType() throws Exception {
    new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
  }

  /**
   * Test method for
   * {@link org.jajuk.services.covers.Cover#Cover(java.io.File, org.jajuk.services.covers.Cover.CoverType)}
   * .
   * 
   * @throws Exception
   */
  public final void testCoverFileCoverType() throws Exception {
    new Cover(new File("testfile.cov"), CoverType.STANDARD_COVER);
  }

  /**
   * Test method for
   * {@link org.jajuk.services.covers.Cover#compareTo(org.jajuk.services.covers.Cover)}
   * .
   * 
   * @throws Exception
   */
  public final void testCompareTo() throws Exception {
    Cover cover = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    Cover equal = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    Cover notequal = new Cover(new URL("http://www.example.com/"), CoverType.LOCAL_COVER);
    Cover notequal2 = new Cover(new URL("http://www.example.com/"), CoverType.NO_COVER);
    Cover notequal3 = new Cover(new URL("http://www.example.com/"), CoverType.REMOTE_COVER);

    JUnitHelpers.CompareToTest(cover, equal, notequal);
    JUnitHelpers.CompareToTest(cover, equal, notequal2);
    JUnitHelpers.CompareToTest(cover, equal, notequal3);
  }

  /**
   * Test method for {@link org.jajuk.services.covers.Cover#getType()}.
   * 
   * @throws Exception
   */
  public final void testGetType() throws Exception {
    Cover cover = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    assertEquals(CoverType.STANDARD_COVER, cover.getType());
  }

  /**
   * Test method for {@link org.jajuk.services.covers.Cover#getURL()}.
   * 
   * @throws Exception
   */
  public final void testGetURL() throws Exception {
    Cover cover = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    assertEquals("http://www.example.com/", cover.getURL().toString());
  }

  /**
   * Test method for {@link org.jajuk.services.covers.Cover#getSize()}.
   * 
   * @throws Exception
   */
  public final void testGetSize() throws Exception {
    File file = File.createTempFile("test", ".txt");

    // remove it and re-create it with some content
    assertTrue(file.delete());
    FileUtils.writeStringToFile(file, StringUtils.repeat(".", 2000));

    Cover cover = new Cover(file, CoverType.STANDARD_COVER);
    assertEquals("2", cover.getSize());
  }

  /**
   * Test method for {@link org.jajuk.services.covers.Cover#getImage()}.
   * 
   * @throws Exception
   */
  public final void testGetImage() throws Exception {
    File file = File.createTempFile("test", ".txt");
    Cover cover = new Cover(file, CoverType.NO_COVER);

    // for no-cover, we get back a default image
    assertNotNull(cover.getImage());

    cover = new Cover(new URL("http://jajuk.info/skins/jajuk2007/jajuk_logotype.png"),
        CoverType.REMOTE_COVER);
    try {
      assertNotNull(cover.getImage());
    } catch (HeadlessException e) {
      // ignore here...
    }

    cover = new Cover(new File("notexists"), CoverType.STANDARD_COVER);
    try {
      cover.getImage();
      fail("Should throw an exception here...");
    } catch (IllegalArgumentException e) {
      // ok here
    }

    // TODO: more testing is necessary here...
  }

  /**
   * Test method for {@link org.jajuk.services.covers.Cover#toString()}.
   * 
   * @throws Exception
   */
  public final void testToString() throws Exception {
    // standard toString
    Cover cover = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    JUnitHelpers.ToStringTest(cover);

    // should also cope with items being null
    cover = new Cover(new URL("http://www.example.com/"), null);
    JUnitHelpers.ToStringTest(cover);
  }

  /**
   * Test method for
   * {@link org.jajuk.services.covers.Cover#equals(java.lang.Object)}.
   * 
   * @throws Exception
   */
  public final void testEqualsObject() throws Exception {
    Cover cover = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    Cover equal = new Cover(new URL("http://www.example.com/"), CoverType.STANDARD_COVER);
    Cover notequal = new Cover(new URL("http://www.example.com/"), CoverType.LOCAL_COVER);
    Cover notequal2 = new Cover(new URL("http://www.example.com/"), CoverType.NO_COVER);
    Cover notequal3 = new Cover(new URL("http://www.test.com/"), CoverType.STANDARD_COVER);

    JUnitHelpers.EqualsTest(cover, equal, notequal);
    JUnitHelpers.EqualsTest(cover, equal, notequal2);
    JUnitHelpers.EqualsTest(cover, equal, notequal3);
  }

  /**
   * Test method for {@link org.jajuk.services.covers.Cover#getFile()}.
   * 
   * @throws Exception
   */
  public final void testGetFile() throws Exception {
    Cover cover = new Cover(new URL("http://www.example.com/"), CoverType.REMOTE_COVER);
    Cover cover2 = new Cover(new File("testfile"), CoverType.STANDARD_COVER);

    assertNotNull(cover.getFile());
    assertNotNull(cover2.getFile());
  }
}
