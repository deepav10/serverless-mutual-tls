// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class AppClient implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  private static final String BACKEND_SERVICE_2_HOST_NAME = System.getenv("BACKEND_SERVICE_2_HOST_NAME");
  private static final HttpClient client = HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_2)
    .connectTimeout(Duration.ofSeconds(10))
    .build();

  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
    context.getLogger().log(input.toString());

    System.out.println("BACKEND_SERVICE_2_HOST_NAME: "+BACKEND_SERVICE_2_HOST_NAME);

    HttpRequest httpRequest = HttpRequest.newBuilder()
      .header("X-Client-ID", "DeepaTest")
      .header("X-Transaction-ID", "DeepaTestTrans")
      .header("callGuid", "DeepaTest123")
      .header("X-Interaction-ID", "TestInteractionId")
      .uri(URI.create(String.format("https://%s", BACKEND_SERVICE_2_HOST_NAME)))
      .timeout(Duration.ofSeconds(5))
      .GET()
      .build();

      System.out.println("reached here");

    try {
        System.out.println("reached httpResponse::"+httpRequest);
  
        HttpResponse<String> httpResponse = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      return new APIGatewayProxyResponseEvent()
        .withStatusCode(200)
        .withHeaders(Map.of("Content-Type", "application/json"))
        .withBody(httpResponse.body());
    } catch (Exception e) {
      context.getLogger().log(e.getMessage());
      return new APIGatewayProxyResponseEvent()
        .withStatusCode(500)
        .withHeaders(Map.of("Content-Type", "text/plain"))
        .withBody("error");
    }
  }
}
