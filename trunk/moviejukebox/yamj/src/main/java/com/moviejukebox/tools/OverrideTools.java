/*
 *      Copyright (c) 2004-2012 YAMJ Members
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
package com.moviejukebox.tools;

import com.moviejukebox.model.Filmography;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.OverrideFlag;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;

/**
 * Holds some override tools.
 *
 * @author modmax
 */
public final class OverrideTools {

    private static final Map<OverrideFlag, List<String>> MOVIE_PRIORITIES_MAP = new HashMap<OverrideFlag,List<String>>();
    private static final Map<OverrideFlag, List<String>> TV_PRIORITIES_MAP = new HashMap<OverrideFlag,List<String>>();
    // check skip if not in priority list
    private static final boolean SKIP_NOT_IN_LIST = PropertiesUtil.getBooleanProperty("priority.checks.skipNotInList", FALSE); 
    // hold max counts for people
    private static final int MAX_COUNT_DIRECTOR = PropertiesUtil.getIntProperty("plugin.people.maxCount.director", "2");
    private static final int MAX_COUNT_WRITER = PropertiesUtil.getIntProperty("plugin.people.maxCount.writer", "3");
    private static final int MAX_COUNT_ACTOR = PropertiesUtil.getIntProperty("plugin.people.maxCount.actor", "10");

