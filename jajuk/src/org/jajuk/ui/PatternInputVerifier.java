/*
 *  Jajuk
 *  Copyright (C) 2006 bflorat
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

package org.jajuk.ui;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

import org.jajuk.Main;
import org.jajuk.i18n.Messages;
import org.jajuk.util.ITechnicalStrings;

/**
 * Input verifier used for predefined patterns. Pattern should contain at least
 * one / as this pattern verifier is used for organizer and organizer need to
 * create at least one directory to avoid mess on disk
 * 
 * @author Bertrand Florat
 * @created 11 oct. 06
 */
public class PatternInputVerifier extends InputVerifier implements ITechnicalStrings {

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.InputVerifier#verify(javax.swing.JComponent)
	 */
	@Override
	public boolean verify(JComponent input) {
		JTextField tf = (JTextField) input;
		String sText = tf.getText().toLowerCase();
		// Check pattern contains at least one /
		if (sText.indexOf('/') == -1) {
			JOptionPane.showMessageDialog(Main.getWindow(), Messages.getString("Error.146"), //$NON-NLS-1$
					Messages.getString("Error"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		try {
			String[] stPattern = sText.split("[% /-]"); //$NON-NLS-1$
			for (String sPattern : stPattern) {
				if (!sPattern.equals("")) { //$NON-NLS-1$
					if (sPattern.equalsIgnoreCase(PATTERN_ALBUM.substring(1))
							|| sPattern.equalsIgnoreCase(PATTERN_ARTIST.substring(1))
							|| sPattern.equalsIgnoreCase(PATTERN_YEAR.substring(1))
							|| sPattern.equalsIgnoreCase(PATTERN_TRACKNAME.substring(1))
							|| sPattern.equalsIgnoreCase(PATTERN_TRACKORDER.substring(1))
							|| sPattern.equalsIgnoreCase(PATTERN_GENRE.substring(1))) {
					} else {
						JOptionPane.showMessageDialog(Main.getWindow(), Messages
								.getString("Error.146"), //$NON-NLS-1$
								Messages.getString("Error"), //$NON-NLS-1$
								JOptionPane.ERROR_MESSAGE);
						return false;
					}
				}
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.swing.InputVerifier#shouldYieldFocus(javax.swing.JComponent)
	 */
	public boolean shouldYieldFocus(JComponent input) {
		return verify(input);
	}

}
