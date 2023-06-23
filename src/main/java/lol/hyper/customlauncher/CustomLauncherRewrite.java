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
import lol.hyper.customlauncher.windows.MainWindow;
import lol.hyper.customlauncher.ttrupdater.TTRUpdater;
import lol.hyper.customlauncher.updater.UpdateChecker;
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

    public static String version;
    public static Logger logger;
    public static Image icon;
    public static String userAgent;

    public static void main(String[] args) throws IOException {
        // load the log4j2config
        System.setProperty("log4j.configurationFile", "log4j2config.xml");
        // load the version
        final Properties properties = new Properties();
        properties.load(CustomLauncherRewrite.class.getClassLoader().getResourceAsStream("project.properties"));
        version = properties.getProperty("version");
        // log some basic info
        logger = LogManager.getLogger(CustomLauncherRewrite.class);
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
            icon = ImageIO.read(iconStream);
        }

        // this is used for removing old versions on Windows
        // passing "--remove-old <version>" will delete that version's exe
        // mainly for cleanup so there aren't 100 exes in the folder
        if (args.length >= 1) {
            String arg1 = args[0];
            if (arg1.equalsIgnoreCase("--remove-old")) {
                String oldVersion = args[1];
                Files.delete(new File("CustomLauncherRewrite-" + oldVersion + ".exe").toPath());
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