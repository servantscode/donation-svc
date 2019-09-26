package org.servantscode.donation;

import org.servantscode.commons.rest.PaginatedResponse;

import java.util.List;

public class PaginatedDonationResponse extends PaginatedResponse<Donation> {
    private float totalDonationValue;

    public PaginatedDonationResponse(int start, int size, int totalDonations, List<Donation> donations, float totalDonationValue) {
        super(start, size, totalDonations, donations);
        this.totalDonationValue = totalDonationValue;
    }

    // ----- Accessors -----
    public float getTotalDonationValue() { return totalDonationValue; }
    public void setTotalDonationValue(float totalDonationValue) { this.totalDonationValue = totalDonationValue; }
}
