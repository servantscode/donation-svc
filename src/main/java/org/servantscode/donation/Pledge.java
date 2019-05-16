package org.servantscode.donation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

public class Pledge {
    public enum PledgeType {EGIFT, BASKET};
    public enum PledgeFrequency {WEEKLY, SEMI_MONTHLY, MONTHLY, QUARTERLY, SEMI_ANNUALLY, ANNUALLY};

    private int id;
    private int familyId;
    private int fundId;
    private String fundName;
    private PledgeType pledgeType;
    private PledgeFrequency pledgeFrequency;
    private float pledgeAmount;
    private float annualPledgeAmount;

    private LocalDate pledgeDate;
    private LocalDate pledgeStart;
    private LocalDate pledgeEnd;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public int getFundId() { return fundId; }
    public void setFundId(int fundId) { this.fundId = fundId; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public PledgeType getPledgeType() { return pledgeType; }
    public void setPledgeType(PledgeType pledgeType) { this.pledgeType = pledgeType; }
    public void setPledgeType(String pledgeType) { this.pledgeType = PledgeType.valueOf(pledgeType); }

    public LocalDate getPledgeDate() { return pledgeDate; }
    public void setPledgeDate(LocalDate pledgeDate) { this.pledgeDate = pledgeDate; }

    public LocalDate getPledgeStart() { return pledgeStart; }
    public void setPledgeStart(LocalDate pledgeStart) { this.pledgeStart = pledgeStart; }

    public LocalDate getPledgeEnd() { return pledgeEnd; }
    public void setPledgeEnd(LocalDate pledgeEnd) { this.pledgeEnd = pledgeEnd; }

    public PledgeFrequency getPledgeFrequency() { return pledgeFrequency; }
    public void setPledgeFrequency(PledgeFrequency pledgeFrequency) { this.pledgeFrequency = pledgeFrequency; }
    public void setPledgeFrequency(String pledgeFrequency) { this.pledgeFrequency = PledgeFrequency.valueOf(pledgeFrequency); }

    public float getPledgeAmount() { return pledgeAmount; }
    public void setPledgeAmount(float pledgeAmount) { this.pledgeAmount = pledgeAmount; }

    public float getAnnualPledgeAmount() { return annualPledgeAmount; }
    public void setAnnualPledgeAmount(float annualPledgeAmount) { this.annualPledgeAmount = annualPledgeAmount; }
}
