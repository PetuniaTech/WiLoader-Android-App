/*
 * Copyright (C) 2017 PetuniaTech LLC.
 *
 * This work is licensed under the Creative Commons Attribution-ShareAlike 4.0 International License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/4.0/
 * or send a letter to Creative Commons, PO Box 1866, Mountain View, CA 94042, USA.
 */

package com.petuniatech.wiloader.tools;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by PetuniaTech on 2017-06-24.
 */

public class LogController {

    private static Logger logger;
    private static FileHandler fileHandler;
    private static Handler guiLogHandler;

    public LogController() {
        //Make this class a singleton
        if (logger != null) {
            return;
        }

        try {
            logger = Logger.getLogger("LogController");
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
        } catch (Exception ex) {
            System.out.println("LogController: Get Logger Error!");
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public static boolean enableLogFile(boolean enable) {

        if (fileHandler != null) {
            try {
                fileHandler.close();
            } catch (Exception ex) {
                return false;
            }
        }

        try {
            logger.removeHandler(fileHandler);
        } catch (Exception ex) {
            return false;
        }

        if (enable) {

            // save log file to ../etc
            String path = LogController.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            path = (new File(path)).getParentFile().getParentFile().getPath() + File.separator + "etc" + File.separator;

            String decodedPath = path;
            try {
                decodedPath = URLDecoder.decode(path, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }

            //Create the log file
            try {
                fileHandler = new FileHandler(decodedPath + "wiloader-log.%u.%g.txt", 20 * 1024 * 1024, 10);
            } catch (Exception ex) {
                return false;
            }

            try {
                fileHandler.setFormatter(new LogFormatter());
                logger.addHandler(fileHandler);
            } catch (Exception ex) {
                return false;
            }

        }

        return true;
    }


}
