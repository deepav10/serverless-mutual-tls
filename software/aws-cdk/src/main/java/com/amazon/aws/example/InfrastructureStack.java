// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazon.aws.example;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.EndpointType;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.*;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.PrivateHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.LoadBalancerTarget;
import software.constructs.Construct;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod.GET;
import static software.amazon.awscdk.services.lambda.Architecture.ARM_64;
import static software.amazon.awscdk.services.lambda.Architecture.X86_64;

public class InfrastructureStack extends Stack {
  public InfrastructureStack(final Construct scope, final String id) {
    this(scope, id, null);
  }

  
  private static final String BACKEND_SERVICE_1_HOST_NAME = "rz59lct053.execute-api.us-east-1.amazonaws.com/Prod/baggage/3.0/claims/ani/5613195444";

  public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
    super(scope, id, props);

    // Vpc vpc = new Vpc(this, "LambdaMutualTLSVpc", VpcProps.builder()
    //     // Create a new VPC and subnets, spanning 2 availability zones
    //     .maxAzs(1)
    //     .build());

        // NetworkLoadBalancer nlb = NetworkLoadBalancer.Builder.create(this, "BackendServiceNLB")
        // .loadBalancerName("BackendServiceNLB")
        // .vpc(vpc)
        // .crossZoneEnabled(true)
        // .internetFacing(false)
        // .build();

          // backend service 1 https listener port
    // NetworkListener httpsListenerPort443 = nlb.addListener("httpsListenerPort443", BaseNetworkListenerProps.builder()
    // .port(443)
    // .build());


    Function lambdaNoMTLSFunction = new Function(this, "IIDSLambdaNoMTLSFunction", FunctionProps.builder()
      .functionName("lambda-no-mtls")
      .handler("com.amazon.aws.example.AppClient::handleRequest")
      .runtime(Runtime.JAVA_11)
      .architecture(ARM_64)
      .code(Code.fromAsset("../0-lambda-no-mtls/target/lambda-no-mtls.jar"))
      .memorySize(1024)
      .environment(Map.of("BACKEND_SERVICE_1_HOST_NAME", BACKEND_SERVICE_1_HOST_NAME))
      .timeout(Duration.seconds(10))
      .logRetention(RetentionDays.ONE_WEEK)
      .build());

    Function lambdaOnlyFunction = new Function(this, "IIDSLambdaMTLSFunction", FunctionProps.builder()
      .functionName("lambda-mtls")
      .handler("com.amazon.aws.example.AppClient::handleRequest")
      .runtime(Runtime.JAVA_11)
      .architecture(ARM_64)
      .code(Code.fromAsset("../1-lambda-mtls/target/lambda-mtls.jar"))
      .memorySize(1024)
      .environment(Map.of(
        "BACKEND_SERVICE_1_HOST_NAME", BACKEND_SERVICE_1_HOST_NAME,
        "JAVA_TOOL_OPTIONS", "-Djavax.net.ssl.keyStore=./qa-cct-webservice-dev.jks -Djavax.net.ssl.keyStorePassword=ivrqa2020 -Djavax.net.debug=all"
      ))
      .timeout(Duration.seconds(10))
      .logRetention(RetentionDays.ONE_WEEK) 
      .build());

    RestApi restApi = new RestApi(this, "IIDSLambdaMTLSApi", RestApiProps.builder()
      .restApiName("IIDSLambdaMTLSApi")
      .endpointTypes(List.of(EndpointType.REGIONAL))
      .build());

    restApi.getRoot()
      .addResource("lambda-no-mtls")
      .addMethod(GET.toString(), LambdaIntegration.Builder.create(lambdaNoMTLSFunction).build());

    restApi.getRoot()
      .addResource("lambda-mtls")
      .addMethod(GET.toString(), LambdaIntegration.Builder.create(lambdaOnlyFunction).build());
    
    new CfnOutput(this, "api-endpoint", CfnOutputProps.builder()
      .value(restApi.getUrl())
      .build());
  }
}
