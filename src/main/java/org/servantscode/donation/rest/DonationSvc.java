package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.donation.Donation;
import org.servantscode.donation.DonationPrediction;
import org.servantscode.donation.FamilyGivingInfo;
import org.servantscode.donation.Pledge;
import org.servantscode.donation.db.DonationDB;
import org.servantscode.donation.db.FamilyGivingInfoDB;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/donation")
public class DonationSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(DonationSvc.class);

    private final DonationDB donationDB;
    private final PledgeDB pledgeDB;
    private final FamilyGivingInfoDB familyDB;

    public DonationSvc() {
        this.donationDB = new DonationDB();
        this.pledgeDB = new PledgeDB();
        this.familyDB = new FamilyGivingInfoDB();
    }

    @GET @Path("/family/{familyId}") @Produces(APPLICATION_JSON)
    public PaginatedResponse<Donation> getDonations(@PathParam("familyId") int familyId,
                                                    @QueryParam("start") @DefaultValue("0") int start,
                                                    @QueryParam("count") @DefaultValue("10") int count,
                                                    @QueryParam("sort_field") @DefaultValue("date DESC") String sortField,
                                                    @QueryParam("partial_name") @DefaultValue("") String search) {

        verifyUserAccess("donation.list");
        try {
            int totalDonations = donationDB.getDonationCount(familyId, search);

            List<Donation> donations = donationDB.getFamilyDonations(familyId, start, count, sortField, search);
            return new PaginatedResponse<>(start, donations.size(), totalDonations, donations);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family donations: " + familyId, t);
            throw t;
        }
    }

    @GET @Path("/predict") @Produces(APPLICATION_JSON)
    public DonationPrediction getDonationPrediction(@QueryParam("familyId") int familyId,
                                                    @QueryParam("envelopeNumber") int envelopeNumber) {
        verifyUserAccess("donation.create");
        if(familyId <= 0 && envelopeNumber <= 0)
            throw new BadRequestException();

        try {
            FamilyGivingInfo info = familyId > 0 ?
                    familyDB.getFamilyPledgeById(familyId):
                    familyDB.getFamilyPledgeByEnvelope(envelopeNumber);

            if(info == null) {
                LOG.error("No family found for prediction");
                throw new NotFoundException("No family specified for prediction");
            }

            Pledge p = pledgeDB.getActivePledge(info.getId());
            Donation d = donationDB.getLastDonation(info.getId());

            DonationPrediction pred = new DonationPrediction();
            pred.setFamilyId(info.getId());
            pred.setFamilyName(info.getSurname());
            pred.setEnvelopeNumber(info.getEnvelopeNumber());

            if(d != null) {
                pred.setAmount(d.getAmount());
                pred.setDonationType(d.getDonationType());
            }

            if(p != null) {
                pred.setAmount(p.getPledgeAmount()); // Prefer the amount from the pledge if available.
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
        verifyUserAccess("donation.create");
        try {
            return new DonationDB().createDonation(donation);
        } catch(Throwable t) {
            LOG.error("Failed to create donation for family: " + donation.getFamilyId(), t);
            throw t;
        }
    }

    @POST @Path("/batch") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public List<Donation> createDonations(List<Donation> donations) {
        verifyUserAccess("donation.create");
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
        verifyUserAccess("donation.update");
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
        verifyUserAccess("donation.delete");
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
