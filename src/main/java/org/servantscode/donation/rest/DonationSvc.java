package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.donation.*;
import org.servantscode.donation.db.DonationDB;
import org.servantscode.donation.db.FamilyGivingInfoDB;
import org.servantscode.donation.db.FundDB;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/donation")
public class DonationSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(DonationSvc.class);

    private final DonationDB donationDB;
    private final PledgeDB pledgeDB;
    private final FamilyGivingInfoDB familyDB;
    private final FundDB fundDB;

    private static final List<String> EXPORTABLE_FIELDS = Arrays.asList("id", "family_name", "fund_name", "amount", "type", "date", "check_number", "transaction_id");

    public DonationSvc() {
        this.donationDB = new DonationDB();
        this.pledgeDB = new PledgeDB();
        this.familyDB = new FamilyGivingInfoDB();
        this.fundDB = new FundDB();
    }

    @GET @Path("/family/{familyId}") @Produces(APPLICATION_JSON)
    public PaginatedResponse<Donation> getFamilyDonations(@PathParam("familyId") int familyId,
                                                    @QueryParam("start") @DefaultValue("0") int start,
                                                    @QueryParam("count") @DefaultValue("10") int count,
                                                    @QueryParam("sort_field") @DefaultValue("date DESC") String sortField,
                                                    @QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("donation.list");
        try {
            int totalDonations = donationDB.getFamilyDonationCount(familyId, search);
            float totalValue = donationDB.getFamilyDonationTotal(familyId, search);

            List<Donation> donations = donationDB.getFamilyDonations(familyId, start, count, sortField, search);
            return new PaginatedDonationResponse(start, donations.size(), totalDonations, donations, totalValue);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family donations: " + familyId, t);
            throw t;
        }
    }

    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getDonationReport(@QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("donation.export");

        try {
            LOG.trace(String.format("Retrieving donation report(%s)", search));

            return Response.ok(donationDB.getReportReader(search, EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving people report failed:", t);
            throw t;
        }
    }


    @GET @Produces(APPLICATION_JSON)
    public PaginatedResponse<Donation> getDonations(@QueryParam("start") @DefaultValue("0") int start,
                                                    @QueryParam("count") @DefaultValue("10") int count,
                                                    @QueryParam("sort_field") @DefaultValue("date DESC") String sortField,
                                                    @QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("donation.list");
        try {
            int totalDonations = donationDB.getDonationCount(search);
            float totalValue = donationDB.getDonationTotal(search);

            List<Donation> donations = donationDB.getDonations(start, count, sortField, search);
            return new PaginatedDonationResponse(start, donations.size(), totalDonations, donations, totalValue);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve donations.", t);
            throw t;
        }
    }

    @GET @Path("/predict") @Produces(APPLICATION_JSON)
    public DonationPrediction getDonationPrediction(@QueryParam("familyId") int familyId,
                                                    @QueryParam("envelopeNumber") int envelopeNumber,
                                                    @QueryParam("fundId") int fundId) {
        verifyUserAccess("donation.create");
        if(familyId <= 0 && envelopeNumber <= 0)
            throw new BadRequestException();

        try {
            FamilyGivingInfo info = familyId > 0 ?
                    familyDB.getFamilyPledgeById(familyId):
                    familyDB.getFamilyPledgeByEnvelope(envelopeNumber);

            if(info == null) {
                LOG.info("No family found for prediction");
                throw new NotFoundException("No family specified for prediction");
            }

            Pledge p = pledgeDB.getActivePledge(info.getId(), fundId);
            Donation d = donationDB.getLastDonation(info.getId(), fundId);

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

        if(donation.getFundId() <= 0 || fundDB.getFund(donation.getFundId()) == null)
            throw new BadRequestException();

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

    @PUT @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Donation updateDonation(Donation donation) {
        verifyUserAccess("donation.update");

        if(donation.getFundId() <= 0 || fundDB.getFund(donation.getFundId()) == null)
            throw new BadRequestException();

        try {
            if(!new DonationDB().updateDonation(donation))
                throw new NotFoundException();

            return donation;
        } catch(Throwable t) {
            LOG.error("Failed to update donation: " + donation.getId(), t);
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
