/*
 *      Copyright (c) 2004-2009 YAMJ Members
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

import junit.framework.TestCase;

public class SratimPosterPluginTestCase extends TestCase {

    private static final String ID_MOVIE = "tt0172495";

    public void testGetId() {
        SratimPosterPlugin toTest = new SratimPosterPlugin();
        String idFromMovieInfo = toTest.getIdFromMovieInfo("Gladiator", null, -1);
        assertEquals(ID_MOVIE, idFromMovieInfo);

        String posterUrl = toTest.getPosterUrl(ID_MOVIE);
        assertEquals("http://www.sratim.co.il/movies/images/1/51.jpg", posterUrl);
    }
}
