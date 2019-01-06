package com.cookedapps.warestore;

import org.apache.commons.cli.*;

import java.util.Optional;

/**
 * Main class for launching the application and starting the processing.
 * Created by sandro on 05.01.2019
 */
public class Main {

    public static void main(String[] args) {
        new Main(args);
    }

    private Main(String[] args) {
        parseArgs(args);
    }

    private void parseArgs(String[] args) {
        getCommandLine(args, CliOptions.getOptions()).ifPresent(this::checkOptions);
    }

    private Optional<CommandLine> getCommandLine(String[] args, Options options) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(options, args);
            return Optional.of(line);
        }
        catch(ParseException e) {
            System.err.println("Invalid command! " + e.getMessage());
            return Optional.empty();
        }
    }

    private void checkOptions(CommandLine cl) {
        if(cl.getOptions().length == 0 || hasOption(cl, CliOptions.help())) {
            printHelp();
        } else if(hasOption(cl, CliOptions.directory())) {
            String directoryPath = cl.getOptionValue(CliOptions.directory().getOpt());

            DateRestore dr = new DateRestore();
            dr.restoreDates(directoryPath, hasOption(cl, CliOptions.lastModified()), hasOption(cl, CliOptions.exifDate()));
        } else {
            System.err.println("You need to specify the input directory!");
            printHelp();
        }
    }

    private boolean hasOption(CommandLine cl, Option option) {
        return cl.hasOption(option.getOpt()) || cl.hasOption(option.getLongOpt());
    }

    private void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(CliOptions.HELP_SYNTAX, CliOptions.HELP_HEADER, CliOptions.getOptions(), CliOptions.HELP_FOOTER, true);
    }

}
