package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.client.ApiClientFactory;
import org.servantscode.client.FamilyServiceClient;
import org.servantscode.client.ParishServiceClient;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Donation;
import org.servantscode.donation.PdfWriter;
import org.servantscode.donation.db.DonationDB;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.servantscode.commons.StringUtils.isEmpty;
import static org.servantscode.commons.StringUtils.isSet;
import static org.servantscode.donation.PdfWriter.Alignment.*;
import static org.servantscode.donation.PdfWriter.TextDecoration.DOUBLE_OVERLINE;

@Path("/donation/report")
public class DonationRecordSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(DonationRecordSvc.class);

    private DonationDB db;

    public DonationRecordSvc() {
        db = new DonationDB();
    }

    @GET @Path("/{familyId}/annual/{year}") @Produces("application/pdf")
    public Response generateAnnualReport(@PathParam("familyId") int familyId,
                                         @PathParam("year") int year) {

        verifyUserAccess("donation.read");
        try {
            List<Donation> d = db.getAnnualDonations(familyId, year);

            ApiClientFactory.instance().authenticateAsSystem();
            Map<String, Object> family = new FamilyServiceClient().getFamily(familyId);
            Map<String, Object> parish = new ParishServiceClient().getParishForOrg(OrganizationContext.orgId());

            StreamingOutput stream = output -> {
                createDonationReport(d, family, parish, output);
            };

            return Response.ok(stream).build();

        } catch(Throwable t) {
            LOG.error("Retrieving annual report failed:", t);
            throw t;
        }
    }

//    @POST @Path("/{familyId}/annual/{year}/email")
//    public void emailAnnualReport(@PathParam("familyId") int familyId, @PathParam("year") int year) {
//
//        verifyUserAccess("donation.read");
//        verifyUserAccess("email.send");
//        try {
//            List<Donation> d = db.getAnnualDonations(familyId, year);
//
//            ApiClientFactory.instance().authenticateAsSystem();
//            Map<String, Object> family = new FamilyServiceClient().getFamily(familyId);
//            Map<String, Object> parish = new ParishServiceClient().getParishForOrg(OrganizationContext.orgId());
//
//            //TODO: Send email
//            createDonationReport(d, family, parish, output);
//        } catch(Throwable t) {
//            LOG.error("Retrieving annual report failed:", t);
//            throw t;
//        }
//    }

    private void createDonationReport(List<Donation> donations, Map<String, Object> family, Map<String, Object> parish, OutputStream output) throws IOException {
        try (PdfWriter writer = new PdfWriter()) {
            AtomicReference<Float> total = new AtomicReference<>((float) 0);
            donations.stream().map(Donation::getAmount).forEach(a -> total.set(total.get() + a));

            writer.beginText();
            writer.setFontSize(20);
            writer.setAlignment(CENTER);

            String parishName = (String) parish.get("name");
            writer.addLine(parishName);
            Map<String, Object> addr = (Map<String, Object>) parish.get("address");
            writer.addLine(addr.get("street1").toString());
            writer.addLine(String.format("%s, %s %s", addr.get("city"), addr.get("state"), addr.get("zip")));
            writer.addLine(parish.get("phoneNumber").toString());

            writer.setFontSize(12);
            writer.setAlignment(LEFT);
            writer.addLine(family.get("formalGreeting").toString());

            Map<String, Object> familyAddr = (Map<String, Object>) family.get("address");
            if(familyAddr != null || isEmpty((String)familyAddr.get("street1"))) {
                writer.addLine(familyAddr.get("street1").toString());
                writer.addLine(String.format("%s, %s %s", familyAddr.get("city"), familyAddr.get("state"), familyAddr.get("zip")));
            }

            String homePhone = (String) family.get("homePhone");
            if(isSet(homePhone))
                writer.addLine(homePhone);

            writer.addBlankLine();
            writer.addParagraph(String.format("%s,", family.get("formalGreeting")));
            writer.addParagraph(String.format("It is time to send out financial statements so that you will have a record of your contributions for your income tax report. Your generosity over the past year has made it possible for us to continue the mission and ministry of Jesus. Were it not for your help this would be impossible. So, on behalf of %s, thank you.", parishName));
            writer.addParagraph("Our records show that you have contributed the following amount:");

            writer.addParagraph(String.format("Total Contributions: $%.2f", total.get()));

            writer.addParagraph("Thank you for your past support. Your continued contributions are greatly appreciated.");
            writer.addParagraph("Sincerely,");
            writer.addBlankSpace(2.0f);
            Map<String, Object> pastor = (Map<String, Object>) parish.get("pastor");
            if(pastor != null && pastor.get("name") != null) {
                writer.addLine(pastor.get("name").toString());
                writer.addLine("Pastoral Administrator");
            }
            writer.addBlankLine();

            writer.addParagraph(String.format("P.S. %s has not provided, in whole or in part, any goods or services to the above named donor in exchange for this gift.", parishName));
            writer.addParagraph(String.format("This statement is provided by %s in order to comply with the Internal Revenue Code. Retain this and your cancelled checks with your tax records.", parishName));
            writer.endText();

            if(donations.stream().anyMatch(don -> don.getAmount() > 250)) {
                List<Donation> bigDonations = donations.stream().filter(don -> don.getAmount() >= 250).collect(Collectors.toList());
                AtomicReference<Float> littleDonations = new AtomicReference<>(0.0f);
                donations.stream().filter(don -> don.getAmount() < 250)
                        .forEach(don -> littleDonations.updateAndGet(v -> v + don.getAmount()));

                writer.newPage();
                writer.beginText();
                writer.addParagraph("Our records show that you have made the following tax deductible contributions:");

                writer.startTable(new int[] {80, 200, 80, 60}, new PdfWriter.Alignment[] {LEFT, LEFT, RIGHT, LEFT});
                writer.addTableHeader("Date", "Description", "Amount", "Check");

                for(Donation don: bigDonations) {
                    writer.addTableRow(don.getDonationDate().format(DateTimeFormatter.ISO_DATE),
                                       don.getFundName(),
                                       String.format("$%.2f", don.getAmount()),
                                       don.getCheckNumber() == 0? "": Long.toString(don.getCheckNumber()));
                }

                writer.addBlankSpace(.5f);
                writer.addTableRow(new int[] {40, 240, 80, 60},
                                   "", "Total of all other deductible contributions that were less than $250.00:",
                                   String.format("$%.2f", littleDonations.get()), "");

                writer.addBlankSpace(.5f);
                writer.addDecoration(DOUBLE_OVERLINE);
                writer.addTableRow("", "Total:", String.format("$%.2f", total.get()), "");
                writer.removeDecoration(DOUBLE_OVERLINE);

                writer.endText();
            }
            writer.writeToStream(output);
        } catch (Throwable t) {
            LOG.error("Failed to create pdf document for annual report", t);
            throw new WebApplicationException("Failed to create annual report pdf", t);
        }
    }
}
