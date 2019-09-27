package org.servantscode.donation;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public class Donation {
    public enum DonationType {CASH, CHECK, EGIFT, CREDIT_CARD, UNKNOWN}

    private long id;
    private int familyId;
    private String familyName;
    private int fundId;
    private String fundName;
    private float amount;
    private int batchNumber;

    private LocalDate donationDate;
    private DonationType donationType;
    private int checkNumber;
    private long transactionId;

    private String notes;

    private ZonedDateTime recordedTime;
    private int recorderId;
    private String recorderName;

    // ------ Accessors -----
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public int getFundId() { return fundId; }
    public void setFundId(int fundId) { this.fundId = fundId; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }

    public int getBatchNumber() { return batchNumber; }
    public void setBatchNumber(int batchNumber) { this.batchNumber = batchNumber; }

    public LocalDate getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDate donationDate) { this.donationDate = donationDate; }

    public DonationType getDonationType() { return donationType; }
    public void setDonationType(DonationType donationType) { this.donationType = donationType; }
    public void setDonationType(String donationType) { this.donationType = DonationType.valueOf(donationType); }

    public int getCheckNumber() { return checkNumber; }
    public void setCheckNumber(int checkNumber) { this.checkNumber = checkNumber; }

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public ZonedDateTime getRecordedTime() { return recordedTime; }
    public void setRecordedTime(ZonedDateTime recordedTime) { this.recordedTime = recordedTime; }

    public int getRecorderId() { return recorderId; }
    public void setRecorderId(int recorderId) { this.recorderId = recorderId; }

    public String getRecorderName() { return recorderName; }
    public void setRecorderName(String recorderName) { this.recorderName = recorderName; }
}
