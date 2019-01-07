package com.cookedapps.warestore;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for extracting and setting the original date of JPG files in the provided directory.
 * Created by sandro on 05.01.2019
 */
class DateRestore {

    static final String OUTPUT_DIR = "wa_date_restored";

    private static final String ERROR_PATH_PREFIX = "Invalid path provided: ";
    private static final String DATE_PATTERN_FILENAME = "yyyyMMdd";
    private static final SimpleDateFormat DATE_FORMAT_PRINT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT_EXIF = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT_FILENAME = new SimpleDateFormat(DATE_PATTERN_FILENAME);

    private String directoryPath;

    void restoreDates(String directoryPath, boolean lastModifiedDate, boolean exifDateTimeOriginal) {
        this.directoryPath = directoryPath;
        Path directory = Paths.get(directoryPath);

        if(Files.exists(directory)) {
            if(Files.isDirectory(directory)) {
                getDirectoryStream(directory).ifPresent(paths -> paths.forEach(path -> {
                    if(!Files.isDirectory(path)) {
                        extractAndWriteDate(path, lastModifiedDate, exifDateTimeOriginal);
                    }
                }));
            } else {
                printDirectoryError("Not a directory!");
            }
        } else {
            printDirectoryError("Directory \"" + directoryPath + "\" not found!");
        }
    }

    private Optional<DirectoryStream<Path>> getDirectoryStream(Path directory) {
        try {
            return Optional.of(Files.newDirectoryStream(directory));
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private void printDirectoryError(String msg) {
        System.err.println(ERROR_PATH_PREFIX + msg);
    }

    private void extractAndWriteDate(Path source, boolean lastModifiedDate, boolean exifDate) {
        String fileName = source.getFileName().toString();

        getDateStringFromFileName(fileName)
                .ifPresent(dateString -> getDate(source, dateString)
                        .ifPresent(date -> processFile(source, lastModifiedDate, exifDate, date)));
    }

    private Optional<String> getDateStringFromFileName(String fileName) {
        Pattern pattern = Pattern.compile("((?<=IMG-)[0-9]*(?=-WA.+))");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            String dateString = matcher.group(1);
            return Optional.of(dateString);
        } else {
            printInlineError("Could not find date in file name: " + fileName);
            return Optional.empty();
        }
    }

    private void printInlineError(String message) {
        System.err.print(" Error: " + message + "\n");
    }

    private Optional<Date> getDate(Path source, String dateString) {
        Optional<Date> lastModDate = getDateFromLastMod(source, dateString);
        if(lastModDate.isPresent()) {
            return lastModDate;
        } else {
            return getDateFromString(dateString);
        }
    }

    private Optional<Date> getDateFromLastMod(Path source, String dateString) {
        Optional<FileTime> lastModTime = getLastModTime(source);
        if(lastModTime.isPresent()) {
            Date lastModDate = Date.from(lastModTime.get().toInstant());
            String lastModDateString = DATE_FORMAT_FILENAME.format(lastModDate);

            if(lastModDateString.equals(dateString)) {
                return Optional.of(lastModDate);
            }
        }
        return Optional.empty();
    }

    private Optional<FileTime> getLastModTime(Path source) {
        try {
            return Optional.of(Files.getLastModifiedTime(source, LinkOption.NOFOLLOW_LINKS));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Optional<Date> getDateFromString(String dateString) {
        try {
            Date date = DATE_FORMAT_FILENAME.parse(dateString);
            date = add12HoursToDate(date);
            return Optional.of(date);
        } catch (ParseException e) {
            printInlineError("Could not parse date " + dateString + " - Not matching " + DATE_PATTERN_FILENAME);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Date add12HoursToDate(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR_OF_DAY, 12);
        return cal.getTime();
    }

    private void processFile(Path source, boolean lastModifiedDate, boolean exifDate, Date date) {
        System.out.print("Processing " + source.getFileName() + ":");
        if(!lastModifiedDate && !exifDate) {
            System.out.print(" Nothing to process");
        } else {
            Path dest = createDestinationFile(source);
            if(exifDate) {
                overwriteExifDate(source, dest, date);
            }
            if(lastModifiedDate) {
                overwriteLastModifiedDate(dest, date);
            }
        }
        System.out.println();
    }

    private Path createDestinationFile(Path source) {
        File dest = getOrCreateFile(source);
        copySourceFileToDest(source, dest);
        return dest.toPath();
    }

    private File getOrCreateFile(Path source) {
        String outputPath = getOutputDirPath();
        File dir = new File(outputPath);
        if (!dir.exists()) dir.mkdirs();
        return new File(outputPath + File.separatorChar + source.getFileName().toString());
    }

    private void copySourceFileToDest(Path source, File dest) {
        try(OutputStream os = new FileOutputStream(dest)) {
            Files.copy(source, os);
        } catch (IOException e) {
            printInlineError("Could not copy content of original file to output file!");
            e.printStackTrace();
        }
    }

    private String getOutputDirPath() {
        if(directoryPath == null || directoryPath.isEmpty()) return null;
        return directoryPath + File.separatorChar + OUTPUT_DIR + File.separatorChar;
    }

    // See https://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html
    private void overwriteExifDate(Path source, Path dest, Date date) {
        try {
            System.out.print(" [EXIF Date -> " + beautifyDate(date) + "]");
            changeExifTime(source.toFile(), dest.toFile(), DATE_FORMAT_EXIF.format(date));
        } catch (Exception e) {
            printInlineError("Could not overwrite EXIF DateTimeOriginal and DateTimeDigitized");
            e.printStackTrace();
        }
    }

    private void changeExifTime(final File source, final File dest, String dateString) throws IOException, ImageReadException, ImageWriteException {
        try (FileOutputStream fos = new FileOutputStream(dest); OutputStream os = new BufferedOutputStream(fos)) {
            TiffOutputSet outputSet = getOutputSet(source);
            setDateExifTimeOriginalAndDigitized(outputSet, dateString);
            new ExifRewriter().updateExifMetadataLossless(source, os, outputSet);
        }
    }

    private TiffOutputSet getOutputSet(File jpegImageFile) throws ImageReadException, IOException, ImageWriteException {
        TiffOutputSet outputSet = null;
        final ImageMetadata metadata = Imaging.getMetadata(jpegImageFile);
        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        if (jpegMetadata != null) {
            final TiffImageMetadata exif = jpegMetadata.getExif();
            if (null != exif) {
                outputSet = exif.getOutputSet();
            }
        }
        if (outputSet == null) {
            outputSet = new TiffOutputSet();
        }
        return outputSet;
    }

    private void setDateExifTimeOriginalAndDigitized(TiffOutputSet outputSet, String dateString) throws ImageWriteException {
        final TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();
        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, dateString);

        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
        exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, dateString);
    }

    private void overwriteLastModifiedDate(Path dest, Date date) {
        try {
            System.out.print(" [LastModifiedTime -> " + beautifyDate(date) + "]");
            FileTime newFileTime = FileTime.from(date.toInstant());
            Files.setLastModifiedTime(dest, newFileTime);
        } catch (IOException e) {
            printInlineError("Could not overwrite LastModifiedTime");
            e.printStackTrace();
        }
    }

    private String beautifyDate(Date date) {
        return DATE_FORMAT_PRINT.format(date);
    }

}
