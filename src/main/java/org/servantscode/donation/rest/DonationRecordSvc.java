package org.servantscode.donation.rest;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Donation;
import org.servantscode.donation.db.DonationDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Path("/donation/report")
public class DonationRecordSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(DonationRecordSvc.class);

    private DonationDB db;

    public DonationRecordSvc() {
        db = new DonationDB();
    }

    @GET
    @Path("/{familyId}/annual/{year}") @Produces("application/pdf")
    public Response generateAnnualReport(@PathParam("familyId") int familyId,
                                         @PathParam("year") int year) {

        verifyUserAccess("donations.read");
        try {
            List<Donation> d = db.getAnnualDonations(familyId, year);
            String orgName = OrganizationContext.getOrganization().getName();

            StreamingOutput stream = output -> {
                try {
                    Document document = new Document();
                    PdfWriter.getInstance(document, output);

                    document.open();
                    Font font = FontFactory.getFont(FontFactory.TIMES_ROMAN, 14, BaseColor.BLACK);
                    Chunk chunk = new Chunk(String.format("Thank you for your contribution to %s!!", orgName), font);
                    document.add(chunk);

                    document.close();
                } catch (DocumentException e) {
                    LOG.error("Failed to create pdf document for annual report", e);
                    throw new WebApplicationException("Failed to create annual report pdf", e);
                }
            };

            return Response.ok(stream).build();
        } catch(Throwable t) {
            LOG.error("Retrieving annual report failed:", t);
            throw t;
        }
    }
}
