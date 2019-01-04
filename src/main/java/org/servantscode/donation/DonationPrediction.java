package org.servantscode.donation;

public class DonationPrediction {
    private int familyId;
    private String familyName;
    private int envelopeNumber;
    private float amount;
    private Donation.DonationType donationType;

    // ----- Accessors -----
    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public int getEnvelopeNumber() { return envelopeNumber; }
    public void setEnvelopeNumber(int envelopeNumber) { this.envelopeNumber = envelopeNumber; }

    public float getAmount() { return amount; }
    public void setAmount(float amount) { this.amount = amount; }

    public Donation.DonationType getDonationType() { return donationType; }
    public void setDonationType(Donation.DonationType donationType) { this.donationType = donationType; }
}
