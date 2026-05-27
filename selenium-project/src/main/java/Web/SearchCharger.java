package Web;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.sql.*;
import java.time.Duration;
import java.util.List;

public class SearchCharger {

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
        if (otp.isEmpty()) throw new RuntimeException("OTP not fetched from DB!");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]"))).sendKeys(otp);
        wait.until(ExpectedConditions.urlContains("dashboard"));
        System.out.println("✅ Login Successful!");

        // ── NAVIGATE: Search Charger ─────────────────────────────────────────
        clickByText(wait, "Search Charger");

        WebElement searchInput = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//input[@placeholder='Search by charger serial no.']")));
        searchInput.click();
        searchInput.sendKeys("DD1751316000010");
        Thread.sleep(1000);

        wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[normalize-space()='Search']"))).click();
        System.out.println("✅ Clicked: Search Button");

        // ── Wait for results to load ─────────────────────────────────────────
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'charger-detail-card')]")));
        Thread.sleep(1500); // Allow all sections to render

        // ── RUN ALL VALIDATIONS ──────────────────────────────────────────────
        System.out.println("\n══════════════════════════════════════════════════════");
        System.out.println("  STARTING CHARGER DETAIL PAGE VALIDATIONS");
        System.out.println("══════════════════════════════════════════════════════\n");

        validateChargerDetail();
        validateCustomerDetail();
        validateAMCDetail();
        validateUserDetail();
        validateTicketHistory();
        validateTransactionHistory();
        validateSAPData();
        // scroll back to top so the final banner is visible
        js.executeScript("window.scrollTo({top: 0, behavior: 'smooth'});");
        Thread.sleep(800);

        System.out.println("\n══════════════════════════════════════════════════════");
        System.out.println("  🎉 ALL VALIDATIONS COMPLETED SUCCESSFULLY!");
        System.out.println("══════════════════════════════════════════════════════\n");

        driver.quit();
    }

    // ════════════════════════════════════════════════════════════════════════
    // 1. CHARGER DETAIL VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private static void validateChargerDetail() throws InterruptedException {
        System.out.println("── [1] Charger Detail ──────────────────────────────");

        slowScrollTo(By.xpath("//div[contains(@class,'charger-detail-card')]"));

        // Charger ID (heading)
        assertText(
            By.xpath("//div[contains(@class,'heading-chargerId')]"),
            "DD1751316000010",
            "Charger ID"
        );

        // Info items – label → value pairs inside .charger-detail-card
        assertInfoItem("Product Type",      "AC Charger");
        assertInfoItem("Rating",            "22kW");
        assertInfoItem("Commissioning Date","2025-05-14");
        assertInfoItem("AMC Status",        "NA");
        assertInfoItem("Part No.",          "HE518173");
        assertInfoItem("Charger Description","35.10.13.01");
        assertInfoItem("Circle",            "Karnataka");

        // Warranty badge (green text)
        assertText(
            By.xpath("//div[contains(@class,'charger-detail-card')]//span[contains(@class,'green-text')]"),
            "Under Warranty",
            "Warranty Status"
        );

        System.out.println("✅ Charger Detail – ALL FIELDS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 2. CUSTOMER DETAIL VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private static void validateCustomerDetail() throws InterruptedException {
        System.out.println("── [2] Customer Detail ─────────────────────────────");

        slowScrollTo(By.xpath("//div[contains(@class,'customer-detail-card')]"));

        // Account highlight
        assertText(
            By.xpath("//div[contains(@class,'account-highlight')]//span[contains(@class,'value')]"),
            "Kia India Pvt Ltd",
            "Account Name"
        );

        // Customer info items
        assertCustomerInfoItem("Customer Name:",     "test account");
        assertCustomerInfoItem("Alt. Contact No.:",  "-");
        assertCustomerInfoItem("SPOC Name:",         "Suchithra");
        assertCustomerInfoItem("Customer Type:",     "Corporate");
        assertCustomerInfoItem("Email (Registered):","suchithra.ml@hummingwave.com");
        assertCustomerInfoItem("SPOC Contact No.:",  "8088031623");
        assertCustomerInfoItem("Contact No.:",       "9891518265");
        assertCustomerInfoItem("Customer Address:",
            "12, 4th Cross, 5th Block, Koramangala, Bengaluru, Karnataka , Bengaluru, Karnataka, India, 571429 - Shivapura Maddur S.O");
        assertCustomerInfoItem("Installation Address:",
            "#2345, Mandya, Karnataka, India, 571429 - Shivapura Maddur S.O");

        System.out.println("✅ Customer Detail – ALL FIELDS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 3. AMC DETAIL VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private static void validateAMCDetail() throws InterruptedException {
        System.out.println("── [3] AMC Detail ──────────────────────────────────");

        slowScrollTo(By.xpath("//div[contains(@class,'plan-name')]"));

        // Plan name (special element, not a date-row)
        assertText(
            By.xpath("//div[contains(@class,'plan-name')]"),
            "Under Warranty",
            "AMC Plan Name"
        );

        // Date rows inside AMC section card
        assertAMCDateRow("Purchase Date:", "30 Sep 2025");
        assertAMCDateRow("Start Date:",    "30 Sep 2025");
        assertAMCDateRow("End Date:",      "29 Sep 2026");

        System.out.println("✅ AMC Detail – ALL FIELDS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 4. OCPP CARD DETAIL VALIDATION  (part of left column)
    // ════════════════════════════════════════════════════════════════════════
    @SuppressWarnings("unused")
	private static void validateOCPPDetail() {
        System.out.println("── [4] OCPP Card Detail ────────────────────────────");

        assertOCPPDateRow("OCPP Enabled Date:", "30 Sep 2025");
        assertOCPPDateRow("Heartbeat Interval:", "50");
        assertOCPPDateRow("Last Ping Date:",     "-");
        assertOCPPDateRow("Last Ping Time:",     "-");

        System.out.println("✅ OCPP Card Detail – ALL FIELDS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 5. USER DETAIL VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private static void validateUserDetail() throws InterruptedException {
        System.out.println("── [5] User Detail ─────────────────────────────────");

        slowScrollTo(By.xpath("//div[contains(@class,'right-detail-column')]"));

        // Primary user
        assertText(
            By.xpath("//div[contains(@class,'user primary')]//div[contains(@class,'usernameprimary')]"),
            "Jhalki Chauhan",
            "Primary User Name"
        );

        // Last 5 Usages table – currently shows "No Data found"
        boolean noDataVisible = isElementPresent(
            By.xpath("//div[contains(@class,'crm__table__dashboard__no__data') and " +
                     "normalize-space(text())='No Data found']")
        );
        if (noDataVisible) {
            System.out.println("   ℹ️  Last 5 Usages: No Data found (expected for this charger)");
        } else {
            // If data exists, validate column headers are visible
            String[] usageColumns = {
                "Session Id", "Session Date", "Charging Medium",
                "Session Scheduled", "Consumption Unit", "Time Duration", "Status"
            };
            validateDataGridHeaders(
                "//div[contains(@class,'usage-datatable')]",
                usageColumns,
                "Last 5 Usages"
            );
        }

        System.out.println("✅ User Detail – ALL FIELDS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 6. TICKET HISTORY VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private static void validateTicketHistory() throws InterruptedException {
        System.out.println("── [6] Ticket History ──────────────────────────────");

        slowScrollTo(By.xpath(
            "//div[contains(@class,'charger-detail-header')]" +
            "//h2[contains(normalize-space(text()),'Ticket History')]"));

        // Validate section heading
        assertSectionHeading("Ticket History");

        // Validate column headers
        String[] ticketColumns = {
            "Ticket No", "Category", "Sub Category",
            "Ticket Date", "Product", "Account", "Pending At"
        };
        validateDataGridHeaders(
            "//div[contains(@class,'history-datatable')]",
            ticketColumns,
            "Ticket History"
        );

        // Expected ticket rows  {ticketNo, category, subCategory, ticketDate, product, account}
        Object[][] expectedTickets = {
            {"61A022011W", "Electrical damage", "Hardware burnt/missing", "2026-01-07 15:33", "AC Charger", "Kia India Pvt Ltd"},
            {"5DA022842W", "Electrical damage", "Cable/Gun",             "2025-12-31 12:23", "AC Charger", "Kia India Pvt Ltd"},
            {"5DA022841W", "Electrical damage", "Cable/Gun",             "2025-12-31 12:17", "AC Charger", "Kia India Pvt Ltd"},
            {"5DAR055",    "On-Boarding",        "Charger Commissioning", "2025-12-18 15:06", "AC Charger", "Kia India Pvt Ltd"},
            {"5DAQ032",    "Software & Connectivity", "Charger Settings", "2025-12-18 15:03", "AC Charger", "Kia India Pvt Ltd"},
        };

        for (Object[] ticket : expectedTickets) {
            String ticketNo    = (String) ticket[0];
            String category    = (String) ticket[1];
            String subCategory = (String) ticket[2];
            String ticketDate  = (String) ticket[3];
            String product     = (String) ticket[4];
            String account     = (String) ticket[5];

            // The ticket number is a bare text node inside the inner wrapper div of the ticketNo cell:
            //   <div role="cell" data-field="ticketNo">
            //     <div class="d-flex gap-8 align-center">
            //       <div class="tickets_c">…</div>   ← type icon
            //       <div class="d-flex">…</div>       ← source icon
            //       61A022011W                         ← bare text node, NOT wrapped in any element
            //     </div>
            //   </div>
            // normalize-space(text()) matches only the direct text nodes of the div, ignoring child elements,
            // so it correctly picks up the ticket number without matching partial text from icon divs.
            By rowLocator = By.xpath(
                "//div[@role='row' and " +
                ".//div[@role='cell' and @data-field='ticketNo']" +
                "//div[contains(@class,'d-flex') and contains(@class,'gap-8') " +
                "      and normalize-space(text())='" + ticketNo + "']]"
            );

            try {
                WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowLocator));

                assertCellInRow(row, "category",    category,    "Ticket[" + ticketNo + "] Category");
                assertCellInRow(row, "subCategory",  subCategory, "Ticket[" + ticketNo + "] SubCategory");
                assertCellInRow(row, "ticketDate",   ticketDate,  "Ticket[" + ticketNo + "] Date");
                assertCellInRow(row, "product",      product,     "Ticket[" + ticketNo + "] Product");
                assertCellInRow(row, "accountName",  account,     "Ticket[" + ticketNo + "] Account");

                System.out.println("   ✅ Ticket row validated: " + ticketNo);

            } catch (TimeoutException e) {
                throw new RuntimeException(
                    "❌ TEST FAILED – Ticket row not found for Ticket No: " + ticketNo, e);
            }
        }

        System.out.println("✅ Ticket History – ALL ROWS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 7. TRANSACTION HISTORY VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private static void validateTransactionHistory() throws InterruptedException {
        System.out.println("── [7] Transaction History ─────────────────────────");

        slowScrollTo(By.xpath(
            "//div[contains(@class,'charger-detail-header')]" +
            "//h2[contains(normalize-space(text()),'Transaction History')]"));

        // Validate section heading
        assertSectionHeading("Transaction History");

        // Validate column headers
        String[] txnColumns = {
            "Transaction ID", "Transaction Date", "Activity Type", "Amount Paid", "Mode"
        };
        validateDataGridHeaders(
            "(//div[contains(@class,'history-datatable')])[2]",
            txnColumns,
            "Transaction History"
        );

        // Expected transaction rows  {transactionId, txnDate, activityType, amountPaid}
        Object[][] expectedTxns = {
            {"7b83ff1f-83db-4b5a-ac2b-71104a00b4e8", "2025-09-30 13:16", "AMC",  "₹1"},
            {"60e8d00e-0fcb-4a55-9fd1-1416058e5a9d", "2025-09-30 11:18", "OCPP", "₹1"},
        };

        for (Object[] txn : expectedTxns) {
            String txnId       = (String) txn[0];
            String txnDate     = (String) txn[1];
            String activityType= (String) txn[2];
            String amountPaid  = (String) txn[3];

            // The transaction ID lives inside a MuiDataGrid-cellContent div whose @title holds the UUID:
            //   <div role="cell" data-field="id">               ← NO title here
            //     <div class="MuiDataGrid-cellContent"
            //          title="7b83ff1f-..."                     ← title is on this INNER div
            //          role="presentation">7b83ff1f-...</div>
            //   </div>
            // So we descend into the cell and match @title on the cellContent child.
            By rowLocator = By.xpath(
                "//div[@role='row' and " +
                ".//div[@role='cell' and @data-field='id']" +
                "//div[contains(@class,'MuiDataGrid-cellContent') and @title='" + txnId + "']]"
            );

            try {
                WebElement row = wait.until(ExpectedConditions.visibilityOfElementLocated(rowLocator));

                assertCellInRow(row, "transactionDate", txnDate,      "Txn[" + txnId.substring(0,8) + "] Date");
                assertCellInRow(row, "transactionType",  activityType, "Txn[" + txnId.substring(0,8) + "] Activity");
                assertAmountInRow(row, amountPaid,                     "Txn[" + txnId.substring(0,8) + "] Amount");

                // Validate success icon is present (green tick)
                boolean hasSuccessIcon = !row.findElements(
                    By.xpath(".//div[contains(@class,'crm__green__tick__icon')]")
                ).isEmpty();
                if (hasSuccessIcon) {
                    System.out.println("   ✅ Success status icon confirmed for Txn: " + txnId.substring(0,8) + "…");
                } else {
                    throw new RuntimeException(
                        "❌ TEST FAILED – Success icon missing for Transaction: " + txnId);
                }

                // Validate Razorpay icon in the Mode (transactionMode) cell
                assertRazorpayIconInRow(row, txnId);

                System.out.println("   ✅ Transaction row validated: " + txnId.substring(0,8) + "…");

            } catch (TimeoutException e) {
                throw new RuntimeException(
                    "❌ TEST FAILED – Transaction row not found for ID: " + txnId, e);
            }
        }

        System.out.println("✅ Transaction History – ALL ROWS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // 8. SAP DATA VALIDATION
    // ════════════════════════════════════════════════════════════════════════
    private static void validateSAPData() throws InterruptedException {
        System.out.println("── [8] SAP / Sales & Dispatch Data ─────────────────");

        slowScrollTo(By.xpath("//div[contains(@class,'sap-detail-card')]"));

        // Section heading
        assertSectionHeading("Data");

        // ── Sales Order Detail ───────────────────────────────────────────────
        assertSAPRow("Sales Order Number:",      "1234567");
        assertSAPRow("Sales Order Date:",        "2026-05-02");
        assertSAPRow("Warranty From Dispatch:",  "39");
        assertSAPRow("Warranty From Commission:","36");
        assertSAPRow("Total Quantity:",          "622");
        assertSAPRow("Parent Account:",          "Kia India Pvt Ltd");

        // ── Dispatch Detail ──────────────────────────────────────────────────
        assertSAPRow("Dispatch Number:",         "2971");
        assertSAPRow("Dispatch Date:",           "2025-04-02");
        assertSAPRow("Invoice No:",              "26102841");
        assertSAPRow("Invoice Date:",            "2025-04-02");
        assertSAPRow("Line Item:",               "1");
        assertSAPRow("Material Document Number:","-");
        assertSAPRow("Account Name:",            "Kia India Pvt Ltd");
        assertSAPRow("Account Address:",         "Plot No.-A-8A, Sector-24, Post Box, Noida, 24");
        assertSAPRow("Dealer Name:",             "Kia India Pvt Ltd");
        assertSAPRow("Dealer Address:",          "Plot No.-A-8A, Sector-24, Post Box, Noida, IN");
        assertSAPRow("Plant Code & Name:",       "-");

        System.out.println("✅ SAP Data – ALL FIELDS VALIDATED\n");
    }

    // ════════════════════════════════════════════════════════════════════════
    // ASSERT HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Asserts visible text of an element matched by locator.
     */
    private static void assertText(By locator, String expected, String fieldName) {
        try {
            WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            String actual = el.getText().trim();
            if (actual.contains(expected)) {
                System.out.println("   ✅ " + fieldName + ": \"" + actual + "\"");
            } else {
                throw new RuntimeException(
                    "❌ TEST FAILED [" + fieldName + "] – Expected: \"" + expected +
                    "\" | Actual: \"" + actual + "\"");
            }
        } catch (TimeoutException e) {
            throw new RuntimeException(
                "❌ TEST FAILED [" + fieldName + "] – Element not found: " + locator, e);
        }
    }

    /**
     * Validates a label→value info-item inside the charger-detail-card.
     * XPath: finds label, then sibling .value div.
     */
    private static void assertInfoItem(String label, String expectedValue) {
        By locator = By.xpath(
            "//div[contains(@class,'charger-detail-card')]" +
            "//div[contains(@class,'info-item')]" +
            "[label[normalize-space(text())='" + label + "']]" +
            "//div[contains(@class,'value')]"
        );
        assertText(locator, expectedValue, "Charger." + label);
    }

    /**
     * Validates a label→value info-item inside the customer-detail-card.
     */
    private static void assertCustomerInfoItem(String label, String expectedValue) {
        By locator = By.xpath(
            "//div[contains(@class,'customer-detail-card')]" +
            "//div[contains(@class,'info-item')]" +
            "[label[normalize-space(text())='" + label + "']]" +
            "//div[contains(@class,'value')]"
        );
        assertText(locator, expectedValue, "Customer." + label);
    }

    /**
     * Validates a date-row label→value in the AMC section-card.
     */
    private static void assertAMCDateRow(String label, String expectedValue) {
        // AMC section card is the FIRST .section-card in left-detail-column
        By locator = By.xpath(
            "(//div[contains(@class,'left-detail-column')]" +
            "//div[contains(@class,'section-card')])[1]" +
            "//div[contains(@class,'date-row')]" +
            "[label[normalize-space(text())='" + label + "']]" +
            "//div[contains(@class,'value')]"
        );
        assertText(locator, expectedValue, "AMC." + label);
    }

    /**
     * Validates a date-row label→value in the OCPP section-card.
     */
    private static void assertOCPPDateRow(String label, String expectedValue) {
        // OCPP section card is the SECOND .section-card in left-detail-column
        By locator = By.xpath(
            "(//div[contains(@class,'left-detail-column')]" +
            "//div[contains(@class,'section-card')])[2]" +
            "//div[contains(@class,'date-row')]" +
            "[label[normalize-space(text())='" + label + "']]" +
            "//div[contains(@class,'value')]"
        );
        assertText(locator, expectedValue, "OCPP." + label);
    }

    /**
     * Asserts that a section heading h2 contains expected text.
     */
    private static void assertSectionHeading(String headingText) {
        By locator = By.xpath(
            "//div[contains(@class,'charger-detail-header')]" +
            "//h2[contains(normalize-space(text()),'" + headingText + "')]"
        );
        try {
            boolean visible = !driver.findElements(locator).isEmpty();
            if (visible) {
                System.out.println("   ✅ Section heading found: \"" + headingText + "\"");
            } else {
                throw new RuntimeException(
                    "❌ TEST FAILED – Section heading not found: \"" + headingText + "\"");
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "❌ TEST FAILED – Section heading assertion error for: \"" + headingText + "\"", e);
        }
    }

    /**
     * Validates DataGrid column headers for a given table container.
     * @param tableXPath  XPath for the root grid element
     * @param columns     expected column header titles
     * @param tableName   label used in output
     */
    private static void validateDataGridHeaders(String tableXPath, String[] columns, String tableName) {
        for (String col : columns) {
            By headerLocator = By.xpath(
                tableXPath +
                "//div[contains(@class,'MuiDataGrid-columnHeaderTitle') " +
                "and normalize-space(text())='" + col + "']"
            );
            try {
                boolean visible = !driver.findElements(headerLocator).isEmpty();
                if (visible) {
                    System.out.println("   ✅ Column header: \"" + col + "\"");
                } else {
                    throw new RuntimeException(
                        "❌ TEST FAILED [" + tableName + "] – Column header not found: \"" + col + "\"");
                }
            } catch (Exception e) {
                throw new RuntimeException(
                    "❌ TEST FAILED [" + tableName + "] – Error checking column: \"" + col + "\"", e);
            }
        }
    }

    /**
     * Asserts a specific cell value (matched by data-field) inside a given row element.
     */
    private static void assertCellInRow(WebElement row, String dataField, String expected, String fieldName) {
        try {
            WebElement cell = row.findElement(
                By.xpath(".//div[@role='cell' and @data-field='" + dataField + "']")
            );
            String actual = cell.getText().trim();
            if (actual.contains(expected)) {
                System.out.println("   ✅ " + fieldName + ": \"" + actual + "\"");
            } else {
                throw new RuntimeException(
                    "❌ TEST FAILED [" + fieldName + "] – Expected: \"" + expected +
                    "\" | Actual: \"" + actual + "\"");
            }
        } catch (NoSuchElementException e) {
            throw new RuntimeException(
                "❌ TEST FAILED [" + fieldName + "] – Cell with data-field='" + dataField + "' not found in row.", e);
        }
    }

    /**
     * Asserts the amountPaid cell (uses renderer, so reads direct text).
     */
    private static void assertAmountInRow(WebElement row, String expected, String fieldName) {
        try {
            WebElement cell = row.findElement(
                By.xpath(".//div[@role='cell' and @data-field='amountPaid']")
            );
            String actual = cell.getText().trim();
            if (actual.equals(expected)) {
                System.out.println("   ✅ " + fieldName + ": \"" + actual + "\"");
            } else {
                throw new RuntimeException(
                    "❌ TEST FAILED [" + fieldName + "] – Expected: \"" + expected +
                    "\" | Actual: \"" + actual + "\"");
            }
        } catch (NoSuchElementException e) {
            throw new RuntimeException(
                "❌ TEST FAILED [" + fieldName + "] – amountPaid cell not found.", e);
        }
    }

    /**
     * Asserts a label→value row inside the SAP detail card.
     * Matches: <label>...</label><span>...</span> pattern inside .sap-detail-card .row
     */
    private static void assertSAPRow(String label, String expectedValue) {
        By locator = By.xpath(
            "//div[contains(@class,'sap-detail-card')]" +
            "//div[contains(@class,'row')]" +
            "[label[normalize-space(text())='" + label + "']]" +
            "/span"
        );
        assertText(locator, expectedValue, "SAP." + label);
    }

    /**
     * Validates that the Razorpay payment icon is present inside the
     * transactionMode cell of a given transaction row.
     *
     * HTML structure:
     *   <div data-field="transactionMode" ...>
     *     <div class="search_charger_tick search_charger_razorpay"></div>
     *   </div>
     *
     * Two CSS classes are verified:
     *   - search_charger_tick      : generic payment icon wrapper
     *   - search_charger_razorpay  : Razorpay-specific icon
     */
    private static void assertRazorpayIconInRow(WebElement row, String txnId) {
        String shortId = txnId.substring(0, 8) + "...";

        // Locate the Mode cell first
        WebElement modeCell;
        try {
            modeCell = row.findElement(
                By.xpath(".//div[@role='cell' and @data-field='transactionMode']")
            );
        } catch (NoSuchElementException e) {
            throw new RuntimeException(
                "FAILED – transactionMode cell not found in row for Txn: " + shortId, e);
        }

        // Check for icon div carrying BOTH classes simultaneously
        List<WebElement> razorpayIcons = modeCell.findElements(
            By.xpath(
                ".//div[contains(concat(' ',normalize-space(@class),' '),' search_charger_razorpay ')" +
                " and contains(concat(' ',normalize-space(@class),' '),' search_charger_tick ')]"
            )
        );

        if (!razorpayIcons.isEmpty()) {
            System.out.println("   Razorpay icon confirmed in Mode cell for Txn: " + shortId);
        } else {
            // Diagnose which class is missing for a clearer failure message
            boolean hasTick = !modeCell.findElements(
                By.xpath(".//div[contains(@class,'search_charger_tick')]")
            ).isEmpty();
            boolean hasRazorpay = !modeCell.findElements(
                By.xpath(".//div[contains(@class,'search_charger_razorpay')]")
            ).isEmpty();

            throw new RuntimeException(
                "TEST FAILED – Razorpay icon missing in Mode cell for Txn: " + shortId + "\n" +
                "   search_charger_tick present    : " + hasTick + "\n" +
                "   search_charger_razorpay present: " + hasRazorpay
            );
        }
    }

    /**
     * Smoothly scrolls the element matched by locator into the centre of the
     * viewport, then pauses so the human eye can follow the movement.
     *
     * @param locator   element to scroll to (must already exist in the DOM)
     * @param pauseMs   milliseconds to wait AFTER the scroll finishes
     */
    private static void slowScrollTo(By locator, int pauseMs) throws InterruptedException {
        try {
            WebElement el = driver.findElement(locator);
            js.executeScript(
                "arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", el);
            Thread.sleep(pauseMs);
        } catch (NoSuchElementException e) {
            // Non-fatal: element not yet rendered; the subsequent wait will catch it
            System.out.println("   ⚠️  slowScrollTo – element not found yet: " + locator);
        }
    }

    /**
     * Convenience overload – default 800 ms pause (comfortable viewing speed).
     */
    private static void slowScrollTo(By locator) throws InterruptedException {
        slowScrollTo(locator, 800);
    }

    /**
     * Checks if an element is present in the DOM (no wait).
     */
    private static boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════
    // REUSED HELPERS (from SearchCharger)
    // ════════════════════════════════════════════════════════════════════════

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
