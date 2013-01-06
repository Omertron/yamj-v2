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
package com.moviejukebox.model.Comparator;

import com.moviejukebox.model.Movie;
import static com.moviejukebox.tools.StringTools.isValidString;
import java.io.Serializable;
import java.util.Comparator;

/**
 * @author ilgizar
 */
public class MovieYearComparator implements Comparator<Movie>, Serializable {

    private static final long serialVersionUID = 1L;
    private boolean ascending = Boolean.TRUE;

    public MovieYearComparator(boolean ascending) {
        this.ascending = ascending;
    }

    @Override
    public int compare(Movie movie1, Movie movie2) {
        return compare(movie1, movie2, ascending);
    }

    public int compare(Movie movie1, Movie movie2, boolean ascending) {
        if (isValidString(movie1.getYear()) && isValidString(movie2.getYear())) {
            try {
                return ascending ? (Integer.parseInt(movie1.getYear()) - Integer.parseInt(movie2.getYear())) : (Integer.parseInt(movie2.getYear()) - Integer.parseInt(movie1.getYear()));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return isValidString(movie1.getYear()) ? ascending ? 1 : -1 : isValidString(movie2.getYear()) ? ascending ? -1 : 1 : 0;
    }
}