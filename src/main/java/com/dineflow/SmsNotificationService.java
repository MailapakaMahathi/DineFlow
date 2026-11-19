
package com.dineflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SmsNotificationService {

    private static final String ACCOUNT_SID =
            System.getenv("TWILIO_ACCOUNT_SID");

    private static final String AUTH_TOKEN =
            System.getenv("TWILIO_AUTH_TOKEN");

    private static final String FROM_NUMBER =
            System.getenv("TWILIO_PHONE_NUMBER");

    // Stores the result of a Twilio SMS request.
    private static class SmsSendResult {
        private final boolean accepted;
        private final String messageSid;

        public SmsSendResult(boolean accepted, String messageSid) {
            this.accepted = accepted;
            this.messageSid = messageSid;
        }
    }

    private static boolean initializeTwilio() {

        if (ACCOUNT_SID == null || ACCOUNT_SID.isBlank()
                || AUTH_TOKEN == null || AUTH_TOKEN.isBlank()
                || FROM_NUMBER == null || FROM_NUMBER.isBlank()) {

            System.err.println("Twilio credentials are not configured.");
            return false;
        }

        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        return true;
    }

    // Send SMS and capture Twilio Message SID.
    private static SmsSendResult sendVendorSMSWithResult(
            String vendorPhone,
            String messageBody) {

        if (vendorPhone == null || vendorPhone.isBlank()
                || messageBody == null || messageBody.isBlank()) {

            System.err.println("Invalid vendor phone number or SMS message.");

            return new SmsSendResult(false, null);
        }

        if (!initializeTwilio()) {
            return new SmsSendResult(false, null);
        }

        try {

            // Trial-account predefined template.
            Message message = Message.creator(
                    new PhoneNumber(vendorPhone),
                    new PhoneNumber(FROM_NUMBER),
                    "sms_internal_alerts"
            ).create();

            String messageSid = message.getSid();

            System.out.println(
                    "Twilio accepted SMS request. SID: " + messageSid
            );

            return new SmsSendResult(true, messageSid);

        } catch (Exception e) {

            System.err.println("SMS sending failed: " + e.getMessage());

            return new SmsSendResult(false, null);
        }
    }

    // Public method retained for compatibility.
    public static boolean sendVendorSMS(
            String vendorPhone,
            String messageBody) {

        return sendVendorSMSWithResult(
                vendorPhone, messageBody
        ).accepted;
    }

    // Create and save a low-stock notification.
    public static boolean saveNotification(
            int itemId,
            String vendorPhone,
            String message,
            String status) {

        if (itemId <= 0 || vendorPhone == null || vendorPhone.isBlank()
                || message == null || message.isBlank()) {
            return false;
        }

        String sql = """
            INSERT INTO vendor_sms_notifications
            (item_id, vendor_phone, message, notification_status)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                return false;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, itemId);
                ps.setString(2, vendorPhone);
                ps.setString(3, message);
                ps.setString(4, status);

                return ps.executeUpdate() == 1;
            }

        } catch (SQLException e) {

            System.err.println("Unable to save vendor notification.");
            e.printStackTrace();
            return false;
        }
    }

    // Process pending notifications.
    public static void processPendingNotifications() {

        String selectSql = """
            SELECT notification_id, vendor_phone, message
            FROM vendor_sms_notifications
            WHERE notification_status = 'PENDING'
            ORDER BY notification_id
            """;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(selectSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int notificationId =
                            rs.getInt("notification_id");

                    String vendorPhone =
                            rs.getString("vendor_phone");

                    String messageBody =
                            rs.getString("message");

                    SmsSendResult result = sendVendorSMSWithResult(
                            vendorPhone,
                            messageBody
                    );

                    updateNotificationStatus(
                            notificationId,
                            result.accepted ? "ACCEPTED" : "FAILED",
                            result.messageSid
                    );
                }
            }

        } catch (SQLException e) {

            System.err.println("Unable to process pending notifications.");
            e.printStackTrace();
        }
    }

    // Update attempt details and save Twilio Message SID.
    private static void updateNotificationStatus(
            int notificationId,
            String status,
            String messageSid) {

        String sql = """
            UPDATE vendor_sms_notifications
            SET notification_status = ?,
                twilio_message_sid = ?,
                attempt_count = attempt_count + 1,
                last_attempt_at = CURRENT_TIMESTAMP
            WHERE notification_id = ?
            """;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, status);
                ps.setString(2, messageSid);
                ps.setInt(3, notificationId);

                ps.executeUpdate();
            }

        } catch (SQLException e) {

            System.err.println("Unable to update notification status.");
            e.printStackTrace();
        }
    }

    // Retrieve vendor notification history.
    public static String getNotificationsJson() {

        JsonArray notifications = new JsonArray();

        String sql = """
            SELECT n.notification_id,
                   n.item_id,
                   m.item_name,
                   m.vendor_name,
                   n.vendor_phone,
                   n.message,
                   n.notification_status,
                   n.attempt_count,
                   n.twilio_message_sid,
                   n.created_at,
                   n.last_attempt_at,
                   n.sent_at
            FROM vendor_sms_notifications n
            LEFT JOIN menu_items m ON n.item_id = m.item_id
            ORDER BY n.created_at DESC
            """;

        try (Connection con = DB.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection unavailable.");
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    JsonObject obj = new JsonObject();

                    obj.addProperty("notificationId",
                            rs.getInt("notification_id"));

                    obj.addProperty("itemId",
                            rs.getInt("item_id"));

                    obj.addProperty("itemName",
                            rs.getString("item_name"));

                    obj.addProperty("vendorName",
                            rs.getString("vendor_name"));

                    obj.addProperty("vendorPhone",
                            rs.getString("vendor_phone"));

                    obj.addProperty("message",
                            rs.getString("message"));

                    obj.addProperty("status",
                            rs.getString("notification_status"));

                    obj.addProperty("attemptCount",
                            rs.getInt("attempt_count"));

                    obj.addProperty("twilioMessageSid",
                            rs.getString("twilio_message_sid"));

                    Timestamp created = rs.getTimestamp("created_at");
                    Timestamp attempted = rs.getTimestamp("last_attempt_at");
                    Timestamp sent = rs.getTimestamp("sent_at");

                    obj.addProperty("createdAt",
                            created == null ? null : created.toString());

                    obj.addProperty("lastAttemptAt",
                            attempted == null ? null : attempted.toString());

                    obj.addProperty("sentAt",
                            sent == null ? null : sent.toString());

                    notifications.add(obj);
                }
            }

            return new Gson().toJson(notifications);

        } catch (SQLException e) {

            System.err.println("Unable to retrieve vendor notifications.");
            e.printStackTrace();

            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("message", e.getMessage());

            return new Gson().toJson(error);
        }
    }
}
