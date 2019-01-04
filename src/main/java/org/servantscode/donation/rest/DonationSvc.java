package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.donation.Donation;
import org.servantscode.donation.DonationPrediction;
import org.servantscode.donation.Pledge;
import org.servantscode.donation.db.DonationDB;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/donation")
public class DonationSvc {
    private static final Logger LOG = LogManager.getLogger(DonationSvc.class);

    @GET @Path("/family/{familyId}") @Produces(APPLICATION_JSON)
    public List<Donation> getDonations(@PathParam("familyId") int familyId) {
        try {
            return new DonationDB().getFamilyDonations(familyId);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family donations: " + familyId, t);
            throw t;
        }
    }

    @GET @Path("/predict") @Produces(APPLICATION_JSON)
    public DonationPrediction getDonationPrediction(@QueryParam("familyId") int familyId,
                                                    @QueryParam("envelopeNumber") int envelopeNumber) {
        if(familyId <= 0 && envelopeNumber <= 0)
            throw new BadRequestException();

        try {
            PledgeDB pdb = new PledgeDB();

            Pledge p;
            int localFamilyId = familyId;

            if(familyId > 0) {
                p = pdb.getActivePledge(familyId);
            } else if(envelopeNumber > 0) {
                p = pdb.getActivePledgeByEnvelope(envelopeNumber);
                if(p == null)
                    throw new NotFoundException();
                localFamilyId = p.getFamilyId();
            } else {
                throw new NotFoundException();
            }

            Donation d = new DonationDB().getLastDonation(localFamilyId );
            String surname = pdb.getFamilySurname(localFamilyId);

            DonationPrediction pred = new DonationPrediction();
            pred.setFamilyName(surname);
            pred.setFamilyId(localFamilyId);

            if(d != null) {
                pred.setAmount(d.getAmount());
                pred.setDonationType(d.getDonationType());
            }

            if(p != null) {
                pred.setAmount(p.getPledgeAmount()); // Prefer the amount from the pledge if available.
                pred.setEnvelopeNumber(p.getEnvelopeNumber());
            }

            return pred;
        } catch (Throwable t) {
            if(familyId > 0) {
                LOG.error("Failed to predict donation details. Family Id: " + familyId, t);
            } else if(envelopeNumber > 0) {
                LOG.error("Failed to predict donation details. Envelope Number: " + envelopeNumber, t);
            }
            throw t;
        }
    }

    @POST @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Donation createDonation(Donation donation) {
        try {
            return new DonationDB().createDonation(donation);
        } catch(Throwable t) {
            LOG.error("Failed to create donation for family: " + donation.getFamilyId(), t);
            throw t;
        }
    }

    @POST @Path("/batch") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public List<Donation> createDonations(List<Donation> donations) {
        try {
            List<Donation> createdDonations = new ArrayList<>(donations.size());
            DonationDB db = new DonationDB();
            for(Donation donation: donations)
                createdDonations.add(db.createDonation(donation));
            return createdDonations;
        } catch(Throwable t) {
            LOG.error("Batch donation creation failed!!", t);
            throw t;
        }
    }

    @PUT @Path("/{donationId}") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Donation updateDonation(@PathParam("donationId") int donationId,
                                  Donation donation) {
        try {
            if(!new DonationDB().updateDonation(donation))
                throw new NotFoundException();

            return donation;
        } catch(Throwable t) {
            LOG.error("Failed to update donation: " + donationId, t);
            throw t;
        }
    }

    @DELETE @Path("/{donationId}")
    public void deleteDonation(@PathParam("donationId") int donationId) {
        try {
            if(!new DonationDB().deleteDonation(donationId))
                throw new NotFoundException();
        } catch(Throwable t) {
            LOG.error("Failed to delete donation: " + donationId, t);
            throw t;
        }
    }

    @GET @Path("/types") @Produces(APPLICATION_JSON)
    public List<String> getDonationTypes() {
        return EnumUtils.listValues(Donation.DonationType.class);
    }
}
