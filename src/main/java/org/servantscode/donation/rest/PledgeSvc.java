package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.donation.Pledge;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static java.time.temporal.ChronoUnit.DAYS;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/pledge")
public class PledgeSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(PledgeSvc.class);

    private static final List<String> EXPORTABLE_FIELDS = Arrays.asList("id", "family_name", "fund_name", "pledge_type", "pledge_date",
            "pledge_start", "pledge_end", "frequency", "pledge_increment", "total_pledge", "total_donations", "collected_pct");

    private final PledgeDB db;

    public PledgeSvc() {
        this.db = new PledgeDB();
    }

    @GET  @Produces(APPLICATION_JSON)
    public PaginatedResponse<Pledge> getActivePledges(@QueryParam("start") @DefaultValue("0") int start,
                                                      @QueryParam("count") @DefaultValue("10") int count,
                                                      @QueryParam("sort_field") @DefaultValue("family_name") String sortField,
                                                      @QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("pledge.list");
        try {
            int totalPledges = db.getActivePledgeCount(search);
            List<Pledge> pledges = db.getActivePledges(start, count, sortField, search);

            return new PaginatedResponse<>(start, pledges.size(), totalPledges, pledges);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve active pledges.", t);
            throw t;
        }
    }


    @GET @Path("/report") @Produces(MediaType.TEXT_PLAIN)
    public Response getPledgeReport(@QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("pledge.export");

        try {
            LOG.trace(String.format("Retrieving pledge report(%s)", search));

            return Response.ok(db.getReportReader(search, EXPORTABLE_FIELDS)).build();
        } catch (Throwable t) {
            LOG.error("Retrieving pledge report failed:", t);
            throw t;
        }
    }


    @GET @Path("/family/{familyId}") @Produces(APPLICATION_JSON)
    public List<Pledge> getFamilyPledges(@PathParam("familyId") int familyId) {
        verifyUserAccess("pledge.read");
        try {
            return db.getActiveFamilyPledges(familyId);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family pledge: " + familyId, t);
            throw t;
        }
    }

    @GET @Path("/family/{familyId}/history") @Produces(APPLICATION_JSON)
    public List<Pledge> getFamilyPledgeHistory(@PathParam("familyId") int familyId) {
        verifyUserAccess("pledge.read");
        try {
            return db.getFamilyPledges(familyId);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family pledge: " + familyId, t);
            throw t;
        }
    }

    @POST @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Pledge createPledge(Pledge pledge) {
        verifyUserAccess("pledge.create");
        try {
            return db.createPledge(pledge);
        } catch(Throwable t) {
            LOG.error("Failed to create family pledge: " + pledge.getFamilyId(), t);
            throw t;
        }
    }

    @PUT @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Pledge updatePledge(Pledge pledge) {
        verifyUserAccess("pledge.update");
        try {
            if(!db.updatePledge(pledge))
                throw new NotFoundException();

            return pledge;
        } catch(Throwable t) {
            LOG.error("Failed to update pledge: " + pledge.getId(), t);
            throw t;
        }
    }

    @DELETE @Path("/{pledgeId}") @Produces(APPLICATION_JSON)
    public void deletePledge(@PathParam("pledgeId") int pledgeId) {
        verifyUserAccess("pledge.delete");
        try {
            if(!db.deletePledge(pledgeId))
                throw new NotFoundException();
        } catch(Throwable t) {
            LOG.error("Failed to delete pledge: " + pledgeId, t);
            throw t;
        }
    }

    @GET @Path("/types") @Produces(APPLICATION_JSON)
    public List<String> getPledgeTypes() { return EnumUtils.listValues(Pledge.PledgeType.class); }

    @GET @Path("/freqs") @Produces(APPLICATION_JSON)
    public List<String> getPledgeFrequencies() {
        return EnumUtils.listValues(Pledge.PledgeFrequency.class);
    }

    @GET @Path("/statuses") @Produces(APPLICATION_JSON)
    public List<String> getPledgeStatuses() {
        return EnumUtils.listValues(Pledge.PledgeStatus.class);
    }
}
