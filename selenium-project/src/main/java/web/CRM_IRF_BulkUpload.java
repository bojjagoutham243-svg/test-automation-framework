package web;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;

public class CRM_IRF_BulkUpload {

    public static void main(String[] args) throws Exception {

        // ================= CONFIGURATION =================
        String mobile = "8639202204";
        String chargerType = "AC";                  // Change to "DC" if needed
        String accountName = "Kia India Pvt Ltd";
        String customerType = "Individual";         //Corporate
        String circleType = "Karnataka";
        String filePath = "/Users/gouthambojja/Documents/Bulk IRF - Corporate - Template.xlsx";

        // ================= SETUP =================
        WebDriverManager.chromedriver().setup();

        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // ================= OPEN APPLICATION =================
            driver.get("https://exicom-crm-dev.hummingwave.com/login");
            driver.manage().window().maximize();

            // ================= LOGIN =================
            driver.findElement(By.name("phone")).sendKeys(mobile);

            // Fetch OTP from DB
            String otp = "";
            for (int i = 0; i < 5; i++) {
                otp = getOTPFromDB(mobile);
                if (!otp.isEmpty()) {
                    break;
                }
                Thread.sleep(2000);
            }

            if (otp.isEmpty()) {
                throw new RuntimeException("OTP not fetched from database.");
            }

            // Enter OTP
            WebElement otpField = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]")
                    )
            );
            otpField.sendKeys(otp);

            // Wait until dashboard loads
            wait.until(ExpectedConditions.urlContains("dashboard"));
            System.out.println("Login Successful!");

            // ================= NAVIGATION =================
            WebElement onBoardMenu = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//*[contains(text(),'On Board')]")
                    )
            );
            onBoardMenu.click();

            WebElement bulkUploadMenu = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//*[contains(text(),'Bulk upload')]")
                    )
            );
            bulkUploadMenu.click();

            // Select I&C Requests
            WebElement icRequests = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath("//div[contains(text(),'I&C Requests')]")
                    )
            );
            js.executeScript("arguments[0].click();", icRequests);

            // ================= SELECT CHARGER TYPE =================
            String cssSelector = chargerType.equalsIgnoreCase("AC")
                    ? ".crm__icon.crm__ac__unchecked"
                    : ".crm__icon.crm__dc__unchecked";

            WebElement chargerOption = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            By.cssSelector(cssSelector)
                    )
            );

            js.executeScript("arguments[0].click();", chargerOption);
            System.out.println("Selected type as " + chargerType + " Charger");

            Thread.sleep(2000);

            // ================= ACCOUNT SELECTION =================
            By accountFieldLocator =
                    By.xpath("//label[contains(text(),'Account')]/following::input[1]");

            WebElement accountField = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(accountFieldLocator)
            );

            js.executeScript("arguments[0].click();", accountField);

            // Clear existing value
            accountField.sendKeys(Keys.chord(Keys.COMMAND, "a"));
            accountField.sendKeys(Keys.DELETE);

            // Enter account name
            accountField.sendKeys(accountName);

            // Wait for dropdown options to appear
            Thread.sleep(3000);

            List<WebElement> options = wait.until(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.xpath("//div[@role='option'] | //li[@role='option']")
                    )
            );

            boolean accountSelected = false;

            for (WebElement option : options) {
                String optionText = option.getText().trim();

                if (optionText.equalsIgnoreCase(accountName)) {
                    js.executeScript("arguments[0].click();", option);
                    accountSelected = true;
                    break;
                }
            }

            if (!accountSelected) {
                throw new RuntimeException("Account not found: " + accountName);
            }

            System.out.println("Selected Account: " + accountName);

            // Optional wait to verify selection
            Thread.sleep(3000);
            
         // ================= CUSTOMER TYPE =================
            

            By customerTypeLocator =
                    By.xpath("//label[contains(text(),'Customer Type')]/following::input[1]");

            WebElement customerTypeField = wait.until(
                    ExpectedConditions.elementToBeClickable(customerTypeLocator)
            );

            // Click the field
            js.executeScript("arguments[0].click();", customerTypeField);

            // Clear any existing value
            customerTypeField.sendKeys(Keys.chord(Keys.COMMAND, "a"));
            customerTypeField.sendKeys(Keys.DELETE);

            // Enter customer type
            customerTypeField.sendKeys(customerType);

            // Wait for dropdown options to load
            Thread.sleep(2000);

            // Get all dropdown options
            List<WebElement> customerTypeOptions = wait.until(
                    ExpectedConditions.presenceOfAllElementsLocatedBy(
                            By.xpath("//div[@role='option'] | //li[@role='option']")
                    )
            );

            boolean customerTypeSelected = false;

            for (WebElement option : customerTypeOptions) {
                String optionText = option.getText().trim();

                if (optionText.equalsIgnoreCase(customerType)) {
                    js.executeScript("arguments[0].click();", option);
                    customerTypeSelected = true;
                    break;
                }
            }

            if (!customerTypeSelected) {
                throw new RuntimeException("Customer Type not found: " + customerType);
            }

            System.out.println("Selected Customer Type: " + customerType);
            Thread.sleep(2000);
            
         // ================= CIRCLE TYPE =================

            // Locate the Circle Type container specifically
            WebElement circleContainer = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//div[@id='cricle_type']")
                    )
            );

            // Scroll into view
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    circleContainer
            );

            // Click the Circle Type dropdown
            js.executeScript("arguments[0].click();", circleContainer);
            Thread.sleep(1000);

            // Locate the input field INSIDE the Circle Type container only
            WebElement circleInput = circleContainer.findElement(
                    By.xpath(".//input")
            );

            // Clear any existing value
            circleInput.sendKeys(Keys.chord(Keys.COMMAND, "a"));
            circleInput.sendKeys(Keys.DELETE);

            // Enter the Circle Type value
            circleInput.sendKeys(circleType);
            Thread.sleep(2000);

            // Select the first matching option
            circleInput.sendKeys(Keys.ARROW_DOWN);
            Thread.sleep(1000);
            circleInput.sendKeys(Keys.ENTER);

            System.out.println("Selected Circle Type: " + circleType);
            Thread.sleep(2000);
            
         // ================= DOCUMENT UPLOAD WITH ERROR VALIDATION =================

         // Verify file exists before uploading
         File file = new File(filePath);

         if (!file.exists()) {
             throw new RuntimeException("File not found: " + filePath);
         }

         // Locate the hidden file input element
         WebElement fileInput = wait.until(
                 ExpectedConditions.presenceOfElementLocated(
                         By.xpath("//input[@type='file']")
                 )
         );

         // Make the file input visible if hidden
         js.executeScript(
                 "arguments[0].style.display='block';" +
                 "arguments[0].style.visibility='visible';" +
                 "arguments[0].style.opacity='1';",
                 fileInput
         );

         // Upload the file
         fileInput.sendKeys(file.getAbsolutePath());

         System.out.println("File uploaded successfully: " + file.getAbsolutePath());

         // Wait for upload processing
         Thread.sleep(5000);

         // ================= CHECK FOR ERROR TOAST =================
         List<WebElement> toastHeaders = driver.findElements(
                 By.xpath("//div[contains(@class,'toast_content_header')]")
         );

         List<WebElement> toastSubtitles = driver.findElements(
                 By.xpath("//div[contains(@class,'toast_content_subtitle')]")
         );

         // If toast message is displayed, print the message and stop execution
         if (!toastHeaders.isEmpty() && !toastSubtitles.isEmpty()) {

             String headerMessage = toastHeaders.get(0).getText().trim();
             String subtitleMessage = toastSubtitles.get(0).getText().trim();

             if (!headerMessage.isEmpty() || !subtitleMessage.isEmpty()) {

                 System.out.println("========================================");
                 System.out.println("Bulk Upload Failed");
                 System.out.println("Header Message   : " + headerMessage);
                 System.out.println("Error Message    : " + subtitleMessage);
                 System.out.println("========================================");

                 // Close browser
                 driver.quit();

                 // Stop execution immediately
                 return;
             }
         }

         // ================= NO ERROR TOAST =================
         // If no error message is displayed, continue with count validation
         System.out.println("File uploaded successfully. No error message displayed.");
         System.out.println("Proceeding to summary count validation...");
            
         // ================= VERIFY BULK UPLOAD COUNTS =================

         // Wait until the count buttons are displayed after file upload
         WebElement allButton = wait.until(
                 ExpectedConditions.visibilityOfElementLocated(
                         By.xpath("//button[contains(@class,'filter-button') and contains(.,'All')]")
                 )
         );

         WebElement validButton = wait.until(
                 ExpectedConditions.visibilityOfElementLocated(
                         By.xpath("//button[contains(@class,'filter-button') and contains(.,'Valid')]")
                 )
         );

         WebElement invalidButton = wait.until(
                 ExpectedConditions.visibilityOfElementLocated(
                         By.xpath("//button[contains(@class,'filter-button') and contains(.,'Invalid')]")
                 )
         );

         // Get text from buttons
         String allText = allButton.getText().trim();         // Example: All (2)
         String validText = validButton.getText().trim();     // Example: Valid (2)
         String invalidText = invalidButton.getText().trim(); // Example: Invalid (0)

         // Extract numeric values
         int actualAllCount = extractCount(allText);
         int actualValidCount = extractCount(validText);
         int actualInvalidCount = extractCount(invalidText);

         // Print extracted counts
         System.out.println("========================================");
         System.out.println("Bulk Upload Summary");
         System.out.println("All Records     : " + actualAllCount);
         System.out.println("Valid Records   : " + actualValidCount);
         System.out.println("Invalid Records : " + actualInvalidCount);
         System.out.println("========================================");

         // Validate count logic only
         if (actualValidCount + actualInvalidCount != actualAllCount) {
             throw new RuntimeException(
                     "Count validation FAILED. " +
                     "All = " + actualAllCount +
                     ", Valid = " + actualValidCount +
                     ", Invalid = " + actualInvalidCount
             );
         }

         // Display result
         if (actualInvalidCount == 0) {
             System.out.println("All uploaded records are valid.");
         } else {
             System.out.println(actualInvalidCount + " record(s) are invalid.");
         }

         System.out.println("Bulk Upload count validation PASSED.");

      
         // ================= CLICK SUBMIT BUTTON =================

         // Wait until the Submit button becomes enabled
         WebElement submitButton = wait.until(webDriver -> {
             WebElement element = webDriver.findElement(By.id("submit"));
             return element.isEnabled() ? element : null;
         });

         // Scroll the button into view
         js.executeScript(
                 "arguments[0].scrollIntoView({block:'center'});",
                 submitButton
         );

         // Small wait to ensure UI updates
         Thread.sleep(1000);

         // Click using JavaScript
         js.executeScript("arguments[0].click();", submitButton);

         System.out.println("Submit button clicked successfully.");
         Thread.sleep(3000);
         
         
      // ================= DOWNLOAD LOG FILE =================

      // Locate the download icon using CSS selector
      // (By.className cannot be used with multiple class names)
      WebElement downloadIcon = wait.until(
              ExpectedConditions.elementToBeClickable(
                      By.cssSelector(".crm__icon.crm__download__blue")
              )
      );

      // Scroll into view
      js.executeScript(
              "arguments[0].scrollIntoView({block:'center'});",
              downloadIcon
      );

      // Click using JavaScript
      js.executeScript("arguments[0].click();", downloadIcon);

      System.out.println("Log file downloaded successfully.");
      
      Thread.sleep(5000);
            
        } finally {
            // ================= CLOSE BROWSER =================
            driver.quit();
        }
    }
    
 // ================= HELPER METHOD =================
 // Add this method OUTSIDE main(), but INSIDE CRM_IRF_BulkUpload class.
 public static int extractCount(String text) {
     // Examples:
     // "All (2)"      -> 2
     // "Valid (2)"    -> 2
     // "Invalid (0)"  -> 0

     java.util.regex.Matcher matcher =
             java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(text);

     if (matcher.find()) {
         return Integer.parseInt(matcher.group(1));
     }

     return 0;
 }

    // ================= DB METHOD TO FETCH OTP =================
    public static String getOTPFromDB(String mobile) {

        String otp = "";

        try {
            Connection con = DriverManager.getConnection(
                    "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev",
                    "hw_goutham",
                    "9qIE0mwg8ehN"
            );

            Statement stmt = con.createStatement();

            String query = "SELECT otp FROM otp " +
                    "WHERE mobile_number = '" + mobile + "' " +
                    "ORDER BY create_time DESC " +
                    "LIMIT 1";

            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                otp = rs.getString("otp");
            }

            rs.close();
            stmt.close();
            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return otp;
    }
}
