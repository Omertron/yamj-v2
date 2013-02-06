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

import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

import com.moviejukebox.tools.PropertiesUtil;

public class ImdbInfoTest {

    public ImdbInfoTest() {
        BasicConfigurator.configure();
    }

    @Test
    public void testImdbPersonId() {
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            PropertiesUtil.setProperty("imdb.site", site);
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbPersonId("Renée Zellweger");
            assertEquals("nm0000250", id);
        }
    }

    @Test
    public void testImdbMovieId_VariableOff() {
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", "false");
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbId("Abraham Lincoln Vampire Hunter", null, false);
            assertNotEquals("Search site " + site, "tt1611224", id); // correct one
        }
    }

    @Test
    public void testImdbMovieId_VariableOn() {
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", "true");
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbId("Abraham Lincoln Vampire Hunter", null, false);
            assertEquals("Search site " + site, "tt1611224", id); // correct one
        }
    }

    @Test
    public void testImdbMovieIdFirstMatch() {
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "first");
            PropertiesUtil.setProperty("imdb.id.search.variable", "false");
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("Search site " + site, "tt0499549", id); // correct one
        }
    }
    
    @Test
    public void testImdbMovieIdRegularMatch() {
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "regular");
            PropertiesUtil.setProperty("imdb.id.search.variable", "false");
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            assertEquals("Search site " + site, "tt0499549", id); // correct one
        }
    }

    @Test
    public void testImdbMovieIdExactMatch() {
        Set<String> keySet = ImdbInfo.MATCHES_DATA_PER_SITE.keySet();
        for (String site : keySet) {
            PropertiesUtil.setProperty("imdb.site", site);
            PropertiesUtil.setProperty("imdb.id.search.match", "exact");
            PropertiesUtil.setProperty("imdb.id.search.variable", "false");
            ImdbInfo imdbInfo = new ImdbInfo();
            
            String id = imdbInfo.getImdbId("Avatar", "2009", false);
            if ("pt".equalsIgnoreCase(site)) {
                // for PT --> found correct on google
                assertEquals("Search site " + site, "tt0499549", id); // correct one
            } else {
                assertNotEquals("Search site " + site, "tt0499549", id); // false one
            }
        }
    }
}