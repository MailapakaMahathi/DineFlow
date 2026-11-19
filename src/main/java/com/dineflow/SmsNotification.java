
package com.dineflow;

import java.time.LocalDateTime;

public class SmsNotification {

    private int notificationId;
    private int itemId;
    private String vendorPhone;
    private String message;
    private String notificationStatus;
    private int attemptCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastAttemptAt;
    private LocalDateTime sentAt;

    // Constructor
    public SmsNotification(
            int notificationId,
            int itemId,
            String vendorPhone,
            String message,
            String notificationStatus,
            int attemptCount) {

        this.notificationId = notificationId;
        this.itemId = itemId;
        this.vendorPhone = vendorPhone;
        this.message = message;
        this.notificationStatus = notificationStatus;
        this.attemptCount = attemptCount;
    }

    // Getters
    public int getNotificationId() {
        return notificationId;
    }

    public int getItemId() {
        return itemId;
    }

    public String getVendorPhone() {
        return vendorPhone;
    }

    public String getMessage() {
        return message;
    }

    public String getNotificationStatus() {
        return notificationStatus;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    // Setters
    public void setNotificationStatus(String notificationStatus) {
        this.notificationStatus = notificationStatus;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setLastAttemptAt(LocalDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

}