    static {
        String sources;
        
        // MOVIE PRIORITIES
        
        // actors
        sources = PropertiesUtil.getProperty("priority.movie.actors", "nfo,imdb");
        putMoviePriorities(OverrideFlag.ACTORS, sources);
        // aspect ratio
        sources = PropertiesUtil.getProperty("priority.movie.aspectratio", "nfo,mediainfo,imdb");
        putMoviePriorities(OverrideFlag.ASPECTRATIO, sources);
        // certification
        sources = PropertiesUtil.getProperty("priority.movie.certification", "nfo,imdb");
        putMoviePriorities(OverrideFlag.CERTIFICATION, sources);
        // certification
        sources = PropertiesUtil.getProperty("priority.movie.container", "nfo,mediainfo,filename");
        putMoviePriorities(OverrideFlag.CONTAINER, sources);
        // company
        sources = PropertiesUtil.getProperty("priority.movie.company", "nfo,imdb");
        putMoviePriorities(OverrideFlag.COMPANY, sources);
        // country
        sources = PropertiesUtil.getProperty("priority.movie.country", "nfo,imdb");
        putMoviePriorities(OverrideFlag.COUNTRY, sources);
        // directors
        sources = PropertiesUtil.getProperty("priority.movie.directors", "nfo,imdb");
        putMoviePriorities(OverrideFlag.DIRECTORS, sources);
        // genres
        sources = PropertiesUtil.getProperty("priority.movie.genres", "nfo,imdb");
        putMoviePriorities(OverrideFlag.GENRES, sources);
        // frames per seconds
        sources = PropertiesUtil.getProperty("priority.movie.fps", "nfo,mediainfo,filename");
        putMoviePriorities(OverrideFlag.FPS, sources);
        // language
        sources = PropertiesUtil.getProperty("priority.movie.language", "nfo,mediainfo,filename");
        putMoviePriorities(OverrideFlag.LANGUAGE, sources);
        // original title
        sources = PropertiesUtil.getProperty("priority.movie.originaltitle", "nfo,imdb");
        putMoviePriorities(OverrideFlag.ORIGINALTITLE, sources);
        // outline
        sources = PropertiesUtil.getProperty("priority.movie.outline", "nfo,imdb");
        putMoviePriorities(OverrideFlag.OUTLINE, sources);
        // outline
        sources = PropertiesUtil.getProperty("priority.movie.plot", "nfo,imdb");
        putMoviePriorities(OverrideFlag.PLOT, sources);
        // quote
        sources = PropertiesUtil.getProperty("priority.movie.quote", "nfo,imdb");
        putMoviePriorities(OverrideFlag.QUOTE, sources);
        // release date
        sources = PropertiesUtil.getProperty("priority.movie.releasedate", "nfo,imdb");
        putMoviePriorities(OverrideFlag.RELEASEDATE, sources);
        // resolution
        sources = PropertiesUtil.getProperty("priority.movie.resolution", "nfo,mediainfo");
        putMoviePriorities(OverrideFlag.RESOLUTION, sources);
        // runtime
        sources = PropertiesUtil.getProperty("priority.movie.runtime", "nfo,mediainfo,filename,imdb");
        putMoviePriorities(OverrideFlag.RUNTIME, sources);
        // tagline
        sources = PropertiesUtil.getProperty("priority.movie.tagline", "nfo,imdb");
        putMoviePriorities(OverrideFlag.TAGLINE, sources);
        // title
        sources = PropertiesUtil.getProperty("priority.movie.title", "nfo,imdb,filename");
        putMoviePriorities(OverrideFlag.TITLE, sources);
        // video output
        sources = PropertiesUtil.getProperty("priority.movie.videooutput", "nfo,mediainfo,filename");
        putMoviePriorities(OverrideFlag.VIDEOOUTPUT, sources);
        // video source
        sources = PropertiesUtil.getProperty("priority.movie.videosource", "nfo,filename,mediainfo");
        putMoviePriorities(OverrideFlag.VIDEOSOURCE, sources);
        // writers
        sources = PropertiesUtil.getProperty("priority.movie.writers", "nfo,filename");
        putMoviePriorities(OverrideFlag.WRITERS, sources);
        // year
        sources = PropertiesUtil.getProperty("priority.movie.year", "nfo,imdb,filename");
        putMoviePriorities(OverrideFlag.YEAR, sources);

        // TV PRIORITIES
        
        sources = PropertiesUtil.getProperty("priority.tv.actors", "nfo,imdb");
        putTvPriorities(OverrideFlag.ACTORS, sources);
        // aspect ratio
        sources = PropertiesUtil.getProperty("priority.tv.aspectratio", "nfo,mediainfo,imdb");
        putTvPriorities(OverrideFlag.ASPECTRATIO, sources);
        // certification
        sources = PropertiesUtil.getProperty("priority.tv.certification", "nfo,imdb");
        putTvPriorities(OverrideFlag.CERTIFICATION, sources);
        // certification
        sources = PropertiesUtil.getProperty("priority.tv.container", "nfo,mediainfo,filename");
        putTvPriorities(OverrideFlag.CONTAINER, sources);
        // company
        sources = PropertiesUtil.getProperty("priority.tv.company", "nfo,imdb");
        putTvPriorities(OverrideFlag.COMPANY, sources);
        // country
        sources = PropertiesUtil.getProperty("priority.tv.country", "nfo,imdb");
        putTvPriorities(OverrideFlag.COUNTRY, sources);
        // directors
        sources = PropertiesUtil.getProperty("priority.tv.directors", "nfo,imdb");
        putTvPriorities(OverrideFlag.DIRECTORS, sources);
        // genres
        sources = PropertiesUtil.getProperty("priority.tv.genres", "nfo,imdb");
        putTvPriorities(OverrideFlag.GENRES, sources);
        // frames per seconds
        sources = PropertiesUtil.getProperty("priority.tv.fps", "nfo,mediainfo,filename");
        putTvPriorities(OverrideFlag.FPS, sources);
        // language
        sources = PropertiesUtil.getProperty("priority.tv.language", "nfo,mediainfo,filename");
        putTvPriorities(OverrideFlag.LANGUAGE, sources);
        // original title
        sources = PropertiesUtil.getProperty("priority.tv.originaltitle", "nfo,imdb");
        putTvPriorities(OverrideFlag.ORIGINALTITLE, sources);
        // outline
        sources = PropertiesUtil.getProperty("priority.tv.outline", "nfo,imdb");
        putTvPriorities(OverrideFlag.OUTLINE, sources);
        // outline
        sources = PropertiesUtil.getProperty("priority.tv.plot", "nfo,imdb");
        putTvPriorities(OverrideFlag.PLOT, sources);
        // quote
        sources = PropertiesUtil.getProperty("priority.tv.quote", "nfo,imdb");
        putTvPriorities(OverrideFlag.QUOTE, sources);
        // release date
        sources = PropertiesUtil.getProperty("priority.tv.releasedate", "nfo,imdb");
        putTvPriorities(OverrideFlag.RELEASEDATE, sources);
        // resolution
        sources = PropertiesUtil.getProperty("priority.tv.resolution", "nfo,mediainfo");
        putTvPriorities(OverrideFlag.RESOLUTION, sources);
        // runtime
        sources = PropertiesUtil.getProperty("priority.tv.runtime", "nfo,mediainfo,filename,imdb");
        putTvPriorities(OverrideFlag.RUNTIME, sources);
        // tagline
        sources = PropertiesUtil.getProperty("priority.tv.tagline", "nfo,imdb");
        putTvPriorities(OverrideFlag.TAGLINE, sources);
        // title
        sources = PropertiesUtil.getProperty("priority.tv.title", "nfo,imdb,filename");
        putTvPriorities(OverrideFlag.TITLE, sources);
        // video output
        sources = PropertiesUtil.getProperty("priority.tv.videooutput", "nfo,mediainfo,filename");
        putTvPriorities(OverrideFlag.VIDEOOUTPUT, sources);
        // video source
        sources = PropertiesUtil.getProperty("priority.tv.videosource", "nfo,filename,mediainfo");
        putTvPriorities(OverrideFlag.VIDEOSOURCE, sources);
        // writers
        sources = PropertiesUtil.getProperty("priority.tv.writers", "nfo,filename");
        putTvPriorities(OverrideFlag.WRITERS, sources);
        // year
        sources = PropertiesUtil.getProperty("priority.tv.year", "nfo,imdb,filename");
        putTvPriorities(OverrideFlag.YEAR, sources);
        
        // EXTRA properties for people scraping (filmography)
        
        // actors
        sources = PropertiesUtil.getProperty("priority.movie.people.actors", "imdb,nfo");
        putMoviePriorities(OverrideFlag.PEOPLE_ACTORS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.people.actors", "imdb,nfo");
        putTvPriorities(OverrideFlag.PEOPLE_ACTORS, sources);
        // directors
        sources = PropertiesUtil.getProperty("priority.movie.people.directors", "imdb,nfo");
        putMoviePriorities(OverrideFlag.PEOPLE_DIRECTORS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.people.directors", "imdb,nfo");
        putTvPriorities(OverrideFlag.PEOPLE_DIRECTORS, sources);
        // writers
        sources = PropertiesUtil.getProperty("priority.movie.people.writers", "imdb,nfo");
        putMoviePriorities(OverrideFlag.PEOPLE_WRITERS, sources);
        sources = PropertiesUtil.getProperty("priority.tv.people.writers", "imdb,nfo");
        putTvPriorities(OverrideFlag.PEOPLE_WRITERS, sources);
        
    }

    /**
     * Put movie priorities into map.
     * 
     * @param overrideFlag
     * @param sources
     */
    private static void putMoviePriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities;
        if (StringUtils.isBlank(sources)) {
            priorities = Collections.emptyList();
        } else  {
            priorities = Arrays.asList(sources.toUpperCase().split(","));
        }
        MOVIE_PRIORITIES_MAP.put(overrideFlag, priorities);
    }

    /**
     * Put movie priorities into map.
     * 
     * @param overrideFlag
     * @param sources
     */
    private static void putTvPriorities(OverrideFlag overrideFlag, String sources) {
        List<String> priorities;
        if (StringUtils.isBlank(sources)) {
            priorities = Collections.emptyList();
        } else  {
            priorities = Arrays.asList(sources.toUpperCase().split(","));
        }
        TV_PRIORITIES_MAP.put(overrideFlag, priorities);
    }
    
    private static boolean skipCheck(Movie movie, OverrideFlag overrideFlag, String source) {
        if (SKIP_NOT_IN_LIST) {

            int index = -1;
            try {
                if (movie.isTVShow()) {
                    index = TV_PRIORITIES_MAP.get(overrideFlag).indexOf(source.toUpperCase());
                } else {
                    index = MOVIE_PRIORITIES_MAP.get(overrideFlag).indexOf(source.toUpperCase());
                }
            } catch (Exception ignore) {
                // ignore this error
            }

            // index < 0 means: not in list, so skip the check
            return (index < 0);
        }
        
        // no skip
        return Boolean.FALSE;
    }
    
    /**
     * Check the priority of a property to set.
     * 
     * @param property the property to test
     * @param actualSource the actual source
     * @param newSource the new source
     * @return true, if new source has higher property than actual source, else false
     */
    private static boolean hasHigherPriority(final OverrideFlag overrideFlag, final String actualSource, final String newSource, boolean isTV) {
        // check sources
        if (StringTools.isNotValidString(actualSource) && StringTools.isNotValidString(newSource)) {
            // both invalid 
            // -> actual source has higher priority
            return Boolean.FALSE;
        } else if (StringTools.isValidString(actualSource) && StringTools.isNotValidString(newSource)) {
            // new source is not valid
            // -> actual source has higher priority
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(actualSource) && StringTools.isValidString(newSource)) {
            // actual source is not valid
            // -> new source has higher priority
            return Boolean.TRUE;
        } else if (actualSource.equalsIgnoreCase(newSource)) {
            // same source may override itself
            return Boolean.TRUE;
        }

        // both sources are valid so get priorities
        List<String> priorities;
        if (isTV) {
            priorities = TV_PRIORITIES_MAP.get(overrideFlag);
        } else {
            priorities = MOVIE_PRIORITIES_MAP.get(overrideFlag);
        }
        
        // get and check new priority
        int newPrio = priorities.indexOf(newSource.toUpperCase());
        if (newPrio == -1) {
            // priority for new source not found
            // -> actual source has higher priority
            return Boolean.FALSE;
        }
        
        // check actual priority
        int actualPrio = priorities.indexOf(actualSource.toUpperCase());
        if (newPrio <= actualPrio) {
            // -> new source has higher priority
            return Boolean.TRUE;
        }
        
        // -> actual source has higher priority
        return Boolean.FALSE;
    }

    private static boolean checkOverwrite(Movie movie, OverrideFlag overrideFlag, String source) {
        String actualSource = movie.getOverrideSource(overrideFlag);
        return OverrideTools.hasHigherPriority(overrideFlag, actualSource, source, movie.isTVShow());
    }

    public static boolean checkOneOverwrite(Movie movie, String source, OverrideFlag... overrideFlags) {
        for (OverrideFlag overrideFlag : overrideFlags) {
            boolean check;
            switch (overrideFlag) {
                case OUTLINE:
                    check = checkOverwriteOutline(movie, source);
                    break;
                case PLOT:
                    check = checkOverwritePlot(movie, source);
                    break;
                case TITLE:
                    check = checkOverwriteTitle(movie, source);
                    break;
                default:
                    check = checkOverwrite(movie, overrideFlag, source);
                    break;
                    
                // TODO until now these checks are enough
            }
            if (check) return true;
        }
        return false;
    }
    
    public static boolean checkOverwriteActors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.ACTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_ACTOR <= 0) {
            // regard no actors
            return Boolean.FALSE;
        } else if (movie.getCast() == null || movie.getCast().isEmpty()) {
            // no actors set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.ACTORS, source);
    }
   
