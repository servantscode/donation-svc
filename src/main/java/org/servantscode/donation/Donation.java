package org.servantscode.donation;

import java.time.ZonedDateTime;

public class Donation {
    public enum DonationType {CASH, CHECK, EGIFT, CREDIT_CARD}

    private long id;
    private int familyId;
    private int fundId;
    private String fundName;
    private float amount;

    private ZonedDateTime donationDate;
    private DonationType donationType;
    private int checkNumber;
    private long transactionId;

    // ------ Accessors -----
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public int getFundId() { return fundId; }
    public void setFundId(int fundId) { this.fundId = fundId; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }

    public ZonedDateTime getDonationDate() { return donationDate; }
    public void setDonationDate(ZonedDateTime donationDate) { this.donationDate = donationDate; }

    public DonationType getDonationType() { return donationType; }
    public void setDonationType(DonationType donationType) { this.donationType = donationType; }
    public void setDonationType(String donationType) { this.donationType = DonationType.valueOf(donationType); }

    public int getCheckNumber() { return checkNumber; }
    public void setCheckNumber(int checkNumber) { this.checkNumber = checkNumber; }

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }
}
