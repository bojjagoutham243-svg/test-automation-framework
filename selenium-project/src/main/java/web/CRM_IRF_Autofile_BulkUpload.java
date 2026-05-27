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

import org.apache.poi.ss.usermodel.*;
import java.io.*;

public class CRM_IRF_Autofile_BulkUpload {

    // ================= TEMPLATE PATHS =================
    private static final String INDIVIDUAL_TEMPLATE =
            "/Users/gouthambojja/Documents/AutoGenerate Template/Bulk IRF - Individual - Template.xlsx";

    private static final String CORPORATE_TEMPLATE =
            "/Users/gouthambojja/Documents/AutoGenerate Template/Bulk IRF - Corporate - Template.xlsx";

    private static final String OUTPUT_FOLDER =
            "/Users/gouthambojja/Documents/AutoGenerate Template/Generated Files/";

    public static void main(String[] args) throws Exception {

        // ================= CONFIGURATION =================
        String mobile = "8639202204";
        String chargerType = "AC";
        String accountName = "Kia India Pvt Ltd";
        String customerType = "Individual"; // Individual or Corporate
        String circleType = "Karnataka";
        int recordCount = 5;

        // ================= GENERATE EXCEL FILE =================
        String filePath = generateBulkIRFExcel(customerType, recordCount);
        System.out.println("Generated File: " + filePath);

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
            
            // ================= FILE UPLOAD =================
            File file = new File(filePath);
            
            System.out.println("Generated File Path: " + filePath);

            File generatedFile = new File(filePath);

            if (!generatedFile.exists()) {
                throw new RuntimeException("Generated file not found.");
            }

            System.out.println("Generated File Size: " + generatedFile.length() + " bytes");

            if (generatedFile.length() == 0) {
                throw new RuntimeException("Generated file is empty.");
            }

            WebElement fileInput = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            By.xpath("//input[@type='file']")
                    )
            );

            js.executeScript(
                    "arguments[0].style.display='block';" +
                            "arguments[0].style.visibility='visible';" +
                            "arguments[0].style.opacity='1';",
                    fileInput
            );

            fileInput.sendKeys(file.getAbsolutePath());
            System.out.println("File uploaded successfully.");

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

 // ============================================================================
 // REPLACE YOUR generateBulkIRFExcel() METHOD WITH THIS VERSION
 // ============================================================================
 // Template Structure:
 // Row 1 (index 0) -> Header Title
 // Row 2 (index 1) -> Column Names
 // Row 3 (index 2) -> First Data Row (Template Row)
 // Data generation should start from Row 3.
 // ============================================================================

 public static String generateBulkIRFExcel(String customerType, int recordCount) throws Exception {

     // ================= SELECT TEMPLATE =================
     String templatePath;

     if (customerType.equalsIgnoreCase("Individual")) {
         templatePath = INDIVIDUAL_TEMPLATE;
     } else if (customerType.equalsIgnoreCase("Corporate")) {
         templatePath = CORPORATE_TEMPLATE;
     } else {
         throw new RuntimeException("Invalid customer type: " + customerType);
     }

     File templateFile = new File(templatePath);

     if (!templateFile.exists()) {
         throw new RuntimeException("Template file not found: " + templatePath);
     }

     // ================= CREATE OUTPUT DIRECTORY =================
     File outputDir = new File(OUTPUT_FOLDER);
     if (!outputDir.exists()) {
         outputDir.mkdirs();
     }

     // ================= OPEN TEMPLATE =================
     FileInputStream fis = new FileInputStream(templateFile);
     Workbook workbook = WorkbookFactory.create(fis);
     Sheet sheet = workbook.getSheetAt(0);

     // =====================================================================
     // TEMPLATE DATA ROW = ROW 3 (Excel Row 3 => index 2)
     // =====================================================================
     Row templateRow = sheet.getRow(2);

     if (templateRow == null) {
         throw new RuntimeException("Template data row (Row 3) not found.");
     }

     // =====================================================================
     // REMOVE EXISTING DATA ROWS STARTING FROM ROW 4 (index 3)
     // KEEP:
     // Row 1 -> Header
     // Row 2 -> Column Names
     // Row 3 -> Template Data Row
     // =====================================================================
     int lastRow = sheet.getLastRowNum();

     if (lastRow > 2) {
         for (int i = lastRow; i >= 3; i--) {
             Row row = sheet.getRow(i);
             if (row != null) {
                 sheet.removeRow(row);
             }
         }
     }

     // =====================================================================
     // POPULATE FIRST RECORD INTO TEMPLATE ROW (ROW 3)
     // =====================================================================
     populateBulkIRFRow(templateRow, 1, customerType);

     // =====================================================================
     // CREATE ADDITIONAL RECORDS STARTING FROM ROW 4 (index 3)
     // =====================================================================
     for (int i = 2; i <= recordCount; i++) {

         // Row indexes:
         // Record 1 -> Row 3 -> index 2
         // Record 2 -> Row 4 -> index 3
         // Record 3 -> Row 5 -> index 4
         int rowIndex = i + 1;

         Row newRow = sheet.createRow(rowIndex);

         // Copy style, validations, formulas
         copyEntireRow1(workbook, templateRow, newRow);

         // Populate row data
         populateBulkIRFRow(newRow, i, customerType);
     }

     // Force formula recalculation
     workbook.setForceFormulaRecalculation(true);

     // ================= SAVE FILE =================
     String timestamp =
             new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                     .format(new java.util.Date());

     String outputPath =
             OUTPUT_FOLDER
                     + "Bulk_IRF_"
                     + customerType
                     + "_"
                     + recordCount
                     + "_Records_"
                     + timestamp
                     + ".xlsx";

     FileOutputStream fos = new FileOutputStream(outputPath);
     workbook.write(fos);

     fos.close();
     workbook.close();
     fis.close();

     // ================= VALIDATE OUTPUT FILE =================
     File generatedFile = new File(outputPath);

     if (!generatedFile.exists()) {
         throw new RuntimeException("Generated file not found.");
     }

     if (generatedFile.length() == 0) {
         throw new RuntimeException("Generated file is empty.");
     }

     System.out.println("========================================");
     System.out.println("Excel File Generated Successfully");
     System.out.println("Path : " + outputPath);
     System.out.println("Size : " + generatedFile.length() + " bytes");
     System.out.println("Records Generated : " + recordCount);
     System.out.println("Data Starts From : Row 3");
     System.out.println("========================================");

     return outputPath;
 }

