/*
 *  Jajuk
 *  Copyright (C) 2003 sgringoi
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
 * $Log$
 * Revision 1.1  2003/10/24 15:19:41  sgringoi
 * Initial commit
 *
 *
 */
package org.jajuk.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.crimson.jaxp.SAXParserFactoryImpl;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

/**
 * @author S�bastien Gringoire
 *
 * Classe repr�sentant le contexte du portail.
 */
public class Contexte
{
	/** Nom de l'�l�ment contexte */
	private String nom = null;
	/** Cha�ne repr�sentant la valeur du tag contexte */
	private String valeur = null;
	/**
	 * Liste des �l�ments fils de l'instance du contexte
	 * - cl�: Nom de l'�l�ment fils
	 * - valeur: Contexte repr�sentant l'�l�ment fils
	 */
	private HashMap lstElements = null;
	/**
	 * Liste des attributs de l'instance du contexte
	 * - cl�: Nom de l'attribut
	 * - valeur: Chaine repr�sentant la valeur de l'attribut
	 */
	private HashMap lstAttributs = null;

	/**
	 * Constructor for Contexte.
	 * @param pNom - Nom du contexte
	 */
	public Contexte(String pNom) {
		super();
		
			// Cr�ation de la liste des �l�ments fils
		lstElements = new HashMap();
			// Cr�ation de la liste des attributs
		lstAttributs = new HashMap();
			// Initialisation du nom du contexte
		nom = pNom;
	}

	/**
	 * Ins�re une valeur dans le contexte.
	 * Si le contexte contient d�j� une valeur de m�me cl�, la valeur est ajout�e sous forme de liste.
	 * @param pCle - Cl� de la valeur � mettre � jour.
	 * @param pValeur - Nouvelle valeur de la donn�e. La valeur peut �tre un �l�ment fils.
	 */
	public void inserer(String pCle, Contexte pValeur)
	{
			// On v�rifie si le contexte contient d�j� la donn�e
		if (lstElements.containsKey(pCle))
		{
			// Le contexte contient d�j� la donn�e, on cr�e une liste de valeurs
			Object val = lstElements.get(pCle);
			if (val instanceof ArrayList)
			{
					// On ajoute la nouvelle valeur � la liste
				ArrayList lstVal = (ArrayList)val;
				lstVal.add(pValeur);
			} else {
					// On cr�e une liste de valeurs
				ArrayList lstVal = new ArrayList();
				lstVal.add(val);
				lstVal.add(pValeur);
				
				lstElements.put(pCle, lstVal);
			}
		} else {
			lstElements.put(pCle, pValeur);
		}

	}
	/**
	 * Met � jour un attribut du contexte.
	 * @param pCle - Cl� de l'attribut � mettre � jour.
	 * @param pValeur - Nouvelle valeur de l'attribut.
	 */
	public void insererAttribut(String pCle, String pValeur)
	{
		lstAttributs.put(pCle, pValeur);
	}
	/**
	 * Met � jour la valeur du contexte courant.
	 * @param pValeur - Nouvelle valeur cha�ne du contexte.
	 */
	public void ecrireChaine(String pValeur)
	{
		valeur = pValeur;
	}
	

	/**
	 * Renvoie la valeur cha�ne d'une donn�e du contexte � partir d'une URL du type client/nom.
	 * L'URL fournie doit �tre relative au contexte courant.
	 * Si la donn�e n'est pas pr�sente dans le contexte, la m�thode renvoie null.
	 * Si plusieurs contexte de m�me nom sont pr�sent, la valeur retourn�e est celle du premier contexte rencontr�.
	 * @param pURL - URL indiquant le nom du noeud du contexte � renvoyer. Les noms des contextes sont s�par�s par un '/'.
	 * @return String - Valeur de la donn�e � lire.
	 */
	public String lireChaine(String pURL)
	{
		Contexte ctx = getContexte(pURL);
		if (ctx == null)
		{
			return null;
		} else {
			return ctx.lireChaine();
		}
	}
	/**
	 * Renvoie la valeur du contexte si le contexte est une feuille.
	 * Si le contexte n'a pas de valeur texte, la m�thode renvoie null.
	 * @return String - Renvoie la chaine correspondant au contexte.
	 */
	public String lireChaine()
	{
		if (valeur != null)
		{
			return new String(valeur);
		} else {
			return null;
		}
	}
	/**
	 * Renvoie la valeur de l'attribut dont le nom est pass� en param�tre.
	 * Si l'attriut n'est pas connu du contexte, la m�thode renvoie null.
	 * @param pCle - Nom de l'attribut � lire
	 * @return String - Valeur de l'attribut.
	 */
	public String lireAttribut(String pCle)
	{
		return (String)lstAttributs.get(pCle);
	}
	/**
	 * Renvoie un flux XML repr�sentant le contexte.
	 * @return String - Cha�ne au format XML repr�sentant le contexte.
	 */
	public String getXML() {
		StringBuffer res = new StringBuffer();
		
			// Nom du contexte
		res.append("<" + nom);
			
				// Liste des attributs
			Iterator iteAtt = lstAttributs.keySet().iterator();
			while (iteAtt.hasNext())
			{
				String cle = (String)iteAtt.next();
				String val = (String)lstAttributs.get(cle);
				
				res.append(" ");
				res.append(cle);
				res.append("='");
				res.append(val);
				res.append("'");
			}
			res.append(">");
			
				// Valeur du contexte
			if (valeur != null)
				res.append(valeur);
			
				// Liste des �l�ments fils
			Iterator iteCtx = lstElements.values().iterator();
			while (iteCtx.hasNext())
			{
				Object fils = iteCtx.next();
				
				if (fils instanceof Contexte)
				{
					res.append(((Contexte)fils).getXML());
				} else if (fils instanceof ArrayList) {
					ArrayList lstCtx = (ArrayList)fils;
					Iterator iteFils = lstCtx.iterator();
					while (iteFils.hasNext())
					{
						Object ctx = iteFils.next();
						if (ctx instanceof Contexte)
						{
							res.append(((Contexte)ctx).getXML());
						} else {
							res.append(iteFils.next().toString());
						}
					}
				}
			}
		res.append("</" + nom + ">\n");
		
		return res.toString();
	}

