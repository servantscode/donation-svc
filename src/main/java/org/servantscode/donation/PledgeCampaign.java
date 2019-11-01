package org.servantscode.donation;

import java.time.LocalDate;

public class PledgeCampaign {
    private int id;
    private int fundId;
    private LocalDate startDate;
    private LocalDate endDate;
    private float target;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getFundId() { return fundId; }
    public void setFundId(int fundId) { this.fundId = fundId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public float getTarget() { return target; }
    public void setTarget(float target) { this.target = target; }
}
