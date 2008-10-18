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

package org.jajuk.util;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jajuk.base.Item;
import org.jajuk.base.PropertyMetaInformation;
import org.jajuk.util.log.Log;

/**
 * Filter on meta information
 */
public class Filter {

  /** Key */
  String key;

  /** Value* */
  String sValue;

  /** Human* */
  boolean bHuman = false;

  /** Exact* */
  boolean bExact = false;

  /**
   * Filter constructor
   * 
   * @param key
   *          key (property name). null if the filter is on any property
   * @param sValue
   *          value
   * @param bHuman
   *          is the filter apply value itself or its human representation if
   *          different ?
   * @param bExact
   *          is the filter should match exactly the value ?
   */
  public Filter(String key, String sValue, boolean bHuman, boolean bExact) {
    this.key = key;
    this.sValue = sValue;
    this.bHuman = bHuman;
    this.bExact = bExact;
  }

  public boolean isExact() {
    return bExact;
  }

  public boolean isHuman() {
    return bHuman;
  }

  public String getProperty() {
    return key;
  }

  public String getValue() {
    return sValue;
  }

  /**
   * Filter a list.
   * <p>
   * Work on the input collection (for performance reasons and to save memory we
   * don't create a new list)
   * </p>
   * <p>
   * This filter is not thread safe.
   * </p>
   * 
   * @param in
   *          input list
   * @param filter
   */
  @SuppressWarnings("unchecked")
  public static void filterItems(List<? extends Item> list, Filter filter) {
    if (filter == null || filter.getValue() == null) {
      return;
    }
    // Check if property is not the "fake" any property
    boolean bAny = (filter.getProperty() == null || "any".equals(filter.getProperty()));

    String comparator = null;
    String checked = filter.getValue();
    // If checked is void, return the list as it
    if (UtilString.isVoid(checked)) {
      return;
    }
    // If pattern is wrong, return a void list
    try {
      Pattern.compile(checked);
    } catch (PatternSyntaxException e) {
      Log.debug("Wrong regexp pattern: " + checked);
      list.clear();
      return;
    }

    Iterator it = list.iterator();
    while (it.hasNext()) {
      Item item = (Item) it.next();
      // If none property set, the search if global "any"
      if (bAny) {
        comparator = item.getAny();
      } else {
        if (filter.isHuman()) {
          comparator = item.getHumanValue(filter.getProperty());
        } else {
          comparator = item.getStringValue(filter.getProperty());
        }
      }
      // perform the test
      boolean bMatch = false;
      if (filter.isExact()) {
        // Check every item property (no not use getAny() string will not match
        // as it is a concatenation of all properties)
        for (String propertyName : item.getProperties().keySet()) {
          // Ignore technical/invisible property (id for instance)
          PropertyMetaInformation meta = item.getMeta(propertyName);
          if (!meta.isVisible()) {
            continue;
          }
          String value = item.getHumanValue(propertyName);
          // Escape the string so regexp ignore special characters
          value = UtilString.escapeString(value);
          if (value.matches(checked)) {
            bMatch = true;
            break;
          }
        }
      } else {
        // Do not use Regexp matches() method, too costly
        bMatch = UtilString.matchesIgnoreCaseAndOrder(checked, comparator);
      }
      if (!bMatch) {
        it.remove();
      }
    }
    return;
  }

}
