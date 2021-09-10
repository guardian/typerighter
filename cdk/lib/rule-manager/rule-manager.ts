import type { App } from "@aws-cdk/core";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core/stack";
import { GuStack } from "@guardian/cdk/lib/constructs/core/stack";
import { AccessScope, GuPlayApp } from "@guardian/cdk";
import { GuGetS3ObjectsPolicy } from "@guardian/cdk/lib/constructs/iam";
import { GuSecurityGroup } from "@guardian/cdk/lib/constructs/ec2";
import { Peer } from "@aws-cdk/aws-ec2";

export class RuleManager extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const appName = "typerighter-rule-manager";

    const app = new GuPlayApp(this, {
      app: appName,
      userData: `#!/bin/bash -ev
        aws --quiet --region ${this.region} s3 cp s3://composer-dist/${this.stack}/${this.stage}/typerighter-rule-manager/typerighter-rule-manager.deb /tmp/package.deb
        dpkg -i /tmp/package.deb`,
      access: {
        scope: AccessScope.PUBLIC,
      },
      certificateProps: {
        CODE: { domainName: "typerighter.code.dev-gutools.co.uk" },
        PROD: { domainName: "typerighter.gutools.co.uk" },
      },
      monitoringConfiguration: {
        noMonitoring: true,
      },
      roleConfiguration: {
        additionalPolicies: [
          new GuGetS3ObjectsPolicy(this, "PandaAuthPolicy", {
            bucketName: "pan-domain-auth-settings",
          }),
        ],
      },
    });

    const dbAccessSecurityGroup = new GuSecurityGroup(
      this,
      "ApplicationSecurityGroup",
      {
        app: appName,
        description: "Application security group",
        vpc: app.vpc,
        allowAllOutbound: false,
        egresses: [{
          range: Peer.anyIpv4(),
          port: 5432,
          description: "Allows application EC2 instance to access postgres DB"
        }]
      }
    );

    app.autoScalingGroup.addSecurityGroup(dbAccessSecurityGroup);
  }
}
