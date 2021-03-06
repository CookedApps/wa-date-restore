package com.cookedapps.warestore;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Application wide command line options.
 * Created by sandro on 05.01.2019
 */
class CliOptions {

    static final String HELP_SYNTAX = "wa-date-restore";
    static final String HELP_HEADER = "Restore the original date of WhatsApp images by extracting the date from the " +
            "file name. The processed images will be saved into a new folder called \"" + DateRestore.OUTPUT_DIR +
            "\" inside the input directory.\n\n";
    static final String HELP_FOOTER = "\nPlease report issues at https://github.com/CookedApps/wa-date-restore\n\n";

    private static Options options = initOptions();

    private static Options initOptions() {
        Options o = new Options();

        o.addOption(help());
        o.addOption(directory());
        o.addOption(exifDate());
        o.addOption(lastModified());

        return o;
    }

    static Options getOptions() {
        return options;
    }

    static Option help() {
        return Option
                .builder("h")
                .desc("Print this help message.")
                .longOpt("help")
                .build();
    }

    static Option directory() {
        return Option
                .builder("d")
                .desc("Full path to the directory in which all files should be processed.")
                .longOpt("directory")
                .argName("path")
                .hasArg()
                .build();
    }

    static Option lastModified() {
        return Option
                .builder("l")
                .desc("Overwrite the \"Last Modified Date\" with the extracted date.")
                .longOpt("lastModifiedDate")
                .build();
    }

    static Option exifDate() {
        return Option
                .builder("e")
                .desc("Overwrite the EXIF \"DateTimeOriginal\" and \"DateTimeDigitized\" tag with the extracted date." +
                        " (This is what you normally want to do.)")
                .longOpt("exifDate")
                .build();
    }

}
