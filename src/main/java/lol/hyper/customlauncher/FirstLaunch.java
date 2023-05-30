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

import lol.hyper.customlauncher.tools.ErrorWindow;
import lol.hyper.customlauncher.tools.InfoWindow;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class FirstLaunch {
    private final Logger logger = LogManager.getLogger(this);

    public FirstLaunch() {
        InfoWindow infoWindow = new InfoWindow("Welcome to CustomLauncherRewrite! I am first going to detect for an existing TTR install.\nI will copy screenshots, settings, and resource packs.");
        infoWindow.dispose();
        if (SystemUtils.IS_OS_LINUX) {
            copyLinuxInstall();
        } else if (SystemUtils.IS_OS_WINDOWS) {
            copyWindowsInstall();
        }
    }

    private void copyWindowsInstall() {
        File windowsInstall = null;
        File[] roots = File.listRoots();
        boolean foundInstall = false;
        for (File root : roots) {
            String WINDOWS_INSTALL = "Program Files (x86)\\Toontown Rewritten";
            windowsInstall = new File(root, WINDOWS_INSTALL);
            if (windowsInstall.exists()) {
                foundInstall = true;
                break;
            }
        }
        // can't find install folder
        if (!foundInstall) {
            InfoWindow infoWindow = new InfoWindow("I am unable to find your TTR install location. You'll have to manually copy things over that you wish to keep.");
            infoWindow.dispose();
            return;
        }
        logger.info("Found existing TTR install at " + windowsInstall);
        copyFiles(windowsInstall);
    }

    private void copyLinuxInstall() {
        String LINUX_INSTALL = "/.var/app/com.toontownrewritten.Launcher/data/";
        File linuxInstall = new File(System.getProperty("user.home") + LINUX_INSTALL);
        if (!linuxInstall.exists()) {
            InfoWindow infoWindow = new InfoWindow("I am unable to find your TTR install location. You'll have to manually copy things over that you wish to keep.");
            infoWindow.dispose();
            return;
        }
        logger.info("Found existing TTR install at " + linuxInstall);
        copyFiles(linuxInstall);
    }

    private void copyFiles(File source) {
        // we found the installation, copy files over
        File settings = new File(source, "settings.json");
        File resourcePacks = new File(source, "resources");
        File screenshots = new File(source, "screenshots");
        File newInstall = new File("ttr-files");
        try {
            FileUtils.copyFileToDirectory(settings, newInstall);
            FileUtils.copyFileToDirectory(resourcePacks, newInstall);
            FileUtils.copyFileToDirectory(screenshots, newInstall);
        } catch (IOException exception) {
            logger.error("Unable to copy TTR files!", exception);
            ErrorWindow errorWindow = new ErrorWindow(exception);
            errorWindow.dispose();
        }
    }
}
