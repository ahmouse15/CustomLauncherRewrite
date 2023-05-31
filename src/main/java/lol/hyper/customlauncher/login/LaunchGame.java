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

package lol.hyper.customlauncher.login;

import lol.hyper.customlauncher.ConfigHandler;
import lol.hyper.customlauncher.tools.ErrorWindow;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.IOException;
import java.util.Map;
import java.io.File;

public final class LaunchGame extends Thread {

    public final Logger logger = LogManager.getLogger(this);
    final String cookie;
    final String gameServer;

    public LaunchGame(String cookie, String gameServer) {
        this.cookie = cookie;
        this.gameServer = gameServer;
    }

    public void run() {
        ProcessBuilder pb = new ProcessBuilder();

        String[] launchCommand = null;

        if (SystemUtils.IS_OS_WINDOWS) {
            if (System.getProperty("sun.arch.data.model").equalsIgnoreCase("64")) {
                launchCommand = new String[]{"cmd", "/c", "TTREngine64.exe"};
            } else {
                launchCommand = new String[]{"cmd", "/c", "TTREngine.exe"};
            }
            logger.info("Launching game from " + ConfigHandler.INSTALL_LOCATION);
        }
        if (SystemUtils.IS_OS_LINUX) {
            launchCommand = new String[]{"./TTREngine"};

            // Make sure it's executable before running
            boolean result;
            File fullPath = new File(ConfigHandler.INSTALL_LOCATION, "TTREngine");
            try {
                result = fullPath.setExecutable(true);
            } catch (SecurityException exception) {
                logger.error("Unable to set " + fullPath.getAbsolutePath() + " as an executable!", exception);
                new ErrorWindow(exception);
                return;
            }

            if (!result) {
                logger.error("Unable to set " + fullPath.getAbsolutePath() + " as an executable!");
                new ErrorWindow("Unable to set " + fullPath.getAbsolutePath() + " as an executable!\nMake sure this file is executable!");
                return;
            }
        }

        if (launchCommand == null) {
            logger.error("Unable to determine operating system!");
            new ErrorWindow("Unable to determine operating system!");
            return;
        }

        // dirty little trick to redirect the output
        // the game freezes if you don't do this
        // https://stackoverflow.com/a/58922302
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        pb.redirectErrorStream(true);
        pb.directory(ConfigHandler.INSTALL_LOCATION);
        pb.command(launchCommand);

        Map<String, String> env = pb.environment();
        env.put("TTR_GAMESERVER", this.gameServer);
        env.put("TTR_PLAYCOOKIE", this.cookie);

        Thread t1 =
                new Thread(
                        () -> {
                            try {
                                Process process = pb.start();
                                process.getInputStream().close();
                                process.waitFor();
                            } catch (IOException | InterruptedException exception) {
                                logger.error("Unable to launch game!", exception);
                                new ErrorWindow(exception);
                            }
                        });
        t1.start();
    }
}
