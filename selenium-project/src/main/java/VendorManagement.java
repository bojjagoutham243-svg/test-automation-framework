import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;

import java.sql.*;
import java.time.Duration;
import java.util.List;

public class VendorManagement {

	// ── Shared wait timeout ──────────────────────────────────────────────────
	private static final int WAIT_SEC = 20;

	// Vendor details
	private static final String VENDOR_NAME = "AutoVendorTest";
	private static final String GROUP_NAME = "AutoGroup";
	private static final String PAN_NO = "ABCDE1234F";
	private static final String GST_NO = "29AAACC1206D2ZB";
	private static final String CONTACT_PERSON = "Test Contact";
	private static final String MOBILE_NUMBER = "9000012345";
	private static final String EMAIL_ID = "autovendor@test.com";
	private static final String ADDRESS_LINE = "123 Auto Test Street";

	// Address – pin code drives city / state / country auto-population
	private static final String PIN_CODE = "560008";
	// Expected auto-populated values after pincode selection (for validation)
	private static final String EXP_CITY = "Bengaluru Urban";
	private static final String EXP_STATE = "Karnataka";
	private static final String EXP_COUNTRY = "India";

	// Access section – exact option labels in the dropdowns
	private static final String PRODUCT_TYPE = "AC Charger"; // validated: AC Charger / DC Charger
	private static final String SERVICE_TYPE = "Task"; // validated: Task / Ticket

	// Picks only VISIBLE, interactive dropdown options
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

		// ── NAVIGATE: Administration → Vendor Management ─────────────────────
		clickByText(wait, "Administration");

		wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//p[contains(@class,'crm__sidebar__text') " + "and text()='Vendor Management']"))).click();
		System.out.println("✅ Clicked: Vendor Management");
		Thread.sleep(1500);

		// ════════════════════════════════════════════════════════════════════
		// STEP 1: CLICK CREATE VENDOR BUTTON
		// ════════════════════════════════════════════════════════════════════
		wait.until(ExpectedConditions.elementToBeClickable(By.id("add_new_btn"))).click();
		System.out.println("✅ Clicked: Create Vendor button");
		Thread.sleep(2000);

		// ════════════════════════════════════════════════════════════════════
		// STEP 2: VENDOR INFO
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Filling Vendor Info ==========");

		typeInField(wait, By.id("vendor_name"), VENDOR_NAME);
		System.out.println("✅ Vendor Name: " + VENDOR_NAME);

		typeInField(wait, By.id("group_name"), GROUP_NAME);
		System.out.println("✅ Group Name: " + GROUP_NAME);

		typeInField(wait, By.id("panNo"), PAN_NO);
		System.out.println("✅ PAN No.: " + PAN_NO);

		typeInField(wait, By.id("gstNo"), GST_NO);
		System.out.println("✅ GST No.: " + GST_NO);

		Thread.sleep(500);

		// ════════════════════════════════════════════════════════════════════
		// STEP 3: CONTACT INFO
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Filling Contact Info ==========");

		typeInField(wait, By.id("vendor_contact_name"), CONTACT_PERSON);
		System.out.println("✅ Contact Person: " + CONTACT_PERSON);

