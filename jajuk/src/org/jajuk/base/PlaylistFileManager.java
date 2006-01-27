/*
 *  Jajuk
 *  Copyright (C) 2003 Bertrand Florat
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

package org.jajuk.base;

import java.util.Iterator;
import java.util.Properties;

import org.jajuk.i18n.Messages;
import org.jajuk.util.MD5Processor;
import org.jajuk.util.Util;
import org.jajuk.util.error.JajukException;
import org.jajuk.util.log.Log;

/**
 * Convenient class to manage playlists files
 * @Author Bertrand Florat 
 * @created 17 oct. 2003
 */
public class PlaylistFileManager extends ItemManager implements Observer{
    /**Self instance*/
    private static PlaylistFileManager singleton;
    
    /**
     * No constructor available, only static access
     */
    private PlaylistFileManager() {
        super();
        //---register properties---
        //ID
        registerProperty(new PropertyMetaInformation(XML_ID,false,true,false,false,false,String.class,null,null));
        //Name
        registerProperty(new PropertyMetaInformation(XML_NAME,false,true,true,true,false,String.class,null,null));
        //Hashcode
        registerProperty(new PropertyMetaInformation(XML_HASHCODE,false,false,false,false,false,String.class,null,null));
        //Directory
        registerProperty(new PropertyMetaInformation(XML_DIRECTORY,false,true,true,false,false,String.class,null,null));
    }
    
    /**
     * @return singleton
     */
    public static PlaylistFileManager getInstance(){
        if (singleton == null){
            singleton = new PlaylistFileManager();
        }
        return singleton;
    }
    
    /**
     * Register an PlaylistFile with a known id
     * 
     * @param fio
     * @param dParentDirectory
     */
    public PlaylistFile registerPlaylistFile(java.io.File fio, Directory dParentDirectory) throws Exception{
        synchronized(PlaylistFileManager.getInstance().getLock()){
            String sId = getID(fio.getName(),dParentDirectory);
            return registerPlaylistFile(sId,fio.getName(),dParentDirectory);
        }
    }
    
    /**
     * @param sName
     * @param dParentDirectory
     * @return Item ID
     */
    protected static String getID(String sName,Directory dParentDirectory){
        return MD5Processor.hash(new StringBuffer(
            dParentDirectory.getDevice().getUrl()).
            append(dParentDirectory.getRelativePath()).
            append(sName).toString());
    }
    
    /**
     * Delete a playlist file
     *
     */
    public void removePlaylistFile(PlaylistFile plf){
        synchronized(PlaylistFileManager.getInstance().getLock()){
            String sFileToDelete = plf.getDirectory().getFio().getAbsoluteFile().toString()+java.io.File.separatorChar+plf.getName(); //$NON-NLS-1$
            java.io.File fileToDelete = new java.io.File(sFileToDelete);
            if ( fileToDelete.exists()){
                fileToDelete.delete();
                //check that file has been really deleted (sometimes, we get no exception)
                if (fileToDelete.exists()){
                    Log.error("131",new JajukException("131")); //$NON-NLS-1$//$NON-NLS-2$
                    Messages.showErrorMessage("131"); //$NON-NLS-1$
                    return;
                }
            }
            //remove reference from playlist
            PlaylistManager.getInstance().removePlaylistFile(plf);
            plf.getDirectory().removePlaylistFile(plf);
            //remove playlist file
            removeItem(plf.getId());
        }
    }
    
    /**
     * Register an PlaylistFile with a known id
     * 
     * @param sName
     */
    public synchronized PlaylistFile registerPlaylistFile(String sId, String sName, Directory dParentDirectory) throws Exception{
        synchronized(PlaylistFileManager.getInstance().getLock()){
            if ( !hmItems.containsKey(sId)){
                PlaylistFile playlistFile = null;
                playlistFile = new PlaylistFile(sId,sName,dParentDirectory);
                hmItems.put(sId, playlistFile);
                if ( dParentDirectory.getDevice().isRefreshing()){
                    Log.debug("Registered new playlist file: "+ playlistFile); //$NON-NLS-1$
                }
            }
            return (PlaylistFile)hmItems.get(sId);
        }
    }
    
