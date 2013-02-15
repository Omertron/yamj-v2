/*
 *      Copyright (c) 2004-2013 YAMJ Members
 *      http://code.google.com/p/moviejukebox/people/list
 *
 *      This file is part of the Yet Another Movie Jukebox (YAMJ).
 *
 *      The YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with the YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: http://code.google.com/p/moviejukebox/
 *
 */
package com.moviejukebox.plugin;

import com.moviejukebox.model.Movie;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;

/**
 * Plugin to retrieve movie data from Swedish movie database www.filmdelta.se Modified from imdb plugin and Kinopoisk
 * plugin written by Yury Sidorov.
 *
 * Contains code for an alternate plugin for fetching information on movies in Swedish
 *
 * @author johan.klinge
 * @version 0.5, 30th April 2009
 */
public class FilmDeltaSEPlugin extends ImdbPlugin {

    private static final Logger logger = Logger.getLogger(FilmDeltaSEPlugin.class);
    private static final String LOG_MESSAGE = "FilmDeltaSEPlugin: ";
    public static final String FILMDELTA_PLUGIN_ID = "filmdelta";
    protected TheTvDBPlugin tvdb;

    public FilmDeltaSEPlugin() {
        super();
        logger.debug(LOG_MESSAGE + "plugin created..");
    }

    @Override
    public String getPluginID() {
        return FILMDELTA_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie mediaFile) {
        boolean retval = false;
        boolean imdbScanned = false;
        String filmdeltaId = mediaFile.getId(FILMDELTA_PLUGIN_ID);
        String imdbId = mediaFile.getId(ImdbPlugin.IMDB_PLUGIN_ID);

        // if IMDB id is specified in the NFO scan imdb first
        // (to get a valid movie title and improve detection rate
        // for getFilmdeltaId-function)
        if (StringTools.isValidString(imdbId)) {
            super.scan(mediaFile);
            imdbScanned = true;
        }
        // get filmdeltaId (if not already set in nfo)
        if (StringTools.isNotValidString(filmdeltaId)) {
            // find a filmdeltaId (url) from google
            filmdeltaId = getFilmdeltaId(mediaFile.getTitle(), mediaFile.getYear(), mediaFile.getSeason());
            if (StringTools.isValidString(filmdeltaId)) {
                mediaFile.setId(FILMDELTA_PLUGIN_ID, filmdeltaId);
            }
        }
        // always scrape info from imdb or tvdb
        if (mediaFile.isTVShow()) {
            tvdb = new TheTvDBPlugin();
            retval = tvdb.scan(mediaFile);
        } else if (!imdbScanned) {
            retval = super.scan(mediaFile);
        }

        // only scrape filmdelta if a valid filmdeltaId was found
        // and the movie is not a tvshow
        if (StringTools.isValidString(filmdeltaId) && !mediaFile.isTVShow()) {
            retval = updateFilmdeltaMediaInfo(mediaFile, filmdeltaId);
        }

        return retval;
    }

    /*
     * Find id from url in nfo. Format: - http://www.filmdelta.se/filmer/<digits>/<movie_name>/ OR -
     * http://www.filmdelta.se/prevsearch/<text>/filmer/<digits>/<movie_name>
     */
    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        // Always look for imdb id look for ttXXXXXX
        super.scanNFO(nfo, movie);
        logger.debug(LOG_MESSAGE + "Scanning NFO for Filmdelta Id");

