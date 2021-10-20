package com.github.supermoonie.iphone.monitor;

import com.github.supermoonie.iphone.monitor.platform.macos.NsUserNotificationsBridge;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author super_w
 * @since 2021/7/24
 */
@Slf4j
public class Notification {

    private static Notification instance;
    @Getter
    private final TrayIcon defaultTrayIcon;

    private Notification() throws IOException, AWTException {
        if (!SystemTray.isSupported()) {
            throw new RuntimeException("当前系统不支持");
        }
        URL resource = Notification.class.getResource("/apple.png");
        assert resource != null;
        BufferedImage icon = ImageIO.read(resource);
        defaultTrayIcon = new TrayIcon(icon);
        defaultTrayIcon.setToolTip("iPhone-monitor");
        PopupMenu popupMenu = new PopupMenu();
        MenuItem quitItem = new MenuItem("Quit");
        quitItem.addActionListener(event -> {
            IPhoneMonitor.getInstance().getScheduledExecutor().shutdownNow();
            try {
                IPhoneMonitor.getInstance().getScheduledExecutor().awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
            IPhoneMonitor.getInstance().dispose();
            System.exit(0);
        });
        popupMenu.add(quitItem);
        defaultTrayIcon.setPopupMenu(popupMenu);
        SystemTray.getSystemTray().add(defaultTrayIcon);
    }

    public static Notification getInstance() throws IOException, AWTException {
        if (null == instance) {
            synchronized (Notification.class) {
                if (null == instance) {
                    instance = new Notification();
                }
            }
        }
        return instance;
    }

    public void doNotify(final String title, final String content, final TrayIcon.MessageType type) {
        if (SystemUtils.IS_OS_MAC && null != NsUserNotificationsBridge.INSTANCE) {
            NsUserNotificationsBridge.INSTANCE.sendNotification(title, content, "", 2);
        } else {
            defaultTrayIcon.displayMessage(title, content, type);
        }
    }
}