//============================================================================
//REPLACE YOUR EXISTING populateBulkIRFRow() METHOD WITH THIS VERSION
//============================================================================
//This method supports BOTH templates:
//
//1. Individual Template
//2. Corporate Template
//
//Template selection is already handled by generateBulkIRFExcel()
//using customerType.
//
//This method populates columns dynamically based on customerType.
//============================================================================

private static void populateBulkIRFRow(Row row, int index, String customerType) {

  Workbook workbook = row.getSheet().getWorkbook();

  // =====================================================================
  // Column 0 : Reference ID
  // =====================================================================
  String referenceId = "REF" + System.currentTimeMillis() + index;
  setCellValue(row, 0, referenceId);

  // =====================================================================
  // Column 1 : Request Date (REAL DATE CELL)
  // =====================================================================
  Cell dateCell = row.getCell(1);
  if (dateCell == null) {
      dateCell = row.createCell(1);
  }

  dateCell.setCellValue(new java.util.Date());

  CellStyle dateStyle = workbook.createCellStyle();
  DataFormat dataFormat = workbook.createDataFormat();
  dateStyle.setDataFormat(dataFormat.getFormat("d/M/yyyy"));
  dateCell.setCellStyle(dateStyle);

  // =====================================================================
  // Column 2 : Charger Serial Number (Optional) - Leave Blank
  // =====================================================================

  // =====================================================================
  // Column 3 : Rating (in kW)
  // =====================================================================
  setCellValue(row, 3, "7.5");

  // =====================================================================
  // Column 4 : Survey Required
  // =====================================================================
  setCellValue(row, 4, "No");

  // =====================================================================
  // Column 5 : Customer/Organisation Name
  // =====================================================================
  String customerName = generateCustomerName();
  setCellValue(row, 5, customerName);

  // =====================================================================
  // Common Fields
  // =====================================================================
  setCellValue(row, 6, "+91");                        // Primary Country Code
  setCellValue(row, 7, generateRandomMobileNumber()); // Primary Number
  setCellValue(row, 8, "+91");                        // Alternate Country Code
  setCellValue(row, 9, generateRandomMobileNumber()); // Alternate Number
  setCellValue(row, 10, "test" + index + "@mail.com");

  // =====================================================================
  // CORPORATE TEMPLATE
  // =====================================================================
  if (customerType.equalsIgnoreCase("Corporate")) {

      // Column 11 : SPOC Name
      setCellValue(row, 11, "SPOC" + customerName);

      // Column 12 : SPOC Country Code
      setCellValue(row, 12, "+91");

      // Column 13 : SPOC Number
      setCellValue(row, 13, generateRandomMobileNumber());

      // Column 14 : SPOC Email
      setCellValue(row, 14, "TestSpoc@mail.com");

      // Column 15 : Location Type
      setCellValue(row, 15, "Commercial");

      // Column 16 : Site Pin Code
      setCellValue(row, 16, "560008");

      // Column 17 : Site Address
      setCellValue(row, 17, "SiteAddress" + index);

      // Column 18 : Site City
      setCellValue(row, 18, "Bengaluru");

      // Column 19 : Site State
      setCellValue(row, 19, "Karnataka");

      // Column 20 : Site Country
      setCellValue(row, 20, "India");

      // Column 21 : Site Geolocation
      setCellValue(row, 21, "12.9716,77.5946");

      // Optional columns
      // 22 Pin Code
      // 23 Address
      // 24 City
      // 25 State
      // 26 Country
  }

  // =====================================================================
  // INDIVIDUAL TEMPLATE
  // =====================================================================
  else {

      // Column 11 : Location Type
      setCellValue(row, 11, "Home");

      // Column 12 : Site Pin Code
      setCellValue(row, 12, "560008");

      // Column 13 : Site Address
      setCellValue(row, 13, "SiteAddress" + index);

      // Column 14 : Site City
      setCellValue(row, 14, "Bengaluru");

      // Column 15 : Site State
      setCellValue(row, 15, "Karnataka");

      // Column 16 : Site Country
      setCellValue(row, 16, "India");

      // Column 17 : Site Geolocation
      setCellValue(row, 17, "12.9716,77.5946");

      // Optional columns
      // 18 Pin Code
      // 19 Address
      // 20 City
      // 21 State
      // 22 Country
  }
}

