import { HealthCheck } from "@aws-cdk/aws-autoscaling";
import { ApplicationProtocol, ListenerAction, TargetType } from "@aws-cdk/aws-elasticloadbalancingv2";
import type { App } from "@aws-cdk/core";
import { Duration, Tags } from "@aws-cdk/core";
import { GuAutoScalingGroup } from "@guardian/cdk/lib/constructs/autoscaling";
import {
  GuArnParameter,
  GuStringParameter,
} from "@guardian/cdk/lib/constructs/core";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core/stack";
import { GuStack } from "@guardian/cdk/lib/constructs/core/stack";
import { GuPublicInternetAccessSecurityGroup, GuVpc, SubnetType } from "@guardian/cdk/lib/constructs/ec2";
import {
  GuApplicationListener,
  GuApplicationLoadBalancer,
  GuApplicationTargetGroup,
} from "@guardian/cdk/lib/constructs/loadbalancing";
import { GuGetS3ObjectPolicy, GuInstanceRole } from "@guardian/cdk/lib/constructs/iam";

export class RuleManager extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const parameters = {
      TLSCert: new GuArnParameter(this, "TLSCert", {
        description: "ARN of a TLS certificate to install on the load balancer",
      }),
      ClusterName: new GuStringParameter(this, "ClusterName", {
        description: "The value of the ElasticSearchCluster tag that this instance should join",
        default: "elk",
      })
    };

    this.addTag("ElasticSearchCluster", parameters.ClusterName.valueAsString);

    const vpc = GuVpc.fromIdParameter(this, "vpc");

    const pandaAuthPolicy = new GuGetS3ObjectPolicy(this, "PandaAuthPolicy", { bucketName: "pan-domain-auth-settings" });

    const ruleManagerRole = new GuInstanceRole(this, "RuleManagerInstanceRole", {
      additionalPolicies: [pandaAuthPolicy]
    });

    const targetGroup = new GuApplicationTargetGroup(this, "PublicTargetGroup", {
      vpc: vpc,
      port: 9000,
      protocol: ApplicationProtocol.HTTP,
      targetType: TargetType.INSTANCE,
      healthCheck: {
        interval: Duration.minutes(1),
        timeout: Duration.seconds(3),
      },
      deregistrationDelay: Duration.seconds(30),
    });

    const loadBalancerSecurityGroup = new GuPublicInternetAccessSecurityGroup(this, "LoadBalancerSecurityGroup", {
      vpc,
      allowAllOutbound: false,
    });

    const privateSubnets = GuVpc.subnetsfromParameter(this, { type: SubnetType.PRIVATE });
    const publicSubnets = GuVpc.subnetsfromParameter(this, { type: SubnetType.PUBLIC });

    const loadBalancer = new GuApplicationLoadBalancer(this, "PublicLoadBalancer", {
      vpc,
      internetFacing: true,
      vpcSubnets: { subnets: publicSubnets },
      securityGroup: loadBalancerSecurityGroup,
    });

    new GuApplicationListener(this, "PublicListener", {
      loadBalancer,
      certificates: [{ certificateArn: parameters.TLSCert.valueAsString }],
      defaultAction: ListenerAction.forward([targetGroup]),
      open: false,
    });

    const userData = `#!/bin/bash -ev
mkdir /etc/gu

cat > /etc/gu/typerighter-rule-manager.conf <<-'EOF'
    include "application"
EOF

aws --quiet --region ${this.region} s3 cp s3://composer-dist/${this.stack}/${this.stage}/typerighter-rule-manager/typerighter-rule-manager.deb /tmp/package.deb
dpkg -i /tmp/package.deb`;

    new GuAutoScalingGroup(this, "AutoscalingGroup", {
      vpc,
      vpcSubnets: { subnets: privateSubnets },
      role: ruleManagerRole,
      userData: userData,
      minCapacity: 1,
      maxCapacity: 3,
      healthCheck: HealthCheck.elb({
        grace: Duration.minutes(5),
      }),
      targetGroup,
      associatePublicIpAddress: false,
    });
  }
}
