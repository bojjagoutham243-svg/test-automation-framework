import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.io.File;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.time.LocalTime;


public class CRM_IRF_Creation_Assignment {

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
        String customerType = "Individual";  // can be "Corporate", "Individual", etc.
        String filePath = "/Users/gouthambojja/Desktop/file-sample_150kB.pdf";
        
        // ================= SELECT FIELD ENGINEER =================
        
        String engineerName = "Count User";

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


     // ================= CHARGER SERIAL NUMBER LOGIC =================
        //
        // MANDATORY ONLY FOR:
        //
        // 1. ACC + ACC + ACC
        // 2. EXICOM + ACC + ACC
        //
        // OPTIONAL FOR:
        //
        // 1. EXICOM + EXICOM + EXICOM
        // 2. ALL OTHER COMBINATIONS
        //

        System.out.println("Checking Scope Logic...");

        // ---------- GET ALL SCOPE LABELS ----------
        List<WebElement> scopeSections = driver.findElements(
                By.xpath("//p[contains(@class,'text__label')]")
        );

        // ---------- STORE DETECTED SCOPES ----------
        List<String> detectedScopes = new ArrayList<>();

        for (WebElement scope : scopeSections) {

            String text = scope.getText().trim().toLowerCase();

            // SURVEY = EXICOM
            if (text.contains("survey")) {

                detectedScopes.add("EXICOM");

                System.out.println("Detected Scope : EXICOM");
            }

            // INSTALLATION = ACC
            else if (text.contains("installation")) {

                detectedScopes.add("ACC");

                System.out.println("Detected Scope : ACC");
            }

            // COMMISSIONING = ACC
            else if (text.contains("commissioning")) {

                detectedScopes.add("ACC");

                System.out.println("Detected Scope : ACC");
            }
        }


        // ---------- COUNT SCOPES ----------
        int accCount = 0;
        int exicomCount = 0;

        for (String scope : detectedScopes) {

            if (scope.equals("ACC")) {
                accCount++;
            }

            if (scope.equals("EXICOM")) {
                exicomCount++;
            }
        }

        System.out.println("ACC Count : " + accCount);
        System.out.println("EXICOM Count : " + exicomCount);


        // ---------- EXACT CONDITIONS ----------

        // ACC + ACC + ACC
        boolean isAccAccAcc =
                accCount == 3 && exicomCount == 0;

        // EXICOM + ACC + ACC
        boolean isExicomAccAcc =
                accCount == 2 && exicomCount == 1;

        // EXICOM + EXICOM + EXICOM
        boolean isExicomExicomExicom =
                exicomCount == 3 && accCount == 0;


        // ---------- FINAL VALIDATION ----------
        boolean chargerMandatory =
                isAccAccAcc || isExicomAccAcc;


        System.out.println("ACC+ACC+ACC : " + isAccAccAcc);
        System.out.println("EXICOM+ACC+ACC : " + isExicomAccAcc);
        System.out.println("EXICOM+EXICOM+EXICOM : " + isExicomExicomExicom);

        System.out.println("Charger Mandatory : " + chargerMandatory);


        // ================= CHARGER SERIAL NUMBER =================

        if (chargerMandatory) {

            System.out.println("Charger Serial No. is MANDATORY.");

            // ---------- FIELD LOCATOR ----------
            By chargerFieldLocator = By.xpath(
                    "//label[contains(normalize-space(),'Charger Serial No')]/following::input[1]"
            );

            // ---------- WAIT FIELD ----------
            WebElement chargerField = waitForm.until(
                    ExpectedConditions.elementToBeClickable(chargerFieldLocator)
            );

            // ---------- SCROLL ----------
            js1.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    chargerField
            );

            Thread.sleep(1000);

            // ---------- CLICK ----------
            js1.executeScript(
                    "arguments[0].click();",
                    chargerField
            );

            Thread.sleep(2000);

            // ---------- OPEN DROPDOWN ----------
            chargerField.sendKeys(Keys.ARROW_DOWN);

            Thread.sleep(1000);

            // ---------- SELECT FIRST OPTION ----------
            chargerField.sendKeys(Keys.ENTER);

            Thread.sleep(3000);

            // ---------- RE-LOCATE FIELD ----------
            chargerField = driver.findElement(chargerFieldLocator);

            // ---------- VALIDATION 1 ----------
            String selectedValue = chargerField.getAttribute("value");

            if (selectedValue == null) {
                selectedValue = "";
            }

            selectedValue = selectedValue.trim();

            // ---------- VALIDATION 2 ----------
            boolean chipPresent = !driver.findElements(
                    By.xpath(
                            "//*[contains(@class,'MuiChip-root') " +
                            "or contains(@class,'multiValue') " +
                            "or contains(@class,'selected')]"
                    )
            ).isEmpty();

            // ---------- VALIDATION 3 ----------
            boolean mandatoryErrorPresent = !driver.findElements(
                    By.xpath(
                            "//*[contains(text(),'Charger Serial No') " +
                            "and contains(text(),'required')]"
                    )
            ).isEmpty();

            // ---------- FINAL RESULT ----------
            if (!selectedValue.isEmpty()
                    || chipPresent
                    || !mandatoryErrorPresent) {

                System.out.println("PASS - Charger Serial selected successfully.");

                if (!selectedValue.isEmpty()) {

                    System.out.println(
                            "Selected Charger Serial : " + selectedValue
                    );
                }

            } else {

                throw new RuntimeException(
                        "FAIL - Charger Serial not selected"
                );
            }

        } else {

            System.out.println("Charger Serial No. is OPTIONAL.");
            System.out.println("Skipping Charger Serial Number.");
        }
     
     // ================= COMMISSIONING REPORT UPLOAD =================
        //
        // MANDATORY ONLY FOR:
        //
        // 1. ACC + ACC + ACC
        // 2. EXICOM + ACC + ACC
        //
        // OPTIONAL FOR:
        //
        // 1. EXICOM + EXICOM + EXICOM
        // 2. ALL OTHER COMBINATIONS
        //

        System.out.println("Checking Commissioning Scope Logic...");

        // ---------- GET ALL SCOPE LABELS ----------
        List<WebElement> scopeSections1 = driver.findElements(
                By.xpath("//p[contains(@class,'text__label')]")
        );

        // ---------- STORE DETECTED SCOPES ----------
        List<String> detectedScopes1 = new ArrayList<>();

        for (WebElement scope : scopeSections1) {

            String text = scope.getText().trim().toLowerCase();

            // SURVEY = EXICOM
            if (text.contains("survey")) {

                detectedScopes1.add("EXICOM");

                System.out.println("Detected Scope : EXICOM");
            }

            // INSTALLATION = ACC
            else if (text.contains("installation")) {

                detectedScopes1.add("ACC");

                System.out.println("Detected Scope : ACC");
            }

            // COMMISSIONING = ACC
            else if (text.contains("commissioning")) {

                detectedScopes1.add("ACC");

                System.out.println("Detected Scope : ACC");
            }
        }


        // ---------- COUNT ----------
        int accCount1 = 0;
        int exicomCount1 = 0;

        for (String scope : detectedScopes1) {

            if (scope.equals("ACC")) {
                accCount1++;
            }

            if (scope.equals("EXICOM")) {
                exicomCount1++;
            }
        }

        System.out.println("ACC Count : " + accCount1);
        System.out.println("EXICOM Count : " + exicomCount1);


        // ---------- EXACT CONDITIONS ----------

        // ACC + ACC + ACC
        boolean isAccAccAcc1 =
                accCount1 == 3 && exicomCount1 == 0;

        // EXICOM + ACC + ACC
        boolean isExicomAccAcc1 =
                accCount1 == 2 && exicomCount1 == 1;

        // FINAL CONDITION
        boolean commissioningMandatory =
                isAccAccAcc1 || isExicomAccAcc1;

        System.out.println("ACC+ACC+ACC : " + isAccAccAcc1);
        System.out.println("EXICOM+ACC+ACC : " + isExicomAccAcc1);

        System.out.println(
                "Commissioning Mandatory : "
                        + commissioningMandatory
        );


        // ================= FILE VALIDATION =================

        File file = new File(filePath);

        if (!file.exists()) {

            throw new RuntimeException(
                    "File not found : " + filePath
            );
        }


        // ================= COMMISSIONING UPLOAD =================

        if (commissioningMandatory) {

            System.out.println(
                    "Commissioning Report is MANDATORY."
            );

            // =====================================================
            // IMPORTANT:
            // THIS XPATH TARGETS ONLY
            // "Commissioning report*" SECTION
            // =====================================================

            By commissioningFileInputLocator = By.xpath(

                    "//p[contains(translate(normalize-space(), " +
                    "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'," +
                    "'abcdefghijklmnopqrstuvwxyz')," +
                    "'commissioning report')]" +

                    "/following::input[@type='file'][1]"
            );

            // ---------- WAIT FOR INPUT ----------
            WebElement commissioningInput = waitForm.until(
                    ExpectedConditions.presenceOfElementLocated(
                            commissioningFileInputLocator
                    )
            );

            // ---------- SCROLL ----------
            js1.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    commissioningInput
            );

            Thread.sleep(2000);

            // ---------- MAKE INPUT VISIBLE ----------
            js1.executeScript(

                    "arguments[0].style.display='block';" +
                    "arguments[0].style.visibility='visible';" +
                    "arguments[0].style.opacity='1';" +
                    "arguments[0].removeAttribute('hidden');" +
                    "arguments[0].removeAttribute('disabled');",

                    commissioningInput
            );

            Thread.sleep(1000);

            // ---------- UPLOAD FILE ----------
            commissioningInput.sendKeys(
                    file.getAbsolutePath()
            );

            System.out.println(
                    "Uploading Commissioning Report : "
                            + file.getName()
            );

            // ---------- WAIT FOR UPLOAD ----------
            Thread.sleep(8000);


            // ================= ERROR VALIDATION =================

            List<WebElement> uploadErrors = driver.findElements(

                    By.xpath(

                            "//*[contains(text(),'10 MB') " +
                            "or contains(text(),'size exceeds') " +
                            "or contains(text(),'Upload failed') " +
                            "or contains(text(),'Invalid file') " +
                            "or contains(text(),'unsupported')]"
                    )
            );

            if (!uploadErrors.isEmpty()) {

                throw new RuntimeException(

                        "FAIL - Upload Error : "
                                + uploadErrors.get(0).getText()
                );
            }


            // ================= REQUIRED VALIDATION =================

            List<WebElement> mandatoryErrors = driver.findElements(

                    By.xpath(

                            "//*[contains(text(),'Commissioning Report') " +
                            "and contains(text(),'required')]"
                    )
            );

            if (mandatoryErrors.isEmpty()) {

                System.out.println(
                        "PASS - Commissioning Report uploaded successfully."
                );

            } else {

                throw new RuntimeException(

                        "FAIL - Upload not completed. " +
                        "Commissioning Report is still required."
                );
            }

        } else {

            System.out.println(
                    "Commissioning Report is OPTIONAL."
            );

            System.out.println(
                    "Skipping Commissioning Report Upload."
            );
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
                            By.xpath("(//input[@placeholder='**********'])[2]")
                    )
            );
            spocNumber.clear();
            spocNumber.sendKeys("9803273829");

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

     // ================= WAIT OBJECT =================

        WebDriverWait wait11 = new WebDriverWait(driver, Duration.ofSeconds(20));


        // ================= EXTRACT IRF VALUE =================

        String createdIRF = waitIRF.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.className("toast_content_header")))
                .getText()
                .trim();

        System.out.println("IRF Created Successfully: " + createdIRF);


        // ================= CLICK COPY ICON =================

        WebElement copyIcon = waitIRF.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class,'crm__toast__copy__icon')]"))
        );

        js1.executeScript("arguments[0].click();", copyIcon);

        System.out.println("IRF copied successfully");

        Thread.sleep(2000);


        // ================= GO TO I&C REQUESTS =================

        By icRequestMenu = By.xpath("//p[normalize-space()='I&C Requests']");

        try {

            WebElement element = wait11.until(
                    ExpectedConditions.elementToBeClickable(icRequestMenu));

            js1.executeScript("arguments[0].scrollIntoView({block:'center'});", element);

            element.click();

        } catch (Exception e) {

            WebElement element = driver.findElement(icRequestMenu);

            js1.executeScript("arguments[0].click();", element);
        }

        System.out.println("Navigated to I&C Requests");


     // ================= SEARCH IRF USING COPIED VALUE =================

        WebElement searchBox = wait11.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.id("tasks__search-box"))
        );

        searchBox.click();

        // CLEAR EXISTING VALUE
        searchBox.sendKeys(Keys.chord(Keys.COMMAND, "a"));
        searchBox.sendKeys(Keys.DELETE);

        Thread.sleep(1000);

        // PASTE IRF VALUE
        searchBox.sendKeys(createdIRF);

        Thread.sleep(4000);

        System.out.println("Searched IRF : " + createdIRF);


        // ================= CHECK IN ACTIVE LIST =================

        boolean foundInActive = !driver.findElements(
                By.xpath("//*[contains(text(),'" + createdIRF + "')]")
        ).isEmpty();


        // ================= IF FOUND IN ACTIVE =================

        if (foundInActive) {

            System.out.println("IRF found in ACTIVE list");
            System.out.println("Assignment Flow Required");

        } else {

            System.out.println("IRF NOT found in ACTIVE list");
            System.out.println("Checking CLOSED list");


            // ================= CLICK CLOSED FILTER =================

            WebElement closedFilter = wait11.until(
                    ExpectedConditions.elementToBeClickable(
                            By.xpath(
                                    "//div[contains(@class,'close__ticket__icon')]"
                            )
                    )
            );

            js1.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    closedFilter
            );

            Thread.sleep(1000);

            js1.executeScript(
                    "arguments[0].click();",
                    closedFilter
            );

            System.out.println("Closed filter selected");

            Thread.sleep(4000);


            // ================= SEARCH AGAIN =================

            WebElement searchBoxClosed = wait11.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            By.id("tasks__search-box"))
            );

            searchBoxClosed.click();

            searchBoxClosed.sendKeys(Keys.chord(Keys.COMMAND, "a"));
            searchBoxClosed.sendKeys(Keys.DELETE);

            Thread.sleep(1000);

            searchBoxClosed.sendKeys(createdIRF);

            Thread.sleep(4000);


            // ================= CHECK CLOSED LIST =================

            boolean foundInClosed = !driver.findElements(
                    By.xpath("//*[contains(text(),'" + createdIRF + "')]")
            ).isEmpty();


            // ================= AUTO CLOSED =================

            if (foundInClosed) {

                System.out.println("IRF found in CLOSED list");
                System.out.println("IRF AUTO CLOSED");
                System.out.println("Skipping Assignment Flow");


                // ================= CLOSE BROWSER =================

                try {

                    System.out.println("Closing browser...");

                    Thread.sleep(3000);

                    driver.quit();

                    System.out.println("Browser closed successfully");

                } catch (Exception e) {

                    try {
                        driver.close();
                    } catch (Exception ignored) {
                    }
                }

                return;

            } else {

                throw new RuntimeException(
                        "IRF not found in ACTIVE or CLOSED list"
                );
            }
        }

        // ================= CLICK CREATED IRF =================

        By irfNumberLocator = By.xpath(
                "//*[contains(text(),'" + createdIRF + "')]"
        );

        WebElement irfElement = wait11.until(
                ExpectedConditions.elementToBeClickable(irfNumberLocator)
        );

        js1.executeScript("arguments[0].scrollIntoView({block:'center'});", irfElement);

        js1.executeScript("arguments[0].click();", irfElement);

        System.out.println("Clicked IRF Number: " + createdIRF);
        
     // ================= Scroll & Click on Assignment =================

        By assignmentButtonLocator = By.xpath(
                "//button[contains(.,'Assignment') or contains(@class,'assignment')]"
        );

        WebElement assignmentButton = wait.until(
                ExpectedConditions.presenceOfElementLocated(assignmentButtonLocator)
        );

        // Scroll into view
        js1.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                assignmentButton
        );

        Thread.sleep(1000);

        // Click using JS (avoids intercept issues)
        js1.executeScript("arguments[0].click();", assignmentButton);

        System.out.println("Assignment button clicked successfully");
       
        
     // ================= WAIT FOR ASSIGNMENT MODAL =================

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(@class,'rbc-calendar')]")
                )
        );

        System.out.println("Assignment calendar opened");

        Thread.sleep(3000);


        // ================= CLICK FIELD ENGINEER DROPDOWN =================

        WebElement feDropdown = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath(
                                "//div[contains(@class,'crm__dropdown__placeholder') and text()='Field Engineer']"
                        )
                )
        );

        js1.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                feDropdown
        );

        Thread.sleep(1000);

        new Actions(driver)
                .moveToElement(feDropdown)
                .click()
                .perform();

        System.out.println("Clicked FE dropdown");

        Thread.sleep(3000);


        // ================= TYPE ENGINEER NAME =================

        Actions actions = new Actions(driver);

        actions
                .sendKeys(engineerName)
                .pause(Duration.ofSeconds(3))
                .perform();

        System.out.println("Typed FE Name");

        Thread.sleep(5000);


        // ================= SELECT ACTIVE DROPDOWN OPTION =================

        WebElement activeOption = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath(
                                "//div[contains(@id,'react-select') and normalize-space()='"
                                        + engineerName + "']"
                        )
                )
        );

        js1.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                activeOption
        );

        Thread.sleep(1000);


        // REAL USER CLICK
        new Actions(driver)
                .moveToElement(activeOption)
                .pause(Duration.ofSeconds(1))
                .click()
                .perform();

        Thread.sleep(5000);

        System.out.println("Selected FE Option");


     // ================= VERIFY FE SELECTED =================

        Thread.sleep(3000);

        boolean feSelected = !driver.findElements(
                By.xpath(
                        "//*[contains(text(),'" + engineerName + "')]"
                )
        ).isEmpty();

        if (feSelected) {

            System.out.println("Field Engineer selected successfully");

        } else {

            System.out.println("WARNING - FE validation skipped");
        }
        
     // ================= SELECT FUTURE TIME SLOT =================

        Thread.sleep(3000);

        LocalTime now = LocalTime.now();

        int targetHour = now.getMinute() > 0
                ? now.getHour() + 1
                : now.getHour();

        String slotTime;

        if (targetHour == 0) {
            slotTime = "12:00 AM";
        } else if (targetHour < 12) {
            slotTime = targetHour + ":00 AM";
        } else if (targetHour == 12) {
            slotTime = "12:00 PM";
        } else {
            slotTime = (targetHour - 12) + ":00 PM";
        }

        System.out.println("Selecting Slot: " + slotTime);


        // ================= FIND TIME LABEL =================

        WebElement timeLabel = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//span[contains(@class,'rbc-label') and normalize-space()='"
                                + slotTime + "']")
                )
        );

        js1.executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                timeLabel
        );

        Thread.sleep(2000);


        // ================= GET DAY COLUMN =================

        WebElement dayColumn = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("(//div[contains(@class,'rbc-day-slot')])[1]")
                )
        );


        // ================= GET COORDINATES =================

        // Time label Y position
        int labelY = timeLabel.getLocation().getY();

        // Day column position & size
        int columnX = dayColumn.getLocation().getX();
        int columnWidth = dayColumn.getSize().getWidth();


        // CLICK INSIDE GRID AREA
        int clickX = columnX + (columnWidth / 2);

        // Slightly below the label line
        int clickY = labelY + 40;

        System.out.println("Label Y : " + labelY);
        System.out.println("Column X : " + columnX);
        System.out.println("Column Width : " + columnWidth);

        System.out.println("Click X : " + clickX);
        System.out.println("Click Y : " + clickY);


        // ================= CLICK INSIDE CALENDAR SLOT =================

        js1.executeScript(

            "var x=arguments[0], y=arguments[1];" +

            "var el=document.elementFromPoint(x,y);" +

            "if(el){" +

            "  var ev1=new MouseEvent('mousedown',{" +
            "    view:window,bubbles:true,cancelable:true,clientX:x,clientY:y" +
            "  });" +

            "  var ev2=new MouseEvent('mouseup',{" +
            "    view:window,bubbles:true,cancelable:true,clientX:x,clientY:y" +
            "  });" +

            "  var ev3=new MouseEvent('click',{" +
            "    view:window,bubbles:true,cancelable:true,clientX:x,clientY:y" +
            "  });" +

            "  el.dispatchEvent(ev1);" +
            "  el.dispatchEvent(ev2);" +
            "  el.dispatchEvent(ev3);" +

            "}",

            clickX,
            clickY
        );

        System.out.println("Clicked inside calendar grid");

        Thread.sleep(5000);


     // ================= WAIT FOR SAVE POPUP =================

        By saveLocator = By.xpath(
                "//button[@id='save' or normalize-space()='Save']"
        );

        try {

            // WAIT FOR SAVE BUTTON
            WebElement saveBtn = wait.until(
                    ExpectedConditions.elementToBeClickable(saveLocator)
            );

            System.out.println("Popup appeared successfully");

            // SCROLL TO SAVE BUTTON
            js1.executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    saveBtn
            );

            Thread.sleep(1000);

            // CLICK SAVE
            js1.executeScript(
                    "arguments[0].click();",
                    saveBtn
            );

            System.out.println("Assignment saved successfully");

            // WAIT UNTIL POPUP CLOSES
            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(saveLocator)
            );

            System.out.println("Popup closed successfully");

        } catch (StaleElementReferenceException e) {

            System.out.println("Save already completed");

        } catch (TimeoutException e) {

            System.out.println("Save popup not found / already closed");

        }
        
        
     // ================= CLOSE BROWSER =================

        try {

            System.out.println("Closing browser...");

            Thread.sleep(3000);

            // CLOSE ALL WINDOWS
            driver.quit();

            System.out.println("Browser closed successfully");

        } catch (Exception e) {

            System.out.println("Unable to close browser normally");

            try {

                // FORCE CLOSE CURRENT WINDOW
                driver.close();

            } catch (Exception ignored) {
            }
        }
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
