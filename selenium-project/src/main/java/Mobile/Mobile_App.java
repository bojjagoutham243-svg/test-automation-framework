package Mobile;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;

import org.openqa.selenium.By;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;

public class Mobile_App {

    public static void main(String[] args) throws Exception {

        // ================= APPIUM OPTIONS =================

        UiAutomator2Options options =
                new UiAutomator2Options();

        options.setPlatformName("Android");

        options.setDeviceName("Android Device");

        options.setAutomationName("UiAutomator2");

        // APK PATH
        options.setApp(
                "/Users/yourname/Desktop/app.apk"
        );

        // ================= DRIVER =================

        AndroidDriver driver =
                new AndroidDriver(

                        URI.create(
                                "http://127.0.0.1:4723"
                        ).toURL(),

                        options
                );

        // ================= WAIT =================

        driver.manage()
                .timeouts()
                .implicitlyWait(Duration.ofSeconds(20));

        System.out.println(
                "Mobile App Opened Successfully"
        );

        // ================= LOGIN =================

        driver.findElement(
                By.xpath("//android.widget.EditText[@text='Mobile Number']")
        ).sendKeys("9191919191");

        driver.findElement(
                By.xpath("//android.widget.Button[@text='Login']")
        ).click();

        System.out.println("Login clicked");

        // ================= IRF FORM =================

        driver.findElement(
                By.id("serial_no")
        ).sendKeys("CHG12345");

        System.out.println("Serial Number Entered");

        // ================= SUBMIT =================

        driver.findElement(
                By.id("submit")
        ).click();

        System.out.println("IRF Submitted Successfully");

        // ================= CLOSE APP =================

        Thread.sleep(3000);

        driver.quit();

        System.out.println("Driver Closed Successfully");
    }

// ================= DB METHOD =================
public static String getOTPFromDB(String mobile) {

    String otp = "";

    try {
        Connection con = DriverManager.getConnection(
                "jdbc:postgresql://172.26.35.4:5432/exicom_crm_dev",
                "hw_goutham",
                "9qIE0mwg8ehN"
        );

        Statement stmt = con.createStatement();

        String query = "SELECT otp FROM otp WHERE mobile_number='"
                + mobile + "' ORDER BY create_time DESC LIMIT 1";

        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            otp = rs.getString("otp");
        }

        con.close();

    } catch (Exception e) {
        e.printStackTrace();
    }

    return otp;
}
}
