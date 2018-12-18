package org.servantscode.donation;

import java.util.Date;

public class Donation {
    public enum DonationType {CASH, CHECK, EGIFT, CREDIT_CARD}

    private long id;
    private int familyId;
    private float amount;
    private Date donationDate;
    private DonationType donationType;
    private int checkNumber;
    private long transactionId;

    // ------ Accessors -----
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }

    public Date getDonationDate() { return donationDate; }
    public void setDonationDate(Date donationDate) { this.donationDate = donationDate; }

    public DonationType getDonationType() { return donationType; }
    public void setDonationType(DonationType donationType) { this.donationType = donationType; }
    public void setDonationType(String donationType) { this.donationType = DonationType.valueOf(donationType); }

    public int getCheckNumber() { return checkNumber; }
    public void setCheckNumber(int checkNumber) { this.checkNumber = checkNumber; }

    public long getTransactionId() { return transactionId; }
    public void setTransactionId(long transactionId) { this.transactionId = transactionId; }
}
