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
package com.moviejukebox.scanner;

import com.moviejukebox.model.Attachment.Attachment;
import com.moviejukebox.model.Attachment.AttachmentType;
import com.moviejukebox.model.Attachment.ContentType;
import com.moviejukebox.model.DirtyFlag;
import com.moviejukebox.model.Library;
import com.moviejukebox.model.Movie;
import com.moviejukebox.model.MovieFile;
import com.moviejukebox.tools.FileTools;
import com.moviejukebox.tools.PropertiesUtil;
import static com.moviejukebox.tools.PropertiesUtil.FALSE;
import static com.moviejukebox.tools.PropertiesUtil.TRUE;
import com.moviejukebox.tools.StringTools;
import com.moviejukebox.tools.SystemTools;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/**
 * Scans and extracts attachments within a file i.e. matroska files.
 *
 * @author modmax
 */
public class AttachmentScanner {

    private static final Logger LOGGER = Logger.getLogger(AttachmentScanner.class);
    private static final String LOG_MESSAGE = "AttachmentScanner: ";
    // Enabled
    private static final Boolean IS_ENABLED = PropertiesUtil.getBooleanProperty("attachment.scanner.enabled", FALSE);
    // mkvToolnix
    private static final File MT_PATH = new File(PropertiesUtil.getProperty("attachment.mkvtoolnix.home", "./mkvToolnix/"));
    private static final String MT_LANGUAGE = PropertiesUtil.getProperty("attachment.mkvtoolnix.language", "");
    // mkvToolnix command line, depend on OS
    private static final List<String> MT_INFO_EXE = new ArrayList<String>();
    private static final List<String> MT_EXTRACT_EXE = new ArrayList<String>();
    private static final String MT_INFO_FILENAME_WINDOWS = "mkvinfo.exe";
    private static final String MT_INFO_FILENAME_LINUX = "mkvinfo";
    private static final String MT_EXTRACT_FILENAME_WINDOWS = "mkvextract.exe";
    private static final String MT_EXTRACT_FILENAME_LINUX = "mkvextract";
    // flag to indicate if scanner is activated
    private static boolean IS_ACTIVATED = Boolean.FALSE;
    // temporary directory
    private static File TEMP_DIRECTORY = null;
    private static boolean TEMP_CLEANUP = PropertiesUtil.getBooleanProperty("attachment.temp.cleanup", TRUE);
    // enable/disable recheck
    private static boolean RECHECK_ENABLED = PropertiesUtil.getBooleanProperty("attachment.recheck.enabled", TRUE);
    // the operating system name
    public static final String OS_NAME = System.getProperty("os.name");
    // properties for NFO handling
    private static final String[] NFO_EXTENSIONS = PropertiesUtil.getProperty("filename.nfo.extensions", "n").toLowerCase().split(",");
    // image tokens
    private static final String POSTER_TOKEN = PropertiesUtil.getProperty("attachment.token.poster", ".poster").toLowerCase();
    private static final String FANART_TOKEN = PropertiesUtil.getProperty("attachment.token.fanart", ".fanart").toLowerCase();
    private static final String BANNER_TOKEN = PropertiesUtil.getProperty("attachment.token.banner", ".banner").toLowerCase();
    private static final String VIDEOIMAGE_TOKEN = PropertiesUtil.getProperty("attachment.token.videoimage", ".videoimage").toLowerCase();
    // valid MIME types
    private static final Set<String> VALID_TEXT_MIME_TYPES = new HashSet<String>();
    private static final Map<String, String> VALID_IMAGE_MIME_TYPES = new HashMap<String, String>();

