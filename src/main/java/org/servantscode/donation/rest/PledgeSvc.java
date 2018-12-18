package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omg.CosNaming.NamingContextPackage.NotFound;
import org.servantscode.donation.Pledge;
import org.servantscode.donation.db.PledgeDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/pledge")
public class PledgeSvc {
    private static final Logger LOG = LogManager.getLogger(PledgeSvc.class);

    @GET @Path("/family/{familyId}") @Produces(MediaType.APPLICATION_JSON)
    public Pledge getFamilyPledges(@PathParam("familyId") int familyId) {
        try {
            return new PledgeDB().getActivePledge(familyId);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family pledge: " + familyId, t);
            throw t;
        }
    }

    @GET @Path("/family/{familyId}/history") @Produces(MediaType.APPLICATION_JSON)
    public List<Pledge> getFamilyPledgeHistory(@PathParam("familyId") int familyId) {
        try {
            return new PledgeDB().getFamilyPledges(familyId);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve family pledge: " + familyId, t);
            throw t;
        }
    }

    @POST @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Pledge createPledge(Pledge pledge) {
        try {
            return new PledgeDB().createPledge(pledge);
        } catch(Throwable t) {
            LOG.error("Failed to create family pledge: " + pledge.getFamilyId(), t);
            throw t;
        }
    }

    @PUT @Path("/{pledgeId}") @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Pledge updatePledge(@PathParam("pledgeId") int pledgeId,
                                  Pledge pledge) {
        try {
            if(pledge.getId() != pledgeId)
                throw new BadRequestException();

            if(!new PledgeDB().updatePledge(pledge))
                throw new NotFoundException();

            return pledge;
        } catch(Throwable t) {
            LOG.error("Failed to update pledge: " + pledgeId, t);
            throw t;
        }
    }

    @DELETE @Path("/{pledgeId}") @Produces(MediaType.APPLICATION_JSON)
    public void deletePledge(@PathParam("pledgeId") int pledgeId) {
        try {
            if(!new PledgeDB().deletePledge(pledgeId))
                throw new NotFoundException();
        } catch(Throwable t) {
            LOG.error("Failed to delete pledge: " + pledgeId, t);
            throw t;
        }
    }
}
