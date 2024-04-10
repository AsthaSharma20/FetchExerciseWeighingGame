import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * FindFakeBarTest - Find fake gold bar
 * This test verifies :
 * <ol>
 * <li>Divides the gold bar into three groups/buckets</li>
 * <li>Fills gold bars in the weighing scales with the divided group</li>
 * <li>Clicks on weigh button</li>
 * <li>Finds the weighing result</li>
 * <li>Resets the entered values</li>
 * <li>Enters the values until fake gold bar is found</li>
 * <li>Clicks on the result gold bar</li>
 * <li>Prints the alert message and dismisses the alert</li>
 * <li>Prints the weighing list</li>
 * </ol>
 */
public class FindFakeBarTest {
    // Finds fake gold bar using two balancing scales

    private WebDriver driver;

    private String goldBarLocator ="//div[@class='coins']//button";
    private String weighingListLocator = "//div[@class='game-info']//li";
    private String leftBowlLocator = "input[data-side='left']";
    private String rightBowlLocator = "input[data-side='right']";
    private String expectedWeightButtonLocator = "weigh";
    private String resultLocator = "#reset[disabled]";
    private String resetButtonLocator = "(//*[@id='reset'])[2]";


    @BeforeAll
    public static void setupWebdriverFirefoxDriver() {
        System.setProperty("webdriver.firefox.bin", "/Applications/Firefox.app/Contents/MacOS/firefox");

        System.setProperty("webdriver.gecko.driver",
                System.getProperty("user.dir") + "/src/main/resources/geckodriver");
    }

    @BeforeEach
    public void setup() {
        driver = new FirefoxDriver();
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @Test
    public void findLeastWeightBar() {
        // Launch the webiste and maximize the window
        driver.manage().window().maximize();
        driver.get("http://sdetchallenge.fetch.com/");
        assertTrue(driver.getTitle().contains("React App"),"Retrieve the page title");
        // Retrieve gold bar elements
        List<WebElement> goldBarResult = driver.findElements(By.xpath(goldBarLocator));
        // Retrieve no. of gold bars
        int totalBars = goldBarResult.size();

        // Create list of inputs
        ArrayList<String> goldBars = goldBarResult.stream()
        .map(WebElement::getText)
        .collect(Collectors.toCollection(ArrayList::new));
        // Find the fake bar
        String fakeBar = recursion(totalBars, goldBars);
        System.out.println("Fake Bar is " + fakeBar);
    }

    private String recursion(int totalBars, ArrayList<String> barElements) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        if (totalBars == 1) {
            List<WebElement> goldBarResult = driver.findElements(By.xpath(goldBarLocator));
            Optional<WebElement> goldbarResultButton = goldBarResult.stream()
                    .filter(button -> button.getText().equals(barElements.get(0)))
                    .findFirst();

            // Click the button if it's found
            goldbarResultButton.ifPresent(button -> {
                button.click();
            });

            // Retrieve the alert and its text message
            Alert alert = new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.alertIsPresent());
            String alertText = alert.getText();

            // Close the alert
            alert.accept();
            // Print the alert message 
            System.out.println("Alert text : " + alertText);

            // Wait for weighing list to appear
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(weighingListLocator)));

            // Get list of weighing result
            List<WebElement> outputList = driver.findElements(By.xpath(weighingListLocator));
            // Print the weighing result list
            outputList.stream().map(WebElement::getText).forEach(System.out::println);

            return barElements.get(0);
        }
        // Divide the total bars into three buckets
        int firstBucket = (int) Math.ceil(totalBars / 3.0);
        int secondBucket = (int) Math.ceil(totalBars / 3.0);
        int thirdBucket = totalBars - 2 * (int) Math.ceil(totalBars / 3.0);

        // Enter three numbers in the left bowl
        List<WebElement> leftInputFieldElements = driver.findElements(By.cssSelector(leftBowlLocator));
        for (int i = 0; i < firstBucket && i < leftInputFieldElements.size(); i++) {
            leftInputFieldElements.get(i).sendKeys(barElements.get(i));
            assertEquals(leftInputFieldElements.get(i).getAttribute("value"), barElements.get(i));
        }

        // Enter three numbers in the right bowl
        List<WebElement> rightInputFieldElements = driver.findElements(By.cssSelector(rightBowlLocator));
        for (int i = 0, j = firstBucket; i < secondBucket && i < rightInputFieldElements.size(); i++, j++) {
            rightInputFieldElements.get(i).sendKeys(barElements.get(j));
            assertEquals(rightInputFieldElements.get(i).getAttribute("value"), barElements.get(j));
        }

        // Locate the weigh button
        WebElement weighButton = driver.findElement(By.id(expectedWeightButtonLocator));

        // Press the weight button
        weighButton.click();

        // Locate the result
        WebElement weighingResult = driver.findElement(By.cssSelector(resultLocator));
        // Wait until result value gets updated
        wait.until(new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                String result = weighingResult.getText();
                return !result.equals("?");
            }
        });

        // Retrieve the value of result sign
        String result = weighingResult.getText();

        // Locate the reset button and click it
        WebElement resetButton = driver.findElement(By.xpath(resetButtonLocator));
        resetButton.click();

        if (result.equals("<")) {
            return recursion(firstBucket, new ArrayList<>(barElements.subList(0, firstBucket)));
        } else if (result.equals(">")) {
            return recursion(secondBucket, new ArrayList<>(barElements.subList(firstBucket, firstBucket + secondBucket)));
        } else {
            return recursion(thirdBucket,
                    new ArrayList<>(barElements.subList(firstBucket + secondBucket, barElements.size())));
        }
    }
}