    public static boolean checkOverwriteAspectRatio(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.ASPECTRATIO, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getAspectRatio())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.ASPECTRATIO, source);
    }

    public static boolean checkOverwriteCertification(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.CERTIFICATION, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getCertification())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.CERTIFICATION, source);
    }

    public static boolean checkOverwriteCompany(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.COMPANY, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getCompany())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.COMPANY, source);
    }
    
    public static boolean checkOverwriteContainer(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.CONTAINER, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getContainer())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.CONTAINER, source);
    }

    public static boolean checkOverwriteCountry(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.COUNTRY, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getCountry())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.COUNTRY, source);
    }

    public static boolean checkOverwriteDirectors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.DIRECTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        }else if (MAX_COUNT_DIRECTOR <= 0) {
            // regard no directors
            return Boolean.FALSE;
        } else if (movie.getDirectors() == null || movie.getDirectors().isEmpty()) {
            // no directors set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.DIRECTORS, source);
    }

    public static boolean checkOverwriteGenres(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.GENRES, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (movie.getGenres() == null || movie.getGenres().isEmpty()) {
            // no genres set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.GENRES, source);
    }

    public static boolean checkOverwriteFPS(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.FPS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (movie.getFps() == 60f) {
            // assume 60 as default value
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.FPS, source);
    }
    
    public static boolean checkOverwriteLanguage(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.LANGUAGE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getLanguage())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.LANGUAGE, source);
    }

    public static boolean checkOverwriteOriginalTitle(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.ORIGINALTITLE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getOriginalTitle())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.ORIGINALTITLE, source);
    }
    
    public static boolean checkOverwriteOutline(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.OUTLINE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getOutline())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.OUTLINE, source);
    }

    public static boolean checkOverwritePlot(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PLOT, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getPlot())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PLOT, source);
    }

    public static boolean checkOverwriteQuote(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.QUOTE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getQuote())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.QUOTE, source);
    }

    public static boolean checkOverwriteReleaseDate(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.RELEASEDATE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getReleaseDate())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.RELEASEDATE, source);
    }

    public static boolean checkOverwriteResolution(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.RESOLUTION, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getResolution())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.RESOLUTION, source);
    }
    
    public static boolean checkOverwriteRuntime(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.RUNTIME, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getRuntime())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.RUNTIME, source);
    }

    public static boolean checkOverwriteTagline(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.TAGLINE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getTagline())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.TAGLINE, source);
    }

    public static boolean checkOverwriteTitle(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.TITLE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getTitle())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.TITLE, source);
    }

    public static boolean checkOverwriteVideoOutput(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.VIDEOOUTPUT, source)) {
            // skip the check
            return Boolean.FALSE;
        }else if (StringTools.isNotValidString(movie.getVideoOutput())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.VIDEOOUTPUT, source);
    }

    public static boolean checkOverwriteVideoSource(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.VIDEOSOURCE, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getVideoSource())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.VIDEOSOURCE, source);
    }

    public static boolean checkOverwriteWriters(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.WRITERS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_WRITER <= 0) {
            // regard no writers
            return Boolean.FALSE;
        } else if (movie.getWriters() == null || movie.getWriters().isEmpty()) {
            // no writers set until now
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.WRITERS, source);
    }

    public static boolean checkOverwriteYear(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.YEAR, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (StringTools.isNotValidString(movie.getYear())) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.YEAR, source);
    }

    public static boolean checkOverwritePeopleActors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PEOPLE_ACTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_ACTOR <= 0) {
            // regard no actors
            return Boolean.FALSE;
        } else if (movie.getPerson(Filmography.DEPT_ACTORS).isEmpty()) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PEOPLE_ACTORS, source);
    }

    public static boolean checkOverwritePeopleDirectors(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PEOPLE_DIRECTORS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_DIRECTOR <= 0) {
            // regard no directors
            return Boolean.FALSE;
        } else if (movie.getPerson(Filmography.DEPT_DIRECTING).isEmpty()) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PEOPLE_DIRECTORS, source);
    }

    public static boolean checkOverwritePeopleWriters(Movie movie, String source) {
        if (skipCheck(movie, OverrideFlag.PEOPLE_WRITERS, source)) {
            // skip the check
            return Boolean.FALSE;
        } else if (MAX_COUNT_WRITER <= 0) {
            // regard no writers
            return Boolean.FALSE;
        } else if (movie.getPerson(Filmography.DEPT_WRITING).isEmpty()) {
            return Boolean.TRUE;
        }
        return checkOverwrite(movie, OverrideFlag.PEOPLE_WRITERS, source);
    }
}
