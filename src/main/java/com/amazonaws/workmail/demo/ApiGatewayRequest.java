package com.amazonaws.workmail.demo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class ApiGatewayRequest {
    private String resource;
    private String path;
    private String httpMethod;
    private Map<String, String> queryStringParameters;
    private Map<String, String> pathParameters;
    private Map<String, String> stageVariables;
    private Map<String, String> headers;
    private RequestContext requestContext;
    private String body;
    private boolean isBase64Encoded;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RequestContext {
        private String accountId;
        private String resourceId;
        private String stage;
        private String requestId;
        private Identity identity;
        private String resourcePath;
        private String httpMethod;
        private String apiId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Identity {
        private String cognitoIdentityPoolId;
        private String accountId;
        private String cognitoIdentityId;
        private String caller;
        private String apiKey;
        private String sourceIp;
        private String cognitoAuthenticationType;
        private String cognitoAuthenticationProvider;
        private String userArn;
        private String userAgent;
        private String user;
    }
}