	/**
	 * Lit un flux XML et construit un contexte en r�sultant.
	 * @param pFluxXML - Flux XML repr�sentant le contexte � parser.
	 * @return Contexte - Renvoie le contexte produit � partir du flux XML.
	 * @exception ExceptionTechAModifier - L�ve une exception si le contexte ne peut �tre cr�� � partir du flux.
	 */
	public static Contexte creerContexte(String pFluxXML)
		throws JajukException
	{
		ContexteHandler ch = new ContexteHandler();
		
		try {
			SAXParserFactory saxFactory = new SAXParserFactoryImpl();
			SAXParser saxParser = saxFactory.newSAXParser();
			XMLReader xmlReader = saxParser.getXMLReader();

			xmlReader.setContentHandler(ch);
			xmlReader.parse(new InputSource(new java.io.StringReader(pFluxXML)));
		} catch (Exception e) {
			JajukException te = new JajukException("jajuk0008", e);
			throw te;
		}

		return ch.getContexte();
	}

	/**
	 * Retourne le nom du contexte.
	 * @return String - Nom du contexte.
	 */
	public String getNom() {
		return nom;
	}

	/**
	 * M�thode renvoyant le contexte point� par la cl�.
	 * Si la cl� pointe une liste de contextes, la m�thode renvoie le premier contexte de la liste.
	 * Si aucun contexte fils ne correspond � la recherche, alors la m�thode renvoie null.
	 * @param pCle - Chaine pointant sur le contexte � renvoyer.
	 * @return Contexte - Contexte recherch�.
	 */
	public Contexte getContexte(String pCle)
	{
		String cle = pCle;

			// Calcul de la position du premier s�parateur
		int indexSep = cle.indexOf('/');
		if (indexSep == 0)
		{
				// On supprime le premier '/'
			cle = cle.substring(1, cle.length());
				// On recalcule le s�parateur suivant
			indexSep = cle.indexOf('/');
		}

		if (cle.equals("")) {
				// On retourne le noeud courant
			return this;
		} else if (indexSep == -1) {
				// pCle correspond � un contexte fils du contexte courant
			Object fils = lstElements.get(cle);
			if (fils == null)
			{
				return null;
			} else if (fils instanceof Contexte) {
				return (Contexte) fils;
			} else if (fils instanceof ArrayList) {
				return (Contexte)((ArrayList)fils).get(0);
			} else {
				return null;
			}
		} else {
				// R�cup�ration du nom du premier contexte fils
			String nomCtxFils = cle.substring(0, indexSep);
				// Reste de l'URL
			String resteURL = null;
			if (cle.length() > 0)
			{
				try {
					resteURL = cle.substring(indexSep+1, cle.length());
				} catch (StringIndexOutOfBoundsException e) {
					resteURL = "";
				}
			}

				// R�cup�ration du contexte
			Object fils = lstElements.get(nomCtxFils);
			if (fils == null)
			{
				return null;
			} else if (fils instanceof Contexte) {
				return ((Contexte) fils).getContexte(resteURL);
			} else if (fils instanceof ArrayList) {
				Contexte ctx = (Contexte)((ArrayList)fils).get(0);
				if (ctx == null)
				{
					return null;
				} else {
					return ctx.getContexte(resteURL);
				}
			} else {
				return null;
			}
		}
	}
	/**
	 * M�thode renvoyant la liste des contextes fils correspondant � la cl� fournie en param�tre.
	 * Si la cl� ne correspond � aucun contexte fils, la m�thode renvoie null.
	 * La liste retourn�e par la m�thode est celle utilis�e par le contexte courant.
	 * Toute modification sur l'un des contextes contenus dans cette liste, est r�percut�e.
	 * Par contre, l'ajout d'un contexte doit se faire via la m�thode inserer() et non directement dans la liste.
	 * @param pCle - Cl� correspondant aux contextes fils � retourner.
	 * @return ArrayList - Liste des contextes fils correspondants � la cl� fournie.
	 */
	public ArrayList getListeContextes(String pCle)
	{
		String cle = pCle;
			// Calcul de l'index du dernier s�parateur
		int indexSep = pCle.lastIndexOf('/');
		if (indexSep == (cle.length()-1))
		{
			cle = cle.substring(0, indexSep);
			indexSep = cle.lastIndexOf('/');
		}

		String nomCtxLst = cle.substring(0, indexSep);
		String nomEltRes = cle.substring(indexSep+1, cle.length());
		
			// R�cup�ration du contexte contenant la liste
		Contexte ctxLst = getContexte(nomCtxLst);

		if (ctxLst == null)
		{
			return null;
		} else {
				// On r�cup�re les fils du contexte s�lectionn�
			Object lstRes = ctxLst.lstElements.get(nomEltRes);
			if (lstRes == null)
			{
				return null;
			} else if (lstRes instanceof ArrayList) {
				return (ArrayList)((ArrayList)lstRes).clone();
			} else if (lstRes instanceof Contexte) {
				ArrayList res = new ArrayList();
				res.add(lstRes);
				
				return res;
			} else {
				return null;
			}
		}
	}

	/*
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getClass().getName() + "-" + nom;
	}

	/**
	 * Return the list of key/value pairs representing the context tree.
	 * The result of this method is used to create the properties representation of the context.
	 * @return Map - List of key/value pairs in a property format.
	 */
	public Map getPropertiesFormat() {
		HashMap res = new HashMap();
Log.debug("getPropertiesFormat de " + getNom());
		String keyPrefix = getNom();
		// if there's a name attribute, then the key is constructed with the name
		if (lstAttributs.containsKey("name")) {
			keyPrefix = (String)lstAttributs.get("name");
		}
		
		// list the node's attributes
		Iterator ite = lstAttributs.keySet().iterator();
		while (ite.hasNext()) {
			String key = (String)ite.next();
			
			// We don't add the name attribute
			if (!key.equals("name")) {
				String value = (String)lstAttributs.get(key);

				// Add the result to the map
				res.put(keyPrefix + "." + key, value);
Log.debug("  - " + keyPrefix + "." + key + " = " + value);
			}
			
		}
		
		// Add the node value
		String nodeValue = lireChaine();
		if ((nodeValue != null) && (!nodeValue.trim().equals(""))){
			res.put(keyPrefix, nodeValue);
Log.debug("  - " + keyPrefix + " = " + nodeValue);
		}
		
		// Add children properties
		ite = lstElements.values().iterator();
		while (ite.hasNext()) {
			// Le contexte contient d�j� la donn�e, on cr�e une liste de valeurs
			Object childObj = ite.next();
			if (childObj instanceof ArrayList)
			{
					// On ajoute la nouvelle valeur � la liste
				ArrayList childArray = (ArrayList)childObj;
				Iterator iteSameKey = childArray.iterator();
				StringBuffer childrenLstName = new StringBuffer(); 
				while (iteSameKey.hasNext()) {
					Contexte childContext = (Contexte)iteSameKey.next();
					
					// construct the list property
					childrenLstName.append(childContext.lireAttribut("name"));
					childrenLstName.append(",");
					
					Map childProperties = childContext.getPropertiesFormat();
			
					Iterator iteChild = childProperties.keySet().iterator();
					while (iteChild.hasNext()) {
						String key = (String)iteChild.next();
						String value = (String)childProperties.get(key);
				
						res.put(keyPrefix + "." + key, value);
Log.debug("  - " + keyPrefix + "." + key + " = " + value);
					}
				}
				
				// Add the list property
				res.put(keyPrefix + ".list", childrenLstName.toString());
Log.debug("  *- " + keyPrefix + ".list"+ " = " + childrenLstName.toString());
			} else {
				Contexte child = (Contexte)childObj;
				Map childProperties = child.getPropertiesFormat();
				
				Iterator iteChild = childProperties.keySet().iterator();
				while (iteChild.hasNext()) {
					String key = (String)iteChild.next();
					String value = (String)childProperties.get(key);
					
					res.put(keyPrefix + "." + key, value);
Log.debug("  - " + keyPrefix + "." + key + " = " + value);
				}
			}
		}
		
		return res;
	}
}
