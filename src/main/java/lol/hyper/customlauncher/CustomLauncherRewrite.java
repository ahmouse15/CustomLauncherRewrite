/*
 * This file is part of CustomLauncherRewrite.
 *
 * CustomLauncherRewrite is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CustomLauncherRewrite is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CustomLauncherRewrite.  If not, see <https://www.gnu.org/licenses/>.
 */

package lol.hyper.customlauncher;

import lol.hyper.customlauncher.changelog.GameUpdateTracker;
import lol.hyper.customlauncher.tools.ExceptionWindow;
import lol.hyper.customlauncher.tools.PopUpWindow;
import lol.hyper.customlauncher.windows.MainWindow;
import lol.hyper.customlauncher.ttrupdater.TTRUpdater;
import lol.hyper.customlauncher.updater.UpdateChecker;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Properties;

public class CustomLauncherRewrite {

    /**
     * Stores the version, loaded from 'project.properties.'
     */
    public static String version;
    /**
     * The main logger.
     */
    public static Logger logger;
    /**
     * The program's icon.
     */
    public static Image icon;
    /**
     * The user agent used for requests.
     * This is set here since it includes the version.
     */
    public static String userAgent;
    /**
     * The operating system the user is using.
     */
    public static String OS = null;

    /**
     * The entry point for the program.
     *
     * @param args Only used for updating the program. Passing "--remove-old VERSION" will simply remove that old version.
     */
    public static void main(String[] args) {
        if (SystemUtils.IS_OS_WINDOWS) {
            OS = "win" + System.getProperty("sun.arch.data.model");
        }
        if (SystemUtils.IS_OS_LINUX) {
            OS = "linux";
        }
        // load the log4j2config
        System.setProperty("log4j.configurationFile", "log4j2config.xml");
        // load the version
        final Properties properties = new Properties();
        try {
            properties.load(CustomLauncherRewrite.class.getClassLoader().getResourceAsStream("project.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        version = properties.getProperty("version");
        // log some basic info
        logger = LogManager.getLogger(CustomLauncherRewrite.class);

        if (OS == null) {
            new PopUpWindow(null, "Unable to determine operating system!");
            logger.error("Unable to determine operating system! Are we running something not supported?");
            logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("sun.arch.data.model") + "bit");
            logger.info("Arch: " + System.getProperty("os.arch"));
            logger.info("Java: " + System.getProperty("java.vm.version") + " (" + System.getProperty("java.vendor") + ")");
            System.exit(1);
        }

        logger.info("OS: " + System.getProperty("os.name") + " " + System.getProperty("sun.arch.data.model") + "bit");
        logger.info("Arch: " + System.getProperty("os.arch"));
        logger.info("Java: " + System.getProperty("java.vm.version") + " (" + System.getProperty("java.vendor") + ")");
        logger.info("Program is starting.");
        logger.info("Running version " + version);
        logger.info("Current directory " + System.getProperty("user.dir"));

        userAgent = "CustomLauncherRewrite https://github.com/hyperdefined/CustomLauncherRewrite " + version;

        // set the config
        ConfigHandler configHandler = new ConfigHandler();

        // load the icon
        InputStream iconStream = CustomLauncherRewrite.class.getResourceAsStream("/icon.png");
        if (iconStream != null) {
            try {
                icon = ImageIO.read(iconStream);
            } catch (IOException exception) {
                logger.error("Unable to load icon!");
                new ExceptionWindow(exception);
            }
        }

        // this is used for removing old versions on Windows
        // passing "--remove-old <version>" will delete that version's exe
        // mainly for cleanup so there aren't 100 exes in the folder
        if (args.length >= 1) {
            String arg1 = args[0];
            if (arg1.equalsIgnoreCase("--remove-old")) {
                String oldVersion = args[1];
                try {
                    Files.delete(new File("CustomLauncherRewrite-" + oldVersion + ".exe").toPath());
                } catch (IOException exception) {
                    logger.error("Unable to delete old version " + oldVersion);
                    new ExceptionWindow(exception);
                }
                logger.info("Deleting old version " + oldVersion);
            }
        }

        // check for updates
        new UpdateChecker(version);

        // load ttr game updates
        GameUpdateTracker gameUpdateTracker = new GameUpdateTracker();

        // run the TTR updater
        TTRUpdater ttrUpdater = new TTRUpdater();
        ttrUpdater.setVisible(true);
        ttrUpdater.checkUpdates();

        // run the main window
        SwingUtilities.invokeLater(() -> {
            MainWindow frame = new MainWindow(configHandler, gameUpdateTracker);
            frame.setVisible(true);
        });
    }
}
