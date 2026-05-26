import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.Duration;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TicketReport {

    private static final int WAIT_SEC = 20;
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;

    // ── Download directory (absolute path) ───────────────────────────────────
    private static final String DOWNLOAD_DIR =
            System.getProperty("user.home") + File.separator + "Downloads";

    public static void main(String[] args) throws Exception {

        WebDriverManager.chromedriver().setup();

        // ── Configure Chrome to download without dialog ───────────────────────
        ChromeOptions options = new ChromeOptions();
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", DOWNLOAD_DIR);
        prefs.put("download.prompt_for_download", false);
        prefs.put("download.directory_upgrade", true);
        prefs.put("safebrowsing.enabled", true);
        options.setExperimentalOption("prefs", prefs);

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SEC));
        js = (JavascriptExecutor) driver;

        driver.get("https://exicom-crm-dev.hummingwave.com/login");
        driver.manage().window().maximize();

        String mobile = "9518755103";

        // ── LOGIN ────────────────────────────────────────────────────────────
        driver.findElement(By.name("phone")).sendKeys(mobile);
        String otp = waitForOTP(mobile, 5, 2000);
        if (otp.isEmpty())
            throw new RuntimeException("OTP not fetched from DB!");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]")))
                .sendKeys(otp);
        wait.until(ExpectedConditions.urlContains("dashboard"));
        System.out.println("✅ Login Successful!");

        // ── STEP 1: Click sidebar "Reports" menu item ────────────────────────
        slowScrollTo(By.xpath("//p[contains(@class,'crm__sidebar__text') and normalize-space()='Reports']"));
        clickByText(wait, "Reports");

        // ── STEP 2: Click the "Reports" sub-menu / page heading ──────────────
        WebElement reportsSubMenu = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("(//*[text()='Reports'])[2]")));
        slowScrollTo(By.xpath("(//*[text()='Reports'])[2]"));
        reportsSubMenu.click();
        System.out.println("✅ Clicked: Reports sub-menu");

        // ── STEP 3: Wait for "Start building your report" placeholder ─────────
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'Start building your report')]")));
        System.out.println("✅ Validated: 'Start building your report' text visible");

        // ── STEP 4: Click the report-type dropdown and select "Ticket Report" ─
        WebElement dropdownInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'crm__dropdown__input-container')]//input[@role='combobox']")));
        dropdownInput.click();
        System.out.println("✅ Clicked: Report type dropdown");
        Thread.sleep(500);

        dropdownInput.sendKeys("Ticket Report");
        Thread.sleep(500);

        WebElement ticketReportOption = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'crm__dropdown__option') and normalize-space(text())='Ticket Report']")));
        ticketReportOption.click();
        System.out.println("✅ Selected: Ticket Report from dropdown");

        // ── STEP 5: Click the "Closed" quick-filter status button ────────────
        WebElement closedButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@aria-label='Closed' and contains(.,'Closed')]")));
        slowScrollTo(By.xpath("//div[@aria-label='Closed' and contains(.,'Closed')]"));
        closedButton.click();
        System.out.println("✅ Clicked: Closed status filter");

        // ── STEP 6: Click "Generate Report" button ───────────────────────────
        WebElement generateBtn = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("report__screen__generate__report")));
        slowScrollTo(By.id("report__screen__generate__report"));
        generateBtn.click();
        System.out.println("✅ Clicked: Generate Report");

        // ── STEP 7: Validate toast message ───────────────────────────────────
        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.toast_content_subtitle")));
        String toastText = toast.getText().trim();
        if (toastText.contains("Report generation in progress.")) {
            System.out.println("✅ Validated toast: \"" + toastText + "\"");
        } else {
            throw new RuntimeException("❌ TEST FAILED – Unexpected toast: \"" + toastText + "\"");
        }

        // ── STEP 8: Click toast to dismiss ───────────────────────────────────
        try {
            WebElement toastContainer = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div.Toastify__toast-container")));
            toastContainer.click();
            System.out.println("✅ Toast clicked to dismiss");
        } catch (Exception e) {
            System.out.println("ℹ️  Toast auto-dismissed");
        }
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div.Toastify__toast-container")));

        // ── STEP 9: Capture the LATEST (top) row ─────────────────────────────
        Thread.sleep(2000);

        WebElement pinnedTopRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]" +
                         "//div[@role='row' and @data-rowindex='0']")));

        WebElement jobIdCell = pinnedTopRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='jobId']" +
                         "//div[contains(@class,'MuiDataGrid-cellContent')]"));
        String capturedJobId = jobIdCell.getAttribute("title").trim();
        System.out.println("✅ Captured Latest Job ID: " + capturedJobId);

        WebElement scrollableTopRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]" +
                         "//div[@role='row' and @data-rowindex='0']")));

        // ── STEP 10: Validate Product Type ───────────────────────────────────
        WebElement productTypeCell = scrollableTopRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='productType']" +
                         "//div[contains(@class,'MuiDataGrid-cellContent')]"));
        String productType = productTypeCell.getAttribute("title").trim();
        System.out.println("✅ Product Type: \"" + productType + "\"");

        // ── STEP 11: Validate Ticket Status ──────────────────────────────────
        WebElement ticketStatusCell = scrollableTopRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='ticketStatus']" +
                         "//div[contains(@class,'MuiDataGrid-cellContent')]"));
        String ticketStatus = ticketStatusCell.getAttribute("title").trim();
        System.out.println("✅ Ticket Status: \"" + ticketStatus + "\"");

        // ── STEP 12: Validate Status label is "Pending" ──────────────────────
        List<WebElement> pendingLabelList = scrollableTopRow.findElements(
                By.xpath(".//p[@id='labelPending']"));
        if (!pendingLabelList.isEmpty() && pendingLabelList.get(0).getText().trim().equals("Pending")) {
            System.out.println("✅ Validated Status: Pending");
        } else {
            String actualStatus = scrollableTopRow.findElements(By.xpath(".//p[contains(@id,'label')]"))
                    .stream().findFirst()
                    .map(e -> e.getText().trim()).orElse("unknown");
            throw new RuntimeException(
                    "❌ TEST FAILED – Top row status is not 'Pending'. Actual: '" + actualStatus + "'");
        }

        // ── STEP 13: Validate download button is DISABLED for Pending row ─────
        List<WebElement> disabledDownload = scrollableTopRow.findElements(
                By.xpath(".//div[contains(@class,'crm__bulk__upload__disabled__download__file')]"));
        if (!disabledDownload.isEmpty()) {
            System.out.println("✅ Validated: Download button is disabled for Pending status");
        } else {
            throw new RuntimeException("❌ TEST FAILED – Disabled download icon not found for Pending row");
        }

        // ── STEP 14: Poll every 25s (max 3 min) until job is Completed or Failed
        System.out.println("⏳ Polling every 25s for Job ID: " + capturedJobId + " to complete (max 3 min)...");
        WebElement completedRow = pollUntilJobDone(capturedJobId, 25, 7);

        // ── STEP 15: Click the Download button for the Completed row ──────────
        WebElement downloadBtn = completedRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='downloadReport']" +
                         "//div[contains(@class,'crm__bulk__upload__download__file')]"));
        js.executeScript("arguments[0].click();", downloadBtn);
        System.out.println("✅ Clicked: Download button for Job ID: " + capturedJobId);

        // ── STEP 16: Wait for the Excel file to appear in Downloads ───────────
        // File name pattern: Ticket_Report<JOBID><date>.xlsx
        // We match by the Job ID letters portion only (e.g. "JOB05266553")
        System.out.println("⏳ Waiting for downloaded Excel file containing Job ID: " + capturedJobId + "...");
        File downloadedFile = waitForDownloadedFile(DOWNLOAD_DIR, capturedJobId, 60);
        System.out.println("✅ Downloaded file found: " + downloadedFile.getName());

        // ── STEP 17–22: Open Excel and validate required fields are not null ───
        validateExcelFields(downloadedFile);

        System.out.println("\n🎉 Ticket Report flow completed successfully!");
        Thread.sleep(2000);
        driver.quit();
    }

    // ════════════════════════════════════════════════════════════════════════
    // STEP 16 – WAIT FOR DOWNLOADED FILE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Polls the download directory every second for up to {@code timeoutSec} seconds,
     * looking for a fully downloaded .xlsx file whose name contains the given Job ID.
     * Ignores Chrome's in-progress ".crdownload" partial files.
     */
    private static File waitForDownloadedFile(String downloadDir, String jobId, int timeoutSec)
            throws InterruptedException {

        long deadline = System.currentTimeMillis() + (timeoutSec * 1000L);

        while (System.currentTimeMillis() < deadline) {
            File dir = new File(downloadDir);
            File[] candidates = dir.listFiles((d, name) ->
                    name.contains(jobId) &&
                    name.toLowerCase().endsWith(".xlsx") &&
                    !name.endsWith(".crdownload"));

            if (candidates != null && candidates.length > 0) {
                // If multiple matches, return the most recently modified one
                File latest = candidates[0];
                for (File f : candidates) {
                    if (f.lastModified() > latest.lastModified()) latest = f;
                }
                return latest;
            }
            Thread.sleep(1000);
        }
        throw new RuntimeException(
                "❌ TEST FAILED – Downloaded Excel file containing Job ID [" + jobId +
                "] not found in [" + downloadDir + "] within " + timeoutSec + " seconds.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // STEPS 17–22 – EXCEL FIELD VALIDATION  (not-null check only)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Opens the downloaded Excel report, finds the first data row, and asserts
     * that every required column is non-null / non-empty.
     * No value matching is performed — values change per run.
     * Test fails as soon as the summary is printed if any field is empty.
     */
    private static void validateExcelFields(File excelFile) throws Exception {

        System.out.println("\n📄 Opening Excel file for validation: " + excelFile.getName());

        // All columns that must NOT be null/empty
        List<String> requiredColumns = Arrays.asList(
                "Ticket ID",
                "Date of Complaint",
                "Created By",
                "Source",
                "Call Type",
                "Category",
                "Sub Category",
                "Account Name",
                "Product Type",
                "Charger Serial No",
                "Part Number",
                "Product Name",
                "Warranty Status",
                "Customer Type",
                "Customer Name",
                "Customer Number",
                "Vendors Mapped",
                "Ageing",
                "Status",
                "Pending At",
                "Pending Since",
                "On Call Resolve",
                "Backend closure",
                "First Time Resolve",
                "Urgency",
                "Commissioning Date"
        );

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            // ── STEP 17: Select the first sheet ───────────────────────────────
            Sheet sheet = workbook.getSheetAt(0);
            System.out.println("✅ Opened sheet: \"" + sheet.getSheetName() + "\"");

            // ── STEP 18: Locate header row and build column-index map ──────────
            Row headerRow = null;
            for (Row row : sheet) {
                Cell firstCell = row.getCell(0);
                if (firstCell != null && !getCellValueAsString(firstCell).trim().isEmpty()) {
                    headerRow = row;
                    break;
                }
            }
            if (headerRow == null)
                throw new RuntimeException("❌ TEST FAILED – Header row not found in Excel file.");

            Map<String, Integer> colIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                String header = getCellValueAsString(cell).trim();
                if (!header.isEmpty()) colIndex.put(header, cell.getColumnIndex());
            }
            System.out.println("✅ Header columns mapped: " + colIndex.keySet());

            // Verify every required column exists in the header
            for (String col : requiredColumns) {
                if (!colIndex.containsKey(col))
                    throw new RuntimeException(
                            "❌ TEST FAILED – Column \"" + col +
                            "\" not found in Excel header. Available: " + colIndex.keySet());
            }

            // ── STEP 19: Use the first data row (row after header) ────────────
            int    firstDataRowNum = headerRow.getRowNum() + 1;
            Row    targetRow       = sheet.getRow(firstDataRowNum);
            if (targetRow == null)
                throw new RuntimeException("❌ TEST FAILED – No data rows found in Excel file.");

            String ticketId = getCellValueAsString(targetRow.getCell(colIndex.get("Ticket ID"))).trim();
            System.out.println("✅ Validating row with Ticket ID: \"" + ticketId + "\"");

            // ── STEPS 20–22: Not-null check for every required column ──────────
            boolean allPassed = true;
            System.out.println("\n📋 Not-null validation for all required fields:");

            for (String fieldName : requiredColumns) {
                Cell   cell        = targetRow.getCell(colIndex.get(fieldName));
                String actualValue = getCellValueAsString(cell).trim();

                if (actualValue.isEmpty()) {
                    System.out.println("   ❌ FAIL – \"" + fieldName + "\" is NULL/EMPTY");
                    allPassed = false;
                } else {
                    System.out.println("   ✅ PASS – \"" + fieldName + "\": \"" + actualValue + "\"");
                }
            }

            // ── Final result ──────────────────────────────────────────────────
            if (!allPassed)
                throw new RuntimeException(
                        "❌ TEST FAILED – One or more fields are NULL/EMPTY. See log above.");

            System.out.println("\n✅ All " + requiredColumns.size() +
                               " fields are non-null for Ticket ID: \"" + ticketId + "\"");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // POLLING HELPER
    // ════════════════════════════════════════════════════════════════════════

    private static WebElement pollUntilJobDone(String jobId, int intervalSec, int maxAttempts)
            throws InterruptedException {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            System.out.println("🔄 Poll attempt " + attempt + "/" + maxAttempts +
                               " – refreshing page for Job ID: " + jobId);

            driver.navigate().refresh();
            Thread.sleep(2000);

            slowScrollTo(By.xpath(
                    "//p[contains(@class,'crm__sidebar__text') and normalize-space()='Reports']"));
            clickByText(wait, "Reports");

            WebElement subMenu = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("(//*[text()='Reports'])[2]")));
            slowScrollTo(By.xpath("(//*[text()='Reports'])[2]"));
            subMenu.click();

            WebElement ddInput = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'crm__dropdown__input-container')]" +
                             "//input[@role='combobox']")));
            ddInput.click();
            Thread.sleep(400);
            ddInput.sendKeys("Ticket Report");
            Thread.sleep(400);

            WebElement ddOption = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//div[contains(@class,'crm__dropdown__option') " +
                             "and normalize-space(text())='Ticket Report']")));
            ddOption.click();
            Thread.sleep(1500);

            List<WebElement> pinnedRows = driver.findElements(
                    By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]" +
                             "//div[@role='row' and " +
                             ".//div[@role='cell' and @data-field='jobId']" +
                             "//div[contains(@class,'MuiDataGrid-cellContent') and @title='" + jobId + "']]"));

            if (pinnedRows.isEmpty()) {
                System.out.println("   ⚠️  Job ID row not visible yet, will retry...");
                Thread.sleep(intervalSec * 1000L);
                continue;
            }

            String rowIndex = pinnedRows.get(0).getAttribute("data-rowindex");

            List<WebElement> scrollableRows = driver.findElements(
                    By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]" +
                             "//div[@role='row' and @data-rowindex='" + rowIndex + "']"));

            if (scrollableRows.isEmpty()) {
                System.out.println("   ⚠️  Scrollable row not rendered yet, will retry...");
                Thread.sleep(intervalSec * 1000L);
                continue;
            }

            WebElement row = scrollableRows.get(0);

            List<WebElement> failedLabel = row.findElements(By.xpath(".//p[@id='labelFailed']"));
            if (!failedLabel.isEmpty()) {
                throw new RuntimeException(
                        "❌ TEST FAILED – Job ID " + jobId + " status is FAILED. Aborting test.");
            }

            List<WebElement> completedLabel = row.findElements(By.xpath(".//p[@id='labelCompleted']"));
            if (!completedLabel.isEmpty()) {
                System.out.println("✅ Job ID " + jobId + " is Completed after attempt " + attempt);
                return row;
            }

            String currentStatus = "unknown";
            List<WebElement> pendingLbl    = row.findElements(By.xpath(".//p[@id='labelPending']"));
            List<WebElement> processingLbl = row.findElements(By.xpath(".//p[contains(@id,'label')]"));
            if (!pendingLbl.isEmpty())
                currentStatus = "Pending";
            else if (!processingLbl.isEmpty())
                currentStatus = processingLbl.get(0).getText().trim();

            System.out.println("   ⏳ Current status: [" + currentStatus +
                               "] – waiting " + intervalSec + "s before next poll...");
            Thread.sleep(intervalSec * 1000L);
        }

        throw new RuntimeException(
                "❌ TEST FAILED – Job ID " + jobId + " did not reach Completed status within " +
                (maxAttempts * intervalSec / 60) + " minutes.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Reads a cell's value as a plain String, handling all common cell types:
     * STRING, NUMERIC (including dates), BOOLEAN, FORMULA, and BLANK.
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private static void slowScrollTo(By locator) throws InterruptedException {
        slowScrollTo(locator, 800);
    }

    private static void slowScrollTo(By locator, int pauseMs) throws InterruptedException {
        try {
            WebElement el = driver.findElement(locator);
            js.executeScript(
                    "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", el);
            Thread.sleep(pauseMs);
        } catch (NoSuchElementException e) {
            System.out.println("   ⚠️  slowScrollTo – element not found yet: " + locator);
        }
    }

    private static void clickByText(WebDriverWait wait, String text) {
        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[contains(text(),'" + text + "')]"))).click();
        System.out.println("✅ Clicked: " + text);
    }

    private static String waitForOTP(String mobile, int retries, long delayMs)
            throws InterruptedException {
        for (int i = 0; i < retries; i++) {
            String otp = getOTPFromDB(mobile);
            if (!otp.isEmpty()) return otp;
            Thread.sleep(delayMs);
        }
        return "";
    }

    public static String getOTPFromDB(String mobile) {
        String otp = "";
        String url  = "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev";
        String user = "hw_goutham";
        String pass = "9qIE0mwg8ehN";
        try (Connection con = DriverManager.getConnection(url, user, pass);
             PreparedStatement ps = con.prepareStatement(
                     "SELECT otp FROM otp WHERE mobile_number = ? " +
                     "ORDER BY create_time DESC LIMIT 1")) {
            ps.setString(1, mobile);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) otp = rs.getString("otp");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return otp;
    }
}
