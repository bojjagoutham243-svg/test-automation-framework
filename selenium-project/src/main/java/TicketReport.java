import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.sql.*;
import java.time.Duration;
import java.util.List;

public class TicketReport {

    private static final int WAIT_SEC = 20;
    private static WebDriver driver;
    private static WebDriverWait wait;
    private static JavascriptExecutor js;

    public static void main(String[] args) throws Exception {

        WebDriverManager.chromedriver().setup();
        driver = new ChromeDriver();
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
        // MUI DataGrid splits into two DOM zones:
        //   • MuiDataGrid-pinnedColumns        → jobId (pinned left column)
        //   • MuiDataGrid-virtualScrollerRenderZone → all other columns
        Thread.sleep(2000);

        // Job ID lives in the PINNED zone
        WebElement pinnedTopRow = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]" +
                         "//div[@role='row' and @data-rowindex='0']")));

        WebElement jobIdCell = pinnedTopRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='jobId']" +
                         "//div[contains(@class,'MuiDataGrid-cellContent')]"));
        String capturedJobId = jobIdCell.getAttribute("title").trim();
        System.out.println("✅ Captured Latest Job ID: " + capturedJobId);

        // All other columns live in the SCROLLABLE render zone
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
        // FIX: was incorrectly referencing undefined 'topRow' — now uses scrollableTopRow
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
        // FIX: was incorrectly referencing undefined 'topRow' — now uses scrollableTopRow
        List<WebElement> disabledDownload = scrollableTopRow.findElements(
                By.xpath(".//div[contains(@class,'crm__bulk__upload__disabled__download__file')]"));
        if (!disabledDownload.isEmpty()) {
            System.out.println("✅ Validated: Download button is disabled for Pending status");
        } else {
            throw new RuntimeException("❌ TEST FAILED – Disabled download icon not found for Pending row");
        }

        // ── STEP 14: Poll every 30s (max 10 min) until job is Completed or Failed
        System.out.println("⏳ Polling every 30s for Job ID: " + capturedJobId + " to complete (max 10 min)...");
        // pollUntilJobDone returns the scrollable-zone row (which has status label + download button)
        WebElement completedRow = pollUntilJobDone(capturedJobId, 30, 20); // 30s × 20 = 10 min

        // ── STEP 15: Click the Download button for the Completed row ──────────
        WebElement downloadBtn = completedRow.findElement(
                By.xpath(".//div[@role='cell' and @data-field='downloadReport']" +
                         "//div[contains(@class,'crm__bulk__upload__download__file')]"));
        js.executeScript("arguments[0].click();", downloadBtn);
        System.out.println("✅ Clicked: Download button for Job ID: " + capturedJobId);

        System.out.println("\n🎉 Ticket Report flow completed successfully!");
        Thread.sleep(2000);
        driver.quit();
    }

    // ════════════════════════════════════════════════════════════════════════
    // POLLING HELPER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Refreshes the page every {@code intervalSec} seconds (up to {@code maxAttempts} times),
     * re-selects "Ticket Report" from the dropdown after each refresh, then looks for the
     * target Job ID row.
     *
     * Returns the SCROLLABLE-ZONE row WebElement (which holds status label + download button).
     *
     * Terminal conditions:
     *   - labelCompleted found → returns the scrollable row WebElement
     *   - labelFailed   found → throws RuntimeException (test fails immediately)
     *   - maxAttempts exhausted → throws RuntimeException (timeout)
     */
    private static WebElement pollUntilJobDone(String jobId, int intervalSec, int maxAttempts)
            throws InterruptedException {

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            System.out.println("🔄 Poll attempt " + attempt + "/" + maxAttempts +
                               " – refreshing page for Job ID: " + jobId);

            driver.navigate().refresh();
            Thread.sleep(2000);

            // Re-navigate: sidebar Reports → sub-menu Reports
            slowScrollTo(By.xpath(
                    "//p[contains(@class,'crm__sidebar__text') and normalize-space()='Reports']"));
            clickByText(wait, "Reports");

            WebElement subMenu = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("(//*[text()='Reports'])[2]")));
            slowScrollTo(By.xpath("(//*[text()='Reports'])[2]"));
            subMenu.click();

            // Re-select Ticket Report from dropdown
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

            // ── FIX: Look for the Job ID in the PINNED zone first ────────────
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

            // Get the data-rowindex of the matched pinned row
            String rowIndex = pinnedRows.get(0).getAttribute("data-rowindex");

            // ── Find the SAME row in the SCROLLABLE zone by rowindex ──────────
            // Status label + download button both live here
            List<WebElement> scrollableRows = driver.findElements(
                    By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]" +
                             "//div[@role='row' and @data-rowindex='" + rowIndex + "']"));

            if (scrollableRows.isEmpty()) {
                System.out.println("   ⚠️  Scrollable row not rendered yet, will retry...");
                Thread.sleep(intervalSec * 1000L);
                continue;
            }

            WebElement row = scrollableRows.get(0);

            // ── Check for FAILED status → fail the test immediately ───────────
            List<WebElement> failedLabel = row.findElements(By.xpath(".//p[@id='labelFailed']"));
            if (!failedLabel.isEmpty()) {
                throw new RuntimeException(
                        "❌ TEST FAILED – Job ID " + jobId + " status is FAILED. Aborting test.");
            }

            // ── Check for COMPLETED status → return the scrollable row ─────────
            List<WebElement> completedLabel = row.findElements(By.xpath(".//p[@id='labelCompleted']"));
            if (!completedLabel.isEmpty()) {
                System.out.println("✅ Job ID " + jobId + " is Completed after attempt " + attempt);
                return row;
            }

            // ── Still Pending / Processing → wait and retry ───────────────────
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

    private static boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
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