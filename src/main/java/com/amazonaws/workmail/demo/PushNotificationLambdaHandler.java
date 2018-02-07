package com.amazonaws.workmail.demo;

import com.amazonaws.services.comprehend.AmazonComprehend;
import com.amazonaws.services.comprehend.AmazonComprehendClientBuilder;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageRequest;
import com.amazonaws.services.comprehend.model.DetectDominantLanguageResult;
import com.amazonaws.services.comprehend.model.DetectSentimentRequest;
import com.amazonaws.services.comprehend.model.DetectSentimentResult;
import com.amazonaws.services.comprehend.model.DominantLanguage;
import com.amazonaws.services.comprehend.model.SentimentScore;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.notification.EventType;
import microsoft.exchange.webservices.data.core.enumeration.property.BasePropertySet;
import microsoft.exchange.webservices.data.core.enumeration.property.BodyType;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.notification.ItemEvent;
import microsoft.exchange.webservices.data.notification.NotificationEvent;
import microsoft.exchange.webservices.data.property.complex.ItemId;
import microsoft.exchange.webservices.data.property.complex.MessageBody;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;


public class PushNotificationLambdaHandler implements RequestHandler<ApiGatewayRequest, ApiGatewayProxyResponse> {

    private static final String DEMO_SUBJECT = "demo";
    private ExchangeService ewsApi;

    /**
     * Lambda handler for Push Notifications
     * @param apiGatewayRequest
     * @param context
     * @return
     */
    @Override
    public ApiGatewayProxyResponse handleRequest(final ApiGatewayRequest apiGatewayRequest, final Context context) {

        Map<String, String> queryStringParameters = apiGatewayRequest.getQueryStringParameters();
        String email = queryStringParameters.get("email");

        String output;
        try {
            ewsApi = EwsApi.get(email);
            String body = apiGatewayRequest.getBody();

            output = handleBody(body);
        } catch (Exception e) {
            e.printStackTrace();
            output = SendNotificationResult.UNSUBSCRIBE;
        }

        return ApiGatewayProxyResponse.builder()
                .body(output)
                .statusCode(200)
                .build();
    }

    /**
     * Deserializes the body to a PushNotificationResponse and processes it further
     * @param body
     * @return
     * @throws Exception
     */
    private String handleBody(String body) throws Exception {
        final PushNotificationResponse pushNotificationResponse = new PushNotificationResponse(body);

        Collection<NotificationEvent> allEvents = pushNotificationResponse.getResponse().getResults()
                .getAllEvents();

        allEvents.stream().filter(p -> p.getEventType().equals(EventType.Created)).forEach(this::handleNotificationEvent);

        return SendNotificationResult.OK;
    }

    /**
     * Unpacks the create event, loads the item and if the subject matches a filter calls AWS Comprehend
     * to extract the language and the sentiment score.
     * The results are emailed back to the sender
     * @param notificationEvent
     */
    private void handleNotificationEvent(NotificationEvent notificationEvent) {
        ItemEvent itemEvent = (ItemEvent) notificationEvent;
        ItemId itemId = itemEvent.getItemId();
        PropertySet propertySet = new PropertySet(BasePropertySet.FirstClassProperties);
        propertySet.setRequestedBodyType(BodyType.Text);

        try {
            Item item = ewsApi.bindToItem(itemId, propertySet);
            EmailMessage receivedMessage = EmailMessage.bind(ewsApi, itemId);

            if (isInteresting(item.getSubject())) {
                MessageBody messageBody = MessageBody.getMessageBodyFromText(checkComprehend(item.getBody().toString
                        ()));
                receivedMessage.reply(messageBody, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isInteresting(String subject) {
        return subject.toLowerCase().contains(DEMO_SUBJECT);
    }


    /**
     * Performs language detection and sentiment analysis on the text.
     * @param text
     * @return
     */
    private String checkComprehend(String text) {
        AmazonComprehend amazonComprehend = AmazonComprehendClientBuilder.defaultClient();

        DetectDominantLanguageResult detectDominantLanguageResult =
                amazonComprehend.detectDominantLanguage(new DetectDominantLanguageRequest().withText(text));

        Optional<String> detectedLanguageOptional = detectDominantLanguageResult.getLanguages().stream()
                .max(Comparator.comparing(DominantLanguage::getScore))
                .map(DominantLanguage::getLanguageCode);

        if (detectedLanguageOptional.isPresent()) {
            String language = detectedLanguageOptional.get();

            if (language.equals("en") || language.equals("es")) {

                DetectSentimentResult detectSentimentResult = amazonComprehend.detectSentiment(new
                        DetectSentimentRequest()
                        .withText(text).withLanguageCode(language));

                String sentiment = detectSentimentResult.getSentiment();
                SentimentScore sentimentScore = detectSentimentResult.getSentimentScore();

                return String.format("Language %s Sentiment %s Score %s", language, sentiment, sentimentScore);
            } else {
                return String.format("Language %s not supported. Can only perform analysis on 'en' and 'es'",
                        language);
            }
        }

        return "Unknown language. Can not perform sentiment analysis.";
    }
}
