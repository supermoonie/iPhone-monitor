package com.github.supermoonie.iphone.monitor.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;

/**
 * @author supermoonie
 * @since 2021-04-11
 */
public class Folders {

    public static File crateTempFolder(String folderName) {
        File tempFolder = new File(SystemUtils.getUserHome().getAbsolutePath() + File.separator + folderName);
        if (!tempFolder.exists() && !tempFolder.mkdirs()) {
            throw new RuntimeException(tempFolder.getAbsolutePath() + " create fail");
        }
        return tempFolder;
    }

    public static File createTempFolder(String... folders) {
        File parentFolder = new File(SystemUtils.getUserHome().getAbsolutePath());
        for (String folder : folders) {
            File tempFolder = new File(parentFolder.getAbsolutePath() + File.separator + folder);
            if (!tempFolder.exists() && !tempFolder.mkdirs()) {
                throw new RuntimeException(tempFolder.getAbsolutePath() + " create fail");
            }
            parentFolder = tempFolder;
        }
        return parentFolder;
    }
}
