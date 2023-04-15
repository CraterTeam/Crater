package dev.crater;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class Main {
    private final static Logger logger = LogManager.getLogger("Main");
    private final static String Version = "0.0.1";
    public static Crater INSTANCE;
    public static void main(String[] args) {
        args = new String[]{"-c", "./cfg.yml"};
        //ToDo:Graphic UI
        logger.info("Crater obfuscator by CraterTeam");
        logger.info(Version);
        Options options = new Options();
        options.addOption("c","config",true,"Config file");
        options.addOption("h","help",false,"Help");
        CommandLine commandLine = null;
        CommandLineParser parser = new PosixParser();
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);
        try {
            commandLine = parser.parse(options, args);
            if (commandLine.hasOption("help")) {
                hf.printHelp("Crater", options, true);
                return;
            }
            if (commandLine.hasOption("config")) {
                try{
                    INSTANCE = new Crater(new File(commandLine.getOptionValue("config")));
                    INSTANCE.saveJar();
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }
                return;
            }
        }catch(Exception e){
        }
        logger.error("Arguments error");
        hf.printHelp("Crater", options, true);
    }
}