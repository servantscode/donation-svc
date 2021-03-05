package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.donation.*;
import org.servantscode.donation.db.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.logging.log4j.core.util.Assert.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;

@Path("/donation")
public class DonationSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(DonationSvc.class);

    private final DonationDB donationDB;
    private final PledgeDB pledgeDB;
    private final FamilyGivingInfoDB familyDB;
    private final FamilyContributionDB contributionDb;
    private final FundDB fundDB;

    private static final List<String> EXPORTABLE_FIELDS = asList("id", "family_id", "family_name", "fund_name", "amount", "type", "date", "check_number", "transaction_id");
    private static final List<String> CONTRIBUTION_FIELDS = asList("id", "surname", "head_name", "spouse_name", "addr_street1", "addr_city", "addr_state", "addr_zip", "donation_count", "total_amount");

    public DonationSvc() {
        this.donationDB = new DonationDB();
        this.pledgeDB = new PledgeDB();
        this.familyDB = new FamilyGivingInfoDB();
        this.fundDB = new FundDB();
        this.contributionDb = new FamilyContributionDB();
    }

    @GET @Path("/family") @Produces(APPLICATION_JSON)
    public PaginatedResponse<FamilyContributions> getFamilyContributions(@QueryParam("dateStart") String startDateStr,
                                                                         @QueryParam("dateEnd") String endDateStr,
                                                                         @QueryParam("start") @DefaultValue("0") int start,
                                                                         @QueryParam("count") @DefaultValue("10") int count,
                                                                         @QueryParam("sort") @DefaultValue("surname") String sortField,
                                                                         @QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("donation.list");

        LocalDate startDate;
        LocalDate endDate;
        try {
            //Default to Year to Date
            startDate = isSet(startDateStr) ? LocalDate.parse(startDateStr): LocalDate.now().withDayOfYear(1);
            endDate = isSet(endDateStr) ? LocalDate.parse(endDateStr): LocalDate.now();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Bad date format.");
        }

        try {
            int totalDonations = contributionDb.getFamilyTotalDonationCount(startDate, endDate, search);

            List<FamilyContributions> donations = contributionDb.getFamilyTotalDonations(startDate, endDate, start, count, sortField, search);
            return new PaginatedResponse<>(start, donations.size(), totalDonations, donations);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family total donations.", t);
            throw t;
        }
    }

    @GET @Path("/family/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getContributionReport(@QueryParam("dateStart") String startDateStr,
                                          @QueryParam("dateEnd") String endDateStr,
                                          @QueryParam("search") @DefaultValue("") String search) {
        verifyUserAccess("donation.export");

        LocalDate startDate;
        LocalDate endDate;
        try {
            //Default to Year to Date
            startDate = isSet(startDateStr) ? LocalDate.parse(startDateStr): LocalDate.now().withDayOfYear(1);
            endDate = isSet(endDateStr) ? LocalDate.parse(endDateStr): LocalDate.now();
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Bad date format.");
        }

        try {
            LOG.debug(String.format("Retrieving contribution report(%s)", search));

            return Response.ok(contributionDb.getReportReader(startDate, endDate, search, CONTRIBUTION_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving contribution report failed:", t);
            throw t;
        }
    }

    @GET @Path("/family/{familyId}") @Produces(APPLICATION_JSON)
    public PaginatedResponse<Donation> getFamilyDonations(@PathParam("familyId") int familyId,
                                                    @QueryParam("start") @DefaultValue("0") int start,
                                                    @QueryParam("count") @DefaultValue("10") int count,
                                                    @QueryParam("sort") @DefaultValue("date DESC, recorded_time DESC") String sortField,
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

    @GET @Path("/total")
    public float getDonationTotal(@QueryParam("search") @DefaultValue("") String search) {
        verifyUserAccess("donation.list");
        try {
            return donationDB.getDonationTotal(search);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve total donations for search: " + search, t);
            throw t;
        }
    }

    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getDonationReport(@QueryParam("search") @DefaultValue("") String search) {
        verifyUserAccess("donation.export");

        try {
            LOG.debug(String.format("Retrieving donation report(%s)", search));

            return Response.ok(donationDB.getReportReader(search, EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving donation report failed:", t);
            throw t;
        }
    }


    @GET @Produces(APPLICATION_JSON)
    public PaginatedResponse<Donation> getDonations(@QueryParam("start") @DefaultValue("0") int start,
                                                    @QueryParam("count") @DefaultValue("10") int count,
                                                    @QueryParam("sort") @DefaultValue("date DESC, recorded_time DESC") String sortField,
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

        donation.setRecordedTime(ZonedDateTime.now());
        donation.setRecorderId(getUserId());
        try {
            if(donation.getPledgeId() <= 0)
                linkPledge(donation);

            return new DonationDB().createDonation(donation);
        } catch(Throwable t) {
            LOG.error("Failed to create donation for family: " + donation.getFamilyId(), t);
            throw t;
        }
    }

    @POST @Path("/batch") @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public List<Donation> createDonations(@QueryParam("skipDuplicates") @DefaultValue("false") boolean skipDuplicates,
                                          List<Donation> donations) {
        verifyUserAccess("donation.create");


        LOG.info(String.format("Recording %d donations.", donations.size()) + (skipDuplicates? " Skipping duplicate entries.": ""));
        try {
            List<Donation> createdDonations = new ArrayList<>(donations.size());
            DonationDB db = new DonationDB();
            for(Donation donation: donations) {
                donation.setRecordedTime(ZonedDateTime.now());
                donation.setRecorderId(getUserId());
                if(donation.getPledgeId() <= 0)
                    linkPledge(donation);


                if(skipDuplicates)
                    createdDonations.add(db.createDonationIfUnique(donation));
                else
                    createdDonations.add(db.createDonation(donation));
            }
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
            if(donation.getPledgeId() <= 0)
                linkPledge(donation);

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

    // ----- Private ----
    private void linkPledge(Donation donation) {
        Pledge linkedPledge = pledgeDB.getRelaventPledge(donation.getFamilyId(), donation.getFundId(), donation.getDonationDate());
        if (linkedPledge != null)
            donation.setPledgeId(linkedPledge.getId());
    }

}
