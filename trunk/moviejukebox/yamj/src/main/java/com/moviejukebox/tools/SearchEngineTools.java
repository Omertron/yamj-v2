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
package com.moviejukebox.tools;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import com.moviejukebox.model.Movie;

public class SearchEngineTools {

    private static final Logger LOGGER = Logger.getLogger(SearchEngineTools.class);
    private static final String LOG_MESSAGE = "SearchEngingTools: ";
    
    private WebBrowser webBrowser;
    private LinkedList<String> searchSites;
    private String searchSuffix = "";
    private String country;
    private String language;
    private String googleHost = "www.google.com";
    private String yahooHost = "search.yahoo.com";
    private String bingHost = "www.bing.com";
    private String blekkoHost = "www.blekko.com";
    private String lycosHost = "search.lycos.com";

    public SearchEngineTools() {
        this("us");
    }

    public SearchEngineTools(String country) {
        webBrowser = new WebBrowser();
        // user agent should be an actual FireFox
        webBrowser.addBrowserProperty("User-Agent", "Mozilla/6.0 (Windows NT 6.2; WOW64; rv:16.0.1) Gecko/20121011 Firefox/16.0.1");
        
        // sites to search for URLs
        searchSites = new LinkedList<String>();
        searchSites.addAll(Arrays.asList(PropertiesUtil.getProperty("searchengine.sites", "google,yahoo,bing,blekko,lycos").split(",")));

        // country specific presets
        if ("de".equalsIgnoreCase(country)) {
            this.country = "de";
            language = "de";
            googleHost = "www.google.de";
            yahooHost = "de.search.yahoo.com";
            lycosHost = "search.lycos.de";
        } else if ("it".equalsIgnoreCase(country)) {
            this.country = "it";
            language = "it";
            googleHost = "www.google.it";
            yahooHost = "it.search.yahoo.com";
            bingHost = "it.bing.com";
            lycosHost = "search.lycos.it";
        } else if ("se".equalsIgnoreCase(country)) {
            this.country = "se";
            language = "sv";
            googleHost = "www.google.se";
            yahooHost = "se.search.yahoo.com";
            lycosHost = "search.lycos.se";
        } else if ("pl".equalsIgnoreCase(country)) {
            this.country = "pl";
            language = "pl";
            googleHost = "www.google.pl";
            yahooHost = "pl.search.yahoo.com";
        } else if ("ru".equalsIgnoreCase(country)) {
            this.country = "ru";
            language = "ru";
            googleHost = "www.google.ru";
            yahooHost = "ru.search.yahoo.com";
        } else if ("il".equalsIgnoreCase(country)) {
            this.country = "il";
            language = "il";
            googleHost = "www.google.co.il";
        } else if ("fr".equalsIgnoreCase(country)) {
            this.country = "fr";
            language = "fr";
            googleHost = "www.google.fr";
        } else if ("nl".equalsIgnoreCase(country)) {
            this.country = "nl";
            language = "nl";
            googleHost = "www.google.nl";
            lycosHost = "search.lycos.nl";
        }
    }

    public void setSearchSites(String searchSites) {
        this.searchSites.clear();
        this.searchSites.addAll(Arrays.asList(searchSites.split(",")));
    }

    public void setSearchSuffix(String searchSuffix) {
        this.searchSuffix = searchSuffix;
    }
    
    public String searchMovieURL(String title, String year, String site) {
        return searchMovieURL(title, year, site, null);
    }
    
    public String searchMovieURL(String title, String year, String site, String additional) {
        String url = null;

        String engine = getNextSearchEngine();
        if ("yahoo".equalsIgnoreCase(engine)) {
            url = searchUrlOnYahoo(title, year, site, additional);
        } else if ("bing".equalsIgnoreCase(engine)) {
            url = searchUrlOnBing(title, year, site, additional);
        } else if ("blekko".equalsIgnoreCase(engine)) {
            url = searchUrlOnBlekko(title, year, site, additional);
        } else if ("lycos".equalsIgnoreCase(engine)) {
            url = searchUrlOnLycos(title, year, site, additional);
        } else {
            url = searchUrlOnGoogle(title, year, site, additional);
        }
        
        return url;
    }

    private String getNextSearchEngine() {
        String engine = searchSites.remove();
        searchSites.addLast(engine);
        return engine;
    }
    
    public int countSearchSites() {
        return searchSites.size();
    }
    
    public String searchUrlOnGoogle(String title, String year, String site, String additional) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on google; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(googleHost);
            sb.append("/search?");
            if (language != null) {
                sb.append("hl=");
                sb.append(language);
                sb.append("&");
            }
            sb.append("q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }
            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by google search : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    public String searchUrlOnYahoo(String title, String year, String site, String additional) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on yahoo; site="+site);

        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(yahooHost);
            sb.append("/search?vc=");
            if (country != null) {
                sb.append(country);
                sb.append("&rd=r2");
            }
            sb.append("&ei=UTF-8&p=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }

            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("//" + site + searchSuffix);
            if (beginIndex != -1) {
                String link = xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
                if (StringTools.isValidString(link)) {
                    return "http:"+link;
                }
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by yahoo search: " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    public String searchUrlOnBing(String title, String year, String site, String additional) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on bing; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(bingHost);
            sb.append("/search?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }
            if (country != null) {
                sb.append("&cc=");
                sb.append(country);
                sb.append("&filt=rf");
            }
            
            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by bing search : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }

    public String searchUrlOnBlekko(String title, String year, String site, String additional) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on blekko; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(blekkoHost);
            sb.append("/ws/?q=");
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }
            
            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by bing search : " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }    

    public String searchUrlOnLycos(String title, String year, String site, String additional) {
        LOGGER.debug(LOG_MESSAGE + "Searching '" + title + "' on lycos; site="+site);
        
        try {
            StringBuilder sb = new StringBuilder("http://");
            sb.append(lycosHost);
            if ("it".equalsIgnoreCase(country)) {
                sb.append("/?tab=web&Search=Cerca&searchArea=loc&query=");
            } else if ("se".equalsIgnoreCase(country)) {
                sb.append("/?tab=web&Search=S%C3%B6ka&searchArea=loc&query=");
            } else if ("nl".equalsIgnoreCase(country)) {
                sb.append("/?tab=web&Search=Zoeken&searchArea=loc&query=");
            } else  {
                sb.append("/web/?q=");
            }
            sb.append(URLEncoder.encode(title, "UTF-8"));
            if (StringTools.isValidString(year)) {
                sb.append("+%28");
                sb.append(year);
                sb.append("%29");
            }
            sb.append("+site%3A");
            sb.append(site);
            if (additional != null) {
                sb.append("+");
                sb.append(URLEncoder.encode(additional, "UTF-8"));
            }

            String xml = webBrowser.request(sb.toString());
           
            int beginIndex = xml.indexOf("http://" + site + searchSuffix);
            if (beginIndex != -1) {
                return xml.substring(beginIndex, xml.indexOf("\"", beginIndex));
            }
        } catch (Exception error) {
            LOGGER.error(LOG_MESSAGE + "Failed retrieving link url by lycos search: " + title);
            LOGGER.error(SystemTools.getStackTrace(error));
        }
        return Movie.UNKNOWN;
    }    
}