    static {
        if (IS_ENABLED) {
            File checkMkvInfo = findMkvInfo();
            File checkMkvExtract = findMkvExtract();

            if (OS_NAME.contains("Windows")) {
                if (MT_INFO_EXE.isEmpty()) {
                    MT_INFO_EXE.add("cmd.exe");
                    MT_INFO_EXE.add("/E:1900");
                    MT_INFO_EXE.add("/C");
                    MT_INFO_EXE.add(checkMkvInfo.getName());
                    MT_INFO_EXE.add("--ui-language");
                    if (StringUtils.isBlank(MT_LANGUAGE)) {
                        MT_INFO_EXE.add("en");
                    } else {
                        MT_INFO_EXE.add(MT_LANGUAGE);
                    }
                }
                if (MT_EXTRACT_EXE.isEmpty()) {
                    MT_EXTRACT_EXE.add("cmd.exe");
                    MT_EXTRACT_EXE.add("/E:1900");
                    MT_EXTRACT_EXE.add("/C");
                    MT_EXTRACT_EXE.add(checkMkvExtract.getName());
                }
            } else {
                if (MT_INFO_EXE.isEmpty()) {
                    MT_INFO_EXE.add("./" + checkMkvInfo.getName());
                    MT_INFO_EXE.add("--ui-language");
                    if (StringUtils.isBlank(MT_LANGUAGE)) {
                        MT_INFO_EXE.add("en_US");
                    } else {
                        MT_INFO_EXE.add(MT_LANGUAGE);
                    }
                }
                if (MT_EXTRACT_EXE.isEmpty()) {
                    MT_EXTRACT_EXE.add("./" + checkMkvExtract.getName());
                }
            }

            if (VALID_TEXT_MIME_TYPES.isEmpty()) {
                VALID_TEXT_MIME_TYPES.add("text/xml");
                VALID_TEXT_MIME_TYPES.add("application/xml");
                VALID_TEXT_MIME_TYPES.add("text/html");
            }

            if (VALID_IMAGE_MIME_TYPES.isEmpty()) {
                VALID_IMAGE_MIME_TYPES.put("image/jpeg", ".jpg");
                VALID_IMAGE_MIME_TYPES.put("image/png", ".png");
                VALID_IMAGE_MIME_TYPES.put("image/gif", ".gif");
                VALID_IMAGE_MIME_TYPES.put("image/x-ms-bmp", ".bmp");
            }

            if (!checkMkvInfo.canExecute()) {
                LOGGER.info(LOG_MESSAGE + "Couldn't find MKV toolnix executable tool 'mkvinfo'");
                IS_ACTIVATED = Boolean.FALSE;
            } else if (!checkMkvExtract.canExecute()) {
                LOGGER.info(LOG_MESSAGE + "Couldn't find MKV toolnix executable tool 'mkvextract'");
                IS_ACTIVATED = Boolean.FALSE;
            } else {
                LOGGER.info(LOG_MESSAGE + "MkvToolnix will be used to extract matroska attachments");
                IS_ACTIVATED = Boolean.TRUE;
            }

            if (IS_ACTIVATED) {
                // just create temporary directories if MkvToolnix is activated
                try {
                    String tempLocation = PropertiesUtil.getProperty("attachment.temp.directory", "");
                    if (StringUtils.isBlank(tempLocation)) {
                        tempLocation = StringTools.appendToPath(PropertiesUtil.getProperty("mjb.jukeboxTempDir", "./temp"), "attachments");
                    }

                    File tempFile = new File(FileTools.getCanonicalPath(tempLocation));
                    if (tempFile.exists()) {
                        TEMP_DIRECTORY = tempFile;
                    } else {
                        LOGGER.debug(LOG_MESSAGE + "Creating temporary attachment location: (" + tempLocation + ")");
                        boolean status = tempFile.mkdirs();
                        int i = 1;
                        while (!status && i++ <= 10) {
                            Thread.sleep(1000);
                            status = tempFile.mkdirs();
                        }

                        if (status && i > 10) {
                            LOGGER.error(LOG_MESSAGE + "Failed creating the temporary attachment directory: (" + tempLocation + ")");
                            // scanner will not be active without temporary directory
                            IS_ACTIVATED = Boolean.FALSE;
                        } else {
                            TEMP_DIRECTORY = tempFile;
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.error(LOG_MESSAGE + "Failed creating the temporary attachment directory: " + ex.getMessage());
                    // scanner will not be active without temporary directory
                    IS_ACTIVATED = Boolean.FALSE;
                }
            }
        } else {
            IS_ACTIVATED = Boolean.FALSE;
        }
    }

    protected AttachmentScanner() {
        throw new UnsupportedOperationException("AttachmentScanner is a utility class and cannot be instatiated");
    }

    /**
     * Checks if a file is scanable for attachments. Therefore the file must
     * exist and the extension must be equal to MKV.
     *
     * @param file the file to scan
     * @return true, if file is scanable, else false
     */
    private static boolean isFileScanable(File file) {
        if (file == null) {
            return Boolean.FALSE;
        } else if (!file.exists()) {
            return Boolean.FALSE;
        } else if (!"MKV".equalsIgnoreCase(FileTools.getFileExtension(file.getName()))) {
            // no matroska file
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * Scan the movie for attachments in each movie file.
     *
     * @param movie
     */
    public static void scan(Movie movie) {
        if (!IS_ACTIVATED) {
            return;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            // movie to scan must have file format
            return;
        }

        for (MovieFile movieFile : movie.getMovieFiles()) {
            if (isFileScanable(movieFile.getFile())) {
                scanAttachments(movieFile);
            }
        }
    }

    /**
     * Rescan the movie for attachments in each movie file.
     *
     * @param movie the movie which will be scanned
     * @param xmlFile the xmlFile to use for checking lastModificationDate
     * @return true, if movieFile is never than xmlFile and attachments have
     * been found, else false
     */
    public static boolean rescan(Movie movie, File xmlFile) {
        if (!IS_ACTIVATED) {
            return Boolean.FALSE;
        } else if (!RECHECK_ENABLED) {
            return Boolean.FALSE;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            // movie the scan must be a file
            return Boolean.FALSE;
        }

        // holds the return value
        boolean returnValue = Boolean.FALSE;

        // flag to indicate the first movie file
        boolean firstMovieFile = Boolean.TRUE;

        for (MovieFile movieFile : movie.getMovieFiles()) {
            if (isFileScanable(movieFile.getFile()) && FileTools.isNewer(movieFile.getFile(), xmlFile)) {
                // scan attachments
                scanAttachments(movieFile);

                // check attachments and determine changes
                for (Attachment attachment : movieFile.getAttachments()) {
                    if (ContentType.NFO == attachment.getContentType()) {
                        returnValue = Boolean.TRUE;
                        movie.setDirty(DirtyFlag.NFO);
                    } else if (ContentType.VIDEOIMAGE == attachment.getContentType()) {
                        returnValue = Boolean.TRUE;
                        // no need for dirty flag
                    } else if (firstMovieFile) {
                        // all other images are only relevant for first movie
                        if (ContentType.POSTER == attachment.getContentType()) {
                            returnValue = Boolean.TRUE;
                            movie.setDirty(DirtyFlag.POSTER);
                            // Set to unknown for not taking poster from Jukebox
                            movie.setPosterURL(Movie.UNKNOWN);
                        } else if (ContentType.FANART == attachment.getContentType()) {
                            returnValue = Boolean.TRUE;
                            movie.setDirty(DirtyFlag.FANART);
                            // Set to unknown for not taking fanart from Jukebox
                            movie.setFanartURL(Movie.UNKNOWN);
                        } else if (ContentType.BANNER == attachment.getContentType()) {
                            returnValue = Boolean.TRUE;
                            movie.setDirty(DirtyFlag.BANNER);
                            // Set to unknown for not taking banner from Jukebox
                            movie.setBannerURL(Movie.UNKNOWN);
                        } else if (ContentType.SET_POSTER == attachment.getContentType()) {
                            returnValue = Boolean.TRUE;
                            movie.setDirty(DirtyFlag.POSTER);
                        } else if (ContentType.SET_FANART == attachment.getContentType()) {
                            returnValue = Boolean.TRUE;
                            movie.setDirty(DirtyFlag.FANART);
                        } else if (ContentType.SET_BANNER == attachment.getContentType()) {
                            returnValue = Boolean.TRUE;
                            movie.setDirty(DirtyFlag.BANNER);
                        }
                    }
                }
            }
            
            // any other movie file will not be the first movie file
            firstMovieFile = Boolean.FALSE;
        }

        return returnValue;
    }

    /**
     * Scans a matroska movie file for attachments.
     *
     * @param movieFile the movie file to scan
     */
    private static void scanAttachments(MovieFile movieFile) {
        if (movieFile.isAttachmentsScanned()) {
            // attachments has been scanned during rescan of movie
            return;
        }

        // clear existing attachments
        movieFile.clearAttachments();

        // the file with possible attachments
        File scanFile = movieFile.getFile();

        LOGGER.debug(LOG_MESSAGE + "Scanning file " + scanFile.getName());
        int attachmentId = 0;
        try {
            // create the command line
            List<String> commandMkvInfo = new ArrayList<String>(MT_INFO_EXE);
            commandMkvInfo.add(scanFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(commandMkvInfo);

            // set up the working directory.
            pb.directory(MT_PATH);

            Process p = pb.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = localInputReadLine(input);
            while (line != null) {
                if (line.contains("+ Attached")) {
                    // increase the attachment id
                    attachmentId++;
                    // next line contains file name
                    String fileNameLine = localInputReadLine(input);
                    // next line contains MIME type
                    String mimeTypeLine = localInputReadLine(input);

                    Attachment attachment = createAttachment(attachmentId, fileNameLine, mimeTypeLine);
                    if (attachment != null) {
                        attachment.setSourceFile(movieFile.getFile());
                        movieFile.addAttachment(attachment);
                    }
                }

                line = localInputReadLine(input);
            }

            if (p.waitFor() != 0) {
                LOGGER.error(LOG_MESSAGE + "Error during attachment retrieval - ErrorCode=" + p.exitValue());
            }
        } catch (Exception error) {
            LOGGER.error(SystemTools.getStackTrace(error));
        }

        // attachments has been scanned; no double scan of attachments needed
        movieFile.setAttachmentsScanned(Boolean.TRUE);
    }

    private static String localInputReadLine(BufferedReader input) {
        String line = null;
        try {
            line = input.readLine();
            while ((line != null) && (line.equals(""))) {
                line = input.readLine();
            }
        } catch (IOException ignore) {
        }
        return line;
    }

    /**
     * Look for the mkvinfo filename and return it.
     *
     * @return
     */
    private static File findMkvInfo() {
        File mkvInfoFile;

        if (OS_NAME.contains("Windows")) {
            mkvInfoFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_INFO_FILENAME_WINDOWS));
        } else {
            mkvInfoFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_INFO_FILENAME_LINUX));
        }

        return mkvInfoFile;
    }

    /**
     * Look for the mkvextract filename and return it.
     *
     * @return
     */
    private static File findMkvExtract() {
        File mkvExtractFile;

        if (OS_NAME.contains("Windows")) {
            mkvExtractFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_EXTRACT_FILENAME_WINDOWS));
        } else {
            mkvExtractFile = new File(StringTools.appendToPath(MT_PATH.getAbsolutePath(), MT_EXTRACT_FILENAME_LINUX));
        }

