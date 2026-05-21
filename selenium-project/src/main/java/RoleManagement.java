import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import java.sql.*;
import java.time.Duration;
import java.util.List;

public class RoleManagement {

	// ── Shared wait timeout ──────────────────────────────────────────────────
	private static final int WAIT_SEC = 20;

	// Role name used across create + search validation
	private static final String ROLE_NAME = "AutoRoleTest";

	// Picks only the VISIBLE, interactive dropdown options – fixes duplicate issue
	private static final String VISIBLE_OPTION_XPATH = "//div[contains(@class,'crm__dropdown__option')"
			+ " and not(contains(@class,'disabled'))" + " and not(ancestor::*[contains(@style,'display:none')"
			+ "                   or contains(@style,'display: none')"
			+ "                   or contains(@style,'visibility:hidden')])]";

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
		if (otp.isEmpty())
			throw new RuntimeException("OTP not fetched from DB!");

		wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]")))
				.sendKeys(otp);

		wait.until(ExpectedConditions.urlContains("dashboard"));
		System.out.println("✅ Login Successful!");

		// ── NAVIGATE: Administration → Role Management ───────────────────────
		clickByText(wait, "Administration");

		wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//p[contains(@class,'crm__sidebar__text') and text()='Role Management']"))).click();
		System.out.println("✅ Clicked: Role Management");

		// ── CLICK CREATE ROLE BUTTON ─────────────────────────────────────────
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='add_new_btn']"))).click();
		System.out.println("✅ Clicked: Create Role button");

		// ── ENTER ROLE NAME ──────────────────────────────────────────────────
		typeInField(wait, By.id("role_name"), ROLE_NAME);
		System.out.println("✅ Role Name entered: " + ROLE_NAME);

		// ── ENTER DESCRIPTION ────────────────────────────────────────────────
		typeInField(wait, By.id("role_desc"), "Role created for Automation test");
		System.out.println("✅ Description entered: Role created for Automation test");
		Thread.sleep(4000);

		// ── CLICK NEXT BUTTON (Step 1 → Step 2: Permissions) ────────────────
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='next']"))).click();
		System.out.println("✅ Clicked: Next button (moving to Permissions screen)");
		Thread.sleep(1000);

		// ════════════════════════════════════════════════════════════════════
		// STEP 2: PERMISSIONS SCREEN
		// ════════════════════════════════════════════════════════════════════

		// ── DASHBOARD: click the checkbox to select the whole row ────────────
		clickRoleCheckbox(driver, wait, js, "Dashboard");
		System.out.println("✅ Dashboard checkbox selected!");

		// ── BOOKING: click the checkbox to select the whole row ──────────────
		clickRoleCheckbox(driver, wait, js, "Booking");
		System.out.println("✅ Booking checkbox selected!");

		// ── BOOKING: click sub-permissions Edit, Generate Irf, Create Booking ─
		clickRoleItem(driver, wait, js, "Booking", "Edit");
		System.out.println("✅ Booking → Edit selected!");

		clickRoleItem(driver, wait, js, "Booking", "Generate Irf");
		System.out.println("✅ Booking → Generate Irf selected!");

		clickRoleItem(driver, wait, js, "Booking", "Create Booking");
		System.out.println("✅ Booking → Create Booking selected!");

		// ── TASK: click the checkbox to select the whole row ─────────────────
		clickRoleCheckbox(driver, wait, js, "Task");
		System.out.println("✅ Task checkbox selected!");

		// ── TASK: click sub-permissions Edit, Assignment, Reschedule ─────────
		clickRoleItem(driver, wait, js, "Task", "Edit");
		System.out.println("✅ Task → Edit selected!");

		clickRoleItem(driver, wait, js, "Task", "Assignment");
		System.out.println("✅ Task → Assignment selected!");

		clickRoleItem(driver, wait, js, "Task", "Reschedule");
		System.out.println("✅ Task → Reschedule selected!");

		// ── CLICK NEXT (Step 2 → Step 3: Preview) ───────────────────────────
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='next']"))).click();
		System.out.println("✅ Clicked: Next button (moving to Preview screen)");
		Thread.sleep(1500);

		// ════════════════════════════════════════════════════════════════════
		// STEP 3: PREVIEW / VALIDATION SCREEN
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Preview Validation ==========");

		validatePreviewRow(driver, "Role", ROLE_NAME);
		validatePreviewRow(driver, "Description", "Role created for Automation test");
		validatePreviewRow(driver, "Dashboard", "View");
		validatePreviewRow(driver, "Booking", "View, Create Booking, Edit, Generate Irf");
		validatePreviewRow(driver, "Task", "View, Edit, Assignment, Reschedule");

		System.out.println("========== Preview Validation Done ==========\n");

		// ── CLICK CREATE BUTTON ──────────────────────────────────────────────
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@id='create_btn']"))).click();
		System.out.println("✅ Clicked: Create button");

		// ════════════════════════════════════════════════════════════════════
		// STEP 4: TOAST VALIDATION – assert success message then dismiss
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Toast Validation ==========");
		validateAndDismissToast(wait, "Role has been successfully created.", "Role has been successfully created.");
		System.out.println("========== Toast Validation Done ==========\n");

		// ════════════════════════════════════════════════════════════════════
		// STEP 5: SEARCH VALIDATION – confirm the new role appears in the list
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Search Validation ==========");
		searchAndValidateRole(driver, wait, js, ROLE_NAME);
		System.out.println("========== Search Validation Done ==========\n");

		// ════════════════════════════════════════════════════════════════════
		// STEP 6: CLICK EDIT BUTTON (role label already clicked in search step)
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Edit Role Flow ==========");
		Thread.sleep(1000);

		// ── CLICK EDIT BUTTON ────────────────────────────────────────────────
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@id='edit-role']"))).click();
		System.out.println("✅ Clicked: Edit Role button");
		Thread.sleep(2000); // wait for permissions screen to fully render

		// ════════════════════════════════════════════════════════════════════
		// STEP 7: CUSTOMER PERMISSIONS
		// ════════════════════════════════════════════════════════════════════

		// ── CUSTOMER: click the checkbox ─────────────────────────────────────
		// Checking the checkbox auto-enables "View"; clicking View again would
		// toggle it OFF.  Only click the additional permissions explicitly.
		clickRoleCheckbox(driver, wait, js, "Customer");
		System.out.println("✅ Customer checkbox selected! (View auto-enabled)");
		Thread.sleep(600); // let the UI settle before clicking sub-items

		clickRoleItem(driver, wait, js, "Customer", "Edit");
		System.out.println("✅ Customer → Edit selected!");

		clickRoleItem(driver, wait, js, "Customer", "Create");
		System.out.println("✅ Customer → Create selected!");

		clickRoleItem(driver, wait, js, "Customer", "Delete");
		System.out.println("✅ Customer → Delete selected!");

		// ════════════════════════════════════════════════════════════════════
		// STEP 8: TICKET PERMISSIONS
		// ════════════════════════════════════════════════════════════════════

		// ── TICKET: click the checkbox ────────────────────────────────────────
		// Same as Customer — checkbox auto-enables "View".
		// List below excludes "View" to avoid toggling it back off.
		clickRoleCheckbox(driver, wait, js, "Ticket");
		System.out.println("✅ Ticket checkbox selected! (View auto-enabled)");
		Thread.sleep(600); // let the UI settle before clicking sub-items

		// ── TICKET: select all required sub-permissions (View already on) ────
		String[] ticketPermissions = {
			"Edit", "Create", "Assignment", "Reschedule", "Resolve",
			"On Hold", "Oow", "Close", "Discard", "Re Open", "Approval",
			"Verify", "Doa Approval", "Doa Submission"
		};

		for (String perm : ticketPermissions) {
			clickRoleItem(driver, wait, js, "Ticket", perm);
			System.out.println("✅ Ticket → " + perm + " selected!");
		}

		// ════════════════════════════════════════════════════════════════════
		// STEP 9: MASTERS AND CONFIGURATION – scroll down then click checkbox
		// ════════════════════════════════════════════════════════════════════

		// Scroll down so Masters And Configuration is in view
		WebElement mastersCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//div[contains(@class,'role__title__text') and text()='Masters And Configuration']"
						+ "/preceding-sibling::button[contains(@class,'MuiIconButton-root')]")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", mastersCheckbox);
		Thread.sleep(600);
		js.executeScript("arguments[0].click();", mastersCheckbox);
		System.out.println("✅ Masters And Configuration checkbox selected!");

		// By default View is selected for the sub-labels; just log them
		String[] mastersSubLabels = {
			"Product Management", "Material Management",
			"Define TAT Category", "Define Distance Range"
		};
		System.out.println("ℹ️  Masters And Configuration sub-labels (View selected by default):");
		for (String label : mastersSubLabels) {
			System.out.println("   • " + label + " → View (default)");
		}

		// ════════════════════════════════════════════════════════════════════
		// STEP 10: EXPENSE MANAGEMENT – click checkbox
		// ════════════════════════════════════════════════════════════════════

		WebElement expenseCheckbox = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//div[contains(@class,'role__title__text') and text()='Expense Management']"
						+ "/preceding-sibling::button[contains(@class,'MuiIconButton-root')]")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", expenseCheckbox);
		Thread.sleep(600);
		js.executeScript("arguments[0].click();", expenseCheckbox);
		System.out.println("✅ Expense Management checkbox selected!");

		// By default View is selected for the sub-labels; just log them
		String[] expenseSubLabels = {
			"Expense Profile", "Expense Dashboard", "My Request", "Expense Detail"
		};
		System.out.println("ℹ️  Expense Management sub-labels (View selected by default):");
		for (String label : expenseSubLabels) {
			System.out.println("   • " + label + " → View (default)");
		}

		// ════════════════════════════════════════════════════════════════════
		// STEP 11: CLICK UPDATE BUTTON
		// ════════════════════════════════════════════════════════════════════

		WebElement updateBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//button[@id='update_btn']")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", updateBtn);
		Thread.sleep(500);
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@id='update_btn']"))).click();
		System.out.println("✅ Clicked: Update button");

		// ════════════════════════════════════════════════════════════════════
		// STEP 12: TOAST VALIDATION for Update – MANDATORY (fail if missing)
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Update Toast Validation ==========");
		validateAndDismissToast(wait, "Role has been successfully updated.", "Role has been successfully updated.");
		System.out.println("========== Update Toast Validation Done ==========\n");

		System.out.println("========== Edit Role Flow Done ==========\n");

		// ════════════════════════════════════════════════════════════════════
		// STEP 13: RE-SEARCH THE ROLE AND VALIDATE UPDATED PREVIEW VALUES
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Post-Update Search & Preview Validation ==========");

		// Clear search box and re-search
		By searchBoxLocator = By.id("roles__search-box");
		WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(searchBoxLocator));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", searchBox);
		searchBox.click();
		searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		searchBox.sendKeys(Keys.BACK_SPACE);
		js.executeScript("arguments[0].value = '';", searchBox);
		Thread.sleep(800);
		searchBox.sendKeys(ROLE_NAME);
		System.out.println("🔍 Re-searching for role: " + ROLE_NAME);
		Thread.sleep(1500);

		// Click the role label to open the detail panel
		By roleLabelLocator = By.xpath(
				"//p[@id='labelvendor__code' and normalize-space(text())='" + ROLE_NAME + "']");
		WebElement roleLabel;
		try {
			roleLabel = wait.until(ExpectedConditions.visibilityOfElementLocated(roleLabelLocator));
		} catch (TimeoutException e) {
			throw new RuntimeException(
					"❌ TEST FAILED – Role '" + ROLE_NAME + "' not found in search after update.", e);
		}
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", roleLabel);
		Thread.sleep(400);
		js.executeScript("arguments[0].click();", roleLabel);
		System.out.println("✅ Clicked role label to open detail panel for preview validation.");
		Thread.sleep(1500);

		// Validate all updated preview rows (order doesn't matter; each label is checked independently)
		System.out.println("\n--- Preview Row Validation after Update ---");
		validatePreviewRow(driver, "Role",        ROLE_NAME);
		validatePreviewRow(driver, "Description", "Role created for Automation test");
		validatePreviewRow(driver, "Dashboard",   "View");
		validatePreviewRow(driver, "Booking",     "View, Edit, Generate Irf, Create Booking");
		validatePreviewRow(driver, "Task",        "View, Edit, Assignment, Reschedule");
		validatePreviewRow(driver, "Customer",    "View, Edit, Create, Delete");
		validatePreviewRow(driver, "Ticket",
				"View, Edit, Create, Assignment, Reschedule, Resolve, On Hold, Oow, Close, "
				+ "Discard, Re Open, Approval, Verify, Doa Approval, Doa Submission");
		validatePreviewRow(driver, "Material Management",  "View");
		validatePreviewRow(driver, "Define TAT Category",  "View");
		validatePreviewRow(driver, "Define Distance Range","View");
		validatePreviewRow(driver, "Expense Profile",      "View");
		validatePreviewRow(driver, "Expense Dashboard",    "View");
		validatePreviewRow(driver, "My Request",           "View");
		validatePreviewRow(driver, "Expense Detail",       "View");
		System.out.println("========== Post-Update Preview Validation Done ==========\n");

		// ════════════════════════════════════════════════════════════════════
		// STEP 14: DELETE THE ROLE
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Delete Role Flow ==========");

		// Click Delete button
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@id='delete-role']"))).click();
		System.out.println("✅ Clicked: Delete Role button");
		Thread.sleep(800);

		// Validate confirmation dialog text
		By dialogLocator = By.xpath(
				"//div[contains(@class,'dialog-info') and normalize-space()='Do you want to delete this role?']");
		WebElement dialogText;
		try {
			dialogText = wait.until(ExpectedConditions.visibilityOfElementLocated(dialogLocator));
		} catch (TimeoutException e) {
			throw new RuntimeException(
					"❌ TEST FAILED – Delete confirmation dialog did not appear.", e);
		}
		System.out.println("✅ Confirmation dialog visible: \"" + dialogText.getText().trim() + "\"");

		// Confirm deletion
		wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//button[@id='delete-user-confirmation']"))).click();
		System.out.println("✅ Clicked: Confirm delete button");

		// ════════════════════════════════════════════════════════════════════
		// STEP 15: VALIDATE DELETE TOAST – MANDATORY (fail if missing)
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Delete Toast Validation ==========");
		validateAndDismissToast(wait,
				"Role has been successfully deleted.",
				"Role has been successfully deleted.");
		System.out.println("========== Delete Toast Validation Done ==========\n");

		// ════════════════════════════════════════════════════════════════════
		// STEP 16: VERIFY ROLE NO LONGER EXISTS IN SEARCH RESULTS
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Post-Delete Search Validation ==========");

		// Clear search box fully
		WebElement searchBoxAfterDelete = wait.until(
				ExpectedConditions.visibilityOfElementLocated(By.id("roles__search-box")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", searchBoxAfterDelete);
		searchBoxAfterDelete.click();
		searchBoxAfterDelete.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		searchBoxAfterDelete.sendKeys(Keys.BACK_SPACE);
		js.executeScript("arguments[0].value = '';", searchBoxAfterDelete);
		Thread.sleep(800);

		// Re-search with the deleted role name
		searchBoxAfterDelete.sendKeys(ROLE_NAME);
		System.out.println("🔍 Searching for deleted role: " + ROLE_NAME);
		Thread.sleep(1500);

		// Expect the "No Roles Found" empty-state element
		By noDataLocator = By.xpath(
				"//div[contains(@class,'crm__table__dashboard__no__data') "
				+ "and normalize-space()='No Roles Found']");
		try {
			WebElement noData = wait.until(ExpectedConditions.visibilityOfElementLocated(noDataLocator));
			System.out.println("✅ POST-DELETE VALIDATION PASSED – Empty state shown: \""
					+ noData.getText().trim() + "\"");
		} catch (TimeoutException e) {
			throw new RuntimeException(
					"❌ TEST FAILED – 'No Roles Found' message did not appear after deletion. "
					+ "The role '" + ROLE_NAME + "' may still exist in the list.", e);
		}
		System.out.println("========== Post-Delete Search Validation Done ==========\n");

		driver.quit();
		System.out.println("\n🎉 Role Management steps completed successfully!");
	}

	// ════════════════════════════════════════════════════════════════════════
	// TOAST VALIDATION
	// ════════════════════════════════════════════════════════════════════════
	private static void validateAndDismissToast(WebDriverWait wait,
			String expectedMessage, String stepLabel) {

		// ── 1. Wait for the toast subtitle element to become visible ────────
		By toastSubtitleLocator = By.cssSelector("div.toast_content_subtitle");

		WebElement toast;
		try {
			toast = wait.until(
					ExpectedConditions.visibilityOfElementLocated(toastSubtitleLocator));
		} catch (TimeoutException e) {
			// Toast never appeared — mandatory failure
			throw new RuntimeException(
					"❌ TEST FAILED – Toast did not appear for step [" + stepLabel + "]. "
					+ "Expected message: \"" + expectedMessage + "\"", e);
		}

		String actualText = toast.getText().trim();
		System.out.println("🔔 Toast message: \"" + actualText + "\"");

		// ── 2. Validate the message ──────────────────────────────────────────
		if (actualText.contains(expectedMessage)) {
			System.out.println("✅ TOAST VALIDATION PASSED – " + stepLabel + ": " + actualText);
		} else {
			// Fail immediately — wrong toast text means something went wrong
			throw new RuntimeException(
					"❌ TEST FAILED – Toast text mismatch for [" + stepLabel + "].\n"
					+ "   Expected to contain : \"" + expectedMessage + "\"\n"
					+ "   Actual              : \"" + actualText + "\"");
		}

		// ── 3. Click the toast to dismiss it ────────────────────────────────
		try {
			By toastContainerLocator = By.cssSelector("div.Toastify__toast-container");
			WebElement toastContainer = wait.until(
					ExpectedConditions.elementToBeClickable(toastContainerLocator));
			toastContainer.click();
			System.out.println("🔔 Toast clicked to dismiss!");
		} catch (Exception e) {
			System.out.println("ℹ️  Toast auto-dismissed before click could execute.");
		}

		// ── 4. Wait until the toast is fully gone ───────────────────────────
		wait.until(ExpectedConditions
				.invisibilityOfElementLocated(By.cssSelector("div.Toastify__toast-container")));
		System.out.println("✅ Toast dismissed – page is ready for next step.");
	}

	// ════════════════════════════════════════════════════════════════════════
	// SEARCH & VALIDATE ROLE
	// ════════════════════════════════════════════════════════════════════════
	private static void searchAndValidateRole(WebDriver driver, WebDriverWait wait,
			JavascriptExecutor js, String expectedRole) throws InterruptedException {

		// ── 1. Locate and clear the search box ──────────────────────────────
		By searchBoxLocator = By.id("roles__search-box");

		WebElement searchBox = wait.until(
				ExpectedConditions.visibilityOfElementLocated(searchBoxLocator));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", searchBox);

		// Clear any pre-existing text robustly (handles React-controlled input)
		searchBox.click();
		searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		searchBox.sendKeys(Keys.BACK_SPACE);
		js.executeScript("arguments[0].value = '';", searchBox);

		// ── 2. Type the role name to trigger the search ──────────────────────
		searchBox.sendKeys(expectedRole);
		System.out.println("🔍 Searching for role: " + expectedRole);

		// ── 3. Wait for the MuiDataGrid to refresh ───────────────────────────
		Thread.sleep(1500);

		// ── 4. Find the role name cell in the DataGrid results ───────────────

		By roleLabelLocator = By.xpath(
				"//p[@id='labelvendor__code' and normalize-space(text())='" + expectedRole + "']");

		WebElement roleLabel;
		try {
			roleLabel = wait.until(
					ExpectedConditions.visibilityOfElementLocated(roleLabelLocator));
		} catch (TimeoutException e) {
			throw new RuntimeException(
					"❌ TEST FAILED – Search returned no results for role: '" + expectedRole + "'", e);
		}

		String foundText = roleLabel.getText().trim();
		System.out.println("   Found row: " + foundText);

		if (foundText.equalsIgnoreCase(expectedRole)) {
			System.out.println("✅ SEARCH VALIDATION PASSED – Role '" + expectedRole + "' found in the list!");
		} else {
			throw new RuntimeException(
					"❌ TEST FAILED – Role label text '" + foundText
					+ "' does not match expected '" + expectedRole + "'.");
		}

		// ── 5. Click the role name to open the detail / side-panel ──────────
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", roleLabel);
		Thread.sleep(400);
		js.executeScript("arguments[0].click();", roleLabel);
		System.out.println("✅ Clicked role label to open detail view: " + foundText);
	}

	// ════════════════════════════════════════════════════════════════════════
	// HELPERS
	// ════════════════════════════════════════════════════════════════════════

	/** Click an element by visible text. */
	private static void clickByText(WebDriverWait wait, String text) {
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(),'" + text + "')]"))).click();
		System.out.println("✅ Clicked: " + text);
	}

	/** Type into a field (clears first). */
	private static void typeInField(WebDriverWait wait, By locator, String value) {
		WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
		el.clear();
		el.sendKeys(value);
	}

	/**
	 * Mac-compatible clear + type used for all text fields.
	 */
	@SuppressWarnings("unused")
	private static void clearAndType(WebDriverWait wait, JavascriptExecutor js, By locator, String value) {
		WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", el);
		el.click();
		el.sendKeys(Keys.chord(Keys.COMMAND, "a"));
		el.sendKeys(Keys.BACK_SPACE);
		el.sendKeys(Keys.chord(Keys.COMMAND, "a"));
		el.sendKeys(Keys.DELETE);
		js.executeScript("arguments[0].value = '';", el);
		el.sendKeys(value);
	}

	/**
	 * Selects a visible dropdown option by exact text.
	 */
	@SuppressWarnings("unused")
	private static void selectDropdownOption(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
			By dropdownLocator, String optionText) throws InterruptedException {

		final int MAX_RETRIES = 3;
		final String xpath = VISIBLE_OPTION_XPATH + "[normalize-space(text())='" + optionText + "']";

		for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
			try {
				WebElement dropdown = scrollIntoView(wait, js, dropdownLocator);
				dropdown.click();
				Thread.sleep(600);

				List<WebElement> options = wait
						.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(xpath)));

				if (options.isEmpty()) {
					throw new RuntimeException("Option not found: " + optionText);
				}

				js.executeScript("arguments[0].click();", options.get(0));
				return;

			} catch (StaleElementReferenceException stale) {
				System.out.println(
						"⚠️  Stale element on attempt " + attempt + " for option '" + optionText + "' – retrying…");
				if (attempt == MAX_RETRIES) {
					throw new RuntimeException(
							"❌ selectDropdownOption failed after " + MAX_RETRIES + " retries for option: " + optionText,
							stale);
				}
				Thread.sleep(500);
			}
		}
	}

	/** Scrolls to an element and returns it. */
	private static WebElement scrollIntoView(WebDriverWait wait, JavascriptExecutor js, By locator) {
		WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", el);
		return el;
	}

	/** Scrolls to a button by id and JS-clicks it. */
	@SuppressWarnings("unused")
	private static void clickById(WebDriverWait wait, JavascriptExecutor js, String id) {
		WebElement btn = scrollIntoView(wait, js, By.id(id));
		js.executeScript("arguments[0].click();", btn);
		System.out.println("✅ Clicked button: #" + id);
	}

	/**
	 * Waits for a toast, validates its message, clicks it to dismiss, then waits
	 * until it fully disappears before returning.
	 */
	@SuppressWarnings("unused")
	private static void validateToast(WebDriverWait wait, String expectedFragment, String passLabel, String failLabel) {
		WebElement toast = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.toast_content_subtitle")));
		String text = toast.getText().trim();
		System.out.println("Toast: " + text);

		if (text.contains(expectedFragment)) {
			System.out.println("✅ VALIDATION PASSED – " + passLabel + ": " + text);
		} else {
			System.out.println("❌ VALIDATION FAILED – " + failLabel + "! Unexpected: " + text);
		}

		try {
			WebElement toastContainer = wait
					.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.Toastify__toast-container")));
			toastContainer.click();
			System.out.println("🔔 Toast clicked to dismiss!");
		} catch (Exception e) {
			System.out.println("ℹ️  Toast auto-dismissed before click.");
		}

		wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.Toastify__toast-container")));
		System.out.println("✅ Toast dismissed – proceeding!");
	}

	/**
	 * Clicks the checkbox for a given role row title.
	 */
	private static void clickRoleCheckbox(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String roleTitle)
			throws InterruptedException {
		WebElement checkbox = wait.until(ExpectedConditions
				.elementToBeClickable(By.xpath("//div[contains(@class,'role__title__text') and text()='" + roleTitle
						+ "']" + "/preceding-sibling::button[contains(@class,'MuiIconButton-root')]")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", checkbox);
		Thread.sleep(400);
		js.executeScript("arguments[0].click();", checkbox);
	}

	/**
	 * Clicks a specific sub-permission item inside a named role row.
	 */
	private static void clickRoleItem(WebDriver driver, WebDriverWait wait, JavascriptExecutor js, String roleTitle,
			String itemText) throws InterruptedException {
		WebElement item = wait.until(ExpectedConditions
				.elementToBeClickable(By.xpath("//div[contains(@class,'role__title__text') and text()='" + roleTitle
						+ "']" + "/ancestor::div[contains(@class,'roles__map__row')]"
						+ "//div[contains(@class,'role__item__wrap') and normalize-space(text())='" + itemText
						+ "']")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", item);
		Thread.sleep(400);
		js.executeScript("arguments[0].click();", item);
	}

	/**
	 * Validates a row in the Preview screen.
	 */
	private static void validatePreviewRow(WebDriver driver, String label, String expectedValue) {
		try {
			String actualValue = driver
					.findElement(By.xpath("//div[contains(@class,'preview__row__label') and normalize-space(text())='"
							+ label + "']" + "/following-sibling::div[contains(@class,'preview__row__value')]"))
					.getText().trim();

			java.util.Set<String> expectedSet = toSet(expectedValue);
			java.util.Set<String> actualSet = toSet(actualValue);

			java.util.Set<String> missing = new java.util.HashSet<>(expectedSet);
			missing.removeAll(actualSet);

			java.util.Set<String> extra = new java.util.HashSet<>(actualSet);
			extra.removeAll(expectedSet);

			if (missing.isEmpty() && extra.isEmpty()) {
				System.out.println("✅ PREVIEW OK  [" + label + "]: " + actualValue);
			} else {
				System.out.println("❌ PREVIEW MISMATCH [" + label + "]");
				System.out.println("   Expected : " + expectedValue);
				System.out.println("   Actual   : " + actualValue);
				if (!missing.isEmpty())
					System.out.println("   Missing  : " + missing);
				if (!extra.isEmpty())
					System.out.println("   Extra    : " + extra);
				throw new RuntimeException("❌ TEST FAILED – Preview mismatch for [" + label + "]. " + "Missing: "
						+ missing + " | Extra: " + extra);
			}
		} catch (NoSuchElementException e) {
			throw new RuntimeException("❌ TEST FAILED – Preview row not found for label: " + label, e);
		}
	}

	/** Splits a comma-separated string into a trimmed lowercase Set. */
	private static java.util.Set<String> toSet(String csv) {
		java.util.Set<String> set = new java.util.HashSet<>();
		for (String s : csv.split(",")) {
			String trimmed = s.trim().toLowerCase();
			if (!trimmed.isEmpty())
				set.add(trimmed);
		}
		return set;
	}

	@SuppressWarnings("unused")
	private static String badge(String value) {
		return value.isEmpty() ? "❌ EMPTY" : "✅ " + value;
	}

	// ── OTP polling ──────────────────────────────────────────────────────────
	private static String waitForOTP(String mobile, int retries, long delayMs) throws InterruptedException {
		for (int i = 0; i < retries; i++) {
			String otp = getOTPFromDB(mobile);
			if (!otp.isEmpty())
				return otp;
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

	// ── DB helper ────────────────────────────────────────────────────────────
	public static String getOTPFromDB(String mobile) {
		String otp = "";
		String url = "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev";
		String user = "hw_goutham";
		String pass = "9qIE0mwg8ehN";

		try (Connection con = DriverManager.getConnection(url, user, pass);
				PreparedStatement ps = con.prepareStatement(
						"SELECT otp FROM otp WHERE mobile_number = ? ORDER BY create_time DESC LIMIT 1")) {

			ps.setString(1, mobile);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					otp = rs.getString("otp");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return otp;
	}
}
