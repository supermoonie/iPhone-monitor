package com.github.supermoonie.iphone.monitor.platform.macos;

import com.github.supermoonie.iphone.monitor.platform.LibraryHolder;
import com.sun.jna.Library;

/**
 * @author supermoonie
 * @since 2021/7/27
 */
public interface NsUserNotificationsBridge extends Library {

    NsUserNotificationsBridge INSTANCE = LibraryHolder.loadNsUserNotificationsBridge();

    /**
     * 发送通知
     *
     * @param title 标题
     * @param subtitle  子标题
     * @param text  描述
     * @param timeOffset 延迟时间/秒
     * @return  1: true 0: false
     */
    int sendNotification(String title, String subtitle, String text, int timeOffset);
}
