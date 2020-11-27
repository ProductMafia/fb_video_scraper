import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipException;

public class ScrapeUtil {
    public static final String VIDEO_FOLDER = "videos/";

    public static void close() {
        if(driver!=null) {
            try {
                driver.close();
            } catch(Exception e) {
                e.printStackTrace();
                System.out.println("Failed to close web driver...");
            }
        }
    }

    private static void handleAlert(WebDriver driver) {
        String window = driver.getWindowHandle();
        if(ExpectedConditions.alertIsPresent().apply(driver)!=null) { // check if alert is present
            driver.switchTo().alert().accept();
            driver.switchTo().window(window);
        }
    }

    private static ChromeDriver driver = null;
    public static String getHTML(String endpoint, boolean useCache, boolean download) {
        try {
            endpoint = endpoint.replace(" ","%20");
            String fileName = endpoint.replaceAll("[^A-Za-z0-9]", "");
            if(fileName.startsWith("https")) {
                // important to keep http and https files the same
                fileName = "http"+fileName.substring(5);
            }
            File file = new File(VIDEO_FOLDER);
            file = new File(file, fileName);

            file = new File(file, fileName+".gzip");
            String json = null;
            ByteArrayOutputStream videoStream = null;
            if(file.exists() && useCache) {
                videoStream = new ByteArrayOutputStream();
                GZIPInputStream inputStream = new GZIPInputStream(new FileInputStream(file));
                try {
                    IOUtils.copy(inputStream, videoStream);
                    videoStream.flush();
                    videoStream.close();
                } catch(Exception e) {
                    System.out.println("Retrying " + endpoint + "...");
                }
            }
            if(videoStream == null) {
                if (!download) {
                    return null;
                } else {
                    ChromeOptions options = new ChromeOptions();
                    options.setCapability(CapabilityType.UNEXPECTED_ALERT_BEHAVIOUR, UnexpectedAlertBehaviour.ACCEPT);
                    options.setCapability(CapabilityType.SUPPORTS_NETWORK_CONNECTION, true);
                    options.addArguments("--incognito", "--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors", "--network-connection-enabled");
                    options.setExperimentalOption("prefs", Collections.singletonMap("profile.default_content_setting_values.notifications", 2));
                    if(driver==null) {
                        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver");
                        System.setProperty("webdriver.firefox.driver", "/usr/local/bin/geckodriver");
                        driver = new ChromeDriver(options);
                    }
                    handleAlert(driver);
                    try {
                        driver.get(endpoint);
                    } catch(Exception e) {
                        e.printStackTrace();
                        System.out.println("retrying");
                        try {
                            driver.close();
                        } catch(Exception e2) {
                            System.out.println("Failed to close.");
                        }
                        driver = new ChromeDriver(options);
                    }
                    //System.out.println("PAGE: "+driver.getPageSource());

                    final long maxTime = 30 * 1000;
                    long t = 0;
                    final int interval = 200;
                    handleAlert(driver);
                    WebElement video = null;

                    TimeUnit.MILLISECONDS.sleep(1000);

                    while(t < maxTime) {
                        handleAlert(driver);
                        t += interval;
                        try {
                            WebElement permalink = driver.findElementById("permalink_video_pagelet");
                            if (permalink != null) {
                                List<WebElement> videos = permalink.findElements(By.tagName("video"));
                                if (videos != null) {
                                    video = videos.get(0);
                                }
                            }
                            if (video != null) {
                                break;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Video element not available yet.");
                        }
                        TimeUnit.MILLISECONDS.sleep(interval);
                    }

                    String resolvedUrl = driver.getCurrentUrl();

                    if (video != null) {
                        driver.findElementsByTagName("body").get(0).click();
                        String script = FileUtils.readFileToString(new File("script.js"), StandardCharsets.UTF_8)
                                .replace("\n", "");

                        System.out.println("Script: "+script);
                        script = script.replace("<<originalVideoUrl>>", endpoint);
                        script = script.replace("<<newVideoUrl>>", resolvedUrl);

                        ((JavascriptExecutor)driver).executeScript(script);

                        while ( true ) {
                            try {
                                if (driver.findElementById("fb-vid-scr-test-elem-message") != null) {
                                    break;
                                }
                            } catch(Exception e) {
                                System.out.println("Waiting for video to complete.");
                            }
                            TimeUnit.MILLISECONDS.sleep(1000);
                        }
                    }

                    System.out.println("Resolved URL: "+resolvedUrl);

                    boolean finished = false;
                    try {
                        GZIPOutputStream outputStream = new GZIPOutputStream(new FileOutputStream(file));
                        IOUtils.copy(new ByteArrayInputStream(json.getBytes()), outputStream);
                        outputStream.flush();
                        outputStream.close();
                        finished = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    } finally {
                        if (!finished) {
                            file.delete();
                        }
                    }
                }
            }
            // System.out.println(json);
            return json;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        // testing
    }
}
