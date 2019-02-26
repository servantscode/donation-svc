package org.servantscode.donation;

public class FamilyGivingInfo {
    private int id;
    private String surname;
    private int envelopeNumber;

    public FamilyGivingInfo() { }

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getSurname() { return surname; }
    public void setSurname(String surname) { this.surname = surname; }

    public int getEnvelopeNumber() { return envelopeNumber; }
    public void setEnvelopeNumber(int envelopeNumber) { this.envelopeNumber = envelopeNumber; }
}
