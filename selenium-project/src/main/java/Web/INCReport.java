package Web;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.NoSuchElementException;

public class INCReport {

	private static final int WAIT_SEC = 20;

	private static WebDriver driver;
	private static WebDriverWait wait;
	private static JavascriptExecutor js;

	private static final String DOWNLOAD_DIR = System.getProperty("user.home") + File.separator + "Downloads";

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

		wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//input[contains(@class,'MuiOutlinedInput-input')]")))
				.sendKeys(otp);

		wait.until(ExpectedConditions.urlContains("dashboard"));

		System.out.println("✅ Login Successful!");

		// ═══════════════════════════════════════════════════════════════
		// FLOW 1 : I&C REPORT FLOW
		// ═══════════════════════════════════════════════════════════════

		System.out.println("\n══════════════════════════════════════");
		System.out.println("🚀 STARTING I&C REPORT FLOW");
		System.out.println("══════════════════════════════════════");

		// STEP 1 – Navigate to Reports → Reports sub-menu → select "I&C Report"
		navigateToINCReport();

		// STEP 2 – Click "Closed" status filter
		clickStatusFilter("Closed");

		// STEP 3 – Select date range as "Current Month"
		selectCurrentMonth();

		// STEP 4 – Click Generate Report
		WebElement generateBtn = wait
				.until(ExpectedConditions.elementToBeClickable(By.id("report__screen__generate__report")));

		slowScrollTo(By.id("report__screen__generate__report"));

		generateBtn.click();

		System.out.println("✅ Clicked: Generate Report");

		// STEP 5 – Validate toast message
		validateToast();

		// STEP 6 – Dismiss toast
		dismissToast();

		// STEP 7 – Capture the latest Job ID from the top pinned row
		Thread.sleep(2000);

