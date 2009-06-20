/*
 * Adapted by Jajuk team
 * Copyright (C) 2003-2009 the Jajuk Team
 * http://jajuk.info
 * 
 * aTunes 1.14.0
 * Copyright (C) 2006-2009 Alex Aranda, Sylvain Gaudard, Thomas Beckers and contributors
 *
 * See http://www.atunes.org/wiki/index.php?title=Contributing for information about contributors
 *
 * http://www.atunes.org
 * http://sourceforge.net/projects/atunes
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package ext.services.lastfm;

import javax.swing.ImageIcon;

import net.roarsoftware.lastfm.Artist;
import net.roarsoftware.lastfm.ImageSize;

/**
 * The Class LastFmArtist.
 */
public class LastFmArtist implements ArtistInfo {

    /** The name. */
    private String name;

    /** The match. */
    private String match;

    /** The url. */
    private String url;

    /** The image url. */
    private String imageUrl;

    // Used by renderers
    /** The image. */
    private ImageIcon image;

    /**
     * <Code>true</code> if this artist is available at repository
     */
    private transient boolean available;

    /**
     * Gets the artist.
     * 
     * @return the artist
     */
    public static LastFmArtist getArtist(Artist a) {
        LastFmArtist artist = new LastFmArtist();
        artist.name = a.getName();
        artist.match = String.valueOf(a.getSimilarityMatch());
        String url2 = a.getUrl();
        artist.url = url2.startsWith("http") ? url2 : "http://" + url2;
        // SMALL images have low quality when scaling. Better to get largest image
        artist.imageUrl = a.getImageURL(ImageSize.LARGE);
        return artist;
    }

    /**
     * Gets the image.
     * 
     * @return the image
     */
    public ImageIcon getImage() {
        return image;
    }

    /**
     * Gets the image url.
     * 
     * @return the image url
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Gets the match.
     * 
     * @return the match
     */
    public String getMatch() {
        return match;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Sets the image.
     * 
     * @param image
     *            the new image
     */
    public void setImage(ImageIcon image) {
        this.image = image;
    }

    /**
     * Sets the image url.
     * 
     * @param imageUrl
     *            the imageUrl to set
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Sets the match.
     * 
     * @param match
     *            the match to set
     */
    public void setMatch(String match) {
        this.match = match;
    }

    /**
     * Sets the name.
     * 
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the url.
     * 
     * @param url
     *            the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the available
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * @param available
     *            the available to set
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }
}
