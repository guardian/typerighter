import {
  App,
  CfnMapping,
  Duration,
  Fn,
  RemovalPolicy,
  SecretValue,
  Token,
} from "@aws-cdk/core";
import {Certificate } from '@aws-cdk/aws-certificatemanager';

import type { GuStackProps } from "@guardian/cdk/lib/constructs/core/stack";
import { GuStack } from "@guardian/cdk/lib/constructs/core/stack";
import { AccessScope, GuPlayApp } from "@guardian/cdk";
import {
  GuGetS3ObjectsPolicy,
  GuPutCloudwatchMetricsPolicy,
} from "@guardian/cdk/lib/constructs/iam";
import { GuSecurityGroup, GuVpc } from "@guardian/cdk/lib/constructs/ec2";
import { Port, SubnetType } from "@aws-cdk/aws-ec2";
import { GuS3Bucket } from "@guardian/cdk/lib/constructs/s3";
import {
  AllowedMethods,
  Distribution,
  OriginProtocolPolicy,
} from "@aws-cdk/aws-cloudfront";
import { LoadBalancerV2Origin } from "@aws-cdk/aws-cloudfront-origins";
import {
  Alarm,
  ComparisonOperator,
  Metric,
  TreatMissingData,
} from "@aws-cdk/aws-cloudwatch";
import { GuDatabaseInstance } from "@guardian/cdk/lib/constructs/rds";
import {
  Credentials,
  DatabaseInstanceEngine,
  PostgresEngineVersion,
  StorageType,
  SubnetGroup,
} from "@aws-cdk/aws-rds";
import { GuArnParameter, GuParameter } from "@guardian/cdk/lib/constructs/core";