        boolean result = true;
        int beginIndex = nfo.indexOf("www.filmdelta.se/prevsearch");
        if (beginIndex != -1) {
            beginIndex = beginIndex + 27;
            String filmdeltaId = makeFilmDeltaId(nfo, beginIndex, 2);
            movie.setId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID, filmdeltaId);
            logger.debug(LOG_MESSAGE + "id found in nfo = " + movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
        } else if (nfo.indexOf("www.filmdelta.se/filmer") != -1) {
            beginIndex = nfo.indexOf("www.filmdelta.se/filmer") + 24;
            String filmdeltaId = makeFilmDeltaId(nfo, beginIndex, 0);
            movie.setId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID, filmdeltaId);
            logger.debug(LOG_MESSAGE + "id found in nfo = " + movie.getId(FilmDeltaSEPlugin.FILMDELTA_PLUGIN_ID));
        } else {
            logger.debug(LOG_MESSAGE + "no id found in nfo for movie: " + movie.getTitle());
            result = false;
        }
        return result;
    }

    /**
     * retrieve FilmDeltaID matching the specified movie name and year. This routine is based on a google request.
     */
    protected String getFilmdeltaId(String movieName, String year, int season) {
        try {
            StringBuilder sb = new StringBuilder("http://www.google.se/search?hl=sv&q=");
            sb.append(URLEncoder.encode(movieName, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+").append(year);
            }
            sb.append(URLEncoder.encode("+site:filmdelta.se/filmer", "UTF-8"));
            String googleHtml = webBrowser.request(sb.toString());
            // String <ul><li is only present in the google page when
            // no matches are found so check if we got a page with results
            if (!googleHtml.contains("<ul><li")) {
                // we have a a google page with valid filmdelta links
                int beginIndex = googleHtml.indexOf("www.filmdelta.se/filmer/") + 24;
                String filmdeltaId = makeFilmDeltaId(googleHtml, beginIndex, 0);
                // regex to match that a valid filmdeltaId contains at least 3 numbers,
                // a dash, and one or more letters (may contain [-&;])
                if (filmdeltaId.matches("\\d{3,}/.+")) {
                    logger.debug(LOG_MESSAGE + "filmdelta id found = " + filmdeltaId);
                    return filmdeltaId;
                } else {
                    logger.info(LOG_MESSAGE + "Found a filmdeltaId but it's not valid. Id: " + filmdeltaId);
                    return Movie.UNKNOWN;
                }
            } else {
                // no valid results for the search
                return Movie.UNKNOWN;
            }

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "error retreiving Filmdelta Id for movie : " + movieName);
            logger.error(LOG_MESSAGE + "Error : " + error.getMessage());
            return Movie.UNKNOWN;
        }
    }

    /*
     * Utility method to make a filmdelta id from a string containing a filmdelta url
     */
    private String makeFilmDeltaId(String nfo, int beginIndex, int skip) {
        StringTokenizer st = new StringTokenizer(new String(nfo.substring(beginIndex)), "/");
        for (int i = 0; i < skip; i++) {
            st.nextToken();
        }
        String result = st.nextToken() + "/" + st.nextToken();
        try {
            result = URLDecoder.decode(result, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.warn(LOG_MESSAGE + "in makeFilmDeltaId for string : " + nfo);
            logger.warn(LOG_MESSAGE + "Error : " + e.getMessage());
        }
        return result;
    }

    /*
     * Scan Filmdelta html page for the specified movie
     */
    protected boolean updateFilmdeltaMediaInfo(Movie movie, String filmdeltaId) {

        // fetch filmdelta html page for movie
        String fdeltaHtml = getFilmdeltaHtml(filmdeltaId);

        if (!fdeltaHtml.equals(Movie.UNKNOWN)) {
            getFilmdeltaTitle(movie, fdeltaHtml);
            getFilmdeltaPlot(movie, fdeltaHtml);
            // Genres - only fetch if there is no IMDb results
            if (movie.getGenres().isEmpty()) {
                getFilmdeltaGenres(movie, fdeltaHtml);
            }
            getFilmdeltaDirector(movie, fdeltaHtml);
            getFilmdeltaCast(movie, fdeltaHtml);
            getFilmdeltaCountry(movie, fdeltaHtml);
            getFilmdeltaYear(movie, fdeltaHtml);
            getFilmdeltaRating(movie, fdeltaHtml);
            getFilmdeltaRuntime(movie, fdeltaHtml);
        }
        return true;
    }

    private String getFilmdeltaHtml(String filmdeltaId) {
        String result = Movie.UNKNOWN;
        try {
            logger.debug(LOG_MESSAGE + "searchstring: " + "http://www.filmdelta.se/filmer/" + filmdeltaId);
            result = webBrowser.request("http://www.filmdelta.se/filmer/" + filmdeltaId + "/");
            // logger.debug("result from filmdelta: " + result);

            result = removeIllegalHtmlChars(result);

        } catch (Exception error) {
            logger.error(LOG_MESSAGE + "Failed retreiving movie data from filmdelta.se : " + filmdeltaId);
            logger.error(SystemTools.getStackTrace(error));
        }
        return result;
    }

    /*
     * utility method to remove illegal html characters from the page that is scraped by the webbrower.request(), ugly as hell, gotta be a better way to do
     * this..
     */
    protected String removeIllegalHtmlChars(String result) {
        String cleanResult = result.replaceAll("\u0093", "&quot;");
        cleanResult = cleanResult.replaceAll("\u0094", "&quot;");
        cleanResult = cleanResult.replaceAll("\u00E4", "&auml;");
        cleanResult = cleanResult.replaceAll("\u00E5", "&aring;");
        cleanResult = cleanResult.replaceAll("\u00F6", "&ouml;");
        cleanResult = cleanResult.replaceAll("\u00C4", "&Auml;");
        cleanResult = cleanResult.replaceAll("\u00C5", "&Aring;");
        cleanResult = cleanResult.replaceAll("\u00D6", "&Ouml;");
        return cleanResult;
    }

    private void getFilmdeltaTitle(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteTitle(movie, FILMDELTA_PLUGIN_ID)) {
            String newTitle = HTMLTools.extractTag(fdeltaHtml, "title>", 0, "<");
            // check if everything is ok
            if (StringTools.isValidString(newTitle)) {
                //split the string so that we get the title at index 0
                String[] titleArray = newTitle.split("-\\sFilmdelta");
                newTitle = titleArray[0];
                logger.debug(LOG_MESSAGE + "scraped title: " + newTitle);
                movie.setTitle(newTitle.trim(), FILMDELTA_PLUGIN_ID);
            } else {
                logger.debug(LOG_MESSAGE + "Error scraping title");
            }
        }

        if (OverrideTools.checkOverwriteOriginalTitle(movie, FILMDELTA_PLUGIN_ID)) {
            String originalTitle = HTMLTools.extractTag(fdeltaHtml, "riginaltitel</h4>", 2);
            logger.debug(LOG_MESSAGE + "scraped original title: " + originalTitle);
            movie.setOriginalTitle(originalTitle, FILMDELTA_PLUGIN_ID);
        }
    }

    protected void getFilmdeltaPlot(Movie movie, String fdeltaHtml) {
        String extracted = HTMLTools.extractTag(fdeltaHtml, "<div class=\"text\">", "</p>");
        //strip remaining html tags
        extracted = HTMLTools.stripTags(extracted);
        if (StringTools.isValidString(extracted)) {

            if (OverrideTools.checkOverwritePlot(movie, FILMDELTA_PLUGIN_ID)) {
                movie.setPlot(extracted, FILMDELTA_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteOutline(movie, FILMDELTA_PLUGIN_ID)) {
                //CJK 2010-09-15 filmdelta.se has no outlines - set outline to same as plot
                movie.setOutline(extracted, FILMDELTA_PLUGIN_ID);
            }
        } else {
            logger.info(LOG_MESSAGE + "error finding plot for movie: " + movie.getTitle());
        }
    }

    private void getFilmdeltaGenres(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteGenres(movie, FILMDELTA_PLUGIN_ID)) {
            List<String> newGenres = new ArrayList<String>();

            List<String> filmdeltaGenres = HTMLTools.extractTags(fdeltaHtml, "<h4>Genre</h4>", "</div>", "<h5>", "</h5>");
            for (String genre : filmdeltaGenres) {
                if (genre.length() > 0) {
                    genre = new String(genre.substring(0, genre.length() - 5));
                    newGenres.add(genre);
                }
            }

            if (!newGenres.isEmpty()) {
                movie.setGenres(newGenres, FILMDELTA_PLUGIN_ID);
                logger.debug(LOG_MESSAGE + "scraped genres: " + movie.getGenres().toString());
            }
        }
    }

    private void getFilmdeltaDirector(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteDirectors(movie, FILMDELTA_PLUGIN_ID)) {
            List<String> filmdeltaDirectors = HTMLTools.extractTags(fdeltaHtml, "<h4>Regiss&ouml;r</h4>", "</div>", "<h5>", "</h5>");
            StringBuilder newDirector = new StringBuilder();

            if (!filmdeltaDirectors.isEmpty()) {
                for (String dir : filmdeltaDirectors) {
                    dir = new String(dir.substring(0, dir.length() - 4));
                    newDirector.append(dir).append(Movie.SPACE_SLASH_SPACE);
                }

                movie.setDirector(newDirector.substring(0, newDirector.length() - 3), FILMDELTA_PLUGIN_ID);
                logger.debug(LOG_MESSAGE + "scraped director: " + movie.getDirector());
            }
        }
    }

    private void getFilmdeltaCast(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteActors(movie, FILMDELTA_PLUGIN_ID)) {
            Collection<String> newCast = new ArrayList<String>();

            for (String actor : HTMLTools.extractTags(fdeltaHtml, "<h4>Sk&aring;despelare</h4>", "</div>", "<h5>", "</h5>")) {
                String[] newActor = actor.split("</a>");
                newCast.add(newActor[0]);
            }
            if (newCast.size() > 0) {
                movie.setCast(newCast, FILMDELTA_PLUGIN_ID);
                logger.debug(LOG_MESSAGE + "scraped actor: " + movie.getCast().toString());
            }
        }
    }

    private void getFilmdeltaCountry(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteCountry(movie, FILMDELTA_PLUGIN_ID)) {
            String country = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 3);
            movie.setCountry(country, FILMDELTA_PLUGIN_ID);
            logger.debug(LOG_MESSAGE + "scraped country: " + movie.getCountry());
        }
    }

    private void getFilmdeltaYear(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteYear(movie, FILMDELTA_PLUGIN_ID)) {
            String year = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 5);
            String[] newYear = year.split("\\s");
            if (newYear.length > 1) {
                movie.setYear(newYear[1], FILMDELTA_PLUGIN_ID);
                logger.debug(LOG_MESSAGE + "scraped year: " + movie.getYear());
            } else {
                logger.debug(LOG_MESSAGE + "error scraping year for movie: " + movie.getTitle());
            }
        }
    }

    private void getFilmdeltaRating(Movie movie, String fdeltaHtml) {
        String rating = HTMLTools.extractTag(fdeltaHtml, "<h4>Medlemmarna</h4>", 3, "<");
        int newRating;
        // check if valid rating string is found
        if (rating.indexOf("Snitt") != -1) {
            String[] result = rating.split(":");
            rating = result[result.length - 1];
            logger.debug(LOG_MESSAGE + "filmdelta rating: " + rating);
            // multiply by 20 to make comparable to IMDB-ratings
            newRating = (int) (Float.parseFloat(rating) * 20);
        } else {
            logger.warn(LOG_MESSAGE + "error finding filmdelta rating for movie " + movie.getTitle());
            return;
        }

        if (newRating != 0) {
            movie.addRating(FILMDELTA_PLUGIN_ID, newRating);
        }
    }

    private void getFilmdeltaRuntime(Movie movie, String fdeltaHtml) {
        if (OverrideTools.checkOverwriteRuntime(movie, FILMDELTA_PLUGIN_ID)) {
            // Run time
            String runtime = HTMLTools.extractTag(fdeltaHtml, "Land, &aring;r, l&auml;ngd", 7);
            String[] newRunTime = runtime.split("\\s");
            if (newRunTime.length > 2) {
                movie.setRuntime(newRunTime[1], FILMDELTA_PLUGIN_ID);
                logger.debug(LOG_MESSAGE + "scraped runtime: " + movie.getRuntime());
            }
        }
    }
}
