package com.moviejukebox.plugin;

import com.moviejukebox.model.Library;
import com.moviejukebox.tools.PropertiesUtil;
import com.moviejukebox.tools.FileTools;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.io.File;

/**
 * User: JDGJr
 * Date: Feb 15, 2009
 */
public class MovieListingPluginBase implements MovieListingPlugin {
    private static Logger logger = Logger.getLogger("moviejukebox");

    protected boolean groupByType = true;
    protected boolean blankUNKNOWN = true;
    protected String baseFilename = "";
    protected String destination = "";

    /**
     * @param jukeboxRoot
     */
    protected void initialize(String jukeboxRoot) {
        groupByType = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.listing.GroupByType", "true"));
        blankUNKNOWN = Boolean.parseBoolean(PropertiesUtil.getProperty("mjb.listing.clear.UNKNOWN", "true"));
        baseFilename = PropertiesUtil.getProperty("mjb.listing.output.filename", "MovieJukebox-listing");
        destination = PropertiesUtil.getProperty("mjb.listing.output.destination", jukeboxRoot);
    } // initialize()

    /**
     * @return ArrayList of selected movie types, possibly from .properties file
     */
    protected ArrayList<String> getSelectedTypes() {
        ArrayList<String> alResult = new ArrayList<String>();

        String types = PropertiesUtil.getProperty("mjb.listing.types", typeAll).trim();
        if (typeAll.equalsIgnoreCase(types)) {
            types = typeMovie + "," + typeTrailer + "," + typeTVShow;
        }

        //break into a list
        StringTokenizer tokenizer = new StringTokenizer(types, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();

            //easy to skip space
            if (typeTVShowNoSpace.equalsIgnoreCase(token)) {
                token = typeTVShow;
            }
            alResult.add(token);
        }

        return alResult;
    } // getSelectedTypes()

    /**
     * @param file
     */
    protected void copyListingFile(File file, String filename) {
        // move to configured (default) location
        String dest = destination + File.separator + filename;
        logger.fine("  Copying to: " + dest);
        FileTools.copyFile(file, new File(dest));
    } // copyListingFile()

    /**
     * @param tempJukeboxRoot
     * @param JukeboxRoot
     * @param library
     */
    public void generate(String tempJukeboxRoot, String JukeboxRoot, Library library) {
        logger.fine("  MovieListingPluginBase: not generating listing file.");
    } // generate()

} // class MovieListingPluginBase
