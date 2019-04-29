package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.EnumUtils;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.donation.Fund;
import org.servantscode.donation.db.FundDB;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.commons.StringUtils.isEmpty;

@Path("/fund")
public class FundSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(FundSvc.class);

    private final FundDB db;

    public FundSvc() {
        this.db = new FundDB();
    }

    @GET @Produces(APPLICATION_JSON)
    public PaginatedResponse<Fund> getFunds(@QueryParam("start") @DefaultValue("0") int start,
                                            @QueryParam("count") @DefaultValue("10") int count,
                                            @QueryParam("sort_field") @DefaultValue("name") String sortField,
                                            @QueryParam("partial_name") @DefaultValue("") String search) {

        verifyUserAccess("fund.list");
        try {
            int totalFunds = db.getFundCount(search);

            List<Fund> funds = db.getFunds(start, count, sortField, search);
            return new PaginatedResponse<>(start, funds.size(), totalFunds, funds);
        } catch(Throwable t) {
            LOG.error("Failed to retrieve funds.", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(APPLICATION_JSON)
    public Fund getFund(@PathParam("id") int id) {
        verifyUserAccess("fund.id");
        if(id <= 0)
            throw new BadRequestException();

        try {
            Fund fund = db.getFund(id);
            if(fund == null)
                throw new NotFoundException();

            return fund;
        } catch(Throwable t) {
            LOG.error("Failed to retrieve fund with id: " + id, t);
            throw t;
        }
    }

    @POST @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Fund createFund(Fund fund) {
        verifyUserAccess("fund.create");
        if(isEmpty(fund.getName()))
            throw new BadRequestException();

        try {
            return db.createFund(fund);
        } catch(Throwable t) {
            LOG.error("Failed to create fund: " + fund.getName(), t);
            throw t;
        }
    }

    @PUT @Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)
    public Fund updateFund(Fund fund) {
        verifyUserAccess("fund.update");
        if(fund.getId() == 0)
            throw new BadRequestException();

        if(db.getFund(fund.getId()) == null)
            throw new NotFoundException();

        try {
            if(!db.updateFund(fund))
                throw new NotFoundException();

            return fund;
        } catch(Throwable t) {
            LOG.error("Failed to update fund: " + fund.getId(), t);
            throw t;
        }
    }

    @DELETE @Path("/{fundId}") @Produces(APPLICATION_JSON)
    public void deleteFund(@PathParam("fundId") int fundId) {
        verifyUserAccess("fund.delete");
        try {
            if(!db.deleteFund(fundId))
                throw new NotFoundException();
        } catch(Throwable t) {
            LOG.error("Failed to delete fund: " + fundId, t);
            throw t;
        }
    }
}
