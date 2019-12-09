package org.servantscode.donation.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.donation.Donation;
import org.servantscode.donation.PdfWriter;
import org.servantscode.donation.db.DonationDB;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup.SUB_TYPE_UNDERLINE;

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
//            Parish parish = null;
//            Family family = null;

            AtomicReference<Float> total = new AtomicReference<>((float) 0);
            d.stream().map(Donation::getAmount).forEach(a -> total.set(total.get() + a));

            StreamingOutput stream = output -> {
                try (PdfWriter writer = new PdfWriter()) {
                    writer.beginText();
                    writer.setFontSize(20);

                    writer.addCenteredLine(String.format("%s", orgName));
                    writer.addCenteredLine("727 S. Travis St.");
                    writer.addCenteredLine("Sherman, TX 75090");
                    writer.addCenteredLine( "903-893-5148");

                    writer.setFontSize(12);
                    writer.addLine("John Doe");
                    writer.addLine("101 Somewhere Dr.");
                    writer.addLine("Sherman, TX 75090");
                    writer.addBlankLine();
                    writer.addParagraph("Dear Mr. and Mrs. Doe,");
                    writer.addParagraph("It is time to send out financial statements so that you will have a record of your contributions for your income tax report. Your generosity over the past year has made it possible for us to continue the mission and ministry of Jesus. Were it not for your help this would be impossible. So, on behalf of St. Mary's Catholic Parish, thank you.");
                    writer.addParagraph("Our records show that you have contributed the following amount:");

                    writer.addParagraph(String.format("Total Contributions: $%.2f", total.get()));

                    writer.addParagraph("Thank you for your past support. Your continued contributions are greatly appreciated.");
                    writer.addParagraph("Sincerely,");
                    writer.addBlankSpace(2.0f);
                    writer.addLine("Fr. Martin Castenada");
                    writer.addLine("Pastoral Administrator");
                    writer.addBlankLine();

                    writer.addParagraph("P.S. St. Mary's Catholic Church has not provided, in whole or in part, any goods or services to the above named donor in exchange for this gift.");
                    writer.addParagraph("This statement is provided by St. Mary's Catholic Church in order to comply with the Internal Revenue Code. Retain this and your cancelled checks with your tax records.");
                    writer.endText();

                    if(d.stream().anyMatch(don -> don.getAmount() > 250)) {
                        List<Donation> bigDonations = d.stream().filter(don -> don.getAmount() >= 250).collect(Collectors.toList());
                        AtomicReference<Float> littleDonations = new AtomicReference<>(0.0f);
                        d.stream().filter(don -> don.getAmount() < 250)
                                .forEach(don -> littleDonations.updateAndGet(v -> v + don.getAmount()));

                        writer.newPage();
                        writer.beginText();
                        writer.addParagraph("Our records show that you have made the following tax deductible contributions:");

                        writer.startTable(new int[] {80, 200, 80, 60}, new boolean[] {true, true, false, true});
                        writer.addTableHeader("Date", "Description", "Amount", "Check");

                        int lineCount = 0;
                        for(Donation don: bigDonations) {
                            writer.addTableRow(don.getDonationDate().format(DateTimeFormatter.ISO_DATE),
                                               don.getFundName(),
                                               String.format("$%.2f", don.getAmount()),
                                               don.getCheckNumber() == 0? "": Long.toString(don.getCheckNumber()));

                            if(++lineCount > 35) {
                                writer.endText();
                                writer.newPage();
                                writer.beginText();
                                lineCount = 0;
                            }
                        }

                        writer.addBlankSpace(.5f);
                        writer.addTableRow(new int[] {40, 240, 80, 60},
                                           "", "Total of all other deductible contributions that were less than $250.00:",
                                           String.format("$%.2f", littleDonations.get()), "");

                        writer.addBlankSpace(.5f);
                        writer.addTableRow("", "Total:", String.format("$%.2f", total.get()), "");

                        writer.endText();
                    }

                    writer.writeToStream(output);
                } catch (Throwable t) {
                    LOG.error("Failed to create pdf document for annual report", t);
                    throw new WebApplicationException("Failed to create annual report pdf", t);
                }
            };

            return Response.ok(stream).build();
        } catch(Throwable t) {
            LOG.error("Retrieving annual report failed:", t);
            throw t;
        }
    }
}
