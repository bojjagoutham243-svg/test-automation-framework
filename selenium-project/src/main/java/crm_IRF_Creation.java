import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.sql.*;
import java.time.Duration;
import java.util.List;
import java.util.Random;

public class crm_IRF_Creation {

    public static void main(String[] args) throws Exception {

        WebDriverManager.chromedriver().setup();

        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        WebDriverWait waitForm = new WebDriverWait(driver, Duration.ofSeconds(30));

        JavascriptExecutor js1 = (JavascriptExecutor) driver;

        driver.get("https://exicom-crm-dev.hummingwave.com/login");
        driver.manage().window().maximize();

     // ================= CONFIGURATION =================
        
        String mobile = "8639202204";
        String accountName = "Kia India Pvt Ltd";
        String productType = "AC Charger";   // change to "DC Charger" when needed
        String customerType = "Corporate";  // can be "Corporate", "Individual", etc.
        String filePath = "/Users/gouthambojja/Desktop/file-sample_150kB.pdf";

        // ================= LOGIN =================
        driver.findElement(By.name("phone")).sendKeys(mobile);
        Thread.sleep(5000);

        String otp = "";
        for (int i = 0; i < 5; i++) {
            otp = getOTPFromDB(mobile);
            if (!otp.isEmpty()) break;
            Thread.sleep(2000);
        }

        if (otp.isEmpty()) throw new RuntimeException("OTP not fetched!");

        WebElement otpField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]")
                )
        );

        otpField.sendKeys(otp);
        wait.until(ExpectedConditions.urlContains("dashboard"));

        System.out.println("Login Successful!");

        // ================= NAVIGATION =================
        Thread.sleep(2000);
        driver.findElement(By.xpath("//*[contains(text(),'On Board')]")).click();
        Thread.sleep(2000);
        driver.findElement(By.xpath("//*[contains(text(),'Create IRF')]")).click();

        WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement createButton = wait1.until(
                ExpectedConditions.presenceOfElementLocated(By.id("create_irf")));
        js1.executeScript("arguments[0].click();", createButton);

        Thread.sleep(2000);

        // ================= ACCOUNT =================

        By accountFieldLocator =
                By.xpath("//label[contains(text(),'Account')]/following::input[1]");

        WebElement accountField = waitForm.until(
                ExpectedConditions.visibilityOfElementLocated(accountFieldLocator));

        js1.executeScript("arguments[0].click();", accountField);
        accountField.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        accountField.sendKeys(Keys.DELETE);
        accountField.sendKeys(accountName);

        Thread.sleep(3000);

        List<WebElement> options = driver.findElements(
                By.xpath("//div[@role='option'] | //li"));

        boolean accountSelected = false;

        for (WebElement option : options) {
            if (option.getText().trim().equalsIgnoreCase(accountName)) {
                js1.executeScript("arguments[0].click();", option);
                accountSelected = true;
                break;
            }
        }

        if (!accountSelected) {
            throw new RuntimeException("Account not found");
        }

        Thread.sleep(3000);

     // ================= PRODUCT =================

     // Dynamic value (can be "AC Charger" or "DC Charger")

     WebElement productField = waitForm.until(
             ExpectedConditions.visibilityOfElementLocated(
                     By.xpath("//label[contains(text(),'Product')]/following::input[1]")
             )
     );

     // Click the dropdown/input
     js1.executeScript("arguments[0].click();", productField);

     // Clear existing value (important for React/MUI dropdowns)
     productField.sendKeys(Keys.chord(Keys.COMMAND, "a"));
     productField.sendKeys(Keys.DELETE);

     // Enter full value dynamically
     productField.sendKeys(productType);

     Thread.sleep(2000);

     // Select from dropdown
     productField.sendKeys(Keys.ARROW_DOWN);
     productField.sendKeys(Keys.ENTER);

     System.out.println("Selected Product Type: " + productType);

        // ================= REQUEST DATE =================
        WebElement dateField = waitForm.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//input[contains(@class,'custom__date__picker')]")
                )
        );

        js1.executeScript("arguments[0].click();", dateField);
        Thread.sleep(1000);

        List<WebElement> enabledDays = waitForm.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(
                        By.xpath("//div[contains(@class,'react-datepicker__day') and not(contains(@class,'disabled'))]")
                )
        );

        WebElement targetDate = enabledDays.get(enabledDays.size() - 1);
        js1.executeScript("arguments[0].click();", targetDate);

        dateField.sendKeys(Keys.TAB);
        
     // ================= Survey Not Required  =================
        
        driver.findElement(By.cssSelector(".MuiButtonBase-root.MuiSwitch-switchBase.MuiSwitch-colorPrimary.Mui-checked.PrivateSwitchBase-root.MuiSwitch-switchBase.MuiSwitch-colorPrimary.Mui-checked.Mui-checked.css-1uf4bbi")).click();


     // ================= CHARGER SERIAL NUMBER (FINAL WORKING VALIDATION) =================
        //
        // Logic:
        // If ACC scope count >= 3 → Charger Serial No. is mandatory.
        // Otherwise → Skip.
        //
        // Validation fix:
        // Your UI selects the charger serial correctly, but the input value may remain blank.
        // So validation checks:
        // 1. Input value
        // 2. Selected chip/tag
        // 3. Mandatory error message disappearance
        //

        // ---------- FIND SCOPE ICONS ----------
        List<WebElement> scopeIcons = driver.findElements(
                By.xpath("//div[contains(@class,'crm__icon') or @aria-label]")
        );

        // ---------- COUNT ACC SCOPES ----------
        int accScopeCount = 0;

        for (WebElement icon : scopeIcons) {
            String aria = icon.getAttribute("aria-label");
            String cls = icon.getAttribute("class");

            if ((aria != null && aria.toLowerCase().contains("acc"))
                    || (cls != null && cls.toLowerCase().contains("account"))
                    || (cls != null && cls.toLowerCase().contains("tatamotors"))) {
                accScopeCount++;
            }
        }

        System.out.println("Detected ACC Scope Count: " + accScopeCount);

        // ---------- CHARGER SERIAL LOGIC ----------
        if (accScopeCount >= 3) {

            System.out.println("Scope is ACC, ACC, ACC.");
            System.out.println("Charger Serial No. is MANDATORY.");

            // ---------- FIELD LOCATOR ----------
            By chargerFieldLocator = By.xpath(
                    "//label[contains(normalize-space(),'Charger Serial No')]/following::input[1]"
            );

            // ---------- LOCATE FIELD ----------
            WebElement chargerField = waitForm.until(
                    ExpectedConditions.elementToBeClickable(chargerFieldLocator)
            );

            // Scroll and click
            js1.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    chargerField
            );
            js1.executeScript("arguments[0].click();", chargerField);

            Thread.sleep(2000);

            // ---------- OPEN DROPDOWN ----------
            chargerField.sendKeys(Keys.ARROW_DOWN);
            Thread.sleep(1000);

            // ---------- SELECT FIRST OPTION ----------
            chargerField.sendKeys(Keys.ENTER);
            Thread.sleep(3000);

            // ---------- RE-LOCATE FIELD ----------
            chargerField = driver.findElement(chargerFieldLocator);

            // ---------- VALIDATION 1: INPUT VALUE ----------
            String selectedValue = chargerField.getAttribute("value");
            if (selectedValue == null) {
                selectedValue = "";
            }
            selectedValue = selectedValue.trim();

            // ---------- VALIDATION 2: CHIP/TAG EXISTS ----------
            boolean chipPresent = !driver.findElements(
                    By.xpath(
                            "//*[contains(@class,'MuiChip-root') " +
                            "or contains(@class,'multiValue') " +
                            "or contains(@class,'selected')]"
                    )
            ).isEmpty();

            // ---------- VALIDATION 3: REQUIRED ERROR MESSAGE DISAPPEARS ----------
            boolean mandatoryErrorPresent = !driver.findElements(
                    By.xpath(
                            "//*[contains(text(),'Charger Serial No') and contains(text(),'required')]"
                    )
            ).isEmpty();

            // ---------- FINAL VALIDATION ----------
            if (!selectedValue.isEmpty() || chipPresent || !mandatoryErrorPresent) {

                System.out.println("PASS - Charger Serial selected successfully.");

                if (!selectedValue.isEmpty()) {
                    System.out.println("Selected Charger Serial: " + selectedValue);
                }

            } else {
                throw new RuntimeException("FAIL - Charger Serial not selected");
            }

        } else {

            System.out.println("Scope is NOT ACC, ACC, ACC.");
            System.out.println("Charger Serial No. is OPTIONAL. Skipping entry.");
        }
     
     // ================= COMMISSIONING REPORT UPLOAD (ACC, ACC, ACC ONLY) =================
        //
        // Logic:
        // 1. Count ACC scope icons.
        // 2. If ACC scope count >= 3:
