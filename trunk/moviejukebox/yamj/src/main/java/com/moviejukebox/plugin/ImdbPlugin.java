/*
 *      Copyright (c) 2004-2014 YAMJ Members
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

import com.moviejukebox.model.Award;
import com.moviejukebox.model.AwardEvent;
import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.Identifiable;
import com.moviejukebox.model.ImdbSiteDataDefinition;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.model.Person;
import com.moviejukebox.scanner.artwork.FanartScanner;
import com.moviejukebox.tools.AspectRatioTools;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.HTMLTools;
import com.moviejukebox.tools.OverrideTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.moviejukebox.tools.StringTools;
import static com.moviejukebox.tools.StringTools.isNotValidString;
import static com.moviejukebox.tools.StringTools.isValidString;
import static com.moviejukebox.tools.StringTools.trimToLength;
import com.moviejukebox.tools.SystemTools;
import com.moviejukebox.tools.WebBrowser;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.pojava.datetime.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImdbPlugin implements MovieDatabasePlugin {

    public static final String IMDB_PLUGIN_ID = "imdb";
    private static final Logger LOG = LoggerFactory.getLogger(ImdbPlugin.class);
    private static final String LOG_MESSAGE = "ImdbPlugin: ";
    protected String preferredCountry;
    private final String imdbPlot;
    protected WebBrowser webBrowser;
    protected boolean downloadFanart;
    private final boolean extractCertificationFromMPAA;
    private final boolean fullInfo;
    protected String fanartToken;
    protected String fanartExtension;
    private final int preferredBiographyLength;
    private final int preferredFilmographyMax;
    protected int actorMax;
    protected int directorMax;
    protected int writerMax;
    private final int triviaMax;
    protected ImdbSiteDataDefinition siteDefinition;
    protected static final String DEFAULT_SITE_DEF = "us";
    protected ImdbInfo imdbInfo;
    protected AspectRatioTools aspectTools;
    private final boolean skipFaceless;
    private final boolean skipVG;
    private final boolean skipTV;
    private final boolean skipV;
    private final List<String> jobsInclude;
    private final boolean scrapeAwards;   // Should we scrape the award information
    private final boolean scrapeWonAwards;// Should we scrape the won awards only
    private final boolean scrapeBusiness; // Should we scrape the business information
    private final boolean scrapeTrivia;   // Shoulw we scrape the trivia information
    // Literals
    private static final String HTML_H5_END = ":</h5>";
    private static final String HTML_H5_START = "<h5>";
    private static final String HTML_DIV_END = "</div>";
    private static final String HTML_A_END = "</a>";
    private static final String HTML_A_START = "<a ";
    private static final String HTML_SLASH_PIPE = "\\|";
    private static final String HTML_SLASH_QUOTE = "/\"";
    private static final String HTML_QUOTE_GT = "\">";
    private static final String HTML_NAME = "name/";
    private static final String HTML_TABLE_END = "</table>";
    private static final String HTML_TD_END = "</td>";
    private static final String HTML_H4_END = ":</h4>";
    private static final String HTML_SITE = ".imdb.com";
    private static final String HTML_SITE_FULL = "http://www.imdb.com/";
    private static final String HTML_TITLE = "title/";
    private static final String HTML_BREAK = "<br/>";
    private static final String HTML_SPAN_END = "</span>";
    private static final String HTML_GT = ">";
    // Patterns for the name searching
    private static final String STRING_PATTERN_NAME = "(?:.*?)/name/(nm\\d+)/(?:.*?)'name'>(.*?)</a>(?:.*?)";
    private static final String STRING_PATTERN_CHAR = "(?:.*?)/character/(ch\\d+)/(?:.*?)>(.*?)</a>(?:.*)";
    private static final Pattern PATTERN_PERSON_NAME = Pattern.compile(STRING_PATTERN_NAME, Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_PERSON_CHAR = Pattern.compile(STRING_PATTERN_CHAR, Pattern.CASE_INSENSITIVE);
    // AKA scraping
    private final boolean akaScrapeTitle;
    private final String[] akaMatchingCountries;
    private final String[] akaIgnoreVersions;
    // Patterns
    private static final Pattern PATTERN_DOB = Pattern.compile("(\\d{1,2})-(\\d{1,2})");

    public ImdbPlugin() {
        imdbInfo = new ImdbInfo();
        siteDefinition = imdbInfo.getSiteDef();
        aspectTools = new AspectRatioTools();

        webBrowser = new WebBrowser();

        preferredCountry = PropertiesUtil.getProperty("imdb.preferredCountry", "USA");
        imdbPlot = PropertiesUtil.getProperty("imdb.plot", "short");
        downloadFanart = PropertiesUtil.getBooleanProperty("fanart.movie.download", Boolean.FALSE);
        fanartToken = PropertiesUtil.getProperty("mjb.scanner.fanartToken", ".fanart");
        fanartExtension = PropertiesUtil.getProperty("fanart.format", "jpg");
        extractCertificationFromMPAA = PropertiesUtil.getBooleanProperty("imdb.getCertificationFromMPAA", Boolean.TRUE);
        fullInfo = PropertiesUtil.getBooleanProperty("imdb.full.info", Boolean.FALSE);

        // People properties
        preferredBiographyLength = PropertiesUtil.getIntProperty("plugin.biography.maxlength", 500);
        preferredFilmographyMax = PropertiesUtil.getIntProperty("plugin.filmography.max", 20);
        actorMax = PropertiesUtil.getReplacedIntProperty("movie.actor.maxCount", "plugin.people.maxCount.actor", 10);
        directorMax = PropertiesUtil.getReplacedIntProperty("movie.director.maxCount", "plugin.people.maxCount.director", 2);
        writerMax = PropertiesUtil.getReplacedIntProperty("movie.writer.maxCount", "plugin.people.maxCount.writer", 3);
        skipFaceless = PropertiesUtil.getBooleanProperty("plugin.people.skip.faceless", Boolean.FALSE);
        skipVG = PropertiesUtil.getBooleanProperty("plugin.people.skip.VG", Boolean.TRUE);
        skipTV = PropertiesUtil.getBooleanProperty("plugin.people.skip.TV", Boolean.FALSE);
        skipV = PropertiesUtil.getBooleanProperty("plugin.people.skip.V", Boolean.FALSE);
        jobsInclude = Arrays.asList(PropertiesUtil.getProperty("plugin.filmography.jobsInclude", "Director,Writer,Actor,Actress").split(","));

        // Trivia properties
        triviaMax = PropertiesUtil.getIntProperty("plugin.trivia.maxCount", 15);

        // Award properties
        String tmpAwards = PropertiesUtil.getProperty("mjb.scrapeAwards", FALSE);
        scrapeWonAwards = tmpAwards.equalsIgnoreCase("won");
        scrapeAwards = tmpAwards.equalsIgnoreCase(TRUE) || scrapeWonAwards;

        // Business properties
        scrapeBusiness = PropertiesUtil.getBooleanProperty("mjb.scrapeBusiness", Boolean.FALSE);

        // Trivia properties
        scrapeTrivia = PropertiesUtil.getBooleanProperty("mjb.scrapeTrivia", Boolean.FALSE);

        // Other properties
        akaScrapeTitle = PropertiesUtil.getBooleanProperty("imdb.aka.scrape.title", Boolean.FALSE);
        akaIgnoreVersions = PropertiesUtil.getProperty("imdb.aka.ignore.versions", "").split(",");

        String fallbacks = PropertiesUtil.getProperty("imdb.aka.fallback.countries", "");
        if (StringTools.isNotValidString(fallbacks)) {
            akaMatchingCountries = new String[]{preferredCountry};
        } else {
            akaMatchingCountries = (preferredCountry + "," + fallbacks).split(",");
        }
    }

    @Override
    public String getPluginID() {
        return IMDB_PLUGIN_ID;
    }

    @Override
    public boolean scan(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        if (isNotValidString(imdbId)) {
            imdbId = imdbInfo.getImdbId(movie.getTitle(), movie.getYear(), movie.isTVShow());
            movie.setId(IMDB_PLUGIN_ID, imdbId);
        }

        boolean retval = Boolean.FALSE;
        if (isValidString(imdbId)) {
            retval = updateImdbMediaInfo(movie);
        }
        return retval;
    }

    protected String getPreferredValue(List<String> values, boolean useLast) {
        String value = Movie.UNKNOWN;

        if (useLast) {
            Collections.reverse(values);
        }

        for (String text : values) {
            String country = null;

            int pos = text.indexOf(':');
            if (pos != -1) {
                country = text.substring(0, pos);
                text = text.substring(pos + 1);
            }
            pos = text.indexOf('(');
            if (pos != -1) {
                text = text.substring(0, pos).trim();
            }

            if (country == null) {
                if (value.equals(Movie.UNKNOWN)) {
                    value = text;
                }
            } else {
                if (country.equals(preferredCountry)) {
                    value = text;
                    // No need to continue scanning
                    break;
                }
            }
        }
        return HTMLTools.stripTags(value);
    }

    /**
     * Scan IMDB HTML page for the specified movie
     */
    private boolean updateImdbMediaInfo(Movie movie) {
        String imdbID = movie.getId(IMDB_PLUGIN_ID);
        boolean returnStatus = Boolean.FALSE;

        try {
            if (!imdbID.startsWith("tt")) {
                imdbID = "tt" + imdbID;
                // Correct the ID if it's wrong
                movie.setId(IMDB_PLUGIN_ID, imdbID);
            }

            String xml = getImdbUrl(movie);

            // Add the combined tag to the end of the request if required
            if (fullInfo) {
                xml += "combined";
            }

            xml = webBrowser.request(xml, siteDefinition.getCharset());

            if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW) && (xml.contains("\"tv-extra\"") || xml.contains("\"tv-series-series\""))) {
                movie.setMovieType(Movie.TYPE_TVSHOW);
                return Boolean.FALSE;
            }

            // We can work out if this is the new site by looking for " - IMDb" at the end of the title
            String title = HTMLTools.extractTag(xml, "<title>");
            if (!movie.getMovieType().equals(Movie.TYPE_TVSHOW) && title.contains("(TV Series")) {
                movie.setMovieType(Movie.TYPE_TVSHOW);
                return Boolean.FALSE;
            }

            // Check for the new version and correct the title if found
            boolean imdbNewVersion = Boolean.FALSE;
            if (StringUtils.endsWithIgnoreCase(title, " - imdb")) {
                title = title.substring(0, title.length() - 7);
                imdbNewVersion = Boolean.TRUE;
            } else if (StringUtils.startsWithIgnoreCase(title, "imdb - ")) {
                title = title.substring(7);
                imdbNewVersion = Boolean.TRUE;
            }

            // Remove the (VG) or (V) tags from the title
            title = title.replaceAll(" \\([VG|V]\\)$", "");

            //String yearPattern = "(?i).\\((?:TV.|VIDEO.)?(\\d{4})(?:/[^\\)]+)?\\)";
            String yearPattern = "(?i).\\((?:TV.|VIDEO.)?(\\d{4})";
            Pattern pattern = Pattern.compile(yearPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                // If we've found a year, set it in the movie
                if (OverrideTools.checkOverwriteYear(movie, IMDB_PLUGIN_ID)) {
                    movie.setYear(matcher.group(1), IMDB_PLUGIN_ID);
                }

                // Remove the year from the title
                title = title.substring(0, title.indexOf(matcher.group(0)));
            }

            if (OverrideTools.checkOverwriteTitle(movie, IMDB_PLUGIN_ID)) {
                movie.setTitle(title, IMDB_PLUGIN_ID);
            }

            if (OverrideTools.checkOverwriteOriginalTitle(movie, IMDB_PLUGIN_ID)) {
                String originalTitle = title;
                if (xml.contains("<span class=\"title-extra\">")) {
                    originalTitle = HTMLTools.extractTag(xml, "<span class=\"title-extra\">", "</span>");
                    if (originalTitle.contains("(original title)")) {
                        originalTitle = originalTitle.replace(" <i>(original title)</i>", "");
                    } else {
                        originalTitle = title;
                    }
                }
                movie.setOriginalTitle(originalTitle, IMDB_PLUGIN_ID);
            }

            ImdbSiteDataDefinition siteDef;
            if (imdbNewVersion) {
                // NEW FORMAT

                // If we are using sitedef=labs, there's no need to change it
                if (imdbInfo.getImdbSite().equals("labs")) {
                    siteDef = this.siteDefinition;
                } else {
                    // Overwrite the normal siteDef with a v2 siteDef if it exists
                    siteDef = imdbInfo.getSiteDef(imdbInfo.getImdbSite() + "2");
                    if (siteDef == null) {
                        // c2 siteDef doesn't exist, so use labs to atleast return something
                        LOG.error(LOG_MESSAGE + "No new format definition found for language '" + imdbInfo.getImdbSite() + "' using default language instead.");
                        siteDef = imdbInfo.getSiteDef(DEFAULT_SITE_DEF);
                    }
                }

                updateInfoNew(movie, xml, siteDef);
            } else {
                // OLD FORMAT

                // use site definition
                siteDef = this.siteDefinition;
                updateInfoOld(movie, xml, siteDef);
            }

            // update common values; matching old and new format
            updateInfoCommon(movie, xml, siteDef, imdbNewVersion);

            if (scrapeAwards) {
                updateAwards(movie);        // Issue 1901: Awards
            }

            if (scrapeBusiness) {
                updateBusiness(movie);      // Issue 2012: Financial information about movie
            }

            if (scrapeTrivia) {
                updateTrivia(movie);        // Issue 2013: Add trivia
            }

            // TODO: Move this check out of here, it doesn't belong.
            if (downloadFanart && isNotValidString(movie.getFanartURL())) {
                movie.setFanartURL(getFanartURL(movie));
                if (isValidString(movie.getFanartURL())) {
                    movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
                }
            }

            // always true
            returnStatus = Boolean.TRUE;

        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving IMDb data for movie : " + movie.getId(IMDB_PLUGIN_ID));
            LOG.error(SystemTools.getStackTrace(error));
        }

        return returnStatus;
    }

    /**
     * Process the old IMDb formatted web page
     *
     * @param movie
     * @param xml
     * @param siteDef
     * @throws IOException
     */
    @Deprecated
    private void updateInfoOld(Movie movie, String xml, ImdbSiteDataDefinition siteDef) throws IOException {
        if (movie.getRating() == -1) {
            String rating = HTMLTools.extractTag(xml, "<div class=\"starbar-meta\">", "</b>").replace(",", ".");
            movie.addRating(IMDB_PLUGIN_ID, parseRating(HTMLTools.stripTags(rating)));
        }

        // TOP250
        if (OverrideTools.checkOverwriteTop250(movie, IMDB_PLUGIN_ID)) {
            movie.setTop250(HTMLTools.extractTag(xml, "Top 250: #"), IMDB_PLUGIN_ID);
        }

        // RELEASE DATE
        if (OverrideTools.checkOverwriteReleaseDate(movie, IMDB_PLUGIN_ID)) {
            movie.setReleaseDate(HTMLTools.extractTag(xml, HTML_H5_START + siteDef.getReleaseDate() + HTML_H5_END, 1), IMDB_PLUGIN_ID);
        }

        // RUNTIME
        if (OverrideTools.checkOverwriteRuntime(movie, IMDB_PLUGIN_ID)) {
            movie.setRuntime(getPreferredValue(HTMLTools.extractTags(xml, HTML_H5_START + siteDef.getRuntime() + HTML_H5_END), false), IMDB_PLUGIN_ID);
        }

        // COUNTRY
        if (OverrideTools.checkOverwriteCountry(movie, IMDB_PLUGIN_ID)) {
            List<String> countries = new ArrayList<String>();
            for (String country : HTMLTools.extractTags(xml, HTML_H5_START + siteDef.getCountry() + HTML_H5_END, HTML_DIV_END)) {
                countries.add(HTMLTools.removeHtmlTags(country));
            }
            movie.setCountries(countries, IMDB_PLUGIN_ID);
        }

        // COMPANY
        if (OverrideTools.checkOverwriteCompany(movie, IMDB_PLUGIN_ID)) {
            for (String company : HTMLTools.extractTags(xml, HTML_H5_START + siteDef.getCompany() + HTML_H5_END, HTML_DIV_END, "<a href", HTML_A_END)) {
                if (company != null) {
                    // TODO Save more than one company
                    movie.setCompany(company, IMDB_PLUGIN_ID);
                    break;
                }
            }
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(movie, IMDB_PLUGIN_ID)) {
            List<String> newGenres = new ArrayList<String>();
            for (String genre : HTMLTools.extractTags(xml, HTML_H5_START + siteDef.getGenre() + HTML_H5_END, HTML_DIV_END)) {
                genre = HTMLTools.removeHtmlTags(genre);
                newGenres.add(Library.getIndexingGenre(cleanStringEnding(genre)));
            }
            movie.setGenres(newGenres, IMDB_PLUGIN_ID);
        }

        // QUOTE
        if (OverrideTools.checkOverwriteQuote(movie, IMDB_PLUGIN_ID)) {
            for (String quote : HTMLTools.extractTags(xml, HTML_H5_START + siteDef.getQuotes() + HTML_H5_END, HTML_DIV_END, "<a href=\"/name/nm", "</a class=\"")) {
                if (quote != null) {
                    quote = HTMLTools.stripTags(quote);
                    movie.setQuote(cleanStringEnding(quote), IMDB_PLUGIN_ID);
                    break;
                }
            }
        }

        String imdbOutline = Movie.UNKNOWN;
        int plotBegin = xml.indexOf((HTML_H5_START + siteDef.getPlot() + HTML_H5_END));
        if (plotBegin > -1) {
            plotBegin += (HTML_H5_START + siteDef.getPlot() + HTML_H5_END).length();
            // search HTML_A_START for the international variety of "more" oder "add synopsis"
            int plotEnd = xml.indexOf(HTML_A_START, plotBegin);
            int plotEndOther = xml.indexOf(HTML_A_END, plotBegin);
            if (plotEnd > -1 || plotEndOther > -1) {
                if ((plotEnd > -1 && plotEndOther < plotEnd) || plotEnd == -1) {
                    plotEnd = plotEndOther;
                }

                String outline = HTMLTools.stripTags(xml.substring(plotBegin, plotEnd)).trim();
                if (outline.length() > 0) {
                    if (outline.endsWith("|")) {
                        // Remove the bar character from the end of the plot
                        outline = outline.substring(0, outline.length() - 1);
                    }

                    if (isValidString(outline)) {
                        imdbOutline = cleanStringEnding(outline);
                    } else {
                        // Ensure the outline isn't blank or null
                        imdbOutline = Movie.UNKNOWN;
                    }
                }
            }
        }

        // OUTLINE
        if (OverrideTools.checkOverwriteOutline(movie, IMDB_PLUGIN_ID)) {
            movie.setOutline(imdbOutline, IMDB_PLUGIN_ID);
        }

        // PLOT
        if (OverrideTools.checkOverwritePlot(movie, IMDB_PLUGIN_ID)) {
            String plot = Movie.UNKNOWN;
            if (imdbPlot.equalsIgnoreCase("long")) {
                plot = getLongPlot(movie);
            }

            // even if "long" is set we will default to the "short" one if none was found
            if (StringTools.isNotValidString(plot)) {
                plot = imdbOutline;
            }

            movie.setPlot(plot, IMDB_PLUGIN_ID);
        }

        // CERTIFICATION
        if (OverrideTools.checkOverwriteCertification(movie, IMDB_PLUGIN_ID)) {
            String certification = movie.getCertification();
            if (extractCertificationFromMPAA) {
                String mpaa = HTMLTools.extractTag(xml, "<h5><a href=\"/mpaa\">MPAA</a>:</h5>", 1);
                if (StringTools.isValidString(mpaa)) {
                    certification = StringTools.processMpaaCertification(siteDef.getRated(), mpaa);
                }
            }

            if (isNotValidString(certification)) {
                certification = getPreferredValue(HTMLTools.extractTags(xml, HTML_H5_START + siteDef.getCertification() + HTML_H5_END, HTML_DIV_END,
                        "<a href=\"/search/title?certificates=", HTML_A_END), true);
            }

            if (isNotValidString(certification)) {
                certification = getPreferredValue(HTMLTools.extractTags(xml, HTML_H5_START + siteDef.getCertification() + HTML_H5_END + "<div class=\"info-content\">", HTML_DIV_END,
                        null, "|", false), true);
            }

            if (isNotValidString(certification)) {
                certification = Movie.NOTRATED;
            }

            movie.setCertification(certification, IMDB_PLUGIN_ID);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(movie, IMDB_PLUGIN_ID)) {
            Pattern getYear = Pattern.compile("(?:\\s*" + "\\((\\d{4})(?:/[^\\)]+)?\\)|<a href=\"/year/(\\d{4}))");
            Matcher m = getYear.matcher(xml);
            if (m.find()) {
                String year = m.group(1);
                if (year == null || year.isEmpty()) {
                    year = m.group(2);
                }

                if (year != null && !year.isEmpty()) {
                    movie.setYear(year, IMDB_PLUGIN_ID);
                }
            }

            // second approach
            if (isNotValidString(movie.getYear())) {
                movie.setYear(HTMLTools.extractTag(xml, "<a href=\"/year/", 1), IMDB_PLUGIN_ID);
                if (isNotValidString(movie.getYear())) {
                    String fullReleaseDate = HTMLTools.getTextAfterElem(xml, HTML_H5_START + siteDef.getOriginalAirDate() + HTML_H5_END, 0);
                    if (isValidString(fullReleaseDate)) {
                        movie.setYear(fullReleaseDate.split(" ")[2], IMDB_PLUGIN_ID);
                    }
                }
            }
        }

        // TAGLINE
        if (OverrideTools.checkOverwriteTagline(movie, IMDB_PLUGIN_ID)) {
            int startTag = xml.indexOf(HTML_H5_START + siteDef.getTaglines() + HTML_H5_END);
            if (startTag != -1) {
                // We need to work out which of the two formats to use, this is dependent on which comes first "<a class" or "</div"
                String endMarker;
                if (StringUtils.indexOf(xml, "<a class", startTag) < StringUtils.indexOf(xml, HTML_DIV_END, startTag)) {
                    endMarker = "<a class";
                } else {
                    endMarker = HTML_DIV_END;
                }

                // Now look for the right string
                String tagline = HTMLTools.extractTag(xml, HTML_H5_START + siteDef.getTaglines() + HTML_H5_END, endMarker);
                tagline = HTMLTools.stripTags(tagline);
                movie.setTagline(cleanStringEnding(tagline), IMDB_PLUGIN_ID);
            }
        }

        if (movie.isTVShow()) {
            updateTVShowInfo(movie);
        }

        if (downloadFanart && isNotValidString(movie.getFanartURL())) {
            movie.setFanartURL(getFanartURL(movie));
            if (isValidString(movie.getFanartURL())) {
                movie.setFanartFilename(movie.getBaseName() + fanartToken + "." + fanartExtension);
            }
        }
    }

    /**
     * Process the new IMDb format web page
     *
     * @param movie
     * @param xml
     * @param siteDef
     * @throws IOException
     */
    private void updateInfoNew(Movie movie, String xml, ImdbSiteDataDefinition siteDef) throws IOException {
        LOG.debug(LOG_MESSAGE + "Detected new IMDb format for '" + movie.getBaseName() + "'");

        // RATING
        if (movie.getRating(IMDB_PLUGIN_ID) == -1) {
            String srtRating = HTMLTools.extractTag(xml, "star-box-giga-star\">", HTML_DIV_END).replace(",", ".");
            int intRating = parseRating(HTMLTools.stripTags(srtRating));

            // Try another format for the rating
            if (intRating == -1) {
                srtRating = HTMLTools.extractTag(xml, "star-bar-user-rate\">", "</span>").replace(",", ".");
                intRating = parseRating(HTMLTools.stripTags(srtRating));
            }

            movie.addRating(IMDB_PLUGIN_ID, intRating);
        }

        // TOP250
        if (OverrideTools.checkOverwriteTop250(movie, IMDB_PLUGIN_ID)) {
            movie.setTop250(HTMLTools.extractTag(xml, "Top 250 #"), IMDB_PLUGIN_ID);
        }

        // RUNTIME
        if (OverrideTools.checkOverwriteRuntime(movie, IMDB_PLUGIN_ID)) {
            String runtime = siteDef.getRuntime() + HTML_H4_END;
            List<String> runtimes = HTMLTools.extractTags(xml, runtime, HTML_DIV_END, null, "|", Boolean.FALSE);
            runtime = getPreferredValue(runtimes, false);

            // Strip any extraneous characters from the runtime
            int pos = runtime.indexOf("min");
            if (pos > 0) {
                runtime = runtime.substring(0, pos + 3);
            }
            movie.setRuntime(runtime, IMDB_PLUGIN_ID);
        }

        // COUNTRY
        if (OverrideTools.checkOverwriteCountry(movie, IMDB_PLUGIN_ID)) {
            List<String> countries = new ArrayList<String>();
            for (String country : HTMLTools.extractTags(xml, siteDef.getCountry() + HTML_H4_END, HTML_DIV_END, "<a href=\"", HTML_A_END)) {
                countries.add(HTMLTools.removeHtmlTags(country));
            }
            movie.setCountries(countries, IMDB_PLUGIN_ID);
        }

        // COMPANY
        if (OverrideTools.checkOverwriteCompany(movie, IMDB_PLUGIN_ID)) {
            for (String company : HTMLTools.extractTags(xml, siteDef.getCompany() + HTML_H4_END, HTML_DIV_END, "<span class", "</span>")) {
                if (company != null) {
                    // TODO Save more than one company
                    movie.setCompany(company, IMDB_PLUGIN_ID);
                    break;
                }
            }
        }

        // GENRES
        if (OverrideTools.checkOverwriteGenres(movie, IMDB_PLUGIN_ID)) {
            List<String> newGenres = new ArrayList<String>();
            for (String genre : HTMLTools.extractTags(xml, siteDef.getGenre() + HTML_H4_END, HTML_DIV_END)) {
                // Check normally for the genre
                String iGenre = HTMLTools.getTextAfterElem(genre, "<a");
                // Sometimes the genre is just "{genre}</a>???" so try and remove the trailing element
                if (StringTools.isNotValidString(iGenre) && genre.contains(HTML_A_END)) {
                    iGenre = genre.substring(0, genre.indexOf(HTML_A_END));
                }
                newGenres.add(iGenre);
            }
            movie.setGenres(newGenres, IMDB_PLUGIN_ID);
        }

        // QUOTE
        if (OverrideTools.checkOverwriteQuote(movie, IMDB_PLUGIN_ID)) {
            for (String quote : HTMLTools.extractTags(xml, "<h4>" + siteDef.getQuotes() + "</h4>", "<span class=\"", "<br", "<br")) {
                if (quote != null) {
                    quote = HTMLTools.stripTags(quote);
                    movie.setQuote(cleanStringEnding(quote), IMDB_PLUGIN_ID);
                    break;
                }
            }
        }

        // OUTLINE
        if (OverrideTools.checkOverwriteOutline(movie, IMDB_PLUGIN_ID)) {
            // The new outline is at the end of the review section with no preceding text
            String imdbOutline = HTMLTools.extractTag(xml, "<p itemprop=\"description\">", "</p>");
            imdbOutline = cleanStringEnding(HTMLTools.removeHtmlTags(imdbOutline)).trim();

            if (isNotValidString(imdbOutline)) {
                // ensure the outline is set to unknown if it's blank or null
                imdbOutline = Movie.UNKNOWN;
            }
            movie.setOutline(imdbOutline, IMDB_PLUGIN_ID);
        }

        // PLOT
        if (OverrideTools.checkOverwritePlot(movie, IMDB_PLUGIN_ID)) {
            String xmlPlot = Movie.UNKNOWN;

            if (imdbPlot.equalsIgnoreCase("long")) {
                // The new plot is now called Storyline
                xmlPlot = HTMLTools.extractTag(xml, "<h2>" + siteDef.getPlot() + "</h2>", "<em class=\"nobr\">");
                xmlPlot = HTMLTools.removeHtmlTags(xmlPlot).trim();

                // This plot didn't work, look for another version
                if (isNotValidString(xmlPlot)) {
                    xmlPlot = HTMLTools.extractTag(xml, "<h2>" + siteDef.getPlot() + "</h2>", "<span class=\"");
                    xmlPlot = HTMLTools.removeHtmlTags(xmlPlot).trim();
                }

                // This plot didn't work, look for another version
                if (isNotValidString(xmlPlot)) {
                    xmlPlot = HTMLTools.extractTag(xml, "<h2>" + siteDef.getPlot() + "</h2>", "<p>");
                    xmlPlot = HTMLTools.removeHtmlTags(xmlPlot).trim();
                }

                // See if the plot has the "metacritic" text and remove it
                int pos = xmlPlot.indexOf("Metacritic.com)");
                if (pos > 0) {
                    xmlPlot = xmlPlot.substring(pos + "Metacritic.com)".length());
                }

                // Check the length of the plot is OK
                if (isValidString(xmlPlot)) {
                    xmlPlot = cleanStringEnding(xmlPlot);
                } else {
                    // The plot might be blank or null so set it to UNKNOWN
                    xmlPlot = Movie.UNKNOWN;
                }
            }

            // Update the plot with the found plot, or the outline if not found
            if (isValidString(xmlPlot)) {
                movie.setPlot(xmlPlot, IMDB_PLUGIN_ID);
            } else {
                movie.setPlot(movie.getOutline(), IMDB_PLUGIN_ID);
            }
        }

        // CERTIFICATION
        if (OverrideTools.checkOverwriteCertification(movie, IMDB_PLUGIN_ID)) {
            String certification = movie.getCertification();
            // Use the default site definition for the certification, because the local versions don't have the parentalguide page
            String certXML = webBrowser.request(getImdbUrl(movie, imdbInfo.getSiteDef(DEFAULT_SITE_DEF)) + "parentalguide#certification", imdbInfo.getSiteDef(DEFAULT_SITE_DEF).getCharset());
            if (extractCertificationFromMPAA) {
                String mpaa = HTMLTools.extractTag(certXML, "<h5><a href=\"/mpaa\">MPAA</a>:</h5>", 1);
                if (!mpaa.equals(Movie.UNKNOWN)) {
                    String key = siteDef.getRated() + " ";
                    int pos = mpaa.indexOf(key);
                    if (pos != -1) {
                        int start = key.length();
                        pos = mpaa.indexOf(" on appeal for ", start);
                        if (pos == -1) {
                            pos = mpaa.indexOf(" for ", start);
                        }
                        if (pos != -1) {
                            certification = mpaa.substring(start, pos);
                        }
                    }
                }
            }

            if (isNotValidString(certification)) {
                certification = getPreferredValue(HTMLTools.extractTags(certXML, HTML_H5_START + siteDef.getCertification() + HTML_H5_END, HTML_DIV_END,
                        "<a href=\"/search/title?certificates=", HTML_A_END), true);
            }

            if (isNotValidString(certification)) {
                certification = Movie.NOTRATED;
            }

            movie.setCertification(certification, IMDB_PLUGIN_ID);
        }

        // YEAR
        if (OverrideTools.checkOverwriteYear(movie, IMDB_PLUGIN_ID)) {
            Pattern getYear = Pattern.compile("(?:\\s*" + "\\((\\d{4})(?:/[^\\)]+)?\\)|<a href=\"/year/(\\d{4}))");
            Matcher m = getYear.matcher(xml);
            if (m.find()) {
                String year = m.group(1);
                if (isNotValidString(year)) {
                    year = m.group(2);
                }
                movie.setYear(year, IMDB_PLUGIN_ID);
            }

            // second approach
            if (isNotValidString(movie.getYear())) {
                movie.setYear(HTMLTools.extractTag(xml, "<a href=\"/year/", 1), IMDB_PLUGIN_ID);
                if (isNotValidString(movie.getYear())) {
                    String fullReleaseDate = HTMLTools.getTextAfterElem(xml, HTML_H5_START + siteDef.getOriginalAirDate() + HTML_H5_END, 0);
                    if (isValidString(fullReleaseDate)) {
                        movie.setYear(fullReleaseDate.split(" ")[2], IMDB_PLUGIN_ID);
                    }
                }
            }
        }

        // TAGLINE
        if (OverrideTools.checkOverwriteTagline(movie, IMDB_PLUGIN_ID)) {
            int startTag = xml.indexOf("<h4 class=\"inline\">" + siteDef.getTaglines() + HTML_H4_END);
            if (startTag != -1) {
                // We need to work out which of the two formats to use, this is dependent on which comes first "<span" or "</div"
                String endMarker;
                if (StringUtils.indexOf(xml, "<span", startTag) < StringUtils.indexOf(xml, HTML_DIV_END, startTag)) {
                    endMarker = "<span";
                } else {
                    endMarker = HTML_DIV_END;
                }

                // Now look for the right string
                String tagline = HTMLTools.extractTag(xml, "<h4 class=\"inline\">" + siteDef.getTaglines() + HTML_H4_END, endMarker);
                tagline = HTMLTools.stripTags(tagline);
                movie.setTagline(cleanStringEnding(tagline), IMDB_PLUGIN_ID);
            }
        }

        // TV SHOW
        if (movie.isTVShow()) {
            updateTVShowInfo(movie);
        }
    }

    /**
     * Scrape info which is common for old and new IMDb.
     *
     * @param movie
     * @param xml
     * @param siteDef
     * @param imdbNewVersion
     * @throws IOException
     */
    private void updateInfoCommon(Movie movie, String xml, ImdbSiteDataDefinition siteDef, boolean imdbNewVersion) throws IOException {
        // Store the release info page for release info & AKAs
        String releaseInfoXML = Movie.UNKNOWN;
        // Store the aka list
        Map<String, String> akas = null;

        // ASPECT RATIO
        if (OverrideTools.checkOverwriteAspectRatio(movie, IMDB_PLUGIN_ID)) {
            // determine start and end string
            String startString;
            String endString;
            if (!fullInfo && imdbInfo.getImdbSite().equals(DEFAULT_SITE_DEF)) {
                startString = "<h4 class=\"inline\">" + siteDef.getAspectRatio() + HTML_H4_END;
                endString = HTML_DIV_END;
            } else {
                startString = HTML_H5_START + siteDef.getAspectRatio() + HTML_H5_END + "<div class=\"info-content\">";
                endString = "<a class";
            }

            // find unclean aspect ratio
            String uncleanAspectRatio = HTMLTools.extractTag(xml, startString, endString).trim();

            if (StringTools.isValidString(uncleanAspectRatio)) {
                // remove spaces and replace , with .
                uncleanAspectRatio = uncleanAspectRatio.replace(" ", "").replace(",", ".");
                // set aspect ratio
                movie.setAspectRatio(aspectTools.cleanAspectRatio(uncleanAspectRatio), IMDB_PLUGIN_ID);
            }
        }

        // RELEASE DATE
        if (OverrideTools.checkOverwriteReleaseDate(movie, IMDB_PLUGIN_ID)) {
            // Load the release page from IMDB
            if (StringTools.isNotValidString(releaseInfoXML)) {
                releaseInfoXML = webBrowser.request(getImdbUrl(movie, siteDef) + "releaseinfo", siteDef.getCharset());
            }

            Pattern pRelease = Pattern.compile("(?:.*?)\\Q" + preferredCountry + "\\E(?:.*?)\\Qrelease_date\">\\E(.*?)(?:<.*?>)(.*?)(?:</a>.*)");
            Matcher mRelease = pRelease.matcher(releaseInfoXML);

            if (mRelease.find()) {
                String releaseDate = mRelease.group(1) + " " + mRelease.group(2);
                try {
                    movie.setReleaseDate(DateTime.parse(releaseDate).toString("yyyy-MM-dd"), IMDB_PLUGIN_ID);
                } catch (IllegalArgumentException ex) {
                    LOG.trace(LOG_MESSAGE + "Failed to convert release date: " + releaseDate, ex);
                    movie.setReleaseDate(Movie.UNKNOWN, IMDB_PLUGIN_ID);
                }
            }
        }

        // ORIGINAL TITLE / AKAS
        if (OverrideTools.checkOverwriteOriginalTitle(movie, IMDB_PLUGIN_ID)) {
            // Load the AKA page from IMDb
            if (StringTools.isNotValidString(releaseInfoXML)) {
                releaseInfoXML = webBrowser.request(getImdbUrl(movie, siteDef) + "releaseinfo", siteDef.getCharset());
            }

            // The AKAs are stored in the format "title", "country"
            // therefore we need to look for the preferredCountry and then work backwards
            if (akas == null) {
                // Just extract the AKA section from the page
                List<String> akaList = HTMLTools.extractTags(releaseInfoXML, "<a id=\"akas\" name=\"akas\">", HTML_TABLE_END, "<td>", HTML_TD_END, Boolean.FALSE);
                akas = buildAkaMap(akaList);
            }

            String foundValue = null;
            for (Map.Entry<String, String> aka : akas.entrySet()) {
                if (aka.getKey().contains(siteDef.getOriginalTitle())) {
                    foundValue = aka.getValue().trim();
                    break;
                }
            }
            movie.setOriginalTitle(foundValue, IMDB_PLUGIN_ID);
        }

        // TITLE for preferred country from AKAS
        if (akaScrapeTitle && OverrideTools.checkOverwriteTitle(movie, IMDB_PLUGIN_ID)) {
            // Load the AKA page from IMDb
            if (StringTools.isNotValidString(releaseInfoXML)) {
                releaseInfoXML = webBrowser.request(getImdbUrl(movie, siteDef) + "releaseinfo", siteDef.getCharset());
            }

            // The AKAs are stored in the format "title", "country"
            // therefore we need to look for the preferredCountry and then work backwards
            if (akas == null) {
                // Just extract the AKA section from the page
                List<String> akaList = HTMLTools.extractTags(releaseInfoXML, "<a id=\"akas\" name=\"akas\">", HTML_TABLE_END, "<td>", HTML_TD_END, Boolean.FALSE);
                akas = buildAkaMap(akaList);
            }

            String foundValue = null;
            // NOTE: First matching country is the preferred country
            for (String matchCountry : akaMatchingCountries) {

                if (StringUtils.isBlank(matchCountry)) {
                    // must be a valid country setting
                    continue;
                }

                for (Map.Entry<String, String> aka : akas.entrySet()) {
                    int startIndex = aka.getKey().indexOf(matchCountry);
                    if (startIndex > -1) {
                        String extracted = aka.getKey().substring(startIndex);
                        int endIndex = extracted.indexOf('/');
                        if (endIndex > -1) {
                            extracted = extracted.substring(0, endIndex);
                        }

                        boolean valid = Boolean.TRUE;
                        for (String ignore : akaIgnoreVersions) {
                            if (StringUtils.isNotBlank(ignore) && StringUtils.containsIgnoreCase(extracted, ignore.trim())) {
                                valid = Boolean.FALSE;
                                break;
                            }
                        }

                        if (valid) {
                            foundValue = aka.getValue().trim();
                            break;
                        }
                    }
                }

                if (foundValue != null) {
                    // we found a title for the country matcher
                    break;
                }
            }
            movie.setTitle(foundValue, IMDB_PLUGIN_ID);
        }

        // holds the full credits page
        String fullcreditsXML = Movie.UNKNOWN;

        // DIRECTOR(S)
        boolean overrideNormal = OverrideTools.checkOverwriteDirectors(movie, IMDB_PLUGIN_ID);
        boolean overridePeople = OverrideTools.checkOverwritePeopleDirectors(movie, IMDB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            boolean found = Boolean.FALSE;

            // get from combined page (same layout as full credits)
            if (fullInfo) {
                found = extractDirectorsFromFullCredits(movie, xml, siteDef, overrideNormal, overridePeople);
            }

            // get from full credits
            if (!found) {
                if (isNotValidString(fullcreditsXML)) {
                    fullcreditsXML = webBrowser.request(getImdbUrl(movie, siteDef) + "fullcredits", siteDef.getCharset());
                }
                found = extractDirectorsFromFullCredits(movie, fullcreditsXML, siteDef, overrideNormal, overridePeople);
            }

            // extract from old layout
            if (!found && !imdbNewVersion) {
                extractDirectorsFromOldLayout(movie, xml, siteDef, overrideNormal, overridePeople);
            }
        }

        // WRITER(S)
        overrideNormal = OverrideTools.checkOverwriteWriters(movie, IMDB_PLUGIN_ID);
        overridePeople = OverrideTools.checkOverwritePeopleWriters(movie, IMDB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            boolean found = Boolean.FALSE;

            // get from combined page (same layout as full credits)
            if (fullInfo) {
                found = extractWritersFromFullCredits(movie, xml, siteDef, overrideNormal, overridePeople);
            }

            // get from full credits
            if (!found) {
                if (isNotValidString(fullcreditsXML)) {
                    fullcreditsXML = webBrowser.request(getImdbUrl(movie, siteDef) + "fullcredits", siteDef.getCharset());
                }
                found = extractWritersFromFullCredits(movie, fullcreditsXML, siteDef, overrideNormal, overridePeople);
            }

            // extract from old layout
            if (!found && !imdbNewVersion) {
                extractWritersFromOldLayout(movie, xml, siteDef, overrideNormal, overridePeople);
            }
        }

        // CAST
        overrideNormal = OverrideTools.checkOverwriteActors(movie, IMDB_PLUGIN_ID);
        overridePeople = OverrideTools.checkOverwritePeopleActors(movie, IMDB_PLUGIN_ID);
        if (overrideNormal || overridePeople) {
            boolean found = Boolean.FALSE;

            // get from combined page (same layout as full credits)
            if (fullInfo) {
                found = extractCastFromFullCredits(movie, xml, siteDef, overrideNormal, overridePeople);
            }

            // get from full credits
            if (!found) {
                if (isNotValidString(fullcreditsXML)) {
                    fullcreditsXML = webBrowser.request(getImdbUrl(movie, siteDef) + "fullcredits", siteDef.getCharset());
                }
                found = extractCastFromFullCredits(movie, fullcreditsXML, siteDef, overrideNormal, overridePeople);
            }

            // extract from old layout
            if (!found && !imdbNewVersion) {
                extractCastFromOldLayout(movie, xml, siteDef, overrideNormal, overridePeople);
            }
        }
    }

    private boolean extractCastFromFullCredits(Movie movie, String fullcreditsXML, ImdbSiteDataDefinition siteDef, boolean overrideNormal, boolean overridePeople) {
        // count for already set cast
        int count = 0;
        // flag to indicate if cast must be cleared
        boolean clearCast = Boolean.TRUE;
        boolean clearPeopleCast = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String actorBlock : HTMLTools.extractTags(fullcreditsXML, "<table class=\"cast_list\">", HTML_TABLE_END, "<td class=\"primary_photo\"", "</tr>")) {
            // skip faceless persons ("loadlate hidden" is present for actors with photos)
            if (skipFaceless && !actorBlock.contains("loadlate hidden")) {
                continue;
            }

            int nmPosition = actorBlock.indexOf("/nm");
            String personID = actorBlock.substring(nmPosition + 1, actorBlock.indexOf("/", nmPosition + 1));

            String name = HTMLTools.stripTags(HTMLTools.extractTag(actorBlock, "itemprop=\"name\">", HTML_SPAN_END));
            String character = HTMLTools.stripTags(HTMLTools.extractTag(actorBlock, "<td class=\"character\">", HTML_TD_END));

            if (overrideNormal) {
                // clear cast if not already done
                if (clearCast) {
                    movie.clearCast();
                    clearCast = Boolean.FALSE;
                }
                // add actor
                movie.addActor(name, IMDB_PLUGIN_ID);
            }

            if (overridePeople) {
                // clear cast if not already done
                if (clearPeopleCast) {
                    movie.clearPeopleCast();
                    clearPeopleCast = Boolean.FALSE;
                }
                // add actor
                movie.addActor(IMDB_PLUGIN_ID + ":" + personID, name, character, siteDef.getSite() + HTML_NAME + personID + "/", Movie.UNKNOWN, IMDB_PLUGIN_ID);
            }

            found = Boolean.TRUE;
            count++;
            if (count == actorMax) {
                break;
            }
        }

        return found;
    }

    private boolean extractCastFromOldLayout(Movie movie, String xml, ImdbSiteDataDefinition siteDef, boolean overrideNormal, boolean overridePeople) {
        // count for already set cast
        int count = 0;
        // flag to indicate if cast must be cleared
        boolean clearCast = Boolean.TRUE;
        boolean clearPeopleCast = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        List<String> peopleList = HTMLTools.extractTags(xml, "<table class=\"cast_list\">", HTML_TABLE_END, "<td class=\"name\"", "</tr>");
        if (peopleList.isEmpty()) {
            // alternative search
            peopleList = HTMLTools.extractTags(xml, "<table class=\"cast_list\">", HTML_TABLE_END, "<td class=\"nm\"", "</tr>", Boolean.TRUE);
        }

        Matcher matcher;

        for (String actorBlock : peopleList) {

            // skip faceless persons
            if (skipFaceless && actorBlock.contains("nopicture")) {
                continue;
            }

            matcher = PATTERN_PERSON_NAME.matcher(actorBlock);
            String personID, name, charID, character;
            if (matcher.find()) {
                personID = matcher.group(1).trim();
                name = matcher.group(2).trim();

                matcher = PATTERN_PERSON_CHAR.matcher(actorBlock);
                if (matcher.find()) {
                    charID = matcher.group(1).trim();
                    character = matcher.group(2).trim();
                } else {
                    charID = Movie.UNKNOWN;
                    character = Movie.UNKNOWN;
                }

                LOG.debug(LOG_MESSAGE + "Found Person ID: " + personID + ", name: " + name + ", Character ID: " + charID + ", name: " + character);

                if (overrideNormal) {
                    // clear cast if not already done
                    if (clearCast) {
                        movie.clearCast();
                        clearCast = Boolean.FALSE;
                    }
                    // add actor
                    movie.addActor(name, IMDB_PLUGIN_ID);
                }

                if (overridePeople) {
                    // clear cast if not already done
                    if (clearPeopleCast) {
                        movie.clearPeopleCast();
                        clearPeopleCast = Boolean.FALSE;
                    }
                    // add actor
                    movie.addActor(IMDB_PLUGIN_ID + ":" + personID, name, character, siteDef.getSite() + HTML_NAME + personID + "/", Movie.UNKNOWN, IMDB_PLUGIN_ID);
                }

                found = Boolean.TRUE;
                count++;
                if (count == actorMax) {
                    break;
                }
            }
        }

        return found;
    }

    private boolean extractDirectorsFromFullCredits(Movie movie, String fullcreditsXML, ImdbSiteDataDefinition siteDef, boolean overrideNormal, boolean overridePeople) {
        // count for already set directors
        int count = 0;
        // flag to indicate if directors must be cleared
        boolean clearDirectors = Boolean.TRUE;
        boolean clearPeopleDirectors = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String directorMatch : siteDef.getDirector().split(HTML_SLASH_PIPE)) {
            if (fullcreditsXML.contains(HTML_GT + directorMatch + "&nbsp;</h4>")) {
                for (String member : HTMLTools.extractTags(fullcreditsXML, HTML_GT + directorMatch + "&nbsp;</h4>", HTML_TABLE_END, HTML_A_START, HTML_A_END, Boolean.FALSE)) {
                    int beginIndex = member.indexOf("href=\"/name/");
                    if (beginIndex > -1) {
                        String personID = member.substring(beginIndex + 12, member.indexOf("/", beginIndex + 12));
                        String director = member.substring(member.indexOf(HTML_GT, beginIndex) + 1).trim();
                        if (overrideNormal) {
                            // clear directors if not already done
                            if (clearDirectors) {
                                movie.clearDirectors();
                                clearDirectors = Boolean.FALSE;
                            }
                            // add director
                            movie.addDirector(director, IMDB_PLUGIN_ID);
                        }

                        if (overridePeople) {
                            // clear directors if not already done
                            if (clearPeopleDirectors) {
                                movie.clearPeopleDirectors();
                                clearPeopleDirectors = Boolean.FALSE;
                            }
                            // add director, but check that there are no invalid characters in the name which may indicate a bad scrape
                            if (StringUtils.containsNone(director, "<>:/")) {
                                movie.addDirector(IMDB_PLUGIN_ID + ":" + personID, director, siteDef.getSite() + HTML_NAME + personID + "/", IMDB_PLUGIN_ID);
                                found = Boolean.TRUE;
                                count++;
                            } else {
                                LOG.debug(LOG_MESSAGE + "Invalid director name found: '" + director + "'");
                            }
                        }

                        if (count == directorMax) {
                            break;
                        }
                    }
                }
            }
            if (found) {
                // We found a match, so stop search.
                break;
            }
        }

        return found;
    }

    private boolean extractDirectorsFromOldLayout(Movie movie, String xml, ImdbSiteDataDefinition siteDef, boolean overrideNormal, boolean overridePeople) {
        // count for already set directors
        int count = 0;
        // flag to indicate if directors must be cleared
        boolean clearDirectors = Boolean.TRUE;
        boolean clearPeopleDirectors = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String directorMatch : siteDef.getDirector().split(HTML_SLASH_PIPE)) {
            for (String member : HTMLTools.extractTags(xml, HTML_H5_START + directorMatch, HTML_DIV_END, "", HTML_A_END)) {
                int beginIndex = member.indexOf("<a href=\"/name/");
                if (beginIndex > -1) {
                    String personID = member.substring(beginIndex + 15, member.indexOf(HTML_SLASH_QUOTE, beginIndex));
                    String director = member.substring(member.indexOf(HTML_QUOTE_GT, beginIndex) + 2);

                    if (overrideNormal) {
                        // clear directors if not already done
                        if (clearDirectors) {
                            movie.clearDirectors();
                            clearDirectors = Boolean.FALSE;
                        }
                        // add director
                        movie.addDirector(director, IMDB_PLUGIN_ID);
                    }

                    if (overridePeople) {
                        // clear directors if not already done
                        if (clearPeopleDirectors) {
                            movie.clearPeopleDirectors();
                            clearPeopleDirectors = Boolean.FALSE;
                        }
                        // add director
                        movie.addDirector(IMDB_PLUGIN_ID + ":" + personID, director, siteDef.getSite() + HTML_NAME + personID + "/", IMDB_PLUGIN_ID);
                    }

                    found = Boolean.TRUE;
                    count++;
                    if (count == directorMax) {
                        break;
                    }
                }
            }
            if (found) {
                // We found a match, so stop search.
                break;
            }
        }

        return found;
    }

    private boolean extractWritersFromFullCredits(Movie movie, String fullcreditsXML, ImdbSiteDataDefinition siteDef, boolean overrideNormal, boolean overridePeople) {
        // count for already set writers
        int count = 0;
        // flag to indicate if writers must be cleared
        boolean clearWriters = Boolean.TRUE;
        boolean clearPeopleWriters = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String writerMatch : siteDef.getWriter().split(HTML_SLASH_PIPE)) {
            if (StringUtils.indexOfIgnoreCase(fullcreditsXML, HTML_GT + writerMatch) >= 0) {
                for (String member : HTMLTools.extractTags(fullcreditsXML, HTML_GT + writerMatch, HTML_TABLE_END, HTML_A_START, HTML_A_END, Boolean.FALSE)) {
                    int beginIndex = member.indexOf("href=\"/name/");
                    if (beginIndex > -1) {
                        String personID = member.substring(beginIndex + 12, member.indexOf("/", beginIndex + 12));
                        String name = StringUtils.trimToEmpty(member.substring(member.indexOf(HTML_GT, beginIndex) + 1));
                        if (!name.contains("more credit")) {

                            if (overrideNormal) {
                                // clear writers if not already done
                                if (clearWriters) {
                                    movie.clearWriters();
                                    clearWriters = Boolean.FALSE;
                                }
                                // add writer
                                movie.addWriter(name, IMDB_PLUGIN_ID);
                            }

                            if (overridePeople) {
                                // clear writers if not already done
                                if (clearPeopleWriters) {
                                    movie.clearPeopleWriters();
                                    clearPeopleWriters = Boolean.FALSE;
                                }
                                // add writer
                                movie.addWriter(IMDB_PLUGIN_ID + ":" + personID, name, siteDef.getSite() + HTML_NAME + personID + "/", IMDB_PLUGIN_ID);
                            }

                            found = Boolean.TRUE;
                            count++;
                            if (count == writerMax) {
                                break;
                            }
                        }
                    }
                }
            }

            if (found) {
                // We found a match, so stop search.
                break;
            }
        }

        return found;
    }

    private boolean extractWritersFromOldLayout(Movie movie, String xml, ImdbSiteDataDefinition siteDef, boolean overrideNormal, boolean overridePeople) {
        // count for already set writers
        int count = 0;
        // flag to indicate if writers must be cleared
        boolean clearWriters = Boolean.TRUE;
        boolean clearPeopleWriters = Boolean.TRUE;
        // flag to indicate if match has been found
        boolean found = Boolean.FALSE;

        for (String categoryMatch : siteDef.getWriter().split(HTML_SLASH_PIPE)) {
            for (String member : HTMLTools.extractTags(xml, HTML_H5_START + categoryMatch, HTML_DIV_END, "", HTML_A_END)) {
                int beginIndex = member.indexOf("<a href=\"/name/");
                if (beginIndex > -1) {
                    String personID = member.substring(beginIndex + 15, member.indexOf(HTML_SLASH_QUOTE, beginIndex));
                    String name = member.substring(member.indexOf(HTML_QUOTE_GT, beginIndex) + 2);
                    if (overrideNormal) {
                        // clear writers if not already done
                        if (clearWriters) {
                            movie.clearWriters();
                            clearWriters = Boolean.FALSE;
                        }
                        // add writer
                        movie.addWriter(name, IMDB_PLUGIN_ID);
                    }

                    if (overridePeople) {
                        // clear writers if not already done
                        if (clearPeopleWriters) {
                            movie.clearPeopleWriters();
                            clearPeopleWriters = Boolean.FALSE;
                        }
                        // add writer
                        movie.addWriter(IMDB_PLUGIN_ID + ":" + personID, name, siteDef.getSite() + HTML_NAME + personID + "/", IMDB_PLUGIN_ID);
                    }

                    found = Boolean.TRUE;
                    count++;
                    if (count == writerMax) {
                        break;
                    }
                }
            }
            if (found) {
                // We found a match, so stop search.
                break;
            }
        }

        return found;
    }

    /**
     * Process a awards in the IMDb web page
     *
     * @param movie
     * @return
     * @throws IOException
     */
    private boolean updateAwards(Movie movie) throws IOException {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        String site = siteDefinition.getSite();
        if (!siteDefinition.getSite().contains(HTML_SITE)) {
            site = HTML_SITE_FULL;
        }
        String awardXML = webBrowser.request(site + HTML_TITLE + imdbId + "/awards");
        if (awardXML.contains("<h1 class=\"header\">Awards</h1>")) {

            List<String> awardHtmlList = HTMLTools.extractTags(awardXML, "<h1 class=\"header\">Awards</h1>", "<div class=\"article\"", "<h3>", "</table>", false);

            Collection<AwardEvent> awardList = new ArrayList<AwardEvent>();
            for (String awardBlock : awardHtmlList) {
                String awardEvent = awardBlock.substring(0, awardBlock.indexOf('<')).trim();

                AwardEvent aEvent = new AwardEvent();
                aEvent.setName(awardEvent);

                String tmpString = HTMLTools.extractTag(awardBlock, "<a href=", HTML_A_END).trim();
                tmpString = tmpString.substring(tmpString.indexOf('>') + 1).trim();
                int awardYear = NumberUtils.isNumber(tmpString) ? Integer.parseInt(tmpString) : -1;

                tmpString = StringUtils.trimToEmpty(HTMLTools.extractTag(awardBlock, "<span class=\"award_category\">", "</span>"));
                Award aAward = new Award();
                aAward.setName(tmpString);
                aAward.setYear(awardYear);

                boolean awardOutcomeWon = true;
                for (String outcomeBlock : HTMLTools.extractHtmlTags(awardBlock, "<table class=", null, "<tr>", "</tr>")) {
                    String outcome = HTMLTools.extractTag(outcomeBlock, "<b>", "</b>");
                    if (StringTools.isValidString(outcome)) {
                        awardOutcomeWon = outcome.equalsIgnoreCase("won");
                    }

                    String awardDescription = StringUtils.trimToEmpty(HTMLTools.extractTag(outcomeBlock, "<td class=\"award_description\">", "<br />"));
                    // Check to see if there was a missing title and just the name in the result
                    if (awardDescription.contains("href=\"/name/")) {
                        awardDescription = StringUtils.trimToEmpty(HTMLTools.extractTag(outcomeBlock, "<span class=\"award_category\">", "</span>"));
                    }

                    if (awardOutcomeWon) {
                        aAward.addWon(awardDescription);
                    } else {
                        aAward.addNomination(awardDescription);
                    }
                }

                if (!scrapeWonAwards || (aAward.getWon() > 0)) {
                    LOG.debug(LOG_MESSAGE + movie.getBaseName() + " - Adding award: " + aAward.toString());
                    aEvent.addAward(aAward);
                }

                if (aEvent.getAwards().size() > 0) {
                    awardList.add(aEvent);
                }
            }

            if (awardList.size() > 0) {
                movie.setAwards(awardList);
            }
        } else {
            LOG.debug(LOG_MESSAGE + "No awards found for " + movie.getBaseName());
        }
        return Boolean.TRUE;
    }

    /**
     * Process financial information in the IMDb web page
     *
     * @param movie
     * @return
     * @throws IOException
     */
    private boolean updateBusiness(Movie movie) throws IOException {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        String site = siteDefinition.getSite();
        if (!siteDefinition.getSite().contains(HTML_SITE)) {
            site = HTML_SITE_FULL;
        }
        String xml = webBrowser.request(site + HTML_TITLE + imdbId + "/business");
        if (isValidString(xml)) {
            String budget = HTMLTools.extractTag(xml, "<h5>Budget</h5>", HTML_BREAK).replaceAll("\\s.*", "");
            movie.setBudget(budget);
            NumberFormat moneyFormat = NumberFormat.getNumberInstance(new Locale("US"));
            for (int i = 0; i < 2; i++) {
                for (String oWeek : HTMLTools.extractTags(xml, HTML_H5_START + (i == 0 ? "Opening Weekend" : "Gross") + "</h5", HTML_H5_START, "", "<br/")) {
                    if (isValidString(oWeek)) {
                        String currency = oWeek.replaceAll("\\d+.*", "");
                        long value = NumberUtils.toLong(oWeek.replaceAll("^\\D*\\s*", "").replaceAll("\\s.*", "").replaceAll(",", ""), -1L);
                        String country = HTMLTools.extractTag(oWeek, "(", ")");
                        if (country.equals("Worldwide") && !currency.equals("$")) {
                            continue;
                        }
                        String money = i == 0 ? movie.getOpenWeek(country) : movie.getGross(country);
                        if (isValidString(money)) {
                            long m = NumberUtils.toLong(money.replaceAll("^\\D*\\s*", "").replaceAll(",", ""), -1L);
                            value = i == 0 ? value + m : value > m ? value : m;
                        }
                        if (i == 0) {
                            movie.setOpenWeek(country, currency + moneyFormat.format(value));
                        } else {
                            movie.setGross(country, currency + moneyFormat.format(value));
                        }
                    }
                }
            }
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Process trivia in the IMDb web page
     *
     * @param movie
     * @return
     * @throws IOException
     */
    private boolean updateTrivia(Movie movie) throws IOException {
        if (triviaMax == 0) {
            return Boolean.FALSE;
        }
        String imdbId = movie.getId(IMDB_PLUGIN_ID);
        String site = siteDefinition.getSite();
        if (!siteDefinition.getSite().contains(HTML_SITE)) {
            site = HTML_SITE_FULL;
        }
        String xml = webBrowser.request(site + HTML_TITLE + imdbId + "/trivia");
        if (isValidString(xml)) {
            int i = 0;
            for (String tmp : HTMLTools.extractTags(xml, "<div class=\"list\">", "<div class=\"list\">", "<div class=\"sodatext\"", HTML_DIV_END)) {
                if (i < triviaMax || triviaMax == -1) {
                    tmp = HTMLTools.removeHtmlTags(tmp);
                    tmp = tmp.trim();
                    movie.addDidYouKnow(tmp);
                    i++;
                } else {
                    break;
                }
            }
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * Process a list of people in the source XML
     *
     * @param sourceXml
     * @param singleCategory The singular version of the category, e.g. "Writer"
     * @param pluralCategory The plural version of the category, e.g. "Writers"
     * @return
     */
    @SuppressWarnings("unused")
    private Collection<String> parseNewPeople(String sourceXml, String[] categoryList) {
        Collection<String> people = new LinkedHashSet<String>();

        for (String category : categoryList) {
            if (sourceXml.contains(category + ":")) {
                people = HTMLTools.extractTags(sourceXml, category, HTML_DIV_END, HTML_A_START, HTML_A_END);
            }
        }
        return people;
    }

    /**
     * Parse the rating
     *
     * @param rating
     * @return
     */
    private int parseRating(String rating) {
        StringTokenizer st = new StringTokenizer(rating, "/ ()");
        return StringTools.parseRating(st.nextToken());
    }

    /**
     * Get the fanart for the movie from the FanartScanner
     *
     * @param movie
     * @return
     */
    protected String getFanartURL(Movie movie) {
        return FanartScanner.getFanartURL(movie);
    }

    @Override
    public void scanTVShowTitles(Movie movie) {
        String imdbId = movie.getId(IMDB_PLUGIN_ID);

        if (!movie.isTVShow() || !movie.hasNewMovieFiles() || isNotValidString(imdbId)) {
            return;
        }

        try {
            int season = movie.getSeason();
            String xml = webBrowser.request(siteDefinition.getSite() + HTML_TITLE + imdbId + "/episodes?season=" + season);

            for (MovieFile file : movie.getMovieFiles()) {

                for (int episode = file.getFirstPart(); episode <= file.getLastPart(); ++episode) {

                    int beginIndex = xml.indexOf("<meta itemprop=\"episodeNumber\" content=\"" + episode + "\"/>");
                    if (beginIndex == -1) {
                        continue;
                    }
                    int endIndex = xml.indexOf("<div class=\"clear\"", beginIndex);
                    String episodeXml = xml.substring(beginIndex, endIndex);

                    if (OverrideTools.checkOverwriteEpisodeTitle(file, episode, IMDB_PLUGIN_ID)) {
                        String episodeName = HTMLTools.extractTag(episodeXml, "itemprop=\"name\">", HTML_A_END);
                        file.setTitle(episode, episodeName, IMDB_PLUGIN_ID);
                    }

                    if (OverrideTools.checkOverwriteEpisodePlot(file, episode, IMDB_PLUGIN_ID)) {
                        String plot = HTMLTools.extractTag(episodeXml, "itemprop=\"description\">", HTML_DIV_END);
                        file.setPlot(episode, plot, IMDB_PLUGIN_ID);
                    }

                    if (OverrideTools.checkOverwriteEpisodeFirstAired(file, episode, IMDB_PLUGIN_ID)) {
                        String firstAired = HTMLTools.extractTag(episodeXml, "<div class=\"airdate\">", "</div>");
                        try {
                            // use same format as TheTvDB
                            firstAired = DateTime.parse(firstAired).toString("yyyy-MM-dd");
                        } catch (Exception ignore) {
                        }
                        file.setFirstAired(episode, firstAired, IMDB_PLUGIN_ID);
                    }
                }
            }
        } catch (IOException error) {
            LOG.error(LOG_MESSAGE + "Failed retrieving episodes titles for: " + movie.getTitle());
            LOG.error(LOG_MESSAGE + "Error: " + error.getMessage());
        }
    }

    /**
     * Get the TV show information from IMDb
     *
     * @param movie
     *
     * @throws IOException
     */
    protected void updateTVShowInfo(Movie movie) throws IOException {
        scanTVShowTitles(movie);
    }

    /**
     * Retrieves the long plot description from IMDB if it exists, else "UNKNOWN"
     *
     * @param movie
     * @return long plot
     */
    private String getLongPlot(Identifiable movie) {
        String plot = Movie.UNKNOWN;

        try {
            String xml = webBrowser.request(siteDefinition.getSite() + HTML_TITLE + movie.getId(IMDB_PLUGIN_ID) + "/plotsummary", siteDefinition.getCharset());

            String result = HTMLTools.extractTag(xml, "<p class=\"plotpar\">", "</p>");
            if (isValidString(result) && !result.contains("This plot synopsis is empty")) {
                plot = HTMLTools.stripTags(result);
            }

            // Second parsing other site (fr/ es / etc ...)
            result = HTMLTools.extractTag(xml, "<div id=\"swiki.2.1\">", HTML_DIV_END);
            if (isValidString(result) && !result.contains("This plot synopsis is empty")) {
                plot = HTMLTools.stripTags(result);
            }
        } catch (IOException error) {
            LOG.warn("Failed to get plot summary: " + error.getMessage());
            plot = Movie.UNKNOWN;
        }

        return plot;
    }

    @Override
    public boolean scanNFO(String nfo, Movie movie) {
        boolean result = Boolean.TRUE;

        // If we already have the ID, skip the scanning of the NFO file
        if (StringTools.isValidString(movie.getId(IMDB_PLUGIN_ID))) {
            return result;
        }

        LOG.debug(LOG_MESSAGE + LOG_MESSAGE + "Scanning NFO for Imdb Id");
        String id = searchIMDB(nfo, movie);
        if (isValidString(id)) {
            movie.setId(IMDB_PLUGIN_ID, id);
            LOG.debug(LOG_MESSAGE + "IMDb Id found in nfo: " + movie.getId(IMDB_PLUGIN_ID));
        } else {
            int beginIndex = nfo.indexOf("/tt");
            if (beginIndex != -1) {
                StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 1), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                movie.setId(IMDB_PLUGIN_ID, st.nextToken());
                LOG.debug(LOG_MESSAGE + "IMDb Id found in nfo: " + movie.getId(IMDB_PLUGIN_ID));
            } else {
                beginIndex = nfo.indexOf("/Title?");
                if (beginIndex != -1 && beginIndex + 7 < nfo.length()) {
                    StringTokenizer st = new StringTokenizer(nfo.substring(beginIndex + 7), "/ \n,:!&Ã©\"'(--Ã¨_Ã§Ã )=$");
                    movie.setId(IMDB_PLUGIN_ID, "tt" + st.nextToken());
                    LOG.debug(LOG_MESSAGE + "IMDb Id found in nfo: " + movie.getId(IMDB_PLUGIN_ID));
                } else {
                    LOG.debug(LOG_MESSAGE + "No IMDb Id found in nfo !");
                    result = Boolean.FALSE;
                }
            }
        }
        return result;
    }

    /**
     * Search for the IMDB Id in the NFO file
     *
     * @param nfo
     * @param movie
     * @return
     */
    private String searchIMDB(String nfo, Movie movie) {
        final int flags = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
        String imdbPattern = ")[\\W].*?(tt\\d{7})";
        // Issue 1912 escape special regex characters in title
        String title = Pattern.quote(movie.getTitle());
        String id = Movie.UNKNOWN;

        Pattern patternTitle;
        Matcher matchTitle;

        try {
            patternTitle = Pattern.compile("(" + title + imdbPattern, flags);
            matchTitle = patternTitle.matcher(nfo);
            if (matchTitle.find()) {
                id = matchTitle.group(2);
            } else {
                String dir = FileTools.getParentFolderName(movie.getFile());
                Pattern patternDir = Pattern.compile("(" + dir + imdbPattern, flags);
                Matcher matchDir = patternDir.matcher(nfo);
                if (matchDir.find()) {
                    id = matchDir.group(2);
                } else {
                    String strippedNfo = nfo.replaceAll("(?is)[^\\w\\r\\n]", "");
                    String strippedTitle = title.replaceAll("(?is)[^\\w\\r\\n]", "");
                    Pattern patternStrippedTitle = Pattern.compile("(" + strippedTitle + imdbPattern, flags);
                    Matcher matchStrippedTitle = patternStrippedTitle.matcher(strippedNfo);
                    if (matchStrippedTitle.find()) {
                        id = matchTitle.group(2);
                    } else {
                        String strippedDir = dir.replaceAll("(?is)[^\\w\\r\\n]", "");
                        Pattern patternStrippedDir = Pattern.compile("(" + strippedDir + imdbPattern, flags);
                        Matcher matchStrippedDir = patternStrippedDir.matcher(strippedNfo);
                        if (matchStrippedDir.find()) {
                            id = matchTitle.group(2);
                        }
                    }
                }
            }
        } catch (Exception error) {
            LOG.error("ImdbPlugin: Error locating the IMDb ID in the nfo file for " + movie.getBaseFilename());
            LOG.error(error.getMessage());
        }

        return StringUtils.trim(id);
    }

    /**
     * Remove the "see more" or "more" values from the end of a string
     *
     * @param uncleanString
     * @return
     */
    protected static String cleanStringEnding(String uncleanString) {
        int pos = uncleanString.indexOf("more");
        // First let's check if "more" exists in the string
        if (pos > 0) {
            if (uncleanString.endsWith("more")) {
                return uncleanString.substring(0, uncleanString.length() - 4).trim();
            }

            pos = uncleanString.toLowerCase().indexOf("see more");
            if (pos > 0) {
                return uncleanString.substring(0, pos).trim();
            }
        }

        pos = uncleanString.toLowerCase().indexOf("see full summary");
        if (pos > 0) {
            return uncleanString.substring(0, pos).trim();
        }

        return uncleanString.trim();
    }

    /**
     * Get the IMDb URL with the default site definition
     *
     * @param movie
     * @return
     */
    protected String getImdbUrl(Movie movie) {
        return getImdbUrl(movie, siteDefinition);
    }

    /**
     * Get the IMDb URL with the default site definition
     *
     * @param person
     * @return
     */
    protected String getImdbUrl(Person person) {
        return getImdbUrl(person, siteDefinition);
    }

    /**
     * Get the IMDb URL with a specific site definition
     *
     * @param movie
     * @param siteDefinition
     * @return
     */
    protected String getImdbUrl(Movie movie, ImdbSiteDataDefinition siteDefinition) {
        return siteDefinition.getSite() + HTML_TITLE + movie.getId(IMDB_PLUGIN_ID) + "/";
    }

    /**
     * Get the IMDb URL with a specific site definition
     *
     * @param person
     * @param siteDefinition
     * @return
     */
    protected String getImdbUrl(Person person, ImdbSiteDataDefinition siteDefinition) {
        return (siteDefinition.getSite().contains(HTML_SITE) ? siteDefinition.getSite() : HTML_SITE_FULL) + HTML_NAME + person.getId(IMDB_PLUGIN_ID) + "/";
    }

    @Override
    public boolean scan(Person person) {
        String imdbId = person.getId(IMDB_PLUGIN_ID);
        if (isNotValidString(imdbId)) {
            LOG.debug("Looking for IMDB ID for " + person.getName());
            String movieId = Movie.UNKNOWN;
            for (Movie movie : person.getMovies()) {
                movieId = movie.getId(IMDB_PLUGIN_ID);
                if (isValidString(movieId)) {
                    break;
                }
            }
            imdbId = imdbInfo.getImdbPersonId(person.getName(), movieId);
            person.setId(IMDB_PLUGIN_ID, imdbId);
        }

        boolean retval = Boolean.TRUE;
        if (isValidString(imdbId)) {
            retval = updateImdbPersonInfo(person);
        } else {
            LOG.debug("IMDB ID not found for " + person.getName());
        }
        return retval;
    }

    /**
     * Scan IMDB HTML page for the specified person
     */
    private boolean updateImdbPersonInfo(Person person) {
        String imdbID = person.getId(IMDB_PLUGIN_ID);
        boolean returnStatus = Boolean.FALSE;

        if (!imdbID.startsWith("nm")) {
            imdbID = "nm" + imdbID;
            // Correct the ID if it's wrong
            person.setId(IMDB_PLUGIN_ID, "nm" + imdbID);
        }

        LOG.info("Getting information for " + person.getName() + " (" + imdbID + ")");

        try {
            String xml = getImdbUrl(person);

            xml = webBrowser.request(xml, siteDefinition.getCharset());

            // We can work out if this is the new site by looking for " - IMDb" at the end of the title
            String title = HTMLTools.extractTag(xml, "<title>");
            // Check for the new version and correct the title if found.
            if (title.toLowerCase().endsWith(" - imdb")) {
                title = title.substring(0, title.length() - 7);
            }
            if (title.toLowerCase().startsWith("imdb - ")) {
                title = title.substring(7);
            }
            person.setName(title);

            returnStatus = updateInfoNew(person, xml);
        } catch (IOException error) {
            LOG.error("Failed retrieving IMDb data for person : " + imdbID);
            LOG.error(SystemTools.getStackTrace(error));
        }
        return returnStatus;
    }

    /**
     * Process the new IMDb format web page
     *
     * @param person
     * @param xml
     * @return
     * @throws IOException
     */
    private boolean updateInfoNew(Person person, String xml) throws IOException {
        person.setUrl(getImdbUrl(person));

        if (xml.contains("Alternate Names:")) {
            String name = HTMLTools.extractTag(xml, "Alternate Names:</h4>", HTML_DIV_END);
            if (isValidString(name)) {
                for (String item : name.split("<span>\\|</span>")) {
                    person.addAka(StringUtils.trimToEmpty(item));
                }
            }
        }

        if (xml.contains("id=\"img_primary\"")) {
            LOG.debug("Looking for image on webpage for " + person.getName());
            String photoURL = HTMLTools.extractTag(xml, "id=\"img_primary\"", HTML_TD_END);

            if (photoURL.contains("http://ia.media-imdb.com/images")) {
                photoURL = "http://ia.media-imdb.com/images" + HTMLTools.extractTag(photoURL, "src=\"http://ia.media-imdb.com/images", "\"");
                if (isValidString(photoURL)) {
                    person.setPhotoURL(photoURL);
                    person.setPhotoFilename();
                }
            }
        } else {
            LOG.debug(LOG_MESSAGE + "No image found on webpage for " + person.getName());
        }

        // get personal information
        String xmlInfo = webBrowser.request(getImdbUrl(person) + "bio", siteDefinition.getCharset());

        StringBuilder date = new StringBuilder();
        int endIndex;
        int beginIndex = xmlInfo.indexOf(">Date of Birth</td>");

        if (beginIndex > -1) {
            endIndex = xmlInfo.indexOf(">Date of Death</td>");
            beginIndex = xmlInfo.indexOf("birth_monthday=", beginIndex);
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                Matcher m = PATTERN_DOB.matcher(xmlInfo.substring(beginIndex + 15, beginIndex + 20));
                if (m.find()) {
                    date.append(m.group(2)).append("-").append(m.group(1));
                }
            }

            beginIndex = xmlInfo.indexOf("birth_year=", beginIndex);
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                if (date.length() > 0) {
                    date.append("-");
                }
                date.append(xmlInfo.substring(beginIndex + 11, beginIndex + 15));
            }

            beginIndex = xmlInfo.indexOf("birth_place=", beginIndex);
            String place;
            if (beginIndex > -1) {
                place = HTMLTools.extractTag(xmlInfo, "birth_place=", HTML_A_END);
                int start = place.indexOf('>');
                if (start > -1 && start < place.length()) {
                    place = place.substring(start + 1);
                }
                if (isValidString(place)) {
                    person.setBirthPlace(place);
                }
            }
        }

        beginIndex = xmlInfo.indexOf(">Date of Death</td>");
        if (beginIndex > -1) {
            endIndex = xmlInfo.indexOf(">Mini Bio (1)</h4>", beginIndex);
            beginIndex = xmlInfo.indexOf("death_monthday=", beginIndex);
            StringBuilder dDate = new StringBuilder();
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                Matcher m = PATTERN_DOB.matcher(xmlInfo.substring(beginIndex + 15, beginIndex + 20));
                if (m.find()) {
                    dDate.append(m.group(2));
                    dDate.append("-");
                    dDate.append(m.group(1));
                }
            }
            beginIndex = xmlInfo.indexOf("death_date=", beginIndex);
            if (beginIndex > -1 && (endIndex == -1 || beginIndex < endIndex)) {
                if (dDate.length() > 0) {
                    dDate.append("-");
                }
                dDate.append(xmlInfo.substring(beginIndex + 11, beginIndex + 15));
            }
            if (dDate.length() > 0) {
                date.append("/").append(dDate);
            }
        }

        if (StringUtils.isNotBlank(date)) {
            person.setYear(date.toString());
        }

        beginIndex = xmlInfo.indexOf(">Birth Name</td>");
        if (beginIndex > -1) {
            beginIndex += 20;
            String name = xmlInfo.substring(beginIndex, xmlInfo.indexOf(HTML_TD_END, beginIndex));
            if (isValidString(name)) {
                person.setBirthName(HTMLTools.decodeHtml(name));
            }
        }

        beginIndex = xmlInfo.indexOf(">Nickname</td>");
        if (beginIndex > -1) {
            String name = xmlInfo.substring(beginIndex + 17, xmlInfo.indexOf(HTML_TD_END, beginIndex));
            if (isValidString(name)) {
                person.addAka(name);
            }
        } else {
            beginIndex = xmlInfo.indexOf(">Nicknames</td>");
            if (beginIndex > -1) {
                String name = xmlInfo.substring(beginIndex + 19, xmlInfo.indexOf(HTML_TD_END, beginIndex + 19));
                for (String n : name.split("<br>")) {
                    person.addAka(n.trim());
                }
            }
        }

        if (xmlInfo.contains(">Mini Bio (1)</h4>")) {
            String biography = HTMLTools.extractTag(xmlInfo, ">Mini Bio (1)</h4>", "<em>- IMDb Mini Biography By");

            if (isValidString(biography)) {
                biography = HTMLTools.removeHtmlTags(biography);
                biography = trimToLength(biography, preferredBiographyLength);
                person.setBiography(biography);
            }
        }

        // get known movies
        xmlInfo = webBrowser.request(getImdbUrl(person) + "filmoyear", siteDefinition.getCharset());
        if (xmlInfo.contains("<div id=\"tn15content\">")) {
            int count = HTMLTools.extractTags(xmlInfo, "<div id=\"tn15content\">", HTML_DIV_END, "<li>", "</li>").size();
            person.setKnownMovies(count);
        }

        // get filmography
        processFilmography(person, xml);

        int version = person.getVersion();
        person.setVersion(++version);
        return Boolean.TRUE;
    }

    /**
     * Process the person's filmography from the source XML
     *
     * @param person
     * @param sourceXml
     * @throws IOException
     */
    private void processFilmography(Person person, String sourceXml) throws IOException {
        int beginIndex, endIndex;

        String xmlInfo = webBrowser.request(getImdbUrl(person) + "filmorate", siteDefinition.getCharset());
        if (xmlInfo.contains("<span class=\"lister-current-first-item\">")) {
            String fg = HTMLTools.extractTag(sourceXml, "<div id=\"filmography\">", "<div class=\"article\" >");
            Map<Float, Filmography> filmography = new TreeMap<Float, Filmography>();
            Pattern tvPattern = Pattern.compile("( \\(#\\d+\\.\\d+\\))|(: Episode #\\d+\\.\\d+)");
            for (String department : HTMLTools.extractTags(xmlInfo, "<div id=\"tn15content\">", "<style>", "<div class=\"filmo\"", HTML_DIV_END)) {
                String job = HTMLTools.removeHtmlTags(HTMLTools.extractTag(department, HTML_H5_START, "</h5>"));
                if (!jobsInclude.contains(job)) {
                    continue;
                }
                for (String item : HTMLTools.extractTags(department, "<ol", "</ol>", "<li", "</li>")) {
                    beginIndex = item.indexOf("<h6>");
                    if (beginIndex > -1) {
                        item = item.substring(0, beginIndex);
                    }
                    int rating = StringTools.parseRating(HTMLTools.extractTag(item, "(", ")"));

                    String id = HTMLTools.extractTag(item, "<a href=\"/title/", "/\">");
                    String name = HTMLTools.extractTag(item, "/\">", HTML_A_END).replaceAll("\"", "");
                    String year = Movie.UNKNOWN;
                    String itemTail = Movie.UNKNOWN;
                    beginIndex = item.indexOf("</a> (");
                    if (beginIndex > -1) {
                        itemTail = item.substring(beginIndex);
                        year = HTMLTools.extractTag(itemTail, "(", ")");
                    }
                    if ((skipVG && (name.endsWith("(VG)") || itemTail.endsWith("(VG)"))) || (skipTV && (name.endsWith("(TV)") || itemTail.endsWith("(TV)"))) || (skipV && (name.endsWith("(V)") || itemTail.endsWith("(V)")))) {
                        continue;
                    } else if (skipTV) {
                        Matcher tvMatcher = tvPattern.matcher(name);
                        if (tvMatcher.find()) {
                            continue;
                        }
                        beginIndex = fg.indexOf("href=\"/title/" + id);
                        if (beginIndex > -1) {
                            beginIndex = fg.indexOf("</b>", beginIndex);
                            if (beginIndex > -1 && fg.indexOf(HTML_BREAK, beginIndex) > -1) {
                                String tail = fg.substring(beginIndex + 4, fg.indexOf(HTML_BREAK, beginIndex));
                                if (tail.contains("(TV series)") || tail.contains("(TV mini-series)") || tail.contains("(TV movie)")) {
                                    continue;
                                }
                            }
                        }
                    }

                    String url = siteDefinition.getSite() + HTML_TITLE + id + "/";
                    String character = Movie.UNKNOWN;
                    if (job.equalsIgnoreCase("actor") || job.equalsIgnoreCase("actress")) {
                        beginIndex = fg.indexOf("href=\"/title/" + id);
                        if (beginIndex > -1) {
                            int brIndex = fg.indexOf(HTML_BREAK, beginIndex);
                            int divIndex = fg.indexOf("<div", beginIndex);
                            int hellipIndex = fg.indexOf("&hellip;", beginIndex);
                            if (divIndex > -1) {
                                if (brIndex > -1 && brIndex < divIndex) {
                                    character = fg.substring(brIndex + 5, divIndex);
                                    character = HTMLTools.removeHtmlTags(character);
                                } else if (hellipIndex > -1 && hellipIndex < divIndex) {
                                    character = fg.substring(hellipIndex + 8, divIndex);
                                    character = HTMLTools.removeHtmlTags(character);
                                }
                            }
                        }
                        if (isNotValidString(character)) {
                            character = Movie.UNKNOWN;
                        }
                    }

                    float key = 101 - (rating + Float.parseFloat("0." + id.substring(2)));

                    if (filmography.get(key) == null) {
                        Filmography film = new Filmography();
                        film.setId(id);
                        film.setName(name);
                        film.setYear(year);
                        film.setJob(job);
                        film.setCharacter(character);
                        film.setDepartment();
                        film.setRating(Integer.toString(rating));
                        film.setUrl(url);
                        filmography.put(key, film);
                    }
                }
            }

            Iterator<Float> iterFilm = filmography.keySet().iterator();
            int count = 0;
            while (iterFilm.hasNext() && count < preferredFilmographyMax) {
                Filmography film = filmography.get(iterFilm.next());
                if ((film.getJob().equalsIgnoreCase("actor") || film.getJob().equalsIgnoreCase("actress")) && isNotValidString(film.getCharacter())) {
                    String movieXML = webBrowser.request(siteDefinition.getSite() + HTML_TITLE + film.getId() + "/" + "fullcredits");

                    beginIndex = movieXML.indexOf("(in credits order)");
                    String character = Movie.UNKNOWN;
                    if (beginIndex > -1) {
                        endIndex = movieXML.indexOf(">Produced by", beginIndex);
                        endIndex = endIndex < 0 ? movieXML.length() : endIndex;

                        character = HTMLTools.extractTag(movieXML.substring(beginIndex, endIndex), "<a href=\"/name/" + person.getId(), "</tr>");
                        character = HTMLTools.stripTags(HTMLTools.extractTag(character, "<td class=\"character\">", "</td>"));
                        // Remove any text in brackets
                        endIndex = character.indexOf('(');
                        if (endIndex > -1) {
                            character = StringUtils.trimToEmpty(character.substring(0, endIndex));
                        }
                    }
                    if (isValidString(character)) {
                        film.setCharacter(character);
                    }
                }
                person.addFilm(film);
                count++;
            }
        }
    }

    /**
     * Create a map of the AKA values
     *
     * @param list
     * @return
     */
    private static Map<String, String> buildAkaMap(List<String> list) {
        Map<String, String> map = new LinkedHashMap<String, String>();
        int i = 0;
        do {
            try {
                String key = list.get(i++);
                String value = list.get(i++);
                map.put(key, value);
            } catch (Exception ignore) {
                i = -1;
            }
        } while (i != -1);
        return map;
    }
}
