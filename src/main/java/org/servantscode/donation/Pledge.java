package org.servantscode.donation;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

public class Pledge {
    public enum PledgeType {EGIFT, BASKET};
    public enum PledgeFrequency {WEEKLY, MONTHLY, QUARTERLY, ANNUALLY};

    private int id;
    private int familyId;
    private PledgeType pledgeType;
    private int envelopeNumber;
    private PledgeFrequency pledgeFrequency;
    private float pledgeAmount;
    private float annualPledgeAmount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date pledgeDate;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date pledgeStart;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date pledgeEnd;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFamilyId() { return familyId; }
    public void setFamilyId(int familyId) { this.familyId = familyId; }

    public PledgeType getPledgeType() { return pledgeType; }
    public void setPledgeType(PledgeType pledgeType) { this.pledgeType = pledgeType; }
    public void setPledgeType(String pledgeType) { this.pledgeType = PledgeType.valueOf(pledgeType); }

    public int getEnvelopeNumber() { return envelopeNumber; }
    public void setEnvelopeNumber(int envelopeNumber) { this.envelopeNumber = envelopeNumber; }

    public Date getPledgeDate() { return pledgeDate; }
    public void setPledgeDate(Date pledgeDate) { this.pledgeDate = pledgeDate; }

    public Date getPledgeStart() { return pledgeStart; }
    public void setPledgeStart(Date pledgeStart) { this.pledgeStart = pledgeStart; }

    public Date getPledgeEnd() { return pledgeEnd; }
    public void setPledgeEnd(Date pledgeEnd) { this.pledgeEnd = pledgeEnd; }

    public PledgeFrequency getPledgeFrequency() { return pledgeFrequency; }
    public void setPledgeFrequency(PledgeFrequency pledgeFrequency) { this.pledgeFrequency = pledgeFrequency; }
    public void setPledgeFrequency(String pledgeFrequency) { this.pledgeFrequency = PledgeFrequency.valueOf(pledgeFrequency); }

    public float getPledgeAmount() { return pledgeAmount; }
    public void setPledgeAmount(float pledgeAmount) { this.pledgeAmount = pledgeAmount; }

    public float getAnnualPledgeAmount() { return annualPledgeAmount; }
    public void setAnnualPledgeAmount(float annualPledgeAmount) { this.annualPledgeAmount = annualPledgeAmount; }
}