export class Typerighter extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const parameters = {
      MasterDBUsername: new GuParameter(this, "MasterDBUsername", {
        description: "Master DB username",
        default: "rule_manager",
        type: "String",
      }),
      CheckerCertificate: new GuArnParameter(this, "CheckerCloudfrontCertificate", {
        description: "The ARN of the certificate for the checker service Cloudfront distribution",
      }),
    };

    const pandaAuthPolicy = new GuGetS3ObjectsPolicy(this, "PandaAuthPolicy", {
      bucketName: "pan-domain-auth-settings",
    });

    const stageLookup = new CfnMapping(this, "LowercaseStageLookup", {
      mapping: {
        lowercase: {
          PROD: "prod",
          CODE: "code",
        },
      },
    });

    const typerighterBucketName = `typerighter-${stageLookup.findInMap(
      "lowercase",
      this.stage
    )}`;

    const dbPort = 5432;

    // Rule manager app

    const ruleManagerAppName = "typerighter-rule-manager";

    const ruleManagerApp = new GuPlayApp(this, {
      app: ruleManagerAppName,
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
        additionalPolicies: [pandaAuthPolicy],
      },
    });

    // Checker app

    const checkerAppName = "typerighter-checker";

    const checkerApp = new GuPlayApp(this, {
      app: checkerAppName,
      userData: `#!/bin/bash -ev
mkdir /etc/gu

cat > /etc/gu/tags << 'EOF'
Stage=${this.stage}
Stack=${this.stack}
App=typerighter-checker
EOF

cat > /etc/gu/typerighter-checker.conf << 'EOF'
include "application"
typerighter.ngramPath="/opt/ngram-data"
EOF

aws --quiet --region ${this.region} s3 cp s3://composer-dist/${this.stack}/${this.stage}/typerighter-checker/typerighter-checker.deb /tmp/package.deb
dpkg -i /tmp/package.deb`,
      access: {
        scope: AccessScope.PUBLIC,
      },
      certificateProps: {
        CODE: { domainName: "api.typerighter.code.dev-gutools.co.uk" },
        PROD: { domainName: "api.typerighter.gutools.co.uk" },
      },
      monitoringConfiguration: {
        noMonitoring: true,
      },
      roleConfiguration: {
        additionalPolicies: [
          pandaAuthPolicy,
          new GuGetS3ObjectsPolicy(this, "RuleBucketPolicy", {
            bucketName: typerighterBucketName,
          }),
          new GuPutCloudwatchMetricsPolicy(this),
        ],
      },
    });

    // @todo – add this when the old stack is gone.
    // const typerighterBucket = new GuS3Bucket(this, "rules-bucket", {
    //   bucketName: typerighterBucketName,
    // });

    const cloudfrontBucket = new GuS3Bucket(this, "cloudfront-bucket", {
      lifecycleRules: [
        {
          expiration: Duration.days(90),
        },
      ],
    });

    const checkerCertificate = Certificate.fromCertificateArn(this, 'CheckerCertificate', parameters.CheckerCertificate.valueAsString)

    const checkerCloudFrontDistro = new Distribution(this, "typerighter-cloudfront", {
      defaultBehavior: {
        origin: new LoadBalancerV2Origin(checkerApp.loadBalancer, {
          protocolPolicy: OriginProtocolPolicy.HTTPS_ONLY,
        }),
        allowedMethods: AllowedMethods.ALLOW_ALL,
      },
      logBucket: cloudfrontBucket,
      certificate: checkerCertificate
    });

    const ruleMetric = new Metric({
      metricName: "RulesNotFound",
      namespace: "Typerighter",
      period: Duration.seconds(60),
      statistic: "Sum",
    });

    const ruleProvisionerAlarm = new Alarm(this, "rule-provisioner-alarm", {
      alarmName: `Typerighter - ${this.stage} - rule provisioner issue`,
      alarmDescription:
        "There was a problem getting rules for Typerighter. Rules might not be present, or might be out of date.",
      threshold: 0,
      evaluationPeriods: 3,
      comparisonOperator: ComparisonOperator.GREATER_THAN_THRESHOLD,
      treatMissingData: TreatMissingData.NOT_BREACHING,
      metric: ruleMetric,
    });

    // Database

    const dbAppName = "rule-manager-db";

    const dbAccessSecurityGroup = new GuSecurityGroup(this, "DBSecurityGroup", {
      app: ruleManagerAppName,
      description: "Allow traffic from EC2 instances to DB",
      vpc: ruleManagerApp.vpc,
      allowAllOutbound: false,
    });

    dbAccessSecurityGroup.connections.allowFrom(
      ruleManagerApp.autoScalingGroup,
      Port.tcp(dbPort),
      "Allow connection from EC2 instances to DB"
    );

    const ruleDB = new GuDatabaseInstance(this, "RuleManagerRDS", {
      app: dbAppName,
      vpc: ruleManagerApp.vpc,
      vpcSubnets: { subnetType: SubnetType.PRIVATE },
      allocatedStorage: 50,
      allowMajorVersionUpgrade: true,
      autoMinorVersionUpgrade: true,
      backupRetention: Duration.days(10),
      engine: DatabaseInstanceEngine.postgres({
        version: PostgresEngineVersion.VER_11_12,
      }),
      instanceType: "t3.micro",
      instanceIdentifier: `typerighter-rule-manager-store-${this.stage}`,
      parameters: {
        max_connections: "100",
        maintenance_work_mem: "245760",
        work_mem: "38912",
        checkpoint_completion_target: "0.9",
        shared_buffers: "131072",
        synchronous_commit: "off",
        effective_cache_size: "393216",
        random_page_cost: "1.1",
      },
      subnetGroup: new SubnetGroup(this, "DBSubnetGroup", {
        vpc: ruleManagerApp.vpc,
        vpcSubnets: {
          subnets: GuVpc.subnetsFromParameter(this),
        },
        description: "Subnet for typerighter rule-manager database",
      }),
      credentials: Credentials.fromPassword(
        parameters.MasterDBUsername.valueAsString,
        SecretValue.ssmSecure(
          `/${this.stage}/${this.stack}/typerighter-rule-manager/db.default.password`,
          "1"
        )
      ),
      multiAz: true,
      port: dbPort,
      preferredBackupWindow: "02:00-02:30",
      preferredMaintenanceWindow: "Mon:06:30-Mon:07:00",
      securityGroups: [dbAccessSecurityGroup],
      storageEncrypted: true,
      storageType: StorageType.GP2,
      removalPolicy: RemovalPolicy.SNAPSHOT,
    });
  }
}
