/*
 *      Copyright (c) 2004-2011 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list 
 *  
 *      Web: http://code.google.com/p/moviejukebox/
 *  
 *      This software is licensed under a Creative Commons License
 *      See this page: http://code.google.com/p/moviejukebox/wiki/License
 *  
 *      For any reuse or distribution, you must make clear to others the 
 *      license terms of this work.  
 */
package com.moviejukebox.plugin.poster;

import java.util.List;
import org.apache.log4j.Logger;

import com.moviejukebox.model.IImage;
import com.moviejukebox.model.IMovieBasicInformation;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.Image;
import com.moviejukebox.model.Movie;
import com.moviejukebox.plugin.TheMovieDbPlugin;
import com.moviejukebox.scanner.artwork.PosterScanner;
import com.moviejukebox.themoviedb.TheMovieDb;
import com.moviejukebox.themoviedb.model.Artwork;
import com.moviejukebox.themoviedb.model.MovieDB;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.WebBrowser;

public class MovieDbPosterPlugin extends AbstractMoviePosterPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");
    private String API_KEY;
    private String language;
    private TheMovieDb theMovieDb;

    public MovieDbPosterPlugin() {
        super();
        
        // Check to see if we are needed
        if (!isNeeded()) {
            return;
        }
        
        API_KEY = PropertiesUtil.getProperty("API_KEY_TheMovieDB");
        language = PropertiesUtil.getProperty("themoviedb.language", "en-US");
        theMovieDb = new TheMovieDb(API_KEY);
        
        // Set the proxy
        theMovieDb.setProxy(WebBrowser.getMjbProxyHost(), WebBrowser.getMjbProxyPort(), WebBrowser.getMjbProxyUsername(), WebBrowser.getMjbProxyPassword());
        
        // Set the timeouts
        theMovieDb.setTimeout(WebBrowser.getMjbTimeoutConnect(), WebBrowser.getMjbTimeoutRead());
    }

    @Override
    public String getIdFromMovieInfo(String title, String year) {
        theMovieDb = new TheMovieDb(API_KEY);
        List<MovieDB> movieList = theMovieDb.moviedbSearch(title, language);
        
        if (movieList.isEmpty()) {
            return Movie.UNKNOWN;
        } else {
            if (movieList.size() == 1) {
                // Only one movie so return that id
                return movieList.get(0).getId();
            }
            
            for (MovieDB moviedb : movieList) {
                if (TheMovieDb.compareMovies(moviedb, title, year)) {
                    return moviedb.getId();
                }
            }
        }
        return Movie.UNKNOWN;
    }
    
    @Override
    public IImage getPosterUrl(String title, String year) {
        return getPosterUrl(getIdFromMovieInfo(title, year));
    }

    @Override
    public IImage getPosterUrl(String id) {
        String posterURL = Movie.UNKNOWN;
        if (StringTools.isNotValidString(id)) {
            return Image.UNKNOWN;
        }

        MovieDB moviedb = theMovieDb.moviedbGetImages(id, language);

        try {
            if (moviedb != null) {
                List<Artwork> artworkList = moviedb.getArtwork(Artwork.ARTWORK_TYPE_POSTER, Artwork.ARTWORK_SIZE_ORIGINAL);
                if (artworkList.size() > 0) {
                    Image image;
                    boolean validImage = false;
                    
                    for (Artwork artwork : artworkList) {
                        posterURL = artwork.getUrl();
                        image = new Image(posterURL);
                        validImage = PosterScanner.validatePoster(image);
                        if (validImage) {
                            logger.debug("MovieDbPosterPlugin : Movie found on TheMovieDB.org: http://www.themoviedb.org/movie/" + id);
                            break;
                        }
                    }
                } else {
                    logger.debug("MovieDbPosterPlugin: Unable to find posters for " + id);
                }
            } else {
                logger.debug("MovieDbPosterPlugin: Unable to find posters for " + id);
            }
        } catch (Exception error) {
            logger.error("MovieDbPosterPlugin: TheMovieDB.org API Error: " + error.getMessage());
        }
        
        if (StringTools.isValidString(posterURL)) {
            return new Image(posterURL);
        } else {
            return Image.UNKNOWN;
        }
    }

    @Override
    public String getName() {
        return "themoviedb";
    }

    @Override
    public IImage getPosterUrl(Identifiable ident, IMovieBasicInformation movieInformation) {
        String id = getId(ident);
        
        if (StringTools.isNotValidString(id)) {
            id = getIdFromMovieInfo(movieInformation.getOriginalTitle(), movieInformation.getYear());
            // Id found
            if (StringTools.isValidString(id)) {
                ident.setId(getName(), id);
            }
        }

        if (StringTools.isValidString(id)) {
            return getPosterUrl(id);
        }
        return Image.UNKNOWN;
    }

    private String getId(Identifiable ident) {
        String response = Movie.UNKNOWN;
        if (ident != null) {
            String imdbID = ident.getId(TheMovieDbPlugin.IMDB_PLUGIN_ID);
            String tmdbID = ident.getId(TheMovieDbPlugin.TMDB_PLUGIN_ID);
            MovieDB moviedb;
            // First look to see if we have a TMDb ID as this will make looking the film up easier
            if (StringTools.isValidString(tmdbID)) {
                response = tmdbID;
            } else if (StringTools.isValidString(imdbID)) {
                // Search based on IMDb ID
                moviedb = theMovieDb.moviedbImdbLookup(imdbID, language);
                if (moviedb != null) {
                    tmdbID = moviedb.getId();
                    if (tmdbID != null && !tmdbID.equals("")) {
                        response = tmdbID;
                    } else {
                        logger.info("MovieDvPosterPlugin: No TMDb ID found for movie!");
                    }
                }
            }
        }
        return response;
    }

}