    /**
     * Clean all references for the given device
     * 
     * @param sId :
     *                   Device id
     */
    public void cleanDevice(String sId) {
        synchronized(PlaylistFileManager.getInstance().getLock()){
            Iterator it = hmItems.values().iterator();
            while (it.hasNext()) {
                PlaylistFile plf = (PlaylistFile) it.next();
                if ( plf.getDirectory()== null 
                        || plf.getDirectory().getDevice().getId().equals(sId)) {
                    it.remove();
                }
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.jajuk.base.ItemManager#getIdentifier()
     */
    public String getIdentifier() {
        return XML_PLAYLIST_FILES;
    }
    
    /**
     * Change a playlist file name
     * @param plfOld
     * @param sNewName
     * @return new playlist file
     */
    public PlaylistFile changePlaylistFileName(PlaylistFile plfOld,String sNewName) throws JajukException{
        synchronized(PlaylistFileManager.getInstance().getLock()){
            //check given name is different
            if (plfOld.getName().equals(sNewName)){
                return plfOld;
            }
            //check if this file still exists
            if (!plfOld.getFio().exists()){
                throw new JajukException("135"); //$NON-NLS-1$
            }
            java.io.File ioNew = new java.io.File(plfOld.getFio().getParentFile().getAbsolutePath()
                +java.io.File.separator+sNewName);
            //recalculate file ID
            Directory dir = plfOld.getDirectory(); 
            String sNewId = PlaylistFileManager.getID(sNewName,plfOld.getDirectory());
            //create a new playlist file (with own fio and sAbs)
            PlaylistFile plfNew = new PlaylistFile(sNewId,sNewName,plfOld.getDirectory());
            plfNew.setProperties(plfOld.getProperties()); //transfert all properties (inc id and name)
            plfNew.setProperty(XML_ID,sNewId); //reset new id and name
            plfNew.setProperty(XML_NAME,sNewName); //reset new id and name
            //check file name and extension
            if (plfNew.getName().lastIndexOf((int)'.') != plfNew.getName().indexOf((int)'.')//just one '.'
                    || !(Util.getExtension(ioNew).equals(EXT_PLAYLIST))){ //check extension
                Messages.showErrorMessage("134"); //$NON-NLS-1$
                throw new JajukException("134"); //$NON-NLS-1$
            }
            //check if future file exists (under windows, file.exists return true even with different case so we test file name is different)
            if ( !ioNew.getName().equalsIgnoreCase(plfOld.getName()) && ioNew.exists()){
                throw new JajukException("134"); //$NON-NLS-1$
            }
            //try to rename file on disk
            try{
                plfOld.getFio().renameTo(ioNew);
            }
            catch(Exception e){
                throw new JajukException("134"); //$NON-NLS-1$
            }
            //OK, remove old file and register this new file
            hmItems.remove(plfOld.getId());
            if (!hmItems.containsKey(sNewId)){
                hmItems.put(sNewId,plfNew);
            }
            //change directory reference
            plfNew.getDirectory().changePlaylistFile(plfOld,plfNew);
            return plfNew;
        }
    }
    
    
    /* (non-Javadoc)
     * @see org.jajuk.base.Observer#update(org.jajuk.base.Event)
     */
    public void update(Event event) {
        synchronized(getLock()){
            String subject = event.getSubject();
            if (EVENT_FILE_NAME_CHANGED.equals(subject)){
                Properties properties = event.getDetails();
                File fNew = (File)properties.get(DETAIL_NEW);
                File fileOld = (File)properties.get(DETAIL_OLD);
                //search references in playlists
                Iterator it = getItems().iterator();
                for (int i=0; it.hasNext(); i++){
                    PlaylistFile plf = (PlaylistFile)it.next();
                    if (plf.isReady()){ //check only in mounted playlists, note that we can't change unmounted playlists
                        try{
                            if (plf.getFiles().contains(fileOld)){
                                plf.replaceFile(fileOld,fNew);
                            }
                        }
                        catch(Exception e){
                            Log.error("017",e); //$NON-NLS-1$
                        }
                    }
                }
                //refresh UI
                ObservationManager.notify(new Event(EVENT_PLAYLIST_REFRESH));
            }
        }
    }
}
