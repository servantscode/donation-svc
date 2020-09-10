package org.servantscode.donation;

public class FamilyContributions {
    private int familyId;
    private String familyName;
    private String headName;
    private String spouseName;
    private int totalDonations;
    private float totalDonationValue;

    // ----- Accesssors -----
    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public String getHeadName() { return headName; }
    public void setHeadName(String headName) { this.headName = headName; }

    public String getSpouseName() { return spouseName; }
    public void setSpouseName(String spouseName) { this.spouseName = spouseName; }

    public int getTotalDonations() { return totalDonations; }
    public void setTotalDonations(int totalDonations) { this.totalDonations = totalDonations; }

    public float getTotalDonationValue() { return totalDonationValue; }
    public void setTotalDonationValue(float totalDonationValue) { this.totalDonationValue = totalDonationValue; }
}