        return mkvExtractFile;
    }

    /**
     * Creates an attachment.
     *
     * @param id
     * @param filename
     * @param mimetype
     * @return Attachment or null
     */
    private static Attachment createAttachment(int id, String filename, String mimetype) {
        String fixedFileName = null;
        if (filename.contains("File name:")) {
            fixedFileName = filename.substring(filename.indexOf("File name:") + 10).trim();
        }
        String fixedMimeType = null;
        if (mimetype.contains("Mime type:")) {
            fixedMimeType = mimetype.substring(mimetype.indexOf("Mime type:") + 10).trim();
        }

        ContentType type = determineContentType(fixedFileName, fixedMimeType);

        Attachment attachment = null;
        if (type == null) {
            LOGGER.debug(LOG_MESSAGE + "Failed to dertermine attachment type for '" + fixedFileName + "' (" + fixedMimeType + ")");
        } else {
            attachment = new Attachment();
            attachment.setType(AttachmentType.MATROSKA); // one and only type at the moment
            attachment.setAttachmentId(id);
            attachment.setContentType(type);
            attachment.setMimeType(fixedMimeType.toLowerCase());
            LOGGER.debug(LOG_MESSAGE + "Found attachment " + attachment);
        }
        return attachment;
    }

    /**
     * Determines the content type of the attachment by file name and mime type.
     *
     * @param inFileName
     * @param inMimeType
     * @return the content type, may be null if determination failed
     */
    private static ContentType determineContentType(String inFileName, String inMimeType) {
        if (inFileName == null) {
            return null;
        }
        if (inMimeType == null) {
            return null;
        }
        String fileName = inFileName.toLowerCase();
        String mimeType = inMimeType.toLowerCase();

        if (VALID_TEXT_MIME_TYPES.contains(mimeType)) {
            // NFO
            for (String extension : NFO_EXTENSIONS) {
                if (extension.equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
                    return ContentType.NFO;
                }
            }
        } else if (VALID_IMAGE_MIME_TYPES.containsKey(mimeType)) {
            String check = FilenameUtils.removeExtension(fileName);
            // check for SET image
            boolean isSetImage = Boolean.FALSE;
            if (check.endsWith(".set")) {
                isSetImage = Boolean.TRUE;
                // fix check to look for image type
                // just removing extension which is ".set" in this moment
                check = FilenameUtils.removeExtension(check);
            }

            if (check.endsWith(POSTER_TOKEN) || check.equals(POSTER_TOKEN.substring(1))) {
                if (isSetImage) {
                    // fileName = <any>.<posterToken>.set.<extension>
                    return ContentType.SET_POSTER;
                } else {
                    // fileName = <any>.<posterToken>.<extension>
                    return ContentType.POSTER;
                }
            }
            if (check.endsWith(FANART_TOKEN) || check.equals(FANART_TOKEN.substring(1))) {
                if (isSetImage) {
                    // fileName = <any>.<fanartToken>.set.<extension>
                    return ContentType.SET_FANART;
                } else {
                    // fileName = <any>.<fanartToken>.<extension>
                    return ContentType.FANART;
                }
            }
            if (check.endsWith(BANNER_TOKEN) || check.equals(BANNER_TOKEN.substring(1))) {
                if (isSetImage) {
                    // fileName = <any>.<bannerToken>.set.<extension>
                    return ContentType.SET_BANNER;
                } else {
                    // fileName = <any>.<bannerToken>.<extension>
                    return ContentType.BANNER;
                }
            }
            if (check.endsWith(VIDEOIMAGE_TOKEN) || check.equals(VIDEOIMAGE_TOKEN.substring(1))) {
                // fileName = <any>.<videoimageToken>.<extension>
                return ContentType.VIDEOIMAGE;
            }
        }

        // no content type determined
        return null;
    }

    public static void addAttachedNfo(Movie movie, List<File> nfoFiles) {
        if (!IS_ACTIVATED) {
            return;
        } else if (!nfoFiles.isEmpty()) {
            // only use attached NFO if there are no locale NFOs
            return;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            return;
        } else if (!isMovieWithAttachments(movie)) {
            // nothing to do if movie has no attachments
            return;
        }

        List<Attachment> attachments = findAttachments(movie, ContentType.NFO, 0);
        for (Attachment attachment : attachments) {
            File nfoFile = extractAttachment(attachment);
            if (nfoFile != null) {
                // extracted a valid NFO file
                LOGGER.debug(LOG_MESSAGE + "Extracted NFO file " + nfoFile.getAbsolutePath());
                // add to NFO file list
                nfoFiles.add(nfoFile);
            }
        }
    }

    private static boolean isMovieWithAttachments(Movie movie) {
        for (MovieFile movieFile : movie.getMovieFiles()) {
            if (movieFile.getAttachments().size() > 0) {
                return Boolean.TRUE;
            }
        }
        // movie has no attachments
        return Boolean.FALSE;
    }

    // ugly hack to determine if a set image is in progress
    private static boolean isSetImage(Movie movie, String imageFileName) {
        if (StringTools.isNotValidString(imageFileName)) {
            // must be valid
            return Boolean.FALSE;
        } else if (!imageFileName.startsWith(Library.INDEX_SET + "_")) {
            // must start with "Set_"
            return Boolean.FALSE;
        }

        if (movie.isTVShow()) {
            // use original title of TV show as set
            StringBuilder sb = new StringBuilder();
            sb.append(Library.INDEX_SET);
            sb.append("_");
            sb.append(FileTools.makeSafeFilename(movie.getOriginalTitle()));
            if (imageFileName.toUpperCase().startsWith(sb.toString().toUpperCase())) {
                return Boolean.TRUE;
            }
        }

        // process sets
        for (String setName : movie.getSets().keySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(Library.INDEX_SET);
            sb.append("_");
            sb.append(FileTools.makeSafeFilename(setName));
            if (imageFileName.toUpperCase().startsWith(sb.toString().toUpperCase())) {
                return Boolean.TRUE;
            }
        }

        return Boolean.FALSE;
    }

    public static File extractAttachedFanart(Movie movie) {
        if (!IS_ACTIVATED) {
            return null;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            return null;
        } else if (!isMovieWithAttachments(movie)) {
            // nothing to do if movie has no attachments
            return null;
        }

        // determine if set fanart should be used
        boolean useSetImage = isSetImage(movie, movie.getFanartFilename());

        List<Attachment> attachments;
        if (useSetImage) {
            // find set fanart attachments
            attachments = findAttachments(movie, ContentType.SET_FANART, 0);
            // add fanart attachments, so they could be used as set fanart
            attachments.addAll(findAttachments(movie, ContentType.FANART, 0));
        } else {
            // find fanart attachments
            attachments = findAttachments(movie, ContentType.FANART, 0);
        }

        // extract image and return image file (may be null)
        return extractImage(attachments);
    }

    public static File extractAttachedPoster(Movie movie) {
        if (!IS_ACTIVATED) {
            return null;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            return null;
        } else if (!isMovieWithAttachments(movie)) {
            // nothing to do if movie has no attachments
            return null;
        }

        // determine if set poster should be used
        boolean useSetImage = isSetImage(movie, movie.getPosterFilename());

        List<Attachment> attachments;
        if (useSetImage) {
            // find set poster attachments
            attachments = findAttachments(movie, ContentType.POSTER, 0);
            // add poster attachments, so they could be used as set poster
            attachments.addAll(findAttachments(movie, ContentType.POSTER, 0));
        } else {
            // find poster attachments
            attachments = findAttachments(movie, ContentType.POSTER, 0);
        }

        // extract image and return image file (may be null)
        return extractImage(attachments);
    }

    public static File extractAttachedBanner(Movie movie) {
        if (!IS_ACTIVATED) {
            return null;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            return null;
        } else if (!isMovieWithAttachments(movie)) {
            // nothing to do if movie has no attachments
            return null;
        }

        // determine if set banner should be used
        boolean useSetImage = isSetImage(movie, movie.getBannerFilename());
        
        List<Attachment> attachments;
        if (useSetImage) {
            // find set banner attachments
            attachments = findAttachments(movie, ContentType.SET_BANNER, 0);
            // add banner attachments, so they could be used as set banner
            attachments.addAll(findAttachments(movie, ContentType.BANNER, 0));
        } else {
            // find banner attachments
            attachments = findAttachments(movie, ContentType.BANNER, 0);
        }

        // extract image and return image file (may be null)
        return extractImage(attachments);
    }

    public static File extractAttachedVideoimage(Movie movie, int part) {
        if (!IS_ACTIVATED) {
            return null;
        } else if (!Movie.TYPE_FILE.equalsIgnoreCase(movie.getFormatType())) {
            return null;
        } else if (!isMovieWithAttachments(movie)) {
            // nothing to do if movie has no attachments
            return null;
        }

        // find banner attachments
        List<Attachment> attachments = findAttachments(movie, ContentType.VIDEOIMAGE, part);

        // extract image and return image file (may be null)
        return extractImage(attachments);
    }

    private static File extractImage(Collection<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            File attachmentFile = extractAttachment(attachment);
            if (attachmentFile != null) {
                LOGGER.debug(LOG_MESSAGE + "Extracted image (" + attachment + ")");
                return attachmentFile;
            }
        }
        // attachments empty or no image file extracted
        return null;
    }

    /**
     * Find attachments for a movie.
     * 
     * @param movie
     *            the movie where attachments should be searched for
     * @param contentType
     *            the content type to use for searching attachments
     * @param part
     *            -1, for all parts (search in attachments of all movie files) 0, just for first part (search in attachments of first movie file) >0, for
     *            explicit part (search in attachments of movie file <part>)
     * @return
     */
    private static List<Attachment> findAttachments(Movie movie, ContentType contentType, int part) {
        Collection<MovieFile> searchMovieFiles = new ArrayList<MovieFile>();

        if (part < 0) {
            // search in all movie files
            searchMovieFiles = movie.getMovieFiles();
        } else {
            Iterator<MovieFile> it = movie.getMovieFiles().iterator();
            while (it.hasNext()) {
                if (part == 0) {
                    // use first movie file
                    searchMovieFiles.add(it.next());
                    break;
                } else {
                    MovieFile mv = it.next();
                    if (mv.getFirstPart() == part) {
                        // just use movie file with matches requested part
                        searchMovieFiles.add(mv);
                        break;
                    }
                }
            }
        }

        List<Attachment> attachments = new ArrayList<Attachment>();
        if (!searchMovieFiles.isEmpty()) {
            for (MovieFile movieFile : searchMovieFiles) {
                for (Attachment attachment : movieFile.getAttachments()) {
                    if (contentType.compareTo(attachment.getContentType()) == 0) {
                        attachments.add(attachment);
                    }
                }
            }
        }
        return attachments;
    }

    /**
     * Extract an attachment
     * 
     * @param attachment
     *            the attachment to extract
     * @param setImage
     *            true, if a set image should be extracted; in this case ".set" is append before file extension
     * @param counter
     *            a counter (only used for NFOs cause there may be multiple NFOs in one file)
     * @return
     */
    private static final File extractAttachment(Attachment attachment) {
        File sourceFile = attachment.getSourceFile();
        if (sourceFile == null) {
            // source file must exist
            return null;
        } else if (!sourceFile.exists()) {
            // source file must exist
            return null;
        }

        // build return file name
        StringBuffer returnFileName = new StringBuffer();
        returnFileName.append(TEMP_DIRECTORY.getAbsolutePath());
        returnFileName.append(File.separatorChar);
        returnFileName.append(FilenameUtils.removeExtension(sourceFile.getName()));
        // add attachment id so the extracted file becomes unique per movie file
        returnFileName.append(".");
        returnFileName.append(attachment.getAttachmentId());

        switch (attachment.getContentType()) {
            case NFO:
                returnFileName.append(".nfo");
                break;
            case POSTER:
            case FANART:
            case BANNER:
            case SET_POSTER:
            case SET_FANART:
            case SET_BANNER:
                returnFileName.append(VALID_IMAGE_MIME_TYPES.get(attachment.getMimeType()));
                break;
            case VIDEOIMAGE:
                // TODO video images files must regard files with multiple episodes
                returnFileName.append(VIDEOIMAGE_TOKEN);
                returnFileName.append(VALID_IMAGE_MIME_TYPES.get(attachment.getMimeType()));
                break;
        }

        File returnFile = new File(returnFileName.toString());
        if (returnFile.exists()) {
            // already present or extracted
            return returnFile;
        }

        //LOGGER.debug(LOG_MESSAGE + "Extract attachement (" + attachment + ")");

        try {
            // Create the command line
            List<String> commandMedia = new ArrayList<String>(MT_EXTRACT_EXE);
            commandMedia.add("attachments");
            commandMedia.add(sourceFile.getAbsolutePath());
            commandMedia.add(attachment.getAttachmentId() + ":" + returnFileName.toString());

            ProcessBuilder pb = new ProcessBuilder(commandMedia);
            pb.directory(MT_PATH);
            Process p = pb.start();

            if (p.waitFor() != 0) {
                LOGGER.error(LOG_MESSAGE + "Error during extraction - ErrorCode=" + p.exitValue());
                returnFile = null;
            }
        } catch (Exception ex) {
            LOGGER.error(SystemTools.getStackTrace(ex));
            returnFile = null;
        }

        if (returnFile != null) {
            if (returnFile.exists()) {
                // need to reset last modification date to last modification date
                // of source file to fulfill later checks
                try {
                    returnFile.setLastModified(sourceFile.lastModified());
                } catch (Exception ignore) {
                    // nothing to do anymore
                }
            } else {
                // reset return file to null if not existent
                returnFile = null;
            }
        }
        return returnFile;
    }

    /**
     * Clean up the temporary directory for attachments
     */
    public static void cleanUp() {
        if (IS_ACTIVATED && TEMP_CLEANUP && (TEMP_DIRECTORY != null) && TEMP_DIRECTORY.exists()) {
            FileTools.deleteDir(TEMP_DIRECTORY);
        }
    }
}
