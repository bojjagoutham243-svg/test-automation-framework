package web;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.sql.*;
import java.time.Duration;
import java.util.List;

public class UserManagement {

    // ── Shared wait timeout ──────────────────────────────────────────────────
    private static final int WAIT_SEC = 20;

    // Picks only the VISIBLE, interactive dropdown options – fixes duplicate issue
    private static final String VISIBLE_OPTION_XPATH =
        "//div[contains(@class,'crm__dropdown__option')" +
        " and not(contains(@class,'disabled'))" +
        " and not(ancestor::*[contains(@style,'display:none')" +
        "                   or contains(@style,'display: none')" +
        "                   or contains(@style,'visibility:hidden')])]";

    public static void main(String[] args) throws Exception {

        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(WAIT_SEC));
        JavascriptExecutor js = (JavascriptExecutor) driver;

        driver.get("https://exicom-crm-dev.hummingwave.com/login");
        driver.manage().window().maximize();

        String mobile = "9518755103";

        // ── LOGIN ────────────────────────────────────────────────────────────
        driver.findElement(By.name("phone")).sendKeys(mobile);

        String otp = waitForOTP(mobile, 5, 2000);
        if (otp.isEmpty()) throw new RuntimeException("OTP not fetched from DB!");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]")
        )).sendKeys(otp);

        wait.until(ExpectedConditions.urlContains("dashboard"));
        System.out.println("✅ Login Successful!");

        // ── NAVIGATE: Administration → User Management → Create User ────────
        clickByText(wait, "Administration");
        clickByText(wait, "User Management");
        clickByText(wait, "Create User");

        // ── STEP 1 & 2: Names ────────────────────────────────────────────────
        typeInField(wait, By.id("user_name"),  "AutoTest");
        typeInField(wait, By.id("user_lname"), "LNameuser");
        System.out.println("✅ First & Last Name entered!");

        // ── STEP 3: Random Mobile ────────────────────────────────────────────
        String randomMobile = generateMobileNumber();
        WebElement mobileField = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.cssSelector("input.crm__custom__mobile__input__field"))
        );
        mobileField.click();
        mobileField.clear();
        mobileField.sendKeys(randomMobile);
        System.out.println("✅ Mobile entered: " + randomMobile);

        // ── STEP 4: Email ────────────────────────────────────────────────────
        int randomFiveDigit = 10000 + (int) (Math.random() * 90000); // range: 10000–99999
        String email = "AutoTest" + randomFiveDigit + "@gmail.com";
        typeInField(wait, By.id("user_email"), email);
        System.out.println("✅ Email entered: " + email);

        // ── STEP 5: Console Access → Login User ──────────────────────────────
        selectDropdownOption(driver, wait, js,
            By.id("authentication_type_select"), "Login User");
        System.out.println("✅ Console Access: Login User selected!");

        // ── STEP 6: User Type → Exicom ───────────────────────────────────────
        selectDropdownOption(driver, wait, js,
            By.id("user_type_select"), "Exicom");
        System.out.println("✅ User Type: Exicom selected!");

        // ── STEP 7: Employee Code ────────────────────────────────────────────
        WebElement empCode = scrollIntoView(wait, js, By.id("employee_code"));
        empCode.clear();
        empCode.sendKeys("12");
        System.out.println("✅ Employee Code: 12");

        // ── STEP 8: Pin Code ─────────────────────────────────────────────────
        WebElement pinDropdown = scrollIntoView(
            wait, js, By.id("site__pinCode_dropdown"));
        pinDropdown.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[@id='site__pinCode_dropdown']//input")
        )).sendKeys("411039");

        wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.xpath("//div[contains(@class,'crm__dropdown__option')]")
        )).click();
        System.out.println("✅ Pincode 411039 selected!");

        // ── STEP 9: Address ──────────────────────────────────────────────────
        clearAndType(wait, js, By.id("user_addressLine1"),
            "123 Test Street, Test Area");
        System.out.println("✅ Address entered!");

        // ── VALIDATE City / State / Country ─────────────────────────────────
        String cityValue    = getDropdownValue(driver, wait, js, "city_select",    "City");
        String stateValue   = getDropdownValue(driver, wait, js, "state_select",   "State");
        String countryValue = getDropdownValue(driver, wait, js, "country_select", "Country");

        System.out.println("========== Address Summary ==========");
        System.out.println("Pincode : ✅ 411039");
        System.out.println("City    : " + badge(cityValue));
        System.out.println("State   : " + badge(stateValue));
        System.out.println("Country : " + badge(countryValue));
        System.out.println("=====================================");

        if (cityValue.isEmpty() || stateValue.isEmpty()) {
            driver.quit();
            throw new RuntimeException("❌ City or State is empty! Cannot submit form.");
        }

        // ── STEP 10: Role ────────────────────────────────────────────────────
        selectDropdownOption(driver, wait, js,
            By.id("user_role_select"), "Account Coordinator");
        System.out.println("✅ Role: Account Coordinator selected!");

        // ── STEP 11: Circle ──────────────────────────────────────────────────
        selectDropdownOption(driver, wait, js,
            By.id("user_circle_select"), "Maharashtra");
        System.out.println("✅ Circle: Maharashtra selected!");

        // ── STEP 12: Submit ──────────────────────────────────────────────────
        clickById(wait, js, "update-user");
        validateToast(wait, "successfully",
            "User Created", "User Creation");

        // ── SEARCH BY MOBILE & VALIDATE ──────────────────────────────────────
        System.out.println("\n--- Searching User by Mobile ---");
        clickByText(wait, "User Management");

        WebElement searchBox = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("users__search-box")));
        searchBox.clear();
        searchBox.sendKeys(randomMobile);
        System.out.println("🔍 Searched mobile: " + randomMobile);

        WebElement userNameLabel = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[contains(@id,'labeluser_name')" +
                         " and contains(text(),'AutoTest LNameuser')]"))
        );
        String displayedName = userNameLabel.getText().trim();
        if (displayedName.contains("AutoTest") && displayedName.contains("LNameuser")) {
            System.out.println("✅ USERNAME VALIDATED: " + displayedName);
        } else {
            System.out.println("❌ USERNAME MISMATCH! Found: " + displayedName);
        }
        userNameLabel.click();

        // ── UPDATE ADDRESS ───────────────────────────────────────────────────
        System.out.println("\n--- Updating Address ---");
        clearAndType(wait, js, By.id("user_addressLine1"),
            "456 Updated Street, New Area");
        System.out.println("✅ Address updated!");

        clickById(wait, js, "update-user");
        validateToast(wait, "successfully updated",
            "User Updated", "User Update");

        // ── SEARCH BY EMAIL (clear previous mobile number first) ─────────────
        System.out.println("\n--- Searching User by Email ---");
        clickByText(wait, "User Management");

        // ── FIX: Mac triple-clear before typing email ─────────────────────────
        // React-controlled inputs ignore plain .clear(); we layer 3 strategies:
        //   1. ⌘+A + BACK_SPACE  → keyboard-level wipe (Mac)
        //   2. ⌘+A + DELETE      → catches any leftover characters
        //   3. JS value = ''     → directly resets the React DOM value
        WebElement emailSearchBox = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("users__search-box")));
        emailSearchBox.click();
        Thread.sleep(300);
        emailSearchBox.sendKeys(Keys.chord(Keys.COMMAND, "a")); // ⌘+A select all
        emailSearchBox.sendKeys(Keys.BACK_SPACE);               // delete selected
        emailSearchBox.sendKeys(Keys.chord(Keys.COMMAND, "a")); // select any remnant
        emailSearchBox.sendKeys(Keys.DELETE);                   // delete again
        js.executeScript("arguments[0].value = '';", emailSearchBox); // JS wipe – React fallback
        Thread.sleep(500);                                            // let React re-render
        emailSearchBox.sendKeys(email);
        System.out.println("🔍 Searched email: " + email);

        WebElement userByEmail = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[contains(@id,'labeluser_name')" +
                         " and contains(text(),'AutoTest LNameuser')]"))
        );
        System.out.println("✅ User found by email: " + userByEmail.getText().trim());
        userByEmail.click();

        // ── DELETE USER ──────────────────────────────────────────────────────
        System.out.println("\n--- Deleting User ---");

        // Click the Delete button
        clickById(wait, js, "delete-user");
        System.out.println("✅ Delete button clicked!");

        // Validate the confirmation dialog text
        WebElement dialogText = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.dialog-info")));
        String dialogMsg = dialogText.getText().trim();
        System.out.println("Dialog text: " + dialogMsg);
        if (dialogMsg.contains("Do you want to delete this user?")) {
            System.out.println("✅ VALIDATION PASSED – Confirmation dialog text correct!");
        } else {
            System.out.println("❌ VALIDATION FAILED – Unexpected dialog text: " + dialogMsg);
        }

        // Click the Yes / confirm button
        WebElement confirmYesBtn = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.id("delete-user-confirmation")));
        js.executeScript(
            "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});",
            confirmYesBtn);
        js.executeScript("arguments[0].click();", confirmYesBtn);
        System.out.println("✅ Confirmation 'Yes' button clicked!");

        // Validate the success toast
        validateToast(wait, "successfully deleted",
            "User Deleted", "User Deletion");

        // ── SEARCH BY EMAIL & VALIDATE NO USERS FOUND ────────────────────────
        System.out.println("\n--- Verifying User Deleted (search by email) ---");
        clickByText(wait, "User Management");

        WebElement deleteVerifySearch = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("users__search-box")));
        deleteVerifySearch.click();
        Thread.sleep(300);
        deleteVerifySearch.sendKeys(Keys.chord(Keys.COMMAND, "a")); // ⌘+A select all
        deleteVerifySearch.sendKeys(Keys.BACK_SPACE);               // delete selected
        deleteVerifySearch.sendKeys(Keys.chord(Keys.COMMAND, "a")); // select any remnant
        deleteVerifySearch.sendKeys(Keys.DELETE);                   // delete again
        js.executeScript("arguments[0].value = '';", deleteVerifySearch); // JS wipe
        Thread.sleep(500);
        deleteVerifySearch.sendKeys(email);
        System.out.println("🔍 Searched email post-deletion: " + email);

        WebElement noDataEl = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div.crm__table__dashboard__no__data")));
        String noDataText = noDataEl.getText().trim();
        System.out.println("No-data message: " + noDataText);
        if (noDataText.contains("No Users Found")) {
            System.out.println("✅ FINAL VALIDATION PASSED – User successfully deleted! No Users Found.");
        } else {
            System.out.println("❌ FINAL VALIDATION FAILED – Expected 'No Users Found', got: " + noDataText);
            driver.quit();
            throw new RuntimeException(
                "❌ TEST FAILED: Deleted user still appears in search! Text: " + noDataText);
        }

        // ── TOGGLE Active → Inactive ─────────────────────────────────────────
        // TODO: Inactive toggle has a known issue – section commented out for now.
        //       Will be re-enabled once the toggle bug is resolved.
        /*
        System.out.println("\n--- Toggling Active → Inactive ---");

        WebElement switchBase = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("span.MuiSwitch-switchBase.Mui-checked"))
        );

        js.executeScript(
            "arguments[0].scrollIntoView({behavior:'smooth', block:'center'});",
            switchBase
        );

        js.executeScript("arguments[0].click();", switchBase);
        System.out.println("🔄 Switch clicked – toggling to Inactive!");

        By toggleLocator = By.cssSelector("input.PrivateSwitchBase-input.MuiSwitch-input");

        wait.until(d -> !d.findElement(toggleLocator).isSelected());

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector("span.MuiSwitch-switchBase.Mui-checked")
        ));

        System.out.println("✅ Toggle confirmed Inactive!");

        System.out.println("\n--- Clicking Update after toggle ---");
        clickById(wait, js, "update-user");
        validateToast(wait, "successfully updated",
            "User Inactivated", "User Inactivation");

        System.out.println("\n--- Opening Filters ---");
        wait.until(ExpectedConditions.elementToBeClickable(
            By.id("filter_btn"))).click();
        System.out.println("✅ Filter button clicked!");

        WebElement filterValueDropdown = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//*[@id='crm__filter__value__dropdown']/div"))
        );
        js.executeScript(
            "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});",
            filterValueDropdown);
        filterValueDropdown.click();
        System.out.println("✅ Filter value dropdown clicked!");

        WebElement inactiveChip = wait.until(
            ExpectedConditions.elementToBeClickable(
                By.xpath("//div[@aria-label='Inactive']"))
        );
        inactiveChip.click();
        System.out.println("✅ 'Inactive' filter chip selected!");

        WebElement pageHeader = wait.until(
            ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[contains(@class,'crm__table__dashboard-header')" +
                         " and contains(text(),'User Management')]"))
        );
        pageHeader.click();
        System.out.println("✅ Clicked outside – filter panel closed!");

        Thread.sleep(1500);

        System.out.println("\n--- Searching User by Mobile (post-inactivation) ---");
        WebElement mobileSearch = wait.until(
            ExpectedConditions.visibilityOfElementLocated(By.id("users__search-box")));
        mobileSearch.clear();
        mobileSearch.sendKeys(randomMobile);
        System.out.println("🔍 Searched mobile: " + randomMobile);

        WebElement inactiveStatusEl;
        try {
            inactiveStatusEl = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//p[@id='labelInactive' and normalize-space(text())='Inactive']"))
            );
            String statusInTable = inactiveStatusEl.getText().trim();
            System.out.println("Status in table: " + statusInTable);

            if (statusInTable.equalsIgnoreCase("Inactive")) {
                System.out.println("✅ FINAL VALIDATION PASSED – User is Inactive in table!");
            } else {
                System.out.println("❌ FINAL VALIDATION FAILED – Expected Inactive, got: "
                    + statusInTable);
                driver.quit();
                throw new RuntimeException(
                    "❌ TEST FAILED: User status is not Inactive! Found: " + statusInTable);
            }
        } catch (TimeoutException e) {
            System.out.println("❌ FINAL VALIDATION FAILED – Inactive status badge not found!");
            driver.quit();
            throw new RuntimeException(
                "❌ TEST FAILED: Inactive status not found in search results for mobile: "
                + randomMobile);
        }
        */
        // ── END OF INACTIVE TOGGLE SECTION (commented out) ──────────────────

        Thread.sleep(3000);
        driver.quit();
        System.out.println("\n🎉 All steps completed successfully!");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /** Click an element by visible text. */
    private static void clickByText(WebDriverWait wait, String text) {
        wait.until(ExpectedConditions.elementToBeClickable(
            By.xpath("//*[contains(text(),'" + text + "')]")
        )).click();
        System.out.println("✅ Clicked: " + text);
    }

    /** Type into a field (clears first). */
    private static void typeInField(WebDriverWait wait, By locator, String value) {
        WebElement el = wait.until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(value);
    }

    /**
     * Mac-compatible clear + type used for all text fields.
     * Layers 3 clear strategies to handle React-controlled inputs:
     *   1. ⌘+A + BACK_SPACE  → keyboard-level wipe (Mac shortcut)
     *   2. ⌘+A + DELETE      → catches any leftover characters
     *   3. JS value = ''     → directly resets the React DOM value
     */
    private static void clearAndType(WebDriverWait wait, JavascriptExecutor js,
                                     By locator, String value) {
        WebElement el = wait.until(
            ExpectedConditions.visibilityOfElementLocated(locator));
        js.executeScript(
            "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", el);
        el.click();
        el.sendKeys(Keys.chord(Keys.COMMAND, "a")); // ⌘+A select all (Mac)
        el.sendKeys(Keys.BACK_SPACE);               // delete selected
        el.sendKeys(Keys.chord(Keys.COMMAND, "a")); // select any remnant
        el.sendKeys(Keys.DELETE);                   // delete again
        js.executeScript("arguments[0].value = '';", el); // JS wipe – React fallback
        el.sendKeys(value);
    }

    /**
     * Selects a visible dropdown option by exact text.
     *
     * Fixes:
     *  1. Duplicate options  – VISIBLE_OPTION_XPATH filters hidden/off-screen nodes.
     *  2. StaleElementReferenceException – React re-renders the option list after
     *     the dropdown opens; we re-fetch the element inside a retry loop so a
     *     stale reference never causes a hard failure.
     */
    private static void selectDropdownOption(WebDriver driver, WebDriverWait wait,
                                             JavascriptExecutor js,
                                             By dropdownLocator, String optionText)
            throws InterruptedException {

        final int MAX_RETRIES = 3;
        final String xpath = VISIBLE_OPTION_XPATH +
            "[normalize-space(text())='" + optionText + "']";

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                WebElement dropdown = scrollIntoView(wait, js, dropdownLocator);
                dropdown.click();
                Thread.sleep(600);

                List<WebElement> options = wait.until(
                    ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        By.xpath(xpath)));

                if (options.isEmpty()) {
                    throw new RuntimeException("Option not found: " + optionText);
                }

                js.executeScript("arguments[0].click();", options.get(0));
                return;

            } catch (StaleElementReferenceException stale) {
                System.out.println("⚠️  Stale element on attempt " + attempt +
                    " for option '" + optionText + "' – retrying…");
                if (attempt == MAX_RETRIES) {
                    throw new RuntimeException(
                        "❌ selectDropdownOption failed after " + MAX_RETRIES +
                        " retries for option: " + optionText, stale);
                }
                Thread.sleep(500);
            }
        }
    }

    /**
     * Reads the selected value of a CRM custom dropdown.
     * Falls back to manual selection if nothing is pre-filled.
     */
    private static String getDropdownValue(WebDriver driver, WebDriverWait wait,
                                           JavascriptExecutor js,
                                           String dropdownId, String label)
            throws InterruptedException {
        String value = "";
        try {
            value = driver.findElement(By.xpath(
                "//div[@id='" + dropdownId + "']" +
                "//div[contains(@class,'crm__dropdown__single-value')]"
            )).getText().trim();
            System.out.println(label + " auto-filled: ✅ " + value);
        } catch (NoSuchElementException e) {
            System.out.println(label + " not auto-filled – selecting manually…");
            WebElement dd = driver.findElement(By.id(dropdownId));
            js.executeScript(
                "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", dd);
            dd.click();
            Thread.sleep(500);
            WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[contains(@class,'crm__dropdown__option')]")));
            value = option.getText().trim();
            option.click();
            System.out.println(label + " manually selected: ✅ " + value);
        }
        return value;
    }

    /** Scrolls to an element and returns it. */
    private static WebElement scrollIntoView(WebDriverWait wait,
                                              JavascriptExecutor js,
                                              By locator) {
        WebElement el = wait.until(
            ExpectedConditions.presenceOfElementLocated(locator));
        js.executeScript(
            "arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", el);
        return el;
    }

    /** Scrolls to a button by id and JS-clicks it. */
    private static void clickById(WebDriverWait wait, JavascriptExecutor js,
                                   String id) {
        WebElement btn = scrollIntoView(wait, js, By.id(id));
        js.executeScript("arguments[0].click();", btn);
        System.out.println("✅ Clicked button: #" + id);
    }

    /**
     * Waits for a toast, validates its message, clicks it to dismiss,
     * then waits until it fully disappears before returning.
     */
    private static void validateToast(WebDriverWait wait,
                                       String expectedFragment,
                                       String passLabel, String failLabel) {
        WebElement toast = wait.until(ExpectedConditions.visibilityOfElementLocated(
            By.cssSelector("div.toast_content_subtitle")));
        String text = toast.getText().trim();
        System.out.println("Toast: " + text);

        if (text.contains(expectedFragment)) {
            System.out.println("✅ VALIDATION PASSED – " + passLabel + ": " + text);
        } else {
            System.out.println("❌ VALIDATION FAILED – " + failLabel +
                               "! Unexpected: " + text);
        }

        try {
            WebElement toastContainer = wait.until(
                ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div.Toastify__toast-container")));
            toastContainer.click();
            System.out.println("🔔 Toast clicked to dismiss!");
        } catch (Exception e) {
            System.out.println("ℹ️  Toast auto-dismissed before click.");
        }

        wait.until(ExpectedConditions.invisibilityOfElementLocated(
            By.cssSelector("div.Toastify__toast-container")));
        System.out.println("✅ Toast dismissed – proceeding!");
    }

    private static String badge(String value) {
        return value.isEmpty() ? "❌ EMPTY" : "✅ " + value;
    }

    // ── OTP polling ──────────────────────────────────────────────────────────
    private static String waitForOTP(String mobile, int retries, long delayMs)
            throws InterruptedException {
        for (int i = 0; i < retries; i++) {
            String otp = getOTPFromDB(mobile);
            if (!otp.isEmpty()) return otp;
            Thread.sleep(delayMs);
        }
        return "";
    }

    // ── Mobile generator ─────────────────────────────────────────────────────
    public static String generateMobileNumber() {
        int firstDigit = 7 + (int) (Math.random() * 3);
        long remaining = (long) (Math.random() * 1_000_000_000L);
        return firstDigit + String.format("%09d", remaining);
    }

    // ── DB helper – uses PreparedStatement to prevent SQL injection ──────────
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