//============================================================================
//ADD THIS METHOD BELOW populateBulkIRFRow()
//============================================================================

//============================================================================
//ADD THIS METHOD BELOW generateCustomerName()
//============================================================================
//Generates a random 10-digit Indian mobile number.
//Starts with 6, 7, 8, or 9.
//Examples:
//9876543210
//8123456789
//============================================================================

private static String generateRandomMobileNumber() {

 java.util.Random random = new java.util.Random();

 // First digit should be 6, 7, 8, or 9
 int firstDigit = 6 + random.nextInt(4);

 StringBuilder mobile = new StringBuilder();
 mobile.append(firstDigit);

 // Generate remaining 9 digits
 for (int i = 0; i < 9; i++) {
     mobile.append(random.nextInt(10));
 }

 return mobile.toString();
}

private static String generateCustomerName() {

    String[] validNames = {
            "Raju",
            "Ravi",
            "Kumar",
            "Suresh",
            "Mahesh",
            "Naresh",
            "Vijay",
            "Ajay",
            "Arun",
            "Kiran",
            "Gopal",
            "Prasad",
            "Ramesh",
            "Dinesh",
            "Ganesh",
            "Harish",
            "Lokesh",
            "Manoj",
            "Naveen",
            "Rajesh"
    };

    java.util.Random random = new java.util.Random();

    // Pick a random name
    String customerName =
            validNames[random.nextInt(validNames.length)];

    // Final safety cleanup:
    // Keep only A-Z and a-z
    customerName = customerName.replaceAll("[^A-Za-z]", "");

    // Ensure length <= 150 characters
    if (customerName.length() > 150) {
        customerName = customerName.substring(0, 150);
    }

    return customerName;
}

//============================================================================
//ADD THIS NEW METHOD BELOW copyRowStyle()
//============================================================================

private static void copyEntireRow1(Workbook workbook, Row sourceRow, Row targetRow) {

 for (int i = 0; i < sourceRow.getLastCellNum(); i++) {

     Cell sourceCell = sourceRow.getCell(i);
     Cell targetCell = targetRow.createCell(i);

     if (sourceCell == null) {
         continue;
     }

     // Copy style
     CellStyle newStyle = workbook.createCellStyle();
     newStyle.cloneStyleFrom(sourceCell.getCellStyle());
     targetCell.setCellStyle(newStyle);

     // Copy cell type and value
     switch (sourceCell.getCellType()) {

         case STRING:
             targetCell.setCellValue(sourceCell.getStringCellValue());
             break;

         case NUMERIC:
             targetCell.setCellValue(sourceCell.getNumericCellValue());
             break;

         case BOOLEAN:
             targetCell.setCellValue(sourceCell.getBooleanCellValue());
             break;

         case FORMULA:
             targetCell.setCellFormula(sourceCell.getCellFormula());
             break;

         case BLANK:
             targetCell.setBlank();
             break;

         default:
             targetCell.setCellValue(sourceCell.toString());
             break;
     }
 }
}

//REPLACE YOUR EXISTING setCellValue() METHOD WITH THIS VERSION
//------------------------------------------------------------
//This prevents writing null or empty values.

private static void setCellValue(Row row, int columnIndex, String value) {

 if (value == null || value.trim().isEmpty()) {
     return; // Skip blank values completely
 }

 Cell cell = row.getCell(columnIndex);

 if (cell == null) {
     cell = row.createCell(columnIndex);
 }

 cell.setCellValue(value);
}
//================================================================
//STEP 8: ADD THIS METHOD BELOW setCellValue()
//================================================================

@SuppressWarnings("unused")
private static void copyRowStyle1(Workbook workbook, Row sourceRow, Row targetRow) {

 for (int i = 0; i < sourceRow.getLastCellNum(); i++) {

     Cell sourceCell = sourceRow.getCell(i);
     Cell targetCell = targetRow.getCell(i);

     if (targetCell == null) {
         targetCell = targetRow.createCell(i);
     }

     if (sourceCell != null) {
         CellStyle newStyle = workbook.createCellStyle();
         newStyle.cloneStyleFrom(sourceCell.getCellStyle());
         targetCell.setCellStyle(newStyle);
     }
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
