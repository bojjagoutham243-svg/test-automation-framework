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

public class CRM_OnBoard_Autofile_BulkUpload {

    // ================= TEMPLATE PATHS =================
    private static final String INDIVIDUAL_TEMPLATE =
            "/Users/gouthambojja/Documents/AutoGenerate Template/Bulk Upload Template_Individual Customers.xlsx";

    private static final String CORPORATE_TEMPLATE =
            "/Users/gouthambojja/Documents/AutoGenerate Template/Bulk Upload Template_Corporate Customers.xlsx";

    private static final String OUTPUT_FOLDER =
            "/Users/gouthambojja/Documents/AutoGenerate Template/Generated Files/";

    public static void main(String[] args) throws Exception {

        // ================= CONFIGURATION =================
        String mobile = "8639202204";
        String chargerType = "AC";
        String accountName = "Kia India Pvt Ltd";
        String customerType = "Individual"; // Individual or Corporate
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
                            By.xpath("(//p[normalize-space()='On Boardings'])[1]")
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
                            By.xpath("//div[contains(text(),'On Boardings')]")
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
 // REPLACE YOUR EXISTING generateBulkIRFExcel() METHOD WITH THIS VERSION
 // ============================================================================
 //
 // TEMPLATE STRUCTURE:
 //
 // Row 1 (index 0) -> Column Titles/Header
 // Row 2 (index 1) -> First Data Row
 //
 // Data generation should start from Row 2.
 //
 // ============================================================================

 public static String generateBulkIRFExcel(
         String customerType,
         int recordCount) throws Exception {

     // =====================================================================
     // SELECT TEMPLATE
     // =====================================================================
     String templatePath;

     if (customerType.equalsIgnoreCase("Individual")) {

         templatePath = INDIVIDUAL_TEMPLATE;

     } else if (customerType.equalsIgnoreCase("Corporate")) {

         templatePath = CORPORATE_TEMPLATE;

     } else {

         throw new RuntimeException(
                 "Invalid customer type: " + customerType);
     }

     File templateFile = new File(templatePath);

     if (!templateFile.exists()) {

         throw new RuntimeException(
                 "Template file not found: " + templatePath);
     }

     // =====================================================================
     // CREATE OUTPUT DIRECTORY
     // =====================================================================
     File outputDir = new File(OUTPUT_FOLDER);

     if (!outputDir.exists()) {
         outputDir.mkdirs();
     }

     // =====================================================================
     // OPEN TEMPLATE
     // =====================================================================
     FileInputStream fis =
             new FileInputStream(templateFile);

     Workbook workbook =
             WorkbookFactory.create(fis);

     Sheet sheet =
             workbook.getSheetAt(0);

     // =====================================================================
     // TEMPLATE DATA ROW
     // Row 2 => index 1
     // =====================================================================
     Row templateRow = sheet.getRow(1);

     if (templateRow == null) {

         throw new RuntimeException(
                 "Template row (Row 2) not found.");
     }

     // =====================================================================
     // REMOVE EXISTING DATA ROWS
     //
     // KEEP:
     // Row 1 -> Column Titles
     //
     // REMOVE:
     // Row 2 onwards
     // =====================================================================
     int lastRow = sheet.getLastRowNum();

     if (lastRow >= 1) {

         for (int i = lastRow; i >= 1; i--) {

             Row row = sheet.getRow(i);

             if (row != null) {
                 sheet.removeRow(row);
             }
         }
     }

     // =====================================================================
     // CREATE FIRST DATA ROW
     // Row 2 => index 1
     // =====================================================================
     Row firstRow = sheet.createRow(1);

     copyEntireRow1(
             workbook,
             templateRow,
             firstRow);

     populateCustomerBulkRow(
             firstRow,
             1,
             customerType);

     // =====================================================================
     // CREATE REMAINING ROWS
     // =====================================================================
     for (int i = 2; i <= recordCount; i++) {

         // Row indexes:
         // Record 1 -> Row 2 -> index 1
         // Record 2 -> Row 3 -> index 2
         // Record 3 -> Row 4 -> index 3

         int rowIndex = i;

         Row newRow =
                 sheet.createRow(rowIndex);

         // Copy style/validation/formulas
         copyEntireRow1(
                 workbook,
                 templateRow,
                 newRow);

         // Populate row data
         populateCustomerBulkRow(
                 newRow,
                 i,
                 customerType);
     }

     // =====================================================================
     // FORCE FORMULA RECALCULATION
     // =====================================================================
     workbook.setForceFormulaRecalculation(true);

     // =====================================================================
     // OUTPUT FILE NAME
     // =====================================================================
     String timestamp =
             new java.text.SimpleDateFormat(
                     "yyyyMMdd_HHmmss")
                     .format(new java.util.Date());

     String outputPath =
             OUTPUT_FOLDER
                     + "Bulk_Customer_"
                     + customerType
                     + "_"
                     + recordCount
                     + "_Records_"
                     + timestamp
                     + ".xlsx";

     // =====================================================================
     // SAVE FILE
     // =====================================================================
     FileOutputStream fos =
             new FileOutputStream(outputPath);

     workbook.write(fos);

     fos.close();
     workbook.close();
     fis.close();

     // =====================================================================
     // VALIDATE GENERATED FILE
     // =====================================================================
     File generatedFile =
             new File(outputPath);

     if (!generatedFile.exists()) {

         throw new RuntimeException(
                 "Generated file not found.");
     }

     if (generatedFile.length() == 0) {

         throw new RuntimeException(
                 "Generated file is empty.");
     }

     System.out.println(
             "========================================");

     System.out.println(
             "Excel File Generated Successfully");

     System.out.println(
             "Path : " + outputPath);

     System.out.println(
             "Size : "
                     + generatedFile.length()
                     + " bytes");

     System.out.println(
             "Records Generated : "
                     + recordCount);

     System.out.println(
             "Data Starts From : Row 2");

     System.out.println(
             "========================================");

     return outputPath;
 }

@SuppressWarnings("unused")
private static void populateBulkIRFRow(Row newRow, int i, String customerType) {
	
}

//============================================================================
//FINAL METHOD
//Supports BOTH:
//
//1. Individual Customer
//2. Corporate Customer
//
//Based on customerType selected.
//
//INDIVIDUAL:
//- SPOC fields left blank
//
//CORPORATE:
//- SPOC fields populated
//
//Charger Serial Number is OPTIONAL -> kept blank
//============================================================================

private static void populateCustomerBulkRow(
      Row row,
      int index,
      String customerType) {

  Workbook workbook = row.getSheet().getWorkbook();

  // =====================================================================
  // Column 0 : Reference Id
  // =====================================================================
  String referenceId =
          "REF" + System.currentTimeMillis() + index;

  setCellValue(row, 0, referenceId);

  // =====================================================================
  // Column 1 : Request Date
  // =====================================================================
  Cell dateCell = row.getCell(1);

  if (dateCell == null) {
      dateCell = row.createCell(1);
  }

  // Current Date
  dateCell.setCellValue(new java.util.Date());

  // Date Format
  CellStyle dateStyle = workbook.createCellStyle();

  DataFormat format = workbook.createDataFormat();

  // Example: 18/5/2026
  dateStyle.setDataFormat(format.getFormat("d/M/yyyy"));

  dateCell.setCellStyle(dateStyle);

  // =====================================================================
  // Column 2 : Charger Serial No.
  // =====================================================================
  // Optional field
  // Leaving blank intentionally

  // =====================================================================
  // Column 3 : Customer Name
  // =====================================================================
  String customerName = generateCustomerName();

  setCellValue(row, 3, customerName);

  // =====================================================================
  // Column 4 : Customer Country Code
  // =====================================================================
  setCellValue(row, 4, "+91");

  // =====================================================================
  // Column 5 : Primary No.
  // =====================================================================
  setCellValue(
          row,
          5,
          generateRandomMobileNumber());

  // =====================================================================
  // Column 6 : Customer Email
  // =====================================================================
  setCellValue(
          row,
          6,
          "customer" + index + "@mail.com");

  // =====================================================================
  // CORPORATE CUSTOMER
  // =====================================================================
  if (customerType.equalsIgnoreCase("Corporate")) {

      // ================================================================
      // Column 7 : SPOC Name
      // ================================================================
      setCellValue(
              row,
              7,
              "SPOC" + customerName);

      // ================================================================
      // Column 8 : SPOC Country Code
      // ================================================================
      setCellValue(row, 8, "+91");

      // ================================================================
      // Column 9 : SPOC No.
      // ================================================================
      setCellValue(
              row,
              9,
              generateRandomMobileNumber());

      // ================================================================
      // Column 10 : SPOC Email
      // ================================================================
      setCellValue(
              row,
              10,
              "spoc" + index + "@mail.com");
  }

  // =====================================================================
  // INDIVIDUAL CUSTOMER
  // =====================================================================
  else {

      // Leave SPOC fields blank intentionally
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
