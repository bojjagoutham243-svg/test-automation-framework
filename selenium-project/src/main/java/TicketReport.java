import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;

public class TicketReport {

    private static final int WAIT_SEC = 20;

    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;

    private static final String DOWNLOAD_DIR =
            System.getProperty("user.home") + File.separator + "Downloads";

    public static void main(String[] args) throws Exception {

        WebDriverManager.chromedriver().setup();

        // ─────────────────────────────────────────────────────────────
        // CHROME OPTIONS
        // ─────────────────────────────────────────────────────────────
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

        // ─────────────────────────────────────────────────────────────
        // LOGIN
        // ─────────────────────────────────────────────────────────────
        driver.findElement(By.name("phone")).sendKeys(mobile);

        String otp = waitForOTP(mobile, 5, 2000);

        if (otp.isEmpty()) {
            throw new RuntimeException("❌ OTP not fetched from DB!");
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]")))
                .sendKeys(otp);

        wait.until(ExpectedConditions.urlContains("dashboard"));

        System.out.println("✅ Login Successful!");

        // ═══════════════════════════════════════════════════════════════
        // FLOW 1 : NORMAL TICKET REPORT FLOW
        // ═══════════════════════════════════════════════════════════════

        // STEP 1
        slowScrollTo(By.xpath(
                "//p[contains(@class,'crm__sidebar__text') and normalize-space()='Reports']"));

        clickByText(wait, "Reports");

        // STEP 2
        WebElement reportsSubMenu = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("(//*[text()='Reports'])[2]")));

        slowScrollTo(By.xpath("(//*[text()='Reports'])[2]"));

        reportsSubMenu.click();

        System.out.println("✅ Clicked: Reports sub-menu");

        // STEP 3
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'Start building your report')]")));

        System.out.println("✅ Validated: Start building your report");

        // STEP 4
        WebElement dropdownInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class,'crm__dropdown__input-container')]//input[@role='combobox']")));

        dropdownInput.click();

        Thread.sleep(500);

        dropdownInput.sendKeys("Ticket Report");

        Thread.sleep(500);

        WebElement ticketReportOption = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class,'crm__dropdown__option') and normalize-space(text())='Ticket Report']")));

        ticketReportOption.click();

        System.out.println("✅ Selected: Ticket Report");

        // STEP 5
        clickStatusFilter("Closed");

        // STEP 6
        WebElement generateBtn = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.id("report__screen__generate__report")));

        slowScrollTo(By.id("report__screen__generate__report"));

        generateBtn.click();

        System.out.println("✅ Clicked: Generate Report");

        // STEP 7
        validateToast();

        // STEP 8
        dismissToast();

        // STEP 9
        Thread.sleep(2000);

        WebElement pinnedTopRow = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]"
                                + "//div[@role='row' and @data-rowindex='0']")));

        String capturedJobId = pinnedTopRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='jobId']"
                        + "//div[contains(@class,'MuiDataGrid-cellContent')]"))
                .getAttribute("title").trim();

        System.out.println("✅ Captured Job ID: " + capturedJobId);

        WebElement scrollableTopRow = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
                                + "//div[@role='row' and @data-rowindex='0']")));

        // STEP 10
        validateCell(scrollableTopRow, "productType", "Product Type");

        // STEP 11
        validateCell(scrollableTopRow, "ticketStatus", "Ticket Status");

        // STEP 12
        validatePendingStatus(scrollableTopRow);

        // STEP 13
        validateDisabledDownload(scrollableTopRow);

        // STEP 14
        System.out.println("⏳ Polling for completion...");
        WebElement completedRow = pollUntilJobDone(capturedJobId, 25, 7);

        // STEP 15
        WebElement downloadBtn = completedRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='downloadReport']"
                        + "//div[contains(@class,'crm__bulk__upload__download__file')]"));

        js.executeScript("arguments[0].click();", downloadBtn);

        System.out.println("✅ Clicked download");

        // STEP 16
        File downloadedFile = waitForDownloadedFile(
                DOWNLOAD_DIR,
                capturedJobId,
                60);

        System.out.println("✅ File Downloaded: " + downloadedFile.getName());

        // STEP 17
        validateExcelFields(downloadedFile);

        System.out.println("\n🎉 NORMAL TICKET REPORT FLOW COMPLETED");

        // ═══════════════════════════════════════════════════════════════
        // FLOW 2 : FTR TOGGLE FLOW
        // ═══════════════════════════════════════════════════════════════

        System.out.println("\n══════════════════════════════════════");
        System.out.println("🚀 STARTING FTR TOGGLE FLOW");
        System.out.println("══════════════════════════════════════");

        // STEP 1
        driver.navigate().refresh();

        Thread.sleep(2000);

        navigateToTicketReport();

        // STEP 2
        System.out.println("🔍 Checking FTR toggle in default state");

        WebElement ftrToggleInitial = getFtrToggle();

        if (!isFtrToggleDisabled(ftrToggleInitial)) {
            throw new RuntimeException(
                    "❌ FTR toggle should be DISABLED initially.");
        }

        System.out.println("✅ FTR toggle is disabled initially");

        // STEP 3
        try {
            js.executeScript("arguments[0].click();", ftrToggleInitial);
            Thread.sleep(300);
        } catch (Exception e) {
            System.out.println("ℹ️ Click ignored as expected");
        }

        if (!isFtrToggleDisabled(getFtrToggle())) {
            throw new RuntimeException(
                    "❌ FTR toggle became enabled unexpectedly.");
        }

        System.out.println("✅ Disabled toggle remained disabled");

        // STEP 4
        clickStatusFilter("Closed");

        // STEP 5
        WebElement ftrAfterClosed = getFtrToggle();

        if (!isFtrToggleDisabled(ftrAfterClosed)) {
            throw new RuntimeException(
                    "❌ FTR toggle should remain disabled.");
        }

        System.out.println("✅ FTR toggle still disabled");

        // STEP 6
        clickStatusFilter("Open");

        Thread.sleep(500);

        // STEP 7
        WebElement enabledToggle = getFtrToggle();

        if (isFtrToggleDisabled(enabledToggle)) {
            throw new RuntimeException(
                    "❌ FTR toggle should now be enabled.");
        }

        System.out.println("✅ FTR toggle enabled");

        // STEP 8
        js.executeScript("arguments[0].click();", enabledToggle);

        Thread.sleep(500);

        System.out.println("✅ FTR toggle turned ON");

        String ariaChecked = enabledToggle.getAttribute("aria-checked");

        System.out.println("ℹ️ aria-checked = " + ariaChecked);

        // STEP 9
        List<WebElement> allComboboxes = driver.findElements(
                By.xpath("//input[@role='combobox']"));

        WebElement dateDropdownInput =
                allComboboxes.get(allComboboxes.size() - 1);

        js.executeScript("arguments[0].click();", dateDropdownInput);

        Thread.sleep(400);

        dateDropdownInput.sendKeys("Current Month");

        Thread.sleep(600);

        dateDropdownInput.sendKeys(Keys.ENTER);

        System.out.println("✅ Selected Current Month");

        // STEP 10
        WebElement generateBtnFTR = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.id("report__screen__generate__report")));

        slowScrollTo(By.id("report__screen__generate__report"));

        generateBtnFTR.click();

        System.out.println("✅ Clicked Generate Report");

        // STEP 11
        validateToast();

        dismissToast();

        // STEP 12
        Thread.sleep(2000);

        WebElement pinnedTopRowFTR = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]"
                                + "//div[@role='row' and @data-rowindex='0']")));

        String ftrJobId = pinnedTopRowFTR.findElement(
                By.xpath(".//div[@role='cell' and @data-field='jobId']"
                        + "//div[contains(@class,'MuiDataGrid-cellContent')]"))
                .getAttribute("title").trim();

        System.out.println("✅ FTR Job ID: " + ftrJobId);

        // STEP 13
        WebElement scrollableFtrRow = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
                                + "//div[@role='row' and @data-rowindex='0']")));

        WebElement ftrCell = scrollableFtrRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='isFirstTimeResolution']"));

        String ftrValue = ftrCell.findElement(
                By.className("ticket_date"))
                .getText().trim();

        if (!"Yes".equalsIgnoreCase(ftrValue)) {
            throw new RuntimeException(
                    "❌ FTR value expected Yes but got: " + ftrValue);
        }

        System.out.println("✅ Validated FTR value = Yes");

        // STEP 14
        WebElement completedFtrRow =
                pollUntilJobDone(ftrJobId, 25, 7);

        // STEP 15
        WebElement ftrDownloadBtn = completedFtrRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='downloadReport']"
                        + "//div[contains(@class,'crm__bulk__upload__download__file')]"));

        js.executeScript("arguments[0].click();", ftrDownloadBtn);

        System.out.println("✅ Download clicked for FTR report");

        // STEP 16
        File ftrDownloadedFile = waitForDownloadedFile(
                DOWNLOAD_DIR,
                ftrJobId,
                60);

        System.out.println("✅ FTR File Downloaded: "
                + ftrDownloadedFile.getName());

        // STEP 17
        validateExcelFields(ftrDownloadedFile);

        System.out.println("\n🎉 COMPLETE FLOW EXECUTED SUCCESSFULLY!");

        Thread.sleep(2000);

        driver.quit();
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private static void validateToast() {

        WebElement toast = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div.toast_content_subtitle")));

        String toastText = toast.getText().trim();

        if (toastText.contains("Report generation in progress.")) {
            System.out.println("✅ Toast validated");
        } else {
            throw new RuntimeException(
                    "❌ Unexpected toast: " + toastText);
        }
    }

    private static void dismissToast() {

        try {
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div.Toastify__toast-container")))
                    .click();
        } catch (Exception e) {
            System.out.println("ℹ️ Toast auto-dismissed");
        }

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div.Toastify__toast-container")));
    }

    private static void validateCell(
            WebElement row,
            String field,
            String label) {

        WebElement cell = row.findElement(
                By.xpath(".//div[@role='cell' and @data-field='"
                        + field + "']"
                        + "//div[contains(@class,'MuiDataGrid-cellContent')]"));

        String value = cell.getAttribute("title").trim();

        System.out.println("✅ " + label + ": " + value);
    }

    private static void validatePendingStatus(WebElement row) {

        List<WebElement> pendingLabelList = row.findElements(
                By.xpath(".//p[@id='labelPending']"));

        if (!pendingLabelList.isEmpty()) {
            System.out.println("✅ Status validated: Pending");
        } else {
            throw new RuntimeException(
                    "❌ Pending status not found");
        }
    }

    private static void validateDisabledDownload(WebElement row) {

        List<WebElement> disabledDownload = row.findElements(
                By.xpath(".//div[contains(@class,'crm__bulk__upload__disabled__download__file')]"));

        if (!disabledDownload.isEmpty()) {
            System.out.println("✅ Download disabled for Pending");
        } else {
            throw new RuntimeException(
                    "❌ Disabled download icon not found");
        }
    }

    private static void clickStatusFilter(String label)
            throws InterruptedException {

        By outerLocator = By.xpath(
                "//div[contains(@class,'reports__square__box__with__inside__text')"
                        + " and .//div[@aria-label='" + label + "']]");

        slowScrollTo(outerLocator, 500);

        WebElement outerDiv = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        outerLocator));

        js.executeScript("arguments[0].click();", outerDiv);

        System.out.println("✅ Clicked status: " + label);

        Thread.sleep(400);
    }

    private static void navigateToTicketReport()
            throws InterruptedException {

        slowScrollTo(By.xpath(
                "//p[contains(@class,'crm__sidebar__text') and normalize-space()='Reports']"));

        clickByText(wait, "Reports");

        WebElement subMenu = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("(//*[text()='Reports'])[2]")));

        slowScrollTo(By.xpath("(//*[text()='Reports'])[2]"));

        subMenu.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//*[contains(text(),'Start building your report')]")));

        WebElement ddInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        By.xpath("//div[contains(@class,'crm__dropdown__input-container')]//input[@role='combobox']")));

        ddInput.click();

        Thread.sleep(400);

        ddInput.sendKeys("Ticket Report");

        Thread.sleep(400);

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//div[contains(@class,'crm__dropdown__option') and normalize-space(text())='Ticket Report']")))
                .click();

        System.out.println("✅ Ticket Report selected");

        Thread.sleep(500);
    }

    private static WebElement getFtrToggle() {

        List<WebElement> byAriaLabel = driver.findElements(
                By.xpath("//input[@type='checkbox' and @aria-label='customized switch']"));

        if (!byAriaLabel.isEmpty()) {
            return byAriaLabel.get(0);
        }

        List<WebElement> bySwitch = driver.findElements(
                By.xpath("//span[contains(@class,'MuiSwitch-switchBase')]"));

        if (!bySwitch.isEmpty()) {
            return bySwitch.get(0);
        }

        List<WebElement> anySwitch = driver.findElements(
                By.xpath("//*[@role='switch']"));

        if (!anySwitch.isEmpty()) {
            return anySwitch.get(0);
        }

        throw new RuntimeException("❌ FTR toggle not found");
    }

    private static boolean isFtrToggleDisabled(WebElement toggle) {

        String ariaDisabled = toggle.getAttribute("aria-disabled");
        String disabled = toggle.getAttribute("disabled");
        String classList = toggle.getAttribute("class");

        return "true".equalsIgnoreCase(ariaDisabled)
                || disabled != null
                || (classList != null
                && classList.contains("Mui-disabled"));
    }

    private static File waitForDownloadedFile(
            String downloadDir,
            String jobId,
            int timeoutSec)
            throws InterruptedException {

        long deadline =
                System.currentTimeMillis() + (timeoutSec * 1000L);

        while (System.currentTimeMillis() < deadline) {

            File dir = new File(downloadDir);

            File[] candidates = dir.listFiles((d, name) ->
                    name.contains(jobId)
                            && name.toLowerCase().endsWith(".xlsx")
                            && !name.endsWith(".crdownload"));

            if (candidates != null && candidates.length > 0) {

                File latest = candidates[0];

                for (File f : candidates) {
                    if (f.lastModified() > latest.lastModified()) {
                        latest = f;
                    }
                }

                return latest;
            }

            Thread.sleep(1000);
        }

        throw new RuntimeException(
                "❌ Downloaded Excel file not found");
    }

    private static void validateExcelFields(File excelFile)
            throws Exception {

        System.out.println("\n📄 Validating Excel: "
                + excelFile.getName());

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
                "On Call Resolve",
                "Backend closure",
                "First Time Resolve",
                "Urgency",
                "Commissioning Date"
        );

        try (FileInputStream fis = new FileInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = null;

            for (Row row : sheet) {

                Cell firstCell = row.getCell(0);

                if (firstCell != null
                        && !getCellValueAsString(firstCell).trim().isEmpty()) {

                    headerRow = row;
                    break;
                }
            }

            if (headerRow == null) {
                throw new RuntimeException(
                        "❌ Header row not found");
            }

            Map<String, Integer> colIndex = new HashMap<>();

            for (Cell cell : headerRow) {

                String header =
                        getCellValueAsString(cell).trim();

                if (!header.isEmpty()) {
                    colIndex.put(header,
                            cell.getColumnIndex());
                }
            }

            for (String col : requiredColumns) {

                if (!colIndex.containsKey(col)) {

                    throw new RuntimeException(
                            "❌ Missing column: " + col);
                }
            }

            Row targetRow =
                    sheet.getRow(headerRow.getRowNum() + 1);

            if (targetRow == null) {
                throw new RuntimeException(
                        "❌ No data rows found");
            }

            String ticketId = getCellValueAsString(
                    targetRow.getCell(
                            colIndex.get("Ticket ID"))).trim();

            System.out.println("✅ Validating Ticket ID: "
                    + ticketId);

            boolean allPassed = true;

            for (String fieldName : requiredColumns) {

                String actualValue = getCellValueAsString(
                        targetRow.getCell(
                                colIndex.get(fieldName))).trim();

                if (actualValue.isEmpty()) {

                    System.out.println(
                            "❌ NULL/EMPTY: " + fieldName);

                    allPassed = false;

                } else {

                    System.out.println(
                            "✅ " + fieldName + " : "
                                    + actualValue);
                }
            }

            if (!allPassed) {
                throw new RuntimeException(
                        "❌ One or more fields empty");
            }

            System.out.println(
                    "✅ All fields validated successfully");
        }
    }

    private static WebElement pollUntilJobDone(
            String jobId,
            int intervalSec,
            int maxAttempts)
            throws InterruptedException {

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            System.out.println(
                    "🔄 Poll Attempt "
                            + attempt + "/" + maxAttempts);

            driver.navigate().refresh();

            Thread.sleep(2000);

            navigateToTicketReport();

            List<WebElement> pinnedRows =
                    driver.findElements(
                            By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]"
                                    + "//div[@role='row' and "
                                    + ".//div[@role='cell' and @data-field='jobId']"
                                    + "//div[contains(@class,'MuiDataGrid-cellContent') and @title='"
                                    + jobId + "']]"));

            if (pinnedRows.isEmpty()) {

                System.out.println(
                        "⚠️ Job row not visible");

                Thread.sleep(intervalSec * 1000L);

                continue;
            }

            String rowIndex =
                    pinnedRows.get(0)
                            .getAttribute("data-rowindex");

            List<WebElement> scrollableRows =
                    driver.findElements(
                            By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
                                    + "//div[@role='row' and @data-rowindex='"
                                    + rowIndex + "']"));

            if (scrollableRows.isEmpty()) {

                Thread.sleep(intervalSec * 1000L);

                continue;
            }

            WebElement row = scrollableRows.get(0);

            if (!row.findElements(
                    By.xpath(".//p[@id='labelFailed']")).isEmpty()) {

                throw new RuntimeException(
                        "❌ Job FAILED");
            }

            if (!row.findElements(
                    By.xpath(".//p[@id='labelCompleted']")).isEmpty()) {

                System.out.println(
                        "✅ Job COMPLETED");

                return row;
            }

            System.out.println(
                    "⏳ Waiting for completion...");

            Thread.sleep(intervalSec * 1000L);
        }

        throw new RuntimeException(
                "❌ Job not completed in time");
    }

    private static String getCellValueAsString(Cell cell) {

        if (cell == null) {
            return "";
        }

        return new DataFormatter()
                .formatCellValue(cell)
                .trim();
    }

    private static void slowScrollTo(By locator)
            throws InterruptedException {

        slowScrollTo(locator, 800);
    }

    private static void slowScrollTo(
            By locator,
            int pauseMs)
            throws InterruptedException {

        try {

            WebElement el = driver.findElement(locator);

            js.executeScript(
                    "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});",
                    el);

            Thread.sleep(pauseMs);

        } catch (NoSuchElementException e) {

            System.out.println(
                    "⚠️ Element not found: " + locator);
        }
    }

    private static void clickByText(
            WebDriverWait wait,
            String text) {

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[contains(text(),'"
                        + text + "')]")))
                .click();

        System.out.println("✅ Clicked: " + text);
    }

    private static String waitForOTP(
            String mobile,
            int retries,
            long delayMs)
            throws InterruptedException {

        for (int i = 0; i < retries; i++) {

            String otp = getOTPFromDB(mobile);

            if (!otp.isEmpty()) {
                return otp;
            }

            Thread.sleep(delayMs);
        }

        return "";
    }

    public static String getOTPFromDB(String mobile) {

        String otp = "";

        String url =
                "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev";

        String user = "hw_goutham";

        String pass = "9qIE0mwg8ehN";

        try (
                Connection con =
                        DriverManager.getConnection(
                                url,
                                user,
                                pass);

                PreparedStatement ps =
                        con.prepareStatement(
                                "SELECT otp FROM otp WHERE mobile_number = ? "
                                        + "ORDER BY create_time DESC LIMIT 1")
        ) {

            ps.setString(1, mobile);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    otp = rs.getString("otp");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return otp;
    }
}
