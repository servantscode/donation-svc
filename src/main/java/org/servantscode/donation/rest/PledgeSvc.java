package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.donation.Donation;
import org.servantscode.donation.Pledge;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/pledge")
public class PledgeSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(PledgeSvc.class);

    private final PledgeDB db;

    public PledgeSvc() {
        this.db = new PledgeDB();
    }

    @GET  @Produces(APPLICATION_JSON)
    public PaginatedResponse<Pledge> getActivePledges(@QueryParam("start") @DefaultValue("0") int start,
                                                      @QueryParam("count") @DefaultValue("10") int count,
                                                      @QueryParam("sort_field") @DefaultValue("pledge_date") String sortField,
                                                      @QueryParam("partial_name") @DefaultValue("") String search,
                                                      @QueryParam("fundId") @DefaultValue("0") int fundId) {

        verifyUserAccess("pledge.list");
        try {
            int totalPledges = db.getActivePledgeCount(search, fundId);
            List<Pledge> pledges = db.getActivePledges(start, count, sortField, search, fundId);

            return new PaginatedResponse<>(start, pledges.size(), totalPledges, pledges);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve active pledges.", t);
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
}
