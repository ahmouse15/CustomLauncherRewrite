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

package lol.hyper.customlauncher.ttrupdater;

import lol.hyper.customlauncher.ConfigHandler;
import lol.hyper.customlauncher.CustomLauncherRewrite;
import lol.hyper.customlauncher.tools.ExceptionWindow;
import lol.hyper.customlauncher.tools.JSONUtils;
import lol.hyper.customlauncher.tools.PopUpWindow;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.swing.*;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TTRUpdater extends JFrame {

    /**
     * The URL used for checking files. Not sure why this is a "txt" file if it returns a JSON.
     */
    public final String PATCHES_URL = "https://cdn.toontownrewritten.com/content/patchmanifest.txt";
    /**
     * The URL used for storing downloads. This is the root URL, so pass in the file name.
     */
    public final String PATCHES_URL_DL = "https://download.toontownrewritten.com/patches/";
    /**
     * The TTRUpdater logger.
     */
    public final Logger logger = LogManager.getLogger(this);
    /**
     * The main progress bar on the window.
     */
    private final JProgressBar progressBar;
    /**
     * The current file status text.
     */
    private final JLabel updateStatus;
    /**
     * The total file status.
     */
    private final JLabel totalUpdateStatus;

    /**
     * Creates the TTR updater window.
     */
    public TTRUpdater() {
        // set up the window elements
        setTitle("TTR Updater");
        setSize(370, 150);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            logger.error(e);
        }

        setIconImage(CustomLauncherRewrite.icon);

        // GUI elements
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        updateStatus = new JLabel("Checking files...");
        updateStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        totalUpdateStatus = new JLabel();
        totalUpdateStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar = new JProgressBar(0, 0);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(Box.createRigidArea(new Dimension(0, 30)));
        panel.add(updateStatus);
        panel.add(progressBar);
        panel.add(totalUpdateStatus);

        progressBar.setBounds(150, 100, 100, 30);
        updateStatus.setBounds(70, 25, 370, 40);

        add(panel);
        setLocationRelativeTo(null);
    }

    /**
     * Check for updates!
     */
    public void checkUpdates() {
        // don't run the updater if the folder doesn't exist
        if (!ConfigHandler.INSTALL_LOCATION.exists()) {
            JOptionPane.showMessageDialog(this, "Unable to check for TTR updates. We are unable to find your TTR install directory.", "Error", JOptionPane.ERROR_MESSAGE);
            dispose();
            logger.warn("Can't find current install directory. Skipping updates.");
        }

        logger.info("We are checking for TTR updates!");
        // read the patches
        JSONObject patches = JSONUtils.requestJSON(PATCHES_URL);
        if (patches == null) {
            logger.error("patchesmanifest.txt returned null!");
            dispose();
            return;
        }
        progressBar.setMaximum(patches.length());

        ArrayList<String> filesToDownload = new ArrayList<>();

        // this loops through the JSON
        // key is the file name
        for (String key : patches.keySet()) {
            progressBar.setValue(progressBar.getValue() + 1);
            JSONObject currentFile = (JSONObject) patches.get(key);
            String onlineHash = currentFile.getString("hash");
            // get the list of OS's the file is for
            List<String> only = currentFile.getJSONArray("only").toList().stream().map(object -> Objects.toString(object, null)).toList();
            // if we are running the OS the file is for, check it
            if (only.contains(CustomLauncherRewrite.OS)) {
                File localFile = new File(ConfigHandler.INSTALL_LOCATION, key);
                updateStatus.setText("Checking file " + localFile.getName());
                if (!localFile.exists()) {
                    logger.info("-----------------------------------------------------------------------");
                    logger.info(ConfigHandler.INSTALL_LOCATION.getAbsolutePath() + File.separator + key);
                    logger.info("This file is missing and will be downloaded.");
                    filesToDownload.add(key);
                    continue;
                }

                // the file exists locally, check the SHA1 and compare it to TTR's
                String localHash;
                try {
                    localHash = calcSHA1(localFile);
                } catch (Exception exception) {
                    logger.error("Unable to calculate SHA1 hash for file " + localFile.getAbsolutePath(), exception);
                    new ExceptionWindow(exception);
                    dispose();
                    return;
                }
                logger.info("-----------------------------------------------------------------------");
                logger.info(ConfigHandler.INSTALL_LOCATION.getAbsolutePath() + File.separator + key);
                logger.info("Local hash: " + localHash.toLowerCase(Locale.ENGLISH));
                logger.info("Expected hash: " + onlineHash);
                logger.info("Type: " + CustomLauncherRewrite.OS);
                if (localHash.equalsIgnoreCase(onlineHash)) {
                    logger.info("File is good!");
                } else {
                    logger.info("File is outdated! Will be downloaded.");
                    filesToDownload.add(key);
                }
            }
        }

        logger.info("-----------------------------------------------------------------------");

        // we store files we need to download in filesToDownload
        // if there are files in that list, download them
        int currentProgress = 0;
        if (filesToDownload.size() > 0) {
            totalUpdateStatus.setText(String.format("Progress: %d / %d", currentProgress, filesToDownload.size()));
            File tempFolder = new File("temp");
            if (!tempFolder.exists() && !tempFolder.mkdirs()) {
                logger.error("Unable to create temp folder!");
                new PopUpWindow(this, "Unable to create temp folder!");
                dispose();
            }

            logger.info(filesToDownload.size() + " file(s) are going to be downloaded.");
            logger.info(filesToDownload);

            progressBar.setValue(0); // reset

            // download each file
            for (String fileToDownload : filesToDownload) {
                progressBar.setMaximum(fileToDownload.length());
                JSONObject file = patches.getJSONObject(fileToDownload);
                String downloadName = file.getString("dl");

                logger.info("Downloading " + PATCHES_URL_DL + downloadName);
                updateStatus.setText("Downloading " + downloadName);
                progressBar.setVisible(true);
                progressBar.setValue(progressBar.getValue() + 1);

                URL downloadURL;
                try {
                    downloadURL = new URL(PATCHES_URL_DL + downloadName);
                } catch (MalformedURLException exception) {
                    logger.error("Invalid URL " + PATCHES_URL_DL + downloadName);
                    new ExceptionWindow(exception);
                    dispose();
                    return;
                }
                File downloadOutput = new File(tempFolder + File.separator + downloadName);
                long downloadStart = System.nanoTime();
                if (!saveFile(downloadURL, downloadOutput)) {
                    logger.error("Unable to download file " + downloadName);
                    new PopUpWindow(this, "Unable to download file " + downloadName + ".");
                    dispose();
                }
                long downloadTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - downloadStart, TimeUnit.NANOSECONDS);
                logger.info("Finished downloading " + downloadOutput.getAbsolutePath() + ". Took " + downloadTime + "ms.");
                updateStatus.setText("Finished downloading " + downloadName);

                long startTime = System.nanoTime();
                logger.info("Extracting " + downloadOutput.getAbsolutePath() + " to " + ConfigHandler.INSTALL_LOCATION + File.separator + fileToDownload);
                updateStatus.setText("Extracting " + downloadOutput + " to " + fileToDownload);
                try {
                    // extract the file to the new location
                    decompressBz2(downloadName, fileToDownload);
                } catch (IOException exception) {
                    logger.error("Unable to extract file " + downloadName, exception);
                    new ExceptionWindow(exception);
                    dispose();
                }
                updateStatus.setText("Finished extracting file " + fileToDownload);
                long extractedTime = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
                logger.info("Finished extracting file " + downloadName + ". Took " + extractedTime + "ms.");
                currentProgress++;
                totalUpdateStatus.setText(String.format("Progress: %d / %d", currentProgress, filesToDownload.size()));
            }
            // delete all files in the temp folder
            File[] tempFolderFiles = tempFolder.listFiles();
            if (tempFolderFiles != null) {
                for (File currentFile : tempFolderFiles) {
                    try {
                        Files.delete(currentFile.toPath());
                    } catch (IOException exception) {
                        logger.error("Unable to delete file " + currentFile.getAbsolutePath(), exception);
                        new ExceptionWindow(exception);
                        dispose();
                    }
                }
            }
            // delete the actual temp folder
            try {
                Files.delete(Paths.get(System.getProperty("user.dir") + File.separator + "temp"));
            } catch (IOException exception) {
                logger.error("Unable to delete temp folder!", exception);
                new ExceptionWindow(exception);
                dispose();
            }
        }
        logger.info("Finished checking for TTR updates!");
        new PopUpWindow(this, "Finished checking for TTR updates!");
        dispose();
    }

    /**
     * Calculates the SHA1 of a file.
     *
     * @param file The file to calculate.
     * @return String representing the SHA1.
     */
    private static String calcSHA1(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        try (InputStream input = new FileInputStream(file)) {

            byte[] buffer = new byte[8192];
            int len = input.read(buffer);

            while (len != -1) {
                sha1.update(buffer, 0, len);
                len = input.read(buffer);
            }
            return new HexBinaryAdapter().marshal(sha1.digest());
        }
    }

    /**
     * Extract the compressed bzip2 files to their output file.
     *
     * @param temp       The temp file's name that was downloaded.
     * @param outputName The file's output name.
     */
    private void decompressBz2(String temp, String outputName) throws IOException {
        File tempFile = new File("temp" + File.separator + temp);
        File output = new File(ConfigHandler.INSTALL_LOCATION, outputName);

        long totalBytes = tempFile.length();
        long bytesRead = 0;
        byte[] buffer = new byte[1024];
        int len;

        try (BZip2CompressorInputStream in = new BZip2CompressorInputStream(new BufferedInputStream(new FileInputStream(tempFile))); FileOutputStream out = new FileOutputStream(output)) {
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                bytesRead += len;
                int progress = (int) ((bytesRead * 100) / totalBytes);
                progressBar.setValue(progress);
            }
        }
    }

    /**
     * Downloads TTR file and saves it to the temp folder.
     *
     * @param downloadURL    The URL to download.
     * @param downloadOutput The file to save to.
     * @return True if successful, false if not.
     */
    private boolean saveFile(URL downloadURL, File downloadOutput) {
        boolean isSucceed = true;

        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpGet httpGet = new HttpGet(downloadURL.toString());
        httpGet.addHeader("User-Agent", CustomLauncherRewrite.userAgent);

        try {
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            HttpEntity imageEntity = httpResponse.getEntity();
            progressBar.setMaximum(100);
            final FileOutputStream output = FileUtils.openOutputStream(downloadOutput);
            try {
                final byte[] buffer = new byte[4046];
                long count = 0;
                int n;
                while (-1 != (n = imageEntity.getContent().read(buffer))) {
                    output.write(buffer, 0, n);
                    count += n;
                    progressBar.setValue((int) (count * 100 / imageEntity.getContentLength()));
                }
                output.close();
            } catch (IOException exception) {
                logger.error("Unable to save file!", exception);
                isSucceed = false;
            }
        } catch (IOException exception) {
            logger.error("Unable to save file!", exception);
            isSucceed = false;
        }

        return isSucceed;
    }
}
