package com.amazonaws.workmail.demo;

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.credential.WebCredentials;

import java.net.URI;

public class EwsApi {
    public static ExchangeService get(String email) {

        ExchangeService exchangeService = new ExchangeService(ExchangeVersion.Exchange2007_SP1);


        AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.defaultClient();
        String password = ssm.getParameter(
                new GetParameterRequest()
                        .withName(getUserFromEmail(email))
                        .withWithDecryption(true)).getParameter()
                .getValue();

        exchangeService.setUrl(URI.create(System.getenv("WORKMAIL_EWS_URL")));

        exchangeService.setCredentials(new WebCredentials(email, password));
        return exchangeService;
    }

    private static String getUserFromEmail(String email) {
        String[] emailParts = email.split("@");

        if (emailParts.length != 2) {
            throw new RuntimeException(email + " is not a valid Email address");
        }
        return emailParts[0];
    }


}
