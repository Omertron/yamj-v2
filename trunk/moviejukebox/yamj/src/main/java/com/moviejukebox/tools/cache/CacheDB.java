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
package com.moviejukebox.tools.cache;

import com.moviejukebox.tools.HibernateUtil;
import com.moviejukebox.tools.PropertiesUtil;
import java.io.Serializable;
import org.apache.log4j.Logger;

/**
 * Utility class to provide a caching mechanism for data across threads Initially this will be stored in memory, but
 * ideally should be cached to a database Many sites provide a "last modified" date/time attribute, so we should
 * consider also caching that in the database
 *
 * @author Stuart.Boston
 *
 */
public class CacheDB {

    private static final Logger logger = Logger.getLogger(CacheDB.class);
    private static boolean cacheEnabled = initCacheState();

    public CacheDB() {
        throw new IllegalArgumentException("Class cannot be initalised!");
    }

    public static boolean initCacheState() {
        boolean isEnabled = PropertiesUtil.getBooleanProperty("mjb.cache", Boolean.TRUE);
        logger.debug("Cache state is " + (isEnabled ? "enabled" : "disabled"));
        return isEnabled;
    }

    /**
     * Add an item to the cache. If the item currently exists in the cache it will be removed before being added.
     *
     * @param key
     * @param value
     */
    public static void addToCache(Serializable key, Object value) {
        if (!cacheEnabled) {
            return;
        }

        boolean isSaved = HibernateUtil.saveObject(value, key);
        if (isSaved) {
            logger.debug("Cache (Add): Adding object (" + value.getClass().getSimpleName() + ") for key " + key);
        } else {
            logger.debug("Cache (Add): Already contains object (" + value.getClass().getSimpleName() + ") with key " + key + " overwriting...");
        }
    }

    /**
     * Get an item from the cache
     *
     * @param key
     * @return
     */
    public static <T> T getFromCache(Serializable key, Class<T> clazz) {
        if (!cacheEnabled) {
            return null;
        }

        T dbObject = HibernateUtil.loadObject(clazz, key);

        if (dbObject != null) {
            logger.debug("Cache (Get): Got object (" + clazz.getSimpleName() + ") for " + key);
        } else {
            logger.debug("Cache (Get): No object found for " + key);
        }

        return dbObject;
    }
}
