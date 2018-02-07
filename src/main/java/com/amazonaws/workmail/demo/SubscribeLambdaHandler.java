package com.amazonaws.workmail.demo;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import microsoft.exchange.webservices.data.core.enumeration.notification.EventType;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.notification.PushSubscription;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SubscribeLambdaHandler implements RequestHandler<Map<String,String>, String> {

    private static Logger LOG = LoggerFactory.getLogger(SubscribeLambdaHandler.class);


    /**
     * Subscribes the given email to push notifications
     * @param input
     * @param context
     * @return
     */
    @Override
    public String handleRequest(final Map<String, String> input, final Context context) {
        String pushNotificationUrl = System.getenv("PUSH_NOTIFICATION_URL");
        String email = input.get("email");

        List<FolderId> folderIds = Collections.singletonList(
                FolderId.getFolderIdFromWellKnownFolderName(WellKnownFolderName.Inbox));
        String output;
        try {
            URI uri = new URIBuilder(URI.create(pushNotificationUrl)).addParameter("email", email).build();

            PushSubscription pushSubscription = EwsApi.get(email)
                    .subscribeToPushNotifications(folderIds, uri, 1, "", EventType.Created);

            output = pushSubscription.getId();
        } catch (Exception e) {
            LOG.error("Exception while calling EWS", e);
            output = ExceptionUtils.getStackTrace(e);
        }

        return output;
    }
}
