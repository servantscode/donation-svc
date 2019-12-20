package org.servantscode.donation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.glassfish.jersey.client.ClientConfig;
import org.servantscode.commons.ConfigUtils;
import org.servantscode.commons.EnvProperty;
import org.servantscode.commons.security.OrganizationContext;
import org.servantscode.commons.security.SystemJWTGenerator;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

public class EmailDonationLetterClient {
    private static final Logger LOG = LogManager.getLogger(EmailDonationLetterClient.class);

    final private Client client;
    final private WebTarget webTarget;
    final private String token;

    private static final String SERVICE_URL = "http://email-svc:8080/rest/email";

    public EmailDonationLetterClient() {
        client = ClientBuilder.newClient(new ClientConfig().register(this.getClass()));
        webTarget = client.target(SERVICE_URL);
        this.token = SystemJWTGenerator.generateToken();
    }

    public void sendDonationReportEmail(String to, String base64File, String fileName) {
        String from = ConfigUtils.getConfiguration("mail.user.account");
        Map<String, Object> attachment = new HashMap<>(4);
        attachment.put("fileName", fileName);
        attachment.put("mimeType", "application/pdf");
        attachment.put("data", base64File);

        sendEmail(from, to, "Annual Donation Letter",
                "Thank you for your contributions to the parish. Please find your annual contribution letter attached.",
                attachment);
    }

    public void sendEmail(String from, String to, String subject, String message, Map<String, Object>... attachments) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("from", from);
        payload.put("to", asList(to));
        payload.put("subject", subject);
        payload.put("message", message);
        payload.put("attachments", attachments);
        post(payload);
    }

    public Response post(Map<String, Object> data) {
        translateDates(data);
        return buildInvocation()
                .post(Entity.entity(data, MediaType.APPLICATION_JSON));
    }

    // ----- Private -----
    private void translateDates(Map<String, Object> data) {
        data.entrySet().forEach( (entry) -> {
            Object obj = entry.getValue();
            if(obj instanceof ZonedDateTime) {
                entry.setValue(((ZonedDateTime)obj).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } else if(obj instanceof List) {
                List list = (List)obj;
                if(!list.isEmpty() && list.get(0) instanceof Map)
                    list.forEach((item) -> translateDates((Map<String, Object>)item));
            } else if(obj instanceof Map) {
                translateDates((Map<String, Object>)obj);
            }
        });
    }

    private Invocation.Builder buildInvocation(Map<String, Object>... optionalParams) {
        WebTarget target = webTarget;

        if(optionalParams.length > 0) {
            Map<String, Object> params = optionalParams[0];
            for(Map.Entry<String, Object> entry: params.entrySet())
                target = target.property(entry.getKey(), entry.getValue());
        }

        return target.request(MediaType.APPLICATION_JSON)
                .header("x-sc-org", OrganizationContext.getOrganization().getHostName())
                .header("x-sc-transaction-id", ThreadContext.get("transaction.id"))
                .header("Authorization", "Bearer " + token);
    }
}
