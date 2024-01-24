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
import java.util.Objects;
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
            SelenideElement acceptButton = $$x("//div[contains(@id, 'message')]" +
                    "//button[./span[text()='Да это я']]").last();
            acceptButton.shouldBe(Condition.visible, Duration.ofSeconds(60L));
            SelenideElement lastMessage = $$x("//div[contains(@id, 'message')]" +
                    "//div[@class='content-inner']/div").last();
            SelenideElement lastMessageWithButton = $$x("//div[contains(@id, 'message') and " +
                    ".//button[./span[text()='Да это я']]]//div[@class='content-inner']" +
                    "/div" ).last();
            SelenideElement scrollButton =
                    $x("//button[@title='Go to bottom' and ./i[contains(@class, icon-arrow-down)]]");
            System.out.println("---> Job started [" + LocalDateTime.now() + "]");
            int lifeCounter = 0;
            int acceptCounter = 0;
            while (!hasShutdownTrigger()){
                if (scrollButton.isDisplayed()){
                    scrollButton.click();
                    lifeCounter = 0;
                }
                if (lastMessage.getOwnText().equals(lastMessageWithButton.getOwnText())){
                    if(acceptButton.isDisplayed()) {
                        acceptButton.click();
                        System.out.println("---> Accepted [" + LocalDateTime.now() + "]");
                        lifeCounter = 0;
                        acceptCounter += 1;
                    }
                }
                else{
                    acceptCounter = 0;
                }
                if (!lastMessage.isDisplayed() || !acceptButton.isDisplayed() || acceptCounter >= 10){
                    Selenide.refresh();
                    System.out.println("---> Refreshed [" + LocalDateTime.now() + "]");
                    acceptCounter = 0;
                    if ((!lastMessage.isDisplayed() || !acceptButton.isDisplayed()) && (!scrollButton.isDisplayed())){
                        lifeCounter += 1;
                        if (lifeCounter >= 10)
                            System.out.println("---> ERROR browser sleeping [" + LocalDateTime.now() + "]");
                    }
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
