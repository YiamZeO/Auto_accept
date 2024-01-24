package com.auto_tests_template;

import com.codeborne.selenide.*;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.awt.*;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import static com.codeborne.selenide.Selenide.*;

public class AutoAccept {
    private static final String SHUTDOWN_TRIGGER_FILENAME = "shutdown_trigger.txt";
    private static final String BUILD_FILES = "build";

    public AutoAccept() {
        ChromeOptions chromeOptions = new ChromeOptions()
                // Fix the issue https://github.com/SeleniumHQ/selenium/issues/11750
                .addArguments("--remote-allow-origins=*")
                .addArguments("--window-size=1920,1080")
                .addArguments("--headless");
        WebDriver driver = new ChromeDriver(chromeOptions);
        WebDriverRunner.setWebDriver(driver);
    }

    public void autoAccept(){
        try {
            open("https://web.telegram.org/a/");
            SelenideElement qrCode = $x("//div[@class='qr-container']");
            qrCode.shouldBe(Condition.visible, Duration.ofSeconds(1800L));
            try {
                Thread.sleep(Duration.ofSeconds(2L));
                String path = URLDecoder.decode(Objects.requireNonNull(screenshot("QR_Code"))
                        .split("file:/")[1], StandardCharsets.UTF_8);
                Desktop.getDesktop().open(Paths.get(path).toFile());
            } catch (InterruptedException | IOException e) {
                throw new RuntimeException(e);
            }
            SelenideElement ditBotChat = $x("//a[.//h3[text()='DITauthBot']]");
            ditBotChat.shouldBe(Condition.visible, Duration.ofSeconds(60L));
            ditBotChat.click();
            SelenideElement lastMessage = $$x("//div[contains(@id, 'message')]" +
                    "//div[@class='content-inner']/div").last();
            executeJavaScript("window.open(arguments[0], '_blank')", "https://web.telegram.org/a/");
            Set<String> windowHandles = WebDriverRunner.getWebDriver().getWindowHandles();
            Iterator<String> windowsIterator = windowHandles.iterator();
            String firstWindow = windowsIterator.next();
            String secondWindow = windowsIterator.next();
            Selenide.switchTo().window(secondWindow);
            SelenideElement controlChat = $x("//a[.//h3[text()='Saved Messages']]");
            controlChat.shouldBe(Condition.visible, Duration.ofSeconds(60L));
            controlChat.click();
//            SelenideElement eircChat = $x("//a[.//h3[text()='ЕИРЦ']]");
//            eircChat.shouldBe(Condition.visible, Duration.ofSeconds(60L));
//            eircChat.click();
//            SelenideElement vpnChat = $x("//a[.//h3[text()='VPN DIT запросы']]");
//            eircChat.shouldBe(Condition.visible, Duration.ofSeconds(60L));
//            vpnChat.click();
            System.out.println("---> Job started [" + LocalDateTime.now() + "]");
            int lifeCounter = 0;
            while (!hasShutdownTrigger()){
                if (!lastMessage.isDisplayed()){
                    Selenide.refresh();
                    System.out.println("---> Refreshed [" + LocalDateTime.now() + "]");
                    if ((!lastMessage.isDisplayed())){
                        lifeCounter += 1;
                        if (lifeCounter >= 10)
                            System.out.println("---> ERROR browser sleeping [" + LocalDateTime.now() + "]");
                    }
                } else if (lastMessage.getOwnText().contains("Vpn bot!")) {
                    Selenide.switchTo().window(firstWindow);
                    SelenideElement acceptButton = $x("//div[contains(@id, 'message')]" +
                    "//button[./span[text()='Да это я']]");
                    acceptButton.click();
                    Selenide.switchTo().window(secondWindow);
                    SelenideElement inputField = $x("//div[@id='message-input-text']/div/div/div");
                    inputField.setValue("Accepted");
                    SelenideElement sendButton = $x("//button[@title='Send Message']");
                    sendButton.click();
                    System.out.println("---> Accepted [" + LocalDateTime.now() + "]");
                    lifeCounter = 0;
                }
                try {
                    Thread.sleep(Duration.ofSeconds(2L));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            if (WebDriverRunner.hasWebDriverStarted())
                WebDriverRunner.closeWebDriver();
            try {
                deleteShutdownTrigger();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            System.out.println("---> Job ended [" + LocalDateTime.now() + "]");
        }
    }

    private static boolean hasShutdownTrigger() {
        Path shutdownTriggerFile = Path.of(SHUTDOWN_TRIGGER_FILENAME);
        return Files.exists(shutdownTriggerFile);
    }

    private static void deleteShutdownTrigger() throws IOException {
        Files.deleteIfExists(Path.of(SHUTDOWN_TRIGGER_FILENAME));
        FileUtils.deleteDirectory(Path.of(BUILD_FILES).toFile());
    }
}
