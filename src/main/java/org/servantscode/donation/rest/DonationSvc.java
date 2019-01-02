package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.donation.Donation;
import org.servantscode.donation.Pledge;
import org.servantscode.donation.db.DonationDB;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
