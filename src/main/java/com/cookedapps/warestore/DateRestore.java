package com.cookedapps.warestore;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for extracting and setting the original date of JPG files in the provided directory.
 * Created by sandro on 05.01.2019
 */
class DateRestore {

    private static final String ERROR_PATH_PREFIX = "Invalid path provided: ";
    private static final SimpleDateFormat DATE_FORMAT_PRINT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");

    void restoreDates(String directoryPath, boolean lastModifiedDate, boolean exifDateTimeOriginal) {
        Path directory = Paths.get(directoryPath);

        if(Files.exists(directory)) {
            if(Files.isDirectory(directory)) {
                getDirectoryStream(directory).ifPresent(paths -> paths.forEach(path -> {
                    if(!Files.isDirectory(path)) {
                        process(path, lastModifiedDate, exifDateTimeOriginal);
                    }
                }));
            } else {
                printDirectoryError("Not a directory!");
            }
        } else {
            printDirectoryError("Directory not found!");
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

    private void process(Path path, boolean overwriteLastModifiedDate, boolean exifDateTimeOriginal) {
        String fileName = path.getFileName().toString();

        getDateStringFromFileName(fileName).ifPresent(dateString -> getDateFromString(dateString).ifPresent(date -> {
            System.out.print("Processing " + path.getFileName() + ":");
            if(!overwriteLastModifiedDate && !exifDateTimeOriginal) {
                System.out.print(" Nothing to process");
            } else {
                if(overwriteLastModifiedDate) {
                    overwriteLastModifiedDate(path, date);
                }
                if(exifDateTimeOriginal) {
                    overwriteExifDateTimeOriginal(path, date);
                }
            }
            System.out.println();
        }));
    }

    // TODO: Support video and gif files by using different regex
    private Optional<String> getDateStringFromFileName(String fileName) {
        Pattern pattern = Pattern.compile("((?<=IMG-)[0-9]*(?=-WA.+))");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            String dateString = matcher.group(1);
            return Optional.of(dateString);
        } else {
            System.err.println("Could not find date in file name: " + fileName);
            return Optional.empty();
        }
    }

    private Optional<Date> getDateFromString(String dateString) {
        String datePattern = "yyyyMMdd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
        try {
            return Optional.of(simpleDateFormat.parse(dateString));
        } catch (ParseException e) {
            System.err.println("Could not parse date " + dateString + " - Not matching " + datePattern);
            return Optional.empty();
        }
    }

    private void overwriteLastModifiedDate(Path path, Date date) {
        try {
            System.out.print(" [LastModifiedTime -> " + beautifyDate(date) + "]");
            FileTime newFileTime = FileTime.from(date.toInstant());
            Files.setLastModifiedTime(path, newFileTime);
        } catch (IOException e) {
            System.err.println("Could not overwrite LastModifiedTime of " + path.getFileName());
        }
    }

    // FIXME: Does not work yet
    // See https://www.awaresystems.be/imaging/tiff/tifftags/privateifd/exif.html
    private void overwriteExifDateTimeOriginal(Path path, Date date) {
        try {
            System.out.print(" [EXIF DateTimeOriginal ->" + beautifyDate(date) + "]");
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            directory.setDate(36867, date);
        } catch (ImageProcessingException | IOException e) {
            System.err.println("Could not overwrite EXIF DateTimeOriginal of " + path.getFileName());
        }
    }

    private String beautifyDate(Date date) {
        return DATE_FORMAT_PRINT.format(date);
    }

}
