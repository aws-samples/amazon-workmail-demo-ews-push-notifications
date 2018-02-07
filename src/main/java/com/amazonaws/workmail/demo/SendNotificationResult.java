package com.amazonaws.workmail.demo;

public class SendNotificationResult {
    private static final String SEND_NOTIFICATION_RESULT_TEMPLATE = "<?xml version=\"1.0\"?>%n" +
            "  <s:Envelope xmlns:s= \"http://schemas.xmlsoap.org/soap/envelope/\">%n" +
            "    <s:Body>%n" +
            "      <SendNotificationResult xmlns=\"http://schemas.microsoft" +
            ".com/exchange/services/2006/messages\">%n" +
            "        <SubscriptionStatus>%s</SubscriptionStatus>%n" +
            "      </SendNotificationResult>%n" +
            "    </s:Body>%n" +
            "  </s:Envelope>";

    public static final String OK = String.format(SEND_NOTIFICATION_RESULT_TEMPLATE, "OK");

    public static final String UNSUBSCRIBE = String.format(SEND_NOTIFICATION_RESULT_TEMPLATE, "Unsubscribe");

}
