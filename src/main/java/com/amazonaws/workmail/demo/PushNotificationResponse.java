package com.amazonaws.workmail.demo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import microsoft.exchange.webservices.data.core.EwsServiceXmlReader;
import microsoft.exchange.webservices.data.core.XmlElementNames;
import microsoft.exchange.webservices.data.core.enumeration.misc.XmlNamespace;
import microsoft.exchange.webservices.data.core.response.GetEventsResponse;
import microsoft.exchange.webservices.data.notification.GetEventsResults;
import microsoft.exchange.webservices.data.security.XmlNodeType;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

/**
 * Data class holding the deserialized PushNotificationResponse
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public final class PushNotificationResponse {

    private String version;

    private String watermark;

    private String subscriptionId;

    private String previousWatermark;

    // piggyback on GetEventsResponse. The internal structure is similar and we get the parsing for free
    private final GetEventsResponse response = new GetEventsResponse();

    public PushNotificationResponse(String inputXml) throws Exception {
        this.parseXml(inputXml);
    }

    private void parseXml(String inputXml) throws Exception {

        EwsServiceXmlReader reader = new EwsServiceXmlReader(
                new ByteArrayInputStream(inputXml.getBytes(StandardCharsets.UTF_8)), null);

        // read xml preamble
        reader.read(new XmlNodeType(XmlNodeType.START_DOCUMENT));
        reader.readStartElement(XmlNamespace.Soap, XmlElementNames.SOAPEnvelopeElementName);

        // read soap header
        reader.readStartElement(XmlNamespace.Soap, XmlElementNames.SOAPHeaderElementName);
        do {
            reader.read();

            if (reader.isStartElement(XmlNamespace.Types, XmlElementNames.ServerVersionInfo)) {
                this.version = reader.readAttributeValue("Version");
            }

            // Ignore anything else inside the SOAP header
        } while (!reader.isEndElement(XmlNamespace.Soap, XmlElementNames.SOAPHeaderElementName));

        //body
        reader.readStartElement(XmlNamespace.Soap, XmlElementNames.SOAPBodyElementName);

        // the operation
        reader.readStartElement(XmlNamespace.Messages, "SendNotification");

        // exactly one response message inside
        reader.readStartElement(XmlNamespace.Messages, XmlElementNames.ResponseMessages);

        response.loadFromXml(reader, "SendNotificationResponseMessage");

        // subscriptionId is a protected field so behold Java reflection magic
        this.subscriptionId = getProtectedField(response.getResults(), "subscriptionId");
        this.watermark = getProtectedField(response.getResults(), "newWatermark");
        this.previousWatermark = getProtectedField(response.getResults(), "previousWatermark");

    }

    private String getProtectedField(GetEventsResults getEventsResults, String field) throws Exception {
        Field subscriptionIdField = getEventsResults.getClass().getDeclaredField(field);
        subscriptionIdField.setAccessible(true);
        return (String) subscriptionIdField.get(response.getResults());
    }
}

