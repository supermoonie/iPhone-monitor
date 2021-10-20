package com.github.supermoonie.iphone.monitor.platform;

import com.github.supermoonie.iphone.monitor.platform.macos.NsUserNotificationsBridge;
import com.github.supermoonie.iphone.monitor.utils.Folders;
import com.sun.jna.Native;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * @author supermoonie
 * @since 2021/7/28
 */
@Slf4j
public class LibraryHolder {

    public static NsUserNotificationsBridge loadNsUserNotificationsBridge() {
        if (SystemUtils.IS_OS_MAC) {
            File nativeFolder = Folders.createTempFolder(".iphone", "native");
            File targetFile = new File(nativeFolder.getAbsolutePath() + File.separator + "NsUserNotificationsBridge.dylib");
            try (InputStream inputStream = LibraryHolder.class.getResourceAsStream("/macos/NsUserNotificationsBridge.dylib")) {
                Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            return Native.load(targetFile.getAbsolutePath(), NsUserNotificationsBridge.class);
        } else {
            return null;
        }
    }
}