		WebElement pinnedTopRow = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
				"//div[contains(@class,'MuiDataGrid-pinnedColumns')]" + "//div[@role='row' and @data-rowindex='0']")));

		String capturedJobId = pinnedTopRow.findElement(By.xpath(
				".//div[@role='cell' and @data-field='jobId']" + "//div[contains(@class,'MuiDataGrid-cellContent')]"))
				.getAttribute("title").trim();

		System.out.println("✅ Captured Job ID: " + capturedJobId);

		// STEP 8 – Locate the matching scrollable row
		WebElement scrollableTopRow = wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
						+ "//div[@role='row' and @data-rowindex='0']")));

		// STEP 9 – Validate Product Type (AC Charger | DC Charger)
		validateINCProductType(scrollableTopRow);

		// STEP 10 – Validate Pending At value
		validateINCPendingAt(scrollableTopRow);

		// STEP 11 – Validate Task Status (Open | Closed | Cancelled)
		validateINCTaskStatus(scrollableTopRow);

		// STEP 12 – Validate row-level job Status = "Pending"
		validatePendingStatus(scrollableTopRow);

		// STEP 13 – Validate download button is disabled while Pending
		validateDisabledDownload(scrollableTopRow);

		// STEP 14 – Poll every 25 s until job Status = Completed
		System.out.println("⏳ Polling for completion (every 25 s)...");

		WebElement completedRow = pollUntilINCJobDone(capturedJobId, 25, 10);

		// STEP 15 – Click Download
		WebElement downloadBtn = completedRow
				.findElement(By.xpath(".//div[@role='cell' and @data-field='downloadReport']"
						+ "//div[contains(@class,'crm__bulk__upload__download__file')]"));

		js.executeScript("arguments[0].click();", downloadBtn);

		System.out.println("✅ Clicked download");

		// STEP 16 – Wait for the .xlsx file named with the Job ID
		File downloadedFile = waitForDownloadedFile(DOWNLOAD_DIR, capturedJobId, 60);

		System.out.println("✅ File Downloaded: " + downloadedFile.getName());

		// STEP 17 – Validate all required I&C Excel columns are non-null
		// and DOA column value = "false"
		validateINCExcelFields(downloadedFile);

		System.out.println("\n🎉 I&C REPORT FLOW COMPLETED SUCCESSFULLY!");

		// ═══════════════════════════════════════════════════════════════
		// FLOW 2 : DOA REPORT FLOW
		// ═══════════════════════════════════════════════════════════════

		System.out.println("\n══════════════════════════════════════");
		System.out.println("🚀 STARTING DOA REPORT FLOW");
		System.out.println("══════════════════════════════════════");

		// STEP D1 – Refresh the page and navigate back to I&C Report
		driver.navigate().refresh();
		Thread.sleep(2000);
		navigateToINCReport();

		// STEP D2 – Click "Open" status filter first, then "Closed"
		clickStatusFilter("Open");
		clickStatusFilter("Closed");

		// STEP D3 – Select date range as "Current Month"
		selectCurrentMonth();

		// STEP D4 – Enable the DOA toggle
		enableDOAToggle();

		// STEP D5 – Click Generate Report
		WebElement doaGenerateBtn = wait
				.until(ExpectedConditions.elementToBeClickable(By.id("report__screen__generate__report")));

		slowScrollTo(By.id("report__screen__generate__report"));

		doaGenerateBtn.click();

		System.out.println("✅ Clicked: Generate Report (DOA)");

		// STEP D6 – Validate toast message
		validateToast();

		// STEP D7 – Dismiss toast
		dismissToast();

		// STEP D8 – Capture the latest Job ID for the DOA report
		Thread.sleep(2000);

		WebElement doaPinnedTopRow = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
				"//div[contains(@class,'MuiDataGrid-pinnedColumns')]" + "//div[@role='row' and @data-rowindex='0']")));

		String doaJobId = doaPinnedTopRow.findElement(By.xpath(
				".//div[@role='cell' and @data-field='jobId']" + "//div[contains(@class,'MuiDataGrid-cellContent')]"))
				.getAttribute("title").trim();

		System.out.println("✅ Captured DOA Job ID: " + doaJobId);

		// STEP D9 – Validate Pending status and disabled download on new row
		WebElement doaScrollableTopRow = wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
						+ "//div[@role='row' and @data-rowindex='0']")));

		validatePendingStatus(doaScrollableTopRow);
		validateDisabledDownload(doaScrollableTopRow);

		// STEP D10 – Poll every 25 s until DOA job is Completed
		System.out.println("⏳ Polling for DOA job completion (every 25 s)...");

		WebElement doaCompletedRow = pollUntilINCJobDone(doaJobId, 25, 10);

		// STEP D11 – Click Download for DOA report
		WebElement doaDownloadBtn = doaCompletedRow
				.findElement(By.xpath(".//div[@role='cell' and @data-field='downloadReport']"
						+ "//div[contains(@class,'crm__bulk__upload__download__file')]"));

		js.executeScript("arguments[0].click();", doaDownloadBtn);

		System.out.println("✅ Clicked DOA download");

		// STEP D12 – Wait for the DOA .xlsx file
		File doaDownloadedFile = waitForDownloadedFile(DOWNLOAD_DIR, doaJobId, 60);

		System.out.println("✅ DOA File Downloaded: " + doaDownloadedFile.getName());

		// STEP D13 – Validate all required DOA Excel columns are non-null
		// and Activity must be "Commissioning" or "Welcome call"
		validateDOAExcelFields(doaDownloadedFile);

		System.out.println("\n🎉 DOA REPORT FLOW COMPLETED SUCCESSFULLY!");

		Thread.sleep(2000);

		driver.quit();
	}

	// ═══════════════════════════════════════════════════════════════
	// I&C REPORT – NAVIGATION
	// ═══════════════════════════════════════════════════════════════

	private static void navigateToINCReport() throws InterruptedException {

		slowScrollTo(By.xpath("//p[contains(@class,'crm__sidebar__text') and normalize-space()='Reports']"));

		clickByText(wait, "Reports");

		WebElement subMenu = wait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("(//*[text()='Reports'])[2]")));

		slowScrollTo(By.xpath("(//*[text()='Reports'])[2]"));

		subMenu.click();

		wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Start building your report')]")));

		WebElement ddInput = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//div[contains(@class,'crm__dropdown__input-container')]//input[@role='combobox']")));

		ddInput.click();

		Thread.sleep(400);

		ddInput.sendKeys("I&C Report");

		Thread.sleep(500);

		WebElement incReportOption = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//div[contains(@class,'crm__dropdown__option') and normalize-space(text())='I&C Report']")));

		incReportOption.click();

		System.out.println("✅ I&C Report selected");

		Thread.sleep(500);
	}

	// ═══════════════════════════════════════════════════════════════
	// SHARED – SELECT "CURRENT MONTH" DATE RANGE
	// ═══════════════════════════════════════════════════════════════

	private static void selectCurrentMonth() throws InterruptedException {

		List<WebElement> allComboboxes = driver.findElements(By.xpath("//input[@role='combobox']"));

		WebElement dateDropdownInput = allComboboxes.get(allComboboxes.size() - 1);

		js.executeScript("arguments[0].click();", dateDropdownInput);

		Thread.sleep(400);

		dateDropdownInput.sendKeys("Current Month");

		Thread.sleep(600);

		dateDropdownInput.sendKeys(Keys.ENTER);

		System.out.println("✅ Selected: Current Month");
	}

	// ═══════════════════════════════════════════════════════════════
	// DOA FLOW – ENABLE DOA TOGGLE
	// ═══════════════════════════════════════════════════════════════
	private static void enableDOAToggle() throws InterruptedException {

		By toggleLocator = By.xpath("//div[contains(@class,'d-flex') and .//*[normalize-space()='DOA']]"
				+ "//span[contains(@class,'MuiSwitch-switchBase')]");

		By checkboxLocator = By.xpath(
				"//div[contains(@class,'d-flex') and .//*[normalize-space()='DOA']]" + "//input[@type='checkbox']");

		WebElement checkbox = wait.until(ExpectedConditions.presenceOfElementLocated(checkboxLocator));

		boolean isChecked = checkbox.isSelected();

		System.out.println("Before Click : " + isChecked);

		if (!isChecked) {

			WebElement toggle = wait.until(ExpectedConditions.visibilityOfElementLocated(toggleLocator));

			js.executeScript("arguments[0].scrollIntoView({block:'center'});", toggle);

			Thread.sleep(1000);

			// REAL CLICK
			Actions actions = new Actions(driver);
			actions.moveToElement(toggle).click().perform();

			Thread.sleep(2000);

			// Re-fetch updated checkbox
			checkbox = driver.findElement(checkboxLocator);

			System.out.println("After Click : " + checkbox.isSelected());

			if (!checkbox.isSelected()) {

				// Final fallback JS click
				js.executeScript("arguments[0].click();", toggle);

				Thread.sleep(2000);

				checkbox = driver.findElement(checkboxLocator);

				if (!checkbox.isSelected()) {
					throw new RuntimeException("❌ DOA toggle not enabled");
				}
			}

			System.out.println("✅ DOA Toggle Enabled");

		} else {
			System.out.println("✅ DOA already enabled");
		}
	}
	// ═══════════════════════════════════════════════════════════════
	// GRID ROW VALIDATIONS – I&C SPECIFIC
	// ═══════════════════════════════════════════════════════════════

	private static final List<String> VALID_PRODUCT_TYPES = Arrays.asList("AC Charger", "DC Charger");

	private static void validateINCProductType(WebElement row) {

		WebElement cell = row.findElement(By.xpath(".//div[@role='cell' and @data-field='productType']"
				+ "//div[contains(@class,'MuiDataGrid-cellContent')]"));

		String value = cell.getAttribute("title").trim();

		String[] tokens = value.split(",");

		List<String> invalid = new ArrayList<>();

		for (String token : tokens) {
			String t = token.trim();
			boolean known = VALID_PRODUCT_TYPES.stream().anyMatch(v -> v.equalsIgnoreCase(t));
			if (!known) {
				invalid.add(t);
			}
		}

		if (invalid.isEmpty()) {
			System.out.println("✅ Product Type: " + value);
		} else {
			throw new RuntimeException(
					"❌ Product Type contains unexpected value(s): " + invalid + " | full cell value: " + value);
		}
	}

	private static final List<String> VALID_PENDING_AT = Arrays.asList("Account Coordinator", "Account Manager",
			"Account Service Engineer", "Circle Coordinator", "Circle Head", "Commercial Team",
			"Commissioning Engineer", "Customer Care Executive", "I and C Coordinator", "Service Engineer",
			"Vendor Service Engineer", "Zonal Head");

	private static void validateINCPendingAt(WebElement row) {

		WebElement cell = row.findElement(By.xpath(".//div[@role='cell' and @data-field='pendingAt']"
				+ "//div[contains(@class,'MuiDataGrid-cellContent')]"));

		String value = cell.getAttribute("title").trim();

		String[] tokens = value.split(",");

		List<String> invalid = new ArrayList<>();

		for (String token : tokens) {
			String t = token.trim();
			boolean known = VALID_PENDING_AT.stream().anyMatch(v -> v.equalsIgnoreCase(t));
			if (!known) {
				invalid.add(t);
			}
		}

		if (invalid.isEmpty()) {
			System.out.println("✅ Pending At: " + value);
		} else {
			throw new RuntimeException(
					"❌ Pending At contains unexpected value(s): " + invalid + " | full cell value: " + value);
		}
	}

	private static final List<String> VALID_TASK_STATUS = Arrays.asList("Open", "Closed", "Cancelled");

	private static void validateINCTaskStatus(WebElement row) {

		WebElement cell = row.findElement(By.xpath(".//div[@role='cell' and @data-field='taskStatus']"
				+ "//div[contains(@class,'MuiDataGrid-cellContent')]"));

		String value = cell.getAttribute("title").trim();

		String[] tokens = value.split(",");

		List<String> invalid = new ArrayList<>();

		for (String token : tokens) {
			String t = token.trim();
			boolean known = VALID_TASK_STATUS.stream().anyMatch(v -> v.equalsIgnoreCase(t));
			if (!known) {
				invalid.add(t);
			}
		}

		if (invalid.isEmpty()) {
			System.out.println("✅ Task Status: " + value);
		} else {
			throw new RuntimeException(
					"❌ Task Status contains unexpected value(s): " + invalid + " | full cell value: " + value);
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// POLL UNTIL I&C JOB IS DONE (every 25 s)
	// ═══════════════════════════════════════════════════════════════

	private static WebElement pollUntilINCJobDone(String jobId, int intervalSec, int maxAttempts)
			throws InterruptedException {

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {

			System.out.println("🔄 Poll Attempt " + attempt + "/" + maxAttempts);

			driver.navigate().refresh();

			Thread.sleep(2000);

			navigateToINCReport();

			List<WebElement> pinnedRows = driver
					.findElements(By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]"
							+ "//div[@role='row' and " + ".//div[@role='cell' and @data-field='jobId']"
							+ "//div[contains(@class,'MuiDataGrid-cellContent') and @title='" + jobId + "']]"));

			if (pinnedRows.isEmpty()) {
				System.out.println("⚠️ Job row not visible yet");
				Thread.sleep(intervalSec * 1000L);
				continue;
			}

			String rowIndex = pinnedRows.get(0).getAttribute("data-rowindex");

			List<WebElement> scrollableRows = driver
					.findElements(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
							+ "//div[@role='row' and @data-rowindex='" + rowIndex + "']"));

			if (scrollableRows.isEmpty()) {
				Thread.sleep(intervalSec * 1000L);
				continue;
			}

			WebElement row = scrollableRows.get(0);

			if (!row.findElements(By.xpath(".//p[@id='labelFailed']")).isEmpty()) {
				throw new RuntimeException("❌ Job FAILED");
			}

			if (!row.findElements(By.xpath(".//p[@id='labelCompleted']")).isEmpty()) {
				System.out.println("✅ Job COMPLETED");
				return row;
			}

			System.out.println("⏳ Still processing, next poll in " + intervalSec + "s...");

			Thread.sleep(intervalSec * 1000L);
		}

		throw new RuntimeException("❌ Job not completed within the allowed attempts");
	}

	// ═══════════════════════════════════════════════════════════════
	// EXCEL VALIDATION – I&C REPORT COLUMNS (DOA = "false")
	// ═══════════════════════════════════════════════════════════════

	private static void validateINCExcelFields(File excelFile) throws Exception {

		System.out.println("\n📄 Validating I&C Excel: " + excelFile.getName());

		List<String> requiredColumns = Arrays.asList("IRF No", "IRF Creation Date", "Created By", "I&C Request No.",
				"Request Date", "Parent Account", "Product Type", "Customer Type", "Customer Name",
				"Customer Contact No", "Customer Email", "Location Type", "Site Address", "City", "State", "PinCode",
				"Locality", "Zone", "Circle", "Activity", "Activity Status", "Ageing");

		try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheetAt(0);

			Row headerRow = null;

			for (Row row : sheet) {
				Cell firstCell = row.getCell(0);
				if (firstCell != null && !getCellValueAsString(firstCell).trim().isEmpty()) {
					headerRow = row;
					break;
				}
			}

			if (headerRow == null) {
				throw new RuntimeException("❌ Header row not found in Excel");
			}

			Map<String, Integer> colIndex = new HashMap<>();

			for (Cell cell : headerRow) {
				String header = getCellValueAsString(cell).trim();
				if (!header.isEmpty()) {
					colIndex.put(header, cell.getColumnIndex());
				}
			}

			for (String col : requiredColumns) {
				if (!colIndex.containsKey(col)) {
					throw new RuntimeException("❌ Missing column in Excel header: " + col);
				}
			}

			Row targetRow = sheet.getRow(headerRow.getRowNum() + 1);

			if (targetRow == null) {
				throw new RuntimeException("❌ No data rows found in Excel");
			}

			String irfNo = getCellValueAsString(targetRow.getCell(colIndex.get("IRF No"))).trim();
			System.out.println("✅ Validating IRF No: " + irfNo);

			boolean allPassed = true;

			for (String fieldName : requiredColumns) {

				String actualValue = getCellValueAsString(targetRow.getCell(colIndex.get(fieldName))).trim();

				if (actualValue.isEmpty()) {
					System.out.println("❌ NULL/EMPTY: " + fieldName);
					allPassed = false;
				} else {
					System.out.println("✅ " + fieldName + " : " + actualValue);
				}
			}

			// DOA column: must be "false"
			if (colIndex.containsKey("DOA")) {

				String doaValue = getCellValueAsString(targetRow.getCell(colIndex.get("DOA"))).trim();

				if (doaValue.equalsIgnoreCase("false")) {
					System.out.println("✅ DOA : " + doaValue + " (expected false ✔)");
				} else {
					System.out.println("❌ DOA expected 'false' but got: '" + doaValue + "'");
					allPassed = false;
				}

			} else {
				System.out.println("⚠️  'DOA' column not found in Excel – skipping DOA value check");
			}

			if (!allPassed) {
				throw new RuntimeException("❌ One or more required fields failed validation in the I&C report Excel");
			}

			System.out.println("✅ All I&C Excel fields validated successfully");
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// EXCEL VALIDATION – DOA REPORT COLUMNS
	// ═══════════════════════════════════════════════════════════════

	/**
	 * Validates the DOA Excel report:
	 * <ul>
	 * <li>All required columns must be present and non-empty in the first data
	 * row.</li>
	 * <li>"Activity" cell value must be exactly "Commissioning" or "Welcome call"
	 * (case-insensitive).</li>
	 * </ul>
	 */
	private static void validateDOAExcelFields(File excelFile) throws Exception {

		System.out.println("\n📄 Validating DOA Excel: " + excelFile.getName());

		List<String> requiredColumns = Arrays.asList("IRF No", "IRF Creation Date", "Created By", "I&C Request No.",
				"Request Date", "Parent Account", "Product Type", "Charger Serial No.", "Part Number", "Product Name",
				"Customer Type", "Customer Name", "Customer Contact No", "Customer Email", "Location Type",
				"Site Address", "City", "State", "PinCode", "Locality", "Zone", "Circle", "Activity", "Activity Status",
				"Ageing");

		List<String> validActivityValues = Arrays.asList("Commissioning", "Welcome call");

		try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheetAt(0);

			// ── Locate header row (first non-empty row) ──────────────
			Row headerRow = null;

			for (Row row : sheet) {
				Cell firstCell = row.getCell(0);
				if (firstCell != null && !getCellValueAsString(firstCell).trim().isEmpty()) {
					headerRow = row;
					break;
				}
			}

			if (headerRow == null) {
				throw new RuntimeException("❌ Header row not found in DOA Excel");
			}

			// ── Build column-name → index map ────────────────────────
			Map<String, Integer> colIndex = new HashMap<>();

			for (Cell cell : headerRow) {
				String header = getCellValueAsString(cell).trim();
				if (!header.isEmpty()) {
					colIndex.put(header, cell.getColumnIndex());
				}
			}

			// ── Assert all required columns exist ────────────────────
			for (String col : requiredColumns) {
				if (!colIndex.containsKey(col)) {
					throw new RuntimeException("❌ Missing column in DOA Excel header: " + col);
				}
			}

			// ── Validate first data row ──────────────────────────────
			Row targetRow = sheet.getRow(headerRow.getRowNum() + 1);

			if (targetRow == null) {
				throw new RuntimeException("❌ No data rows found in DOA Excel");
			}

			String irfNo = getCellValueAsString(targetRow.getCell(colIndex.get("IRF No"))).trim();
			System.out.println("✅ Validating DOA IRF No: " + irfNo);

			boolean allPassed = true;

			for (String fieldName : requiredColumns) {

				String actualValue = getCellValueAsString(targetRow.getCell(colIndex.get(fieldName))).trim();

				if (actualValue.isEmpty()) {
					System.out.println("❌ NULL/EMPTY: " + fieldName);
					allPassed = false;

				} else if (fieldName.equals("Activity")) {

					// ── Activity: must be "Commissioning" or "Welcome call" ──
					boolean activityValid = validActivityValues.stream().anyMatch(v -> v.equalsIgnoreCase(actualValue));

					if (activityValid) {
						System.out.println("✅ Activity : " + actualValue + " (valid ✔)");
					} else {
						System.out.println("❌ Activity expected one of " + validActivityValues + " but got: '"
								+ actualValue + "'");
						allPassed = false;
					}

				} else {
					System.out.println("✅ " + fieldName + " : " + actualValue);
				}
			}

			// ── DOA column: must be "true" (toggle was ON when report generated) ──
			if (colIndex.containsKey("DOA")) {

				String doaValue = getCellValueAsString(targetRow.getCell(colIndex.get("DOA"))).trim();

				if (doaValue.equalsIgnoreCase("true")) {
					System.out.println("✅ DOA : " + doaValue + " (expected true ✔)");
				} else {
					System.out.println("❌ DOA expected 'true' but got: '" + doaValue + "'");
					allPassed = false;
				}

			} else {
				System.out.println("⚠️  'DOA' column not found in Excel – skipping DOA value check");
			}

			if (!allPassed) {
				throw new RuntimeException("❌ One or more required fields failed validation in the DOA report Excel");
			}

			System.out.println("✅ All DOA Excel fields validated successfully");
		}
	}

	// ═══════════════════════════════════════════════════════════════
	// SHARED HELPER METHODS
	// ═══════════════════════════════════════════════════════════════

	private static void validateToast() {

		WebElement toast = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.toast_content_subtitle")));

		String toastText = toast.getText().trim();

		if (toastText.contains("Report generation in progress.")) {
			System.out.println("✅ Toast validated");
		} else {
			throw new RuntimeException("❌ Unexpected toast: " + toastText);
		}
	}

	private static void dismissToast() {

		try {
			wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.Toastify__toast-container")))
					.click();
		} catch (Exception e) {
			System.out.println("ℹ️ Toast auto-dismissed");
		}

		wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.Toastify__toast-container")));
	}

	private static void validatePendingStatus(WebElement row) {

		List<WebElement> pendingLabelList = row.findElements(By.xpath(".//p[@id='labelPending']"));

		if (!pendingLabelList.isEmpty()) {
			System.out.println("✅ Status validated: Pending");
		} else {
			throw new RuntimeException("❌ Pending status label not found in row");
		}
	}

	private static void validateDisabledDownload(WebElement row) {

		List<WebElement> disabledDownload = row
				.findElements(By.xpath(".//div[contains(@class,'crm__bulk__upload__disabled__download__file')]"));

		if (!disabledDownload.isEmpty()) {
			System.out.println("✅ Download disabled for Pending");
		} else {
			throw new RuntimeException("❌ Disabled download icon not found in row");
		}
	}

	private static void clickStatusFilter(String label) throws InterruptedException {

		By outerLocator = By.xpath("//div[contains(@class,'reports__square__box__with__inside__text')"
				+ " and .//div[@aria-label='" + label + "']]");

		slowScrollTo(outerLocator, 500);

		WebElement outerDiv = wait.until(ExpectedConditions.presenceOfElementLocated(outerLocator));

		js.executeScript("arguments[0].click();", outerDiv);

		System.out.println("✅ Clicked status: " + label);

		Thread.sleep(400);
	}

	private static File waitForDownloadedFile(String downloadDir, String jobId, int timeoutSec)
			throws InterruptedException {

		long deadline = System.currentTimeMillis() + (timeoutSec * 1000L);

		while (System.currentTimeMillis() < deadline) {

			File dir = new File(downloadDir);

			File[] candidates = dir.listFiles((d, name) -> name.contains(jobId) && name.toLowerCase().endsWith(".xlsx")
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

		throw new RuntimeException("❌ Downloaded Excel file not found for Job ID: " + jobId);
	}

	private static String getCellValueAsString(Cell cell) {

		if (cell == null) {
			return "";
		}

		return new DataFormatter().formatCellValue(cell).trim();
	}

	private static void slowScrollTo(By locator) throws InterruptedException {
		slowScrollTo(locator, 800);
	}

	private static void slowScrollTo(By locator, int pauseMs) throws InterruptedException {

		try {
			WebElement el = driver.findElement(locator);
			js.executeScript("arguments[0].scrollIntoView({behavior:'smooth',block:'center'});", el);
			Thread.sleep(pauseMs);
		} catch (NoSuchElementException e) {
			System.out.println("⚠️ Element not found: " + locator);
		}
	}

	private static void clickByText(WebDriverWait wait, String text) {

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[contains(text(),'" + text + "')]"))).click();

		System.out.println("✅ Clicked: " + text);
	}

	private static String waitForOTP(String mobile, int retries, long delayMs) throws InterruptedException {

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

		String url = "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev";
		String user = "hw_goutham";
		String pass = "9qIE0mwg8ehN";

		try (Connection con = DriverManager.getConnection(url, user, pass);
				PreparedStatement ps = con.prepareStatement(
						"SELECT otp FROM otp WHERE mobile_number = ? ORDER BY create_time DESC LIMIT 1")) {

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
