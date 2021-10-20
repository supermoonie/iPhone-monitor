package com.github.supermoonie.iphone.monitor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.formdev.flatlaf.util.SystemInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.prefs.Preferences;

/**
 * @author supermoonie
 * @since 2021/10/19
 */
@Slf4j
public class IPhoneMonitor extends JFrame {

    @Getter
    private static Preferences preferences;
    @Getter
    private static IPhoneMonitor instance;
    private Map<String, Map<String, String>> categoryMap;
    private JComboBox<String> versionComboBox;
    private JComboBox<String> modelComboBox;
    private JTextArea logArea;
    private final ScheduledExecutorService scheduledExecutor;

    public IPhoneMonitor() throws HeadlessException, IOException {
        super();
        scheduledExecutor = new ScheduledThreadPoolExecutor(
                5,
                new BasicThreadFactory.Builder()
                        .namingPattern("schedule-%d")
                        .daemon(false)
                        .uncaughtExceptionHandler((thread, throwable) -> {
                            String error = String.format("thread: %s, error: %s", thread.toString(), throwable.getMessage());
                            log.error(error, throwable);
                        }).build(), (r, executor) -> log.warn("Thread: {} reject by {}", r.toString(), executor.toString()));

        setIconImages(FlatSVGUtils.createWindowIconImages("/iphone.svg"));
        setTitle("iPhone Monitor");

        JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        selectPanel.setBorder(BorderFactory.createTitledBorder("手机类型选择"));
        JLabel versionLabel = new JLabel("版本选择: ");
        versionLabel.setPreferredSize(new Dimension(100, 25));
        versionLabel.setHorizontalAlignment(JLabel.CENTER);
        selectPanel.add(versionLabel);
        String category = new String(IOUtils.resourceToByteArray("/category.json"), StandardCharsets.UTF_8);
        categoryMap = JSON.parseObject(category, new TypeReference<>() {
        });
        String[] versions = categoryMap.keySet().toArray(new String[]{});
        versionComboBox = new JComboBox<>(versions);
        versionComboBox.setPreferredSize(new Dimension(220, 25));
        selectPanel.add(versionComboBox);
        JLabel modelLabel = new JLabel("型号选择: ");
        modelLabel.setPreferredSize(new Dimension(100, 25));
        modelLabel.setHorizontalAlignment(JLabel.CENTER);
        selectPanel.add(modelLabel);
        String[] models = categoryMap.get(versions[0]).keySet().toArray(new String[]{});
        modelComboBox = new JComboBox<>(models);
        modelComboBox.setPreferredSize(new Dimension(220, 25));
        selectPanel.add(modelComboBox);
        versionComboBox.addItemListener(e -> {
            if(e.getStateChange() == ItemEvent.SELECTED) {
                String selectedVersion = Objects.requireNonNull(versionComboBox.getSelectedItem()).toString();
                System.out.println(selectedVersion);
                modelComboBox.removeAllItems();
                Map<String, String> modelMap = categoryMap.get(selectedVersion);
                for (String model : modelMap.keySet()) {
                    modelComboBox.addItem(model);
                }
            }
        });



        JButton confirmButton = new JButton("确定");
        confirmButton.addActionListener(e -> {
            String version = Objects.requireNonNull(versionComboBox.getSelectedItem()).toString();
            String model = Objects.requireNonNull(modelComboBox.getSelectedItem()).toString();
            logArea.append("---------------分割线---------------\n");
            logArea.append(String.format("[%s] 已选择: %s %s\n", DateFormatUtils.format(new Date(), "HH:mm:ss"), version, model));
            try {
                getStock(categoryMap.get(version).get(model), "北京 北京 朝阳区");
//                getAddress(List.of("state=北京", "city=北京", "district=朝阳区"), categoryMap.get(version).get(model));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        });
        selectPanel.add(confirmButton);

        Box box = Box.createVerticalBox();
        box.add(selectPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.setBorder(BorderFactory.createTitledBorder("日志"));
        logArea = new JTextArea();
        logArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(logArea);
        container.add(scrollPane, BorderLayout.CENTER);
        JButton clearLogButton = new JButton("清除日志");
        clearLogButton.addActionListener(e -> logArea.setText(""));
        JPanel logButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        logButtonsPanel.add(clearLogButton);
        container.add(logButtonsPanel, BorderLayout.SOUTH);


        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(box, BorderLayout.NORTH);
        getContentPane().add(container, BorderLayout.CENTER);

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - 980 / 2, screenSize.height / 2 - 600 / 2);
        setMinimumSize(new Dimension(980, 600));
        setPreferredSize(new Dimension(980, 600));
        pack();
        setResizable(true);
        setFocusable(true);
        setAutoRequestFocus(true);
        setVisible(true);
    }

    private void getStock(String iphoneCode, String location) throws IOException {
        String url = String.format("https://www.apple.com.cn/shop/fulfillment-messages?pl=true&parts.0=%s&location=%s", iphoneCode, URLEncoder.encode(location, StandardCharsets.UTF_8));
        String res = Request.Get(url)
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"93\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"93\"")
                .addHeader("Referer", String.format("https://www.apple.com.cn/shop/buy-iphone/iphone-13-pro/%s", iphoneCode))
                .addHeader("DNT", "1")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36")
                .addHeader("sec-ch-ua-platform", "macOS")
                .execute().handleResponse(response -> EntityUtils.toString(response.getEntity()));
        JSONObject json = JSON.parseObject(res);
        JSONArray stores = json.getJSONObject("body").getJSONObject("content").getJSONObject("pickupMessage").getJSONArray("stores");
        for (int i = 0; i < stores.size(); i ++) {
            JSONObject store = stores.getJSONObject(i);
            String storeName = store.getString("storeName");
            JSONObject partsAvailability = store.getJSONObject("partsAvailability");
            if (partsAvailability.containsKey(iphoneCode)) {
                JSONObject info = partsAvailability.getJSONObject(iphoneCode);
                String pickupSearchQuote = info.getString("pickupSearchQuote");
                if ("暂无供应".equals(pickupSearchQuote)) {
                    logArea.append(String.format("[%s] %s 暂无供应\n", DateFormatUtils.format(new Date(), "HH:mm:ss"), storeName));
                    continue;
                }
                logArea.append(String.format("[%s] %s %s\n", DateFormatUtils.format(new Date(), "HH:mm:ss"), storeName, pickupSearchQuote));
            }
        }
    }

    private void getAddress(List<String> params, String selectPhone) throws IOException {
        String url = String.format("https://www.apple.com.cn/shop/address-lookup?%s", String.join("&", params));
        log.info("url: {}", url);
        String res = Request.Get(url)
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"93\", \" Not;A Brand\";v=\"99\", \"Chromium\";v=\"93\"")
                .addHeader("Referer", String.format("https://www.apple.com.cn/shop/buy-iphone/iphone-13-pro/%s", selectPhone))
                .addHeader("DNT", "1")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/93.0.4577.82 Safari/537.36")
                .addHeader("sec-ch-ua-platform", "macOS")
                .execute().handleResponse(response -> EntityUtils.toString(response.getEntity()));
        JSONObject json = JSON.parseObject(res);
        JSONObject body = json.getJSONObject("body");
        System.out.println(res);
    }

    public static void main(String[] args) {
        try {
            java.awt.Toolkit.getDefaultToolkit();
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
            if (SystemInfo.isMacOS) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.UIElement", "true");
                Class<?> appleAppClass = Class.forName("com.apple.eawt.Application");
                Method getApplication = appleAppClass.getMethod("getApplication");
                Object app = getApplication.invoke(appleAppClass);
                Method setDockIconImage = appleAppClass.getMethod("setDockIconImage", Image.class);
                URL url = IPhoneMonitor.class.getClassLoader().getResource("mitm.png");
                Image image = Toolkit.getDefaultToolkit().getImage(url);
                setDockIconImage.invoke(app, image);
            }
            preferences = Preferences.userRoot().node("/IPhoneMonitor");
            String themeName = preferences.get("/theme", FlatDarkLaf.class.getName());
            log.info("current theme: {}", themeName);
            FlatLightLaf.setup();
            FlatDarkLaf.setup();
            UIManager.setLookAndFeel(FlatDarkLaf.class.getName());
            FlatInspector.install("ctrl shift alt X");
            FlatUIDefaultsInspector.install("ctrl shift alt Y");
            instance = new IPhoneMonitor();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            System.exit(0);
        }

    }
}