		// Mobile – clear the default +91 prefix then type
		WebElement mobileInput = wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.cssSelector("input.form-control.crm__custom__mobile__input__field")));
		mobileInput.click();
		mobileInput.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		mobileInput.sendKeys(Keys.DELETE);
		mobileInput.sendKeys(MOBILE_NUMBER);
		System.out.println("✅ Mobile Number: " + MOBILE_NUMBER);

		typeInField(wait, By.id("vendor_email"), EMAIL_ID);
		System.out.println("✅ Email ID: " + EMAIL_ID);

		Thread.sleep(500);

		// ════════════════════════════════════════════════════════════════════
		// STEP 4: ADDRESS – pincode → auto-fill city / state / country
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Filling Address ==========");

		// 4a. Select pin code (search-able React Select)
		selectReactSearchDropdown(driver, wait, js, "site__pinCode_dropdown", PIN_CODE);
		System.out.println("✅ Pin Code selected: " + PIN_CODE);
		Thread.sleep(1200); // allow auto-population to settle

		// 4b. Validate auto-populated City
		validateAutoPopulatedDropdown(wait, "city_select", EXP_CITY, "City (Address)");

		// 4c. Validate auto-populated State
		validateAutoPopulatedDropdown(wait, "state_select", EXP_STATE, "State (Address)");

		// 4d. Validate auto-populated Country (disabled field)
		validateAutoPopulatedDropdown(wait, "country_select", EXP_COUNTRY, "Country (Address)");

		// 4e. Address Line
		typeInField(wait, By.id("vendor_addressLine1"), ADDRESS_LINE);
		System.out.println("✅ Address: " + ADDRESS_LINE);

		Thread.sleep(500);

		// ════════════════════════════════════════════════════════════════════
		// STEP 5: ACCESS SECTION
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Filling Access Info ==========");

		// ── 5a. Product Type: validate options AC Charger & DC Charger present,
		// then select AC Charger ────────────────────────────────────────
		System.out.println("--- Product Type ---");
		openDropdown(wait, js, By.cssSelector("#product__access__select"));
		Thread.sleep(600);

		validateOptionPresent(wait, "AC Charger");
		validateOptionPresent(wait, "DC Charger");
		System.out.println("✅ Product Type options validated: AC Charger, DC Charger");

		clickVisibleOption(wait, js, "AC Charger");
		System.out.println("✅ Product Type selected: AC Charger");
		Thread.sleep(500);

		// ── 5b. Service Type: validate Task & Ticket present, select Task ────
		System.out.println("--- Service Type ---");
		openDropdown(wait, js, By.cssSelector("#feature_select_access"));
		Thread.sleep(600);

		validateOptionPresent(wait, "Task");
		validateOptionPresent(wait, "Ticket");
		System.out.println("✅ Service Type options validated: Task, Ticket");

		clickVisibleOption(wait, js, "Task");
		System.out.println("✅ Service Type selected: Task");
		Thread.sleep(500);

		// ── 5c. Operational Regions: click "All" ─────────────────────────────
		System.out.println("--- Operational Regions ---");
		openDropdown(wait, js, By.cssSelector("#region_select"));
		Thread.sleep(600);
		clickVisibleOption(wait, js, "All");
		System.out.println("✅ Operational Regions: All selected");
		Thread.sleep(500);

		// ── 5d. City (Access section): click "All" ───────────────────────────
		System.out.println("--- City (Access) ---");
		openDropdown(wait, js, By.cssSelector("#vendor_city_select"));
		Thread.sleep(600);
		clickVisibleOption(wait, js, "All");
		System.out.println("✅ City (Access): All selected");
		Thread.sleep(500);

		// ── 5e. Pincode (Access section): click "All" ────────────────────────
		System.out.println("--- Pincode (Access) ---");
		openDropdown(wait, js, By.cssSelector("#vendor_city_pincode_select"));
		Thread.sleep(600);
		clickVisibleOption(wait, js, "All");
		System.out.println("✅ Pincode (Access): All selected");
		Thread.sleep(500);

		// ── 5f. Account: click "All" ─────────────────────────────────────────
		System.out.println("--- Account ---");
		openDropdown(wait, js, By.cssSelector("#account_select"));
		Thread.sleep(600);
		clickVisibleOption(wait, js, "All");
		System.out.println("✅ Account: All selected");
		Thread.sleep(500);

		// ════════════════════════════════════════════════════════════════════
		// STEP 6: CLICK CREATE BUTTON
		// ════════════════════════════════════════════════════════════════════
		WebElement createBtn = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("update-vendor")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", createBtn);
		Thread.sleep(500);
		wait.until(ExpectedConditions.elementToBeClickable(By.id("update-vendor"))).click();
		System.out.println("✅ Clicked: Create button");

		// ════════════════════════════════════════════════════════════════════
		// STEP 7: TOAST VALIDATION
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Toast Validation ==========");
		validateAndDismissToast(wait, "Vendor has been successfully created.", "Vendor created successfully.");
		System.out.println("========== Toast Validation Done ==========\n");

		// ════════════════════════════════════════════════════════════════════
		// STEP 8: SEARCH VALIDATION
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Search Validation ==========");
		searchAndValidateVendor(driver, wait, js, VENDOR_NAME);
		System.out.println("========== Search Validation Done ==========\n");
		System.out.println("\n========== Clicking Vendor Code ==========");

		WebElement vendorCodeElement = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.id("labelvendor__code")));

		String vendorCode = vendorCodeElement.getText().trim();

		System.out.println("✅ Vendor Code Found: " + vendorCode);

		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", vendorCodeElement);

		Thread.sleep(500);

		js.executeScript("arguments[0].click();", vendorCodeElement);

		System.out.println("✅ Clicked Vendor Code: " + vendorCode);

		Thread.sleep(2500);

		// ════════════════════════════════════════════════════════════════════
		// STEP 09: OPEN DATE PICKER
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Opening Date Picker ==========");

		// Click date field first
		WebElement dateField = wait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[contains(@placeholder,'Select')]")));

		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", dateField);

		Thread.sleep(500);

		js.executeScript("arguments[0].click();", dateField);

		System.out.println("✅ Date picker opened");

		Thread.sleep(1500);

		System.out.println("\n========== Selecting Today Date ==========");

		WebElement todayDate = wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//div[contains(@class,'react-datepicker__day--today')]")));

		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", todayDate);

		Thread.sleep(500);

		js.executeScript("arguments[0].click();", todayDate);

		System.out.println("✅ Today date selected successfully");

		Thread.sleep(1500);

		System.out.println("\n========== Closing Date Picker ==========");

		// click outside to close calendar popup
		js.executeScript("document.body.click();");

		Thread.sleep(1000);

		System.out.println("✅ Date picker closed successfully");

		// ════════════════════════════════════════════════════════════════════
		// STEP 10: CLICK VENDOR PROFILE MAPPING BUTTON
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Vendor Profile Mapping ==========");

		WebElement vendorMappingBtn = wait.until(ExpectedConditions.elementToBeClickable(By.id("vendor-mapping")));

		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", vendorMappingBtn);

		js.executeScript("arguments[0].click();", vendorMappingBtn);

		System.out.println("✅ Clicked: Vendor Profile Mapping");
		Thread.sleep(2000);

		// ════════════════════════════════════════════════════════════════════
		// STEP 11: VALIDATE VENDOR NAME
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Validating Vendor Name ==========");

		WebElement vendorNameLabel = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//p[@id='labelvendor__name']")));

		String actualVendorName = vendorNameLabel.getText().trim();

		if (actualVendorName.equalsIgnoreCase(VENDOR_NAME)) {

			System.out.println("✅ Vendor Name validated: " + actualVendorName);

		} else {

			throw new RuntimeException(
					"❌ Vendor Name mismatch. Expected: " + VENDOR_NAME + " | Actual: " + actualVendorName);
		}

		// ════════════════════════════════════════════════════════════════════
		// STEP 13: VALIDATE ALL CIRCLES FROM VENDOR PROFILE MAPPING UI
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Validating Circle ==========");

		// Vendor circles list from API response
		String[] vendorCircles = {

				"Telangana", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Tripura", "Uttar Pradesh",
				"Uttarakhand", "West Bengal", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Mizoram",
				"Nagaland", "Puducherry", "Gujarat", "Lakshadweep", "Madhya Pradesh", "Maharashtra", "Manipur",
				"Meghalaya", "Dadra and Nagar Haveli and Daman and Diu", "Ladakh", "Chhattisgarh",
				"Andaman and Nicobar Islands", "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chandigarh",
				"Delhi", "Goa", "Haryana" };

		// Updated xpath using label id
		List<WebElement> circleElements = wait
				.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath("//p[starts-with(@id,'label')]")));

		if (circleElements.isEmpty()) {

			throw new RuntimeException("❌ No circles found in Vendor Profile Mapping UI");
		}

		System.out.println("✅ Total circle labels found: " + circleElements.size());

		for (WebElement element : circleElements) {

			String actualCircle = element.getText().trim();

			// Skip blank labels
			if (actualCircle.isEmpty()) {
				continue;
			}

			System.out.println("🔍 Circle displayed in UI: " + actualCircle);

			// Skip numeric values
			if (actualCircle.matches("\\d+")) {

				System.out.println("⚠ Skipping numeric value: " + actualCircle);

				continue;
			}

			boolean isCircleValid = false;

			for (String expectedCircle : vendorCircles) {

				if (actualCircle.equalsIgnoreCase(expectedCircle)) {

					isCircleValid = true;
					break;
				}
			}

			if (isCircleValid) {

				System.out.println("✅ Circle validation passed: " + actualCircle);

			} else {

				System.out.println("⚠ Non-circle label skipped: " + actualCircle);
			}
		}

		System.out.println("✅ Vendor Profile Mapping circle validation completed successfully");

		// ════════════════════════════════════════════════════════════════════
		// STEP : SELECT PROFILE TYPE = TAT BASED
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Selecting Profile Type ==========");

		WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(20));
		JavascriptExecutor js1 = (JavascriptExecutor) driver;

		By profileTypeLocator =
		        By.xpath("(//input[contains(@class,'crm__dropdown__input')])[2]");

		// Wait for element
		WebElement profileTypeField = wait1.until(
		        ExpectedConditions.elementToBeClickable(profileTypeLocator)
		);

		// Scroll
		js1.executeScript(
		        "arguments[0].scrollIntoView({block:'center'});",
		        profileTypeField
		);

		Thread.sleep(1000);

		// Click dropdown
		js1.executeScript("arguments[0].click();", profileTypeField);

		Thread.sleep(1000);

		// Re-find element after DOM refresh
		profileTypeField = wait1.until(
		        ExpectedConditions.presenceOfElementLocated(profileTypeLocator)
		);

		// Use Actions instead of direct sendKeys
		Actions actions = new Actions(driver);

		actions.moveToElement(profileTypeField)
		        .click()
		        .sendKeys("TAT Based")
		        .pause(Duration.ofSeconds(2))
		        .sendKeys(Keys.ARROW_DOWN)
		        .sendKeys(Keys.ENTER)
		        .perform();

		System.out.println("✅ Selected Profile Type : TAT Based");
		// ════════════════════════════════════════════════════════════════════
		// STEP 14: SELECT PROFILE NAME - FIRST VALUE
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Selecting Profile Name ==========");

		// Click Profile Name dropdown
		WebElement profileNameDropdown = wait1.until(ExpectedConditions
				.elementToBeClickable(By.xpath("(//div[contains(@class,'crm__dropdown__control')])[4]")));

		js1.executeScript("arguments[0].scrollIntoView({block:'center'});", profileNameDropdown);

		Thread.sleep(500);

		js1.executeScript("arguments[0].click();", profileNameDropdown);

		System.out.println("✅ Clicked Profile Name dropdown");

		Thread.sleep(1500);

		// Select first option
		WebElement firstProfile = wait1.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("(//div[contains(@class,'crm__dropdown__option')])[1]")));

		String selectedProfile = firstProfile.getText().trim();

		js1.executeScript("arguments[0].click();", firstProfile);

		System.out.println("✅ Selected Profile Name : " + selectedProfile);

		Thread.sleep(1500);
		// ════════════════════════════════════════════════════════════════════
		// STEP 15: CLICK APPLY TO ALL CHECKBOX
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Apply To All ==========");

		WebElement applyAllCheckbox = wait1.until(ExpectedConditions
				.elementToBeClickable(By.xpath("//span[text()='Apply to All']/preceding-sibling::button")));

		js1.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", applyAllCheckbox);

		js1.executeScript("arguments[0].click();", applyAllCheckbox);

		System.out.println("✅ Apply To All checkbox clicked");
		Thread.sleep(1000);

		// ════════════════════════════════════════════════════════════════════
		// STEP 16: CLICK SAVE BUTTON
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Saving Vendor Mapping ==========");

		WebElement saveBtn = wait1.until(ExpectedConditions.elementToBeClickable(By.id("update-user")));

		js1.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", saveBtn);

		js1.executeScript("arguments[0].click();", saveBtn);

		System.out.println("✅ Clicked: Save button");

		// ════════════════════════════════════════════════════════════════════
		// STEP 17: VALIDATE SUCCESS TOAST
		// ════════════════════════════════════════════════════════════════════
		System.out.println("\n========== Toast Validation ==========");

		validateAndDismissToast(wait1, "Vendor has been successfully updated.", "Vendor Profile Mapping Updated");
		System.out.println("✅ Vendor Profile Mapping completed successfully");

		driver.quit();
		System.out.println("\n🎉 Vendor Management – Create Vendor completed successfully!");
	}

	// ════════════════════════════════════════════════════════════════════════
	// DROPDOWN HELPERS
	// ════════════════════════════════════════════════════════════════════════

	/**
	 * Opens a crm__dropdown by clicking it.
	 */
	private static void openDropdown(WebDriverWait wait, JavascriptExecutor js, By locator) {
		WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", el);
		el.click();
	}

	/**
	 * Validates that an option with the given text is PRESENT in the currently open
	 * dropdown. Throws RuntimeException if not found.
	 */
	private static void validateOptionPresent(WebDriverWait wait, String optionText) {
		String xpath = VISIBLE_OPTION_XPATH + "[normalize-space(text())='" + optionText + "']";
		try {
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
			System.out.println("   ✔ Option present: " + optionText);
		} catch (TimeoutException e) {
			throw new RuntimeException("❌ VALIDATION FAILED – Expected dropdown option NOT found: '" + optionText + "'",
					e);
		}
	}

	/**
	 * Clicks a visible dropdown option that exactly matches optionText.
	 */
	private static void clickVisibleOption(WebDriverWait wait, JavascriptExecutor js, String optionText) {
		String xpath = VISIBLE_OPTION_XPATH + "[normalize-space(text())='" + optionText + "']";
		WebElement option = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpath)));
		js.executeScript("arguments[0].click();", option);
	}

	/**
	 * Validates that a React-Select dropdown (by container id) shows the expected
	 * value after auto-population. Works for both enabled and disabled (read-only)
	 * dropdowns.
	 */
	private static void validateAutoPopulatedDropdown(WebDriverWait wait, String containerId, String expectedValue,
			String fieldLabel) {

		// The selected value is rendered inside a single-value or option div
		By singleValueLocator = By.cssSelector("#" + containerId + " .crm__dropdown__single-value p");
		By textLabelLocator = By.cssSelector("#" + containerId + " p.text__label");

		String actualValue = "";
		try {
			// Try single-value label first (standard selected state)
			WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(textLabelLocator));
			actualValue = el.getText().trim();
		} catch (TimeoutException e) {
			// Fallback: read from single-value wrapper text
			try {
				WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(singleValueLocator));
				actualValue = el.getText().trim();
			} catch (TimeoutException ex) {
				throw new RuntimeException("❌ VALIDATION FAILED – Could not read auto-populated value for ["
						+ fieldLabel + "]. Expected: '" + expectedValue + "'", ex);
			}
		}

		if (actualValue.equalsIgnoreCase(expectedValue)) {
			System.out.println("✅ AUTO-POPULATED OK  [" + fieldLabel + "]: " + actualValue);
		} else {
			throw new RuntimeException("❌ VALIDATION FAILED – [" + fieldLabel + "] " + "Expected: '" + expectedValue
					+ "' | Actual: '" + actualValue + "'");
		}
	}

	/**
	 * Types into a React-Select search-able dropdown and selects the first matching
	 * result.
	 *
	 * @param containerId HTML id of the react-select container div
	 * @param searchText  text to type into the search input
	 */
	private static void selectReactSearchDropdown(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
			String containerId, String searchText) throws InterruptedException {

		WebElement container = wait.until(ExpectedConditions.elementToBeClickable(By.id(containerId)));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", container);
		container.click();
		Thread.sleep(400);

		WebElement input = container.findElement(By.cssSelector("input.crm__dropdown__input"));
		input.sendKeys(searchText);
		Thread.sleep(1000);

		List<WebElement> options = wait
				.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(VISIBLE_OPTION_XPATH)));

		if (options.isEmpty())
			throw new RuntimeException("No dropdown options found for search text: " + searchText);

		js.executeScript("arguments[0].click();", options.get(0));
		Thread.sleep(400);
	}

	// ════════════════════════════════════════════════════════════════════════
	// TOAST VALIDATION
	// ════════════════════════════════════════════════════════════════════════
	private static void validateAndDismissToast(WebDriverWait wait, String expectedMessage, String stepLabel) {

		WebElement toast;
		try {
			toast = wait
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.toast_content_subtitle")));
		} catch (TimeoutException e) {
			throw new RuntimeException("❌ TEST FAILED – Toast did not appear for step [" + stepLabel + "]. Expected: \""
					+ expectedMessage + "\"", e);
		}

		String actualText = toast.getText().trim();
		System.out.println("🔔 Toast message: \"" + actualText + "\"");

		if (actualText.contains(expectedMessage)) {
			System.out.println("✅ TOAST PASSED – " + stepLabel + ": " + actualText);
		} else {
			throw new RuntimeException("❌ TEST FAILED – Toast mismatch for [" + stepLabel + "].\n" + "   Expected : \""
					+ expectedMessage + "\"\n" + "   Actual   : \"" + actualText + "\"");
		}

		try {
			wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.Toastify__toast-container")))
					.click();
			System.out.println("🔔 Toast clicked to dismiss.");
		} catch (Exception e) {
			System.out.println("ℹ️  Toast auto-dismissed.");
		}

		wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.Toastify__toast-container")));
		System.out.println("✅ Toast dismissed – ready for next step.");
	}

	// ════════════════════════════════════════════════════════════════════════
	// SEARCH & VALIDATE VENDOR
	// ════════════════════════════════════════════════════════════════════════
	private static void searchAndValidateVendor(WebDriver driver, WebDriverWait wait, JavascriptExecutor js,
			String expectedVendorName) throws InterruptedException {

		WebElement searchBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("vendors__search-box")));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", searchBox);
		searchBox.click();
		searchBox.sendKeys(Keys.chord(Keys.CONTROL, "a"));
		searchBox.sendKeys(Keys.BACK_SPACE);
		js.executeScript("arguments[0].value = '';", searchBox);
		searchBox.sendKeys(expectedVendorName);
		System.out.println("🔍 Searching for vendor: " + expectedVendorName);
		Thread.sleep(1500);

		By vendorNameLocator = By
				.xpath("//p[@id='labelvendor__name' " + "and normalize-space(text())='" + expectedVendorName + "']");

		WebElement vendorLabel;
		try {
			vendorLabel = wait.until(ExpectedConditions.visibilityOfElementLocated(vendorNameLocator));
		} catch (TimeoutException e) {
			throw new RuntimeException(
					"❌ TEST FAILED – Vendor '" + expectedVendorName + "' not found in search results.", e);
		}

		String foundText = vendorLabel.getText().trim();
		if (foundText.equalsIgnoreCase(expectedVendorName)) {
			System.out.println("✅ SEARCH PASSED – Vendor '" + expectedVendorName + "' found in the list!");
		} else {
			throw new RuntimeException(
					"❌ TEST FAILED – Label text '" + foundText + "' does not match '" + expectedVendorName + "'.");
		}

		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", vendorLabel);
		Thread.sleep(400);
		js.executeScript("arguments[0].click();", vendorLabel);
		System.out.println("✅ Clicked vendor to open detail panel: " + foundText);
		Thread.sleep(2500);
	}

	// ════════════════════════════════════════════════════════════════════════
	// GENERIC HELPERS
	// ════════════════════════════════════════════════════════════════════════

	private static void clickByText(WebDriverWait wait, String text) {
		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(),'" + text + "')]"))).click();
		System.out.println("✅ Clicked: " + text);
	}

	private static void typeInField(WebDriverWait wait, By locator, String value) {
		WebElement el = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
		el.clear();
		el.sendKeys(value);
	}

	private static WebElement scrollIntoView(WebDriverWait wait, JavascriptExecutor js, By locator) {
		WebElement el = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", el);
		return el;
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

	public static String getOTPFromDB(String mobile) {
		String otp = "";
		String url = "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev";
		String user = "hw_goutham";
		String pass = "9qIE0mwg8ehN";

		try (Connection con = DriverManager.getConnection(url, user, pass);
				PreparedStatement ps = con.prepareStatement(
						"SELECT otp FROM otp WHERE mobile_number = ? " + "ORDER BY create_time DESC LIMIT 1")) {
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

	public static String generateMobileNumber() {
		int firstDigit = 7 + (int) (Math.random() * 3);
		long remaining = (long) (Math.random() * 1_000_000_000L);
		return firstDigit + String.format("%09d", remaining);
	}
}
