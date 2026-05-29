package web;

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

public class FEAppUsageReport {

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

		Thread.sleep(1000);

		// ═══════════════════════════════════════════════════════════════
		// FLOW 1 : FE APP USAGE REPORT FLOW
		// ═══════════════════════════════════════════════════════════════

		// STEP 1
		slowScrollTo(By.xpath("//p[contains(@class,'crm__sidebar__text') and normalize-space()='Reports']"));

		clickByText(wait, "Reports");

		// STEP 2
		WebElement reportsSubMenu = wait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("(//*[text()='Reports'])[2]")));

		slowScrollTo(By.xpath("(//*[text()='Reports'])[2]"));

		reportsSubMenu.click();

		System.out.println("✅ Clicked: Reports sub-menu");

		// STEP 3
		wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//*[contains(text(),'Start building your report')]")));

		System.out.println("✅ Validated: Start building your report");

		// STEP 4
		WebElement dropdownInput = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//div[contains(@class,'crm__dropdown__input-container')]//input[@role='combobox']")));

		dropdownInput.click();

		Thread.sleep(500);

		dropdownInput.sendKeys("FE App Usage Report");

		Thread.sleep(500);

		WebElement ticketReportOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
				"//div[contains(@class,'crm__dropdown__option') and normalize-space(text())='FE App Usage Report']")));

		ticketReportOption.click();

		System.out.println("✅ Selected: FE App Usage Report");

		// STEP 5 - Select Organisation = All
		selectDropdownById("react-select-4-input", "All", "Organisation");

		// STEP 6 - Select Vendor = All
		selectDropdownById("react-select-6-input", "All", "Vendor");

		// STEP 7 - Select Account = All
		selectDropdownById("react-select-7-input", "All", "Account");

		// STEP 8 - Select Current Month
		selectCurrentMonth();

		// STEP 9
		WebElement generateBtn = wait
				.until(ExpectedConditions.elementToBeClickable(By.id("report__screen__generate__report")));

		slowScrollTo(By.id("report__screen__generate__report"));

		generateBtn.click();

		System.out.println("✅ Clicked: Generate Report");

		// STEP 10
		validateToast();

		// STEP 11
		dismissToast();

		// STEP 12
		Thread.sleep(2000);

		WebElement pinnedTopRow = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(
				"//div[contains(@class,'MuiDataGrid-pinnedColumns')]" + "//div[@role='row' and @data-rowindex='0']")));

		String capturedJobId = pinnedTopRow.findElement(By.xpath(
				".//div[@role='cell' and @data-field='jobId']" + "//div[contains(@class,'MuiDataGrid-cellContent')]"))
				.getAttribute("title").trim();

		System.out.println("✅ Captured Job ID: " + capturedJobId);

		WebElement scrollableTopRow = wait.until(ExpectedConditions
				.visibilityOfElementLocated(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
						+ "//div[@role='row' and @data-rowindex='0']")));

		// STEP 13
		validateCell(scrollableTopRow, "productType", "Product Type");

		// STEP 14
		validateJobStatusCell(scrollableTopRow);

		// STEP 15
		validatePendingStatus(scrollableTopRow);

		// STEP 16 ← get the rowindex string first, then pass it
		String topRowIndex = scrollableTopRow.getAttribute("data-rowindex");
		validateDisabledDownload(topRowIndex);

		// STEP 17
		System.out.println("⏳ Polling for completion...");
		WebElement completedRow = pollUntilJobDone(capturedJobId, 25, 10);

		// STEP 18 - Click download on completed row
		String completedRowIndex = completedRow.getAttribute("data-rowindex");

		// Scroll right so downloadReport column enters the virtual render zone
		WebElement scroller18 = driver.findElement(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScroller')]"));
		js.executeScript("arguments[0].scrollLeft = arguments[0].scrollWidth;", scroller18);
		Thread.sleep(800);

		WebElement downloadCell = wait.until(ExpectedConditions
				.presenceOfElementLocated(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
						+ "//div[@role='row' and @data-rowindex='" + completedRowIndex + "']"
						+ "//div[@role='cell' and @data-field='downloadReport']")));

		WebElement downloadIconDiv = downloadCell
				.findElement(By.xpath(".//button[@type='button']/div[contains(@class,'crm__icon')]"));

		String downloadClass = downloadIconDiv.getAttribute("class");
		System.out.println("ℹ️ Download icon class on completed row: " + downloadClass);

		if (downloadClass == null || downloadClass.contains("disabled")) {
			throw new RuntimeException("❌ Download icon is still disabled on Completed row. Class: " + downloadClass);
		}

		js.executeScript("arguments[0].click();", downloadIconDiv);
		System.out.println("✅ Clicked download");

		// STEP 19
		File downloadedFile = waitForDownloadedFile(DOWNLOAD_DIR, capturedJobId, 60);

		System.out.println("✅ File Downloaded: " + downloadedFile.getName());

		// STEP 20
		validateExcelFields(downloadedFile);

		System.out.println("\n🎉 FE APP USAGE REPORT FLOW COMPLETED");

		Thread.sleep(2000);
		driver.quit();
	}

	// ═══════════════════════════════════════════════════════════════
	// HELPER METHODS
	// ═══════════════════════════════════════════════════════════════

	private static void selectDropdownById(String inputId, String value, String label) throws InterruptedException {

		WebElement input = wait.until(ExpectedConditions.elementToBeClickable(By.id(inputId)));

		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", input);

		Thread.sleep(400);

		js.executeScript("arguments[0].click();", input);

		Thread.sleep(400);

		input.sendKeys(value);

		Thread.sleep(500);

		WebElement option = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
				"//div[contains(@class,'crm__dropdown__option')" + " and normalize-space(text())='" + value + "']")));

		option.click();

		System.out.println("✅ Selected " + label + ": " + value);

		Thread.sleep(500);
	}

	private static void selectCurrentMonth() throws InterruptedException {

		List<WebElement> allComboboxes = driver.findElements(By.xpath("//input[@role='combobox']"));

		WebElement dateDropdownInput = allComboboxes.get(allComboboxes.size() - 1);

		js.executeScript("arguments[0].scrollIntoView({behavior:'smooth', block:'center'});", dateDropdownInput);

		Thread.sleep(400);

		js.executeScript("arguments[0].click();", dateDropdownInput);

		Thread.sleep(400);

		dateDropdownInput.sendKeys("Current Month");

		Thread.sleep(600);

		WebElement currentMonthOption = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
				"//div[contains(@class,'crm__dropdown__option')" + " and normalize-space(text())='Current Month']")));

		currentMonthOption.click();

		System.out.println("✅ Selected: Current Month");
	}

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

	// ─────────────────────────────────────────────────────────────
	// For plain text cells that use MuiDataGrid-cellContent wrapper
	// e.g. productType, jobId, reportType, etc.
	// ─────────────────────────────────────────────────────────────
	private static void validateCell(WebElement row, String field, String label) {

		WebElement cell = row.findElement(By.xpath(".//div[@role='cell' and @data-field='" + field + "']"
				+ "//div[contains(@class,'MuiDataGrid-cellContent')]"));

		String value = cell.getAttribute("title").trim();

		System.out.println("✅ " + label + ": " + value);
	}

	// ─────────────────────────────────────────────────────────────
	// For jobStatus cell — uses crm__table__status__wrap widget,
	// NOT MuiDataGrid-cellContent. Reads <p id="labelXxx"> text.
	// ─────────────────────────────────────────────────────────────
	private static void validateJobStatusCell(WebElement row) {

		WebElement statusCell = row.findElement(By.xpath(".//div[@role='cell' and @data-field='jobStatus']"));

		// Reads whichever label is present: labelPending / labelCompleted / labelFailed
		WebElement statusLabel = statusCell.findElement(By.xpath(".//p[starts-with(@id,'label')]"));

		String statusText = statusLabel.getText().trim();

		System.out.println("✅ Job Status: " + statusText);
	}

	private static void validatePendingStatus(WebElement row) {

		List<WebElement> pendingLabelList = row.findElements(By.xpath(".//p[@id='labelPending']"));

		if (!pendingLabelList.isEmpty()) {
			System.out.println("✅ Status validated: Pending");
		} else {
			throw new RuntimeException("❌ Pending status not found");
		}
	}

	private static void validateDisabledDownload(String rowIndex) throws InterruptedException {

		// 1. Scroll the virtualScroller horizontally so the downloadReport column
		// renders
		WebElement scroller = driver.findElement(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScroller')]"));
		js.executeScript("arguments[0].scrollLeft = arguments[0].scrollWidth;", scroller);
		Thread.sleep(800); // allow column to render after horizontal scroll

		// 2. Now wait for the cell — it should be in the DOM after scrolling
		WebElement downloadCell = wait.until(ExpectedConditions
				.presenceOfElementLocated(By.xpath("//div[contains(@class,'MuiDataGrid-virtualScrollerRenderZone')]"
						+ "//div[@role='row' and @data-rowindex='" + rowIndex + "']"
						+ "//div[@role='cell' and @data-field='downloadReport']")));

		WebElement button = downloadCell.findElement(By.xpath(".//button[@type='button']"));

		WebElement iconDiv = button.findElement(By.xpath("./div[contains(@class,'crm__icon')]"));

		String iconClass = iconDiv.getAttribute("class");

		if (iconClass != null && iconClass.contains("crm__bulk__upload__disabled__download__file")) {
			System.out.println("✅ Download disabled for Pending");
		} else if (iconClass != null && iconClass.contains("crm__bulk__upload__download__file")) {
			throw new RuntimeException(
					"❌ Download is ENABLED — expected DISABLED for Pending status. Class: " + iconClass);
		} else {
			throw new RuntimeException("❌ Unexpected download icon class: " + iconClass);
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

	private static void navigateToTicketReport() throws InterruptedException {

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

		ddInput.sendKeys("FE App Usage Report");

		Thread.sleep(1000);

		wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
				"//div[contains(@class,'crm__dropdown__option') and normalize-space(text())='FE App Usage Report']")))
				.click();

		System.out.println("✅ FE App Usage Report selected");

		Thread.sleep(500);
	}

	private static WebElement getFtrToggle() {

		List<WebElement> byAriaLabel = driver
				.findElements(By.xpath("//input[@type='checkbox' and @aria-label='customized switch']"));

		if (!byAriaLabel.isEmpty()) {
			return byAriaLabel.get(0);
		}

		List<WebElement> bySwitch = driver.findElements(By.xpath("//span[contains(@class,'MuiSwitch-switchBase')]"));

		if (!bySwitch.isEmpty()) {
			return bySwitch.get(0);
		}

		List<WebElement> anySwitch = driver.findElements(By.xpath("//*[@role='switch']"));

		if (!anySwitch.isEmpty()) {
			return anySwitch.get(0);
		}

		throw new RuntimeException("❌ FTR toggle not found");
	}

	private static boolean isFtrToggleDisabled(WebElement toggle) {

		String ariaDisabled = toggle.getAttribute("aria-disabled");
		String disabled = toggle.getAttribute("disabled");
		String classList = toggle.getAttribute("class");

		return "true".equalsIgnoreCase(ariaDisabled) || disabled != null
				|| (classList != null && classList.contains("Mui-disabled"));
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

		throw new RuntimeException("❌ Downloaded Excel file not found");
	}

	private static void validateExcelFields(File excelFile) throws Exception {

		System.out.println("\n📄 Validating Excel: " + excelFile.getName());

		List<String> notNullColumns = Arrays.asList("Field Engineer", "Role", "Circle", "Checkin Count", "Usage");

		List<String> allReportColumns = Arrays.asList("Field Engineer", "Role", "Organisation", "Circle", "Circle Head",
				"Checkin Count", "Usage", "Last Checkin", "Pending Requests", "Closed Requests", "Hold Requests");

		try (FileInputStream fis = new FileInputStream(excelFile); Workbook workbook = new XSSFWorkbook(fis)) {

			Sheet sheet = workbook.getSheetAt(0);

			System.out.println("📊 Total Rows in Sheet: " + sheet.getLastRowNum());

			Row headerRow = null;

			for (Row row : sheet) {

				if (row == null)
					continue;

				String firstCellValue = getCellValueAsString(row.getCell(0)).trim();

				if (firstCellValue.equalsIgnoreCase("Field Engineer")) {
					headerRow = row;
					break;
				}
			}

			if (headerRow == null) {
				throw new RuntimeException("❌ Header row not found");
			}

			System.out.println("✅ Header Row Found at Row Index: " + headerRow.getRowNum());

			Map<String, Integer> colIndex = new LinkedHashMap<>();

			for (Cell cell : headerRow) {
				String header = getCellValueAsString(cell).trim();
				if (!header.isEmpty()) {
					colIndex.put(header, cell.getColumnIndex());
				}
			}

			System.out.println("📋 Detected Columns: " + colIndex.keySet());

			for (String col : allReportColumns) {
				if (!colIndex.containsKey(col)) {
					throw new RuntimeException("❌ Missing column in header: " + col);
				}
				System.out.println("✅ Column Present: " + col);
			}

			System.out.println("\n✅ All required columns validated successfully");

			List<Row> dataRows = new ArrayList<>();

			for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null)
					continue;

				boolean hasData = false;
				for (Cell cell : row) {
					if (!getCellValueAsString(cell).trim().isEmpty()) {
						hasData = true;
						break;
					}
				}
				if (hasData) {
					dataRows.add(row);
				}
			}

			if (dataRows.isEmpty()) {
				System.out.println("\n⚠️ Report contains NO DATA rows.");
				System.out.println("✅ Header validation completed successfully.");
				System.out.println("✅ Excel structure validation PASSED.");
				return;
			}

			System.out.println("✅ Total Data Rows Found: " + dataRows.size());

			System.out.println("\n🔍 Running NOT NULL check on columns: " + notNullColumns);

			boolean allNotNullPassed = true;

			for (Row row : dataRows) {
				for (String fieldName : notNullColumns) {

					if (!colIndex.containsKey(fieldName))
						continue;

					Cell cell = row.getCell(colIndex.get(fieldName));
					String actualValue = getCellValueAsString(cell).trim();

					if (actualValue.isEmpty()) {
						System.out.println("❌ NULL/EMPTY at Row " + row.getRowNum() + ", Column: " + fieldName);
						allNotNullPassed = false;
					}
				}
			}

			if (!allNotNullPassed) {
				throw new RuntimeException("❌ NOT NULL check FAILED — one or more required fields are NULL/EMPTY");
			}

			System.out.println("✅ NOT NULL check PASSED for all rows");

			System.out.println("\n📊 ===== FULL REPORT DATA =====\n");

			Map<String, Integer> colWidths = new LinkedHashMap<>();
			for (String col : allReportColumns) {
				colWidths.put(col, col.length());
			}
			for (Row row : dataRows) {
				for (String col : allReportColumns) {
					if (!colIndex.containsKey(col))
						continue;
					Cell cell = row.getCell(colIndex.get(col));
					int len = getCellValueAsString(cell).trim().length();
					colWidths.put(col, Math.max(colWidths.get(col), len));
				}
			}

			StringBuilder separator = new StringBuilder("+");
			for (String col : allReportColumns) {
				separator.append("-".repeat(colWidths.get(col) + 2)).append("+");
			}

			System.out.println(separator);
			StringBuilder headerLine = new StringBuilder("|");
			for (String col : allReportColumns) {
				headerLine.append(String.format(" %-" + colWidths.get(col) + "s |", col));
			}
			System.out.println(headerLine);
			System.out.println(separator);

			for (Row row : dataRows) {
				StringBuilder rowLine = new StringBuilder("|");
				for (String col : allReportColumns) {
					String value = "";
					if (colIndex.containsKey(col)) {
						value = getCellValueAsString(row.getCell(colIndex.get(col))).trim();
					}
					rowLine.append(String.format(" %-" + colWidths.get(col) + "s |", value));
				}
				System.out.println(rowLine);
			}

			System.out.println(separator);
			System.out.println("📊 Total Rows Printed: " + dataRows.size());

			System.out.println("\n🎉 Excel validation completed successfully!");
		}
	}

	private static WebElement pollUntilJobDone(String jobId, int intervalSec, int maxAttempts)
			throws InterruptedException {

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {

			System.out.println("🔄 Poll Attempt " + attempt + "/" + maxAttempts);

			driver.navigate().refresh();

			Thread.sleep(2000);

			navigateToTicketReport();

			List<WebElement> pinnedRows = driver
					.findElements(By.xpath("//div[contains(@class,'MuiDataGrid-pinnedColumns')]"
							+ "//div[@role='row' and " + ".//div[@role='cell' and @data-field='jobId']"
							+ "//div[contains(@class,'MuiDataGrid-cellContent') and @title='" + jobId + "']]"));

			if (pinnedRows.isEmpty()) {

				System.out.println("⚠️ Job row not visible");

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

			System.out.println("⏳ Waiting for completion...");

			Thread.sleep(intervalSec * 1000L);
		}

		throw new RuntimeException("❌ Job not completed in time");
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
						"SELECT otp FROM otp WHERE mobile_number = ? " + "ORDER BY create_time DESC LIMIT 1")) {

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