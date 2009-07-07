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

package com.moviejukebox.model;

import java.util.Comparator;

/**
 * @author altman.matthew
 */
public class Top250Comparator implements Comparator<Movie> {

    @Override
    public int compare(Movie movie1, Movie movie2) {
        return movie1.getTop250() - movie2.getTop250();
    }
}
