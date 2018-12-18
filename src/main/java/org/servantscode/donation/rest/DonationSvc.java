package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.donation.Donation;
import org.servantscode.donation.Pledge;
import org.servantscode.donation.db.DonationDB;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/donation")
public class DonationSvc {
    private static final Logger LOG = LogManager.getLogger(DonationSvc.class);

    @GET @Path("/family/{familyId}") @Produces(MediaType.APPLICATION_JSON)
    public List<Donation> getDonations(@PathParam("familyId") int familyId) {
        try {
            return new DonationDB().getFamilyDonations(familyId);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family donations: " + familyId, t);
            throw t;
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Donation createDonation( Donation donation) {
        try {
            return new DonationDB().createDonation(donation);
        } catch(Throwable t) {
            LOG.error("Failed to create donation for family: " + donation.getFamilyId(), t);
            throw t;
        }
    }

    @PUT @Path("/{donationId}") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
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
}
