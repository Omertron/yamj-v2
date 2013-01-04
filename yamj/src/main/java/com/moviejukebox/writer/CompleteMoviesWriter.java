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
package com.moviejukebox.writer;

import com.moviejukebox.MovieJukebox;
import com.moviejukebox.model.Jukebox;
import com.moviejukebox.model.Library;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.SystemTools;
import java.io.File;
import java.io.OutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.log4j.Logger;

/**
 *
 * @author stuart.boston
 */
public class CompleteMoviesWriter {

    private static final Logger logger = Logger.getLogger(CompleteMoviesWriter.class);

    protected CompleteMoviesWriter() {
        throw new UnsupportedOperationException("Class cannot be initialised");
    }

    public static void writeCompleteMovies(Library library, Jukebox jukebox) {
        String completeMoviesXmlFileName = "CompleteMovies.xml";
        String rssXmlFileName = "rss.xml";
        String rssXslFileName = "rss.xsl";
        JAXBContext context;

        try {
            context = JAXBContext.newInstance(MovieJukebox.JukeboxXml.class);
        } catch (JAXBException error) {
            logger.warn("CompleteMovies: RSS is not generated (Context creation error).");
            logger.warn(SystemTools.getStackTrace(error));
            return;
        }

        MovieJukebox.JukeboxXml jukeboxXml = new MovieJukebox.JukeboxXml();
        jukeboxXml.movies = library.values();

        if (library.isDirty()) {
            try {
                File totalMoviesXmlFile = new File(jukebox.getJukeboxTempLocationDetails(), completeMoviesXmlFileName);

                OutputStream marStream = FileTools.createFileOutputStream(totalMoviesXmlFile);
                context.createMarshaller().marshal(jukeboxXml, marStream);
                marStream.close();

                Transformer transformer = MovieJukeboxHTMLWriter.getTransformer(new File(rssXslFileName), jukebox.getJukeboxRootLocationDetails());

                Result xmlResult = new StreamResult(new File(jukebox.getJukeboxTempLocationDetails(), rssXmlFileName));
                transformer.transform(new StreamSource(totalMoviesXmlFile), xmlResult);
                logger.debug("CompleteMovies: RSS is generated.");
            } catch (Exception error) {
                logger.warn("CompleteMovies: RSS is not generated (Jukebox error).");
                logger.warn(SystemTools.getStackTrace(error));
            }
        }
        // These should be added to the list of jukebox files regardless of the state of the library
        FileTools.addJukeboxFile(completeMoviesXmlFileName);
        FileTools.addJukeboxFile(rssXmlFileName);
    }
}