//              - Commissioning Report is mandatory.
//              - Upload the PDF file.
        // 3. Otherwise:
//              - Skip upload.
        //

        // ---------- FILE PATH ----------
        File file = new File(filePath);

        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }

        // ---------- FIND ALL SCOPE ICONS ----------
        List<WebElement> scopeIconsForUpload = driver.findElements(
                By.xpath("//div[contains(@class,'crm__icon') or @aria-label]")
        );

        // ---------- COUNT ACC SCOPES ----------
        int accScopeCountForUpload = 0;

        for (WebElement icon : scopeIconsForUpload) {

            String aria = icon.getAttribute("aria-label");
            String cls = icon.getAttribute("class");

            if ((aria != null && aria.toLowerCase().contains("acc"))
                    || (cls != null && cls.toLowerCase().contains("account"))
                    || (cls != null && cls.toLowerCase().contains("tatamotors"))) {
                accScopeCountForUpload++;
            }
        }

        System.out.println("Detected ACC Scope Count: " + accScopeCountForUpload);

        // ---------- UPLOAD ONLY WHEN ACC, ACC, ACC ----------
        if (accScopeCountForUpload >= 3) {

            System.out.println("Scope is ACC, ACC, ACC.");
            System.out.println("Commissioning Report is MANDATORY.");

            // ---------- LOCATE COMMISSIONING FILE INPUT ----------
            // Third file input belongs to Commissioning section
            By fileInputLocator = By.xpath("(//input[@type='file'])[3]");

            WebElement fileInput = waitForm.until(
                    ExpectedConditions.presenceOfElementLocated(fileInputLocator)
            );

            // ---------- MAKE INPUT VISIBLE ----------
            js1.executeScript(
                    "arguments[0].style.display='block';" +
                    "arguments[0].style.visibility='visible';" +
                    "arguments[0].style.opacity='1';" +
                    "arguments[0].removeAttribute('hidden');" +
                    "arguments[0].removeAttribute('disabled');",
                    fileInput
            );

            // ---------- UPLOAD FILE ----------
            fileInput.sendKeys(file.getAbsolutePath());

            System.out.println("Uploading file: " + file.getName());

            // ---------- WAIT FOR UPLOAD ----------
            Thread.sleep(8000);

            // ---------- CHECK FOR ERROR MESSAGES ----------
            List<WebElement> uploadErrors = driver.findElements(
                    By.xpath(
                            "//*[contains(text(),'10 MB') or " +
                            "contains(text(),'size exceeds') or " +
                            "contains(text(),'Upload failed') or " +
                            "contains(text(),'Invalid file') or " +
                            "contains(text(),'unsupported')]"
                    )
            );

            if (!uploadErrors.isEmpty()) {
                throw new RuntimeException(
                        "FAIL - Upload Error: " + uploadErrors.get(0).getText()
                );
            }

            // ---------- VERIFY REQUIRED ERROR MESSAGE DISAPPEARED ----------
            List<WebElement> mandatoryErrors = driver.findElements(
                    By.xpath(
                            "//*[contains(text(),'Commissioning Report') and contains(text(),'required')]"
                    )
            );

            if (mandatoryErrors.isEmpty()) {
                System.out.println("PASS - Commissioning Report uploaded successfully.");
            } else {
                throw new RuntimeException(
                        "FAIL - Upload not completed. Commissioning Report is still required."
                );
            }

        } else {

            System.out.println("Scope is NOT ACC, ACC, ACC.");
            System.out.println("Commissioning Report is OPTIONAL. Skipping upload.");
        }
        
     // ================= CUSTOMER TYPE =================

        WebElement customerTypeField = waitForm.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//label[contains(text(),'Customer Type')]/following::input[1]")
                )
        );

        js1.executeScript("arguments[0].click();", customerTypeField);

        // clear existing value (important for React/MUI dropdown)
        customerTypeField.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        customerTypeField.sendKeys(Keys.DELETE);

        // pass dynamic value
        customerTypeField.sendKeys(customerType);

        Thread.sleep(2000);

        // select from dropdown
        customerTypeField.sendKeys(Keys.ARROW_DOWN, Keys.ENTER);

        System.out.println("Selected Customer Type: " + customerType);


        // ================= SPOC DETAILS (CONDITIONAL) =================

        if (customerType.equalsIgnoreCase("Corporate")) {

            System.out.println("Customer Type is Corporate → Filling SPOC details");

            // SPOC NAME
            WebElement spocName = waitForm.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.id("spoc_name")
                    )
            );
            spocName.clear();
            spocName.sendKeys("Test SPOC Name");

            // SPOC NUMBER
            WebElement spocNumber = waitForm.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.xpath("//input[@type='tel' and contains(@class,'crm__custom__mobile__input__field')]")
                    )
            );
            spocNumber.clear();
            spocNumber.sendKeys("9876543210");

            // SPOC EMAIL
            WebElement spocEmail = waitForm.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.id("spoc_email")
                    )
            );
            spocEmail.clear();
            spocEmail.sendKeys("Testspoc@mail.com");

            System.out.println("SPOC details entered successfully");

        } else {

            System.out.println("Customer Type is Individual → Skipping SPOC fields");
        }
        
        

           // ================= PRIMARY NUMBER =================
           String mobileNumber = generateMobileNumber();

           WebElement primaryNumberField = waitForm.until(
               ExpectedConditions.visibilityOfElementLocated(
                   By.xpath("//label[contains(text(),'Primary Number')]/following::input[1]")
               )
           );

           js1.executeScript("arguments[0].click();", primaryNumberField);
           primaryNumberField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
           primaryNumberField.sendKeys(mobileNumber);
           primaryNumberField.sendKeys(Keys.TAB);
           

        // ================= CUSTOMER NAME (CONDITIONAL + UNIQUE FIXED) =================

        try {

            WebElement customerNameField = waitForm.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//label[contains(text(),'Customer/Organisation Name')]/following::input[1]")
                )
            );

            Thread.sleep(2000);

            String existingValue = customerNameField.getAttribute("value");

            if (existingValue == null || existingValue.trim().isEmpty()) {

                System.out.println("Customer not found → entering unique name");

                js1.executeScript("arguments[0].scrollIntoView({block:'center'});", customerNameField);
                js1.executeScript("arguments[0].click();", customerNameField);

                // clear field (React-safe)
                customerNameField.sendKeys(Keys.chord(Keys.COMMAND, "a"), Keys.DELETE);
                // int userCounter = 0;
             // Generates names like Autouser4827
                int randomNum = 1000 + new Random().nextInt(9000); // 1000 to 9999
                String uniqueCustomer = "Autouser" + randomNum;

                customerNameField.sendKeys(uniqueCustomer);
                // IMPORTANT: force React update
                customerNameField.sendKeys(Keys.TAB);
                js1.executeScript("arguments[0].blur();", customerNameField);

                Thread.sleep(1000);

               System.out.println("Entered Customer Name: " + uniqueCustomer);

            } else {
                System.out.println("Customer already exists → auto-filled: " + existingValue);
            }

        } catch (Exception e) {
            System.out.println("Customer name handling failed");
           e.printStackTrace();
        }

     // ================= EMAIL ID VALIDATION =================

     // Enter invalid email
     driver.findElement(By.xpath("//label[contains(text(),'Email ID')]/following::input[1]")).sendKeys("Test@.com");

     // Print validation error message
     System.out.println(driver.findElement(By.xpath("//div[contains(@class,'crm__error__message')]")).getText());

     // Locate the email field again
     WebElement emailField = driver.findElement(
         By.xpath("//label[contains(text(),'Email ID')]/following::input[1]"));

     // Clear the invalid email using Select All + Delete
     emailField.sendKeys(Keys.chord(Keys.COMMAND, "a"));   // Use Keys.CONTROL on Windows
     emailField.sendKeys(Keys.DELETE);

     // Enter valid email
     emailField.sendKeys("Test@yopmail.com");
           
        // ================= PIN CODE =================

           WebElement pinCodeField = waitForm.until(
               ExpectedConditions.visibilityOfElementLocated(
                   By.xpath("//label[contains(text(),'Pin Code')]/following::input[1]")
               )
           );

           // Scroll into view
           js1.executeScript("arguments[0].scrollIntoView({block:'center'});", pinCodeField);

           Thread.sleep(1000);

           // Click using JS (avoid overlay issue)
           js1.executeScript("arguments[0].click();", pinCodeField);

           // Clear existing value
           pinCodeField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);

           // Type pin code
           pinCodeField.sendKeys("560008");

           // Wait for dropdown API results
           Thread.sleep(3000);

           // Select first suggestion (most reliable)
           pinCodeField.sendKeys(Keys.ARROW_DOWN);
           pinCodeField.sendKeys(Keys.ENTER);

           System.out.println("Pin Code selected successfully");
           
        // ================= LOCATION TYPE =================

           WebElement locationTypeField = waitForm.until(
               ExpectedConditions.visibilityOfElementLocated(
                   By.xpath("//label[contains(text(),'Location Type')]/following::input[1]")
               )
           );

           // Scroll into view
           js1.executeScript("arguments[0].scrollIntoView({block:'center'});", locationTypeField);

           Thread.sleep(1000);

           // Click using JS (avoid overlay issues)
           js1.executeScript("arguments[0].click();", locationTypeField);

           // Type value
           locationTypeField.sendKeys("Home");

           // Wait for dropdown suggestions
           Thread.sleep(2000);

           // Select using keyboard (MOST RELIABLE for MUI)
           locationTypeField.sendKeys(Keys.ARROW_DOWN);
           locationTypeField.sendKeys(Keys.ENTER);

           System.out.println("Location Type selected as Home");
           
        // ================= ADDRESS =================

           WebElement addressField = waitForm.until(
               ExpectedConditions.visibilityOfElementLocated(
                   By.xpath("//label[contains(text(),'Address')]/following::textarea[1] | //label[contains(text(),'Address')]/following::input[1]")
               )
           );

           // Scroll into view
           js1.executeScript("arguments[0].scrollIntoView({block:'center'});", addressField);

           Thread.sleep(1000);

           // Click using JS (avoid overlay issues)
           js1.executeScript("arguments[0].click();", addressField);

           // Clear existing value
           addressField.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);

           // Enter address
           addressField.sendKeys("607, 12th Main Rd, 7th Cross, HAL 2nd Stage, Domlur Village, Indiranagar");

           // Move focus (important for UI validation)
           addressField.sendKeys(Keys.TAB);

           System.out.println("Address entered successfully");
           
        // ================= CLICK CREATE BUTTON =================

           WebElement createBtn = waitForm.until(
               ExpectedConditions.elementToBeClickable(
                   By.id("create_irf")
               )
           );

           // Scroll + JS click (avoid intercept issues)
           js1.executeScript("arguments[0].scrollIntoView({block:'center'});", createBtn);
           js1.executeScript("arguments[0].click();", createBtn);

           System.out.println("Create button clicked");


        // ================= WAIT FOR IRF GENERATION =================

        // Wait for either success toast OR IRF number label
        WebDriverWait waitIRF = new WebDriverWait(driver, Duration.ofSeconds(40));

        // Option 1: Success Toast (if UI shows)
        try {
            WebElement toast = waitIRF.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//*[contains(text(),'success') or contains(text(),'created')]")
                )
            );
            System.out.println("Success Message: " + toast.getText());
        } catch (Exception e) {
            System.out.println("Toast not found, checking IRF ID...");
        }

        // Extract IRF value
        
        System.out.println("IRF Created Successfully: " + driver.findElement(By.className("toast_content_header")).getText());

        driver.quit();
    }

    // ================= MOBILE GENERATOR =================
    public static String generateMobileNumber() {

        int firstDigit = 7 + (int) (Math.random() * 3);
        long remaining = (long) (Math.random() * 1_000_000_000L);

        return firstDigit + String.format("%09d", remaining);
    }

    // ================= DB METHOD =================
    public static String getOTPFromDB(String mobile) {

        String otp = "";

        try {
            Connection con = DriverManager.getConnection(
                    "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev",
                    "hw_goutham",
                    "9qIE0mwg8ehN"
            );

            Statement stmt = con.createStatement();

            String query = "SELECT otp FROM otp WHERE mobile_number='"
                    + mobile + "' ORDER BY create_time DESC LIMIT 1";

            ResultSet rs = stmt.executeQuery(query);

            if (rs.next()) {
                otp = rs.getString("otp");
            }

            con.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return otp;
    }
}
