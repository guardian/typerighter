import {
  App,
  CfnMapping,
  Duration,
  RemovalPolicy,
  SecretValue,
} from "@aws-cdk/core";
import { Certificate } from "@aws-cdk/aws-certificatemanager";
import type { GuStackProps } from "@guardian/cdk/lib/constructs/core/stack";
import {
  GuDnsRecordSet,
  RecordType,
} from "@guardian/cdk/lib/constructs/dns/dns-records";
import { GuStack } from "@guardian/cdk/lib/constructs/core/stack";
import { GuPlayApp } from "@guardian/cdk";
import {
  GuGetS3ObjectsPolicy,
  GuPutCloudwatchMetricsPolicy,
} from "@guardian/cdk/lib/constructs/iam";
import { GuSecurityGroup, GuVpc } from "@guardian/cdk/lib/constructs/ec2";
import { InstanceType, Port, SubnetType } from "@aws-cdk/aws-ec2";
import { GuS3Bucket } from "@guardian/cdk/lib/constructs/s3";
import {
  AllowedMethods,
  CacheCookieBehavior,
  CacheHeaderBehavior,
  CachePolicy,
  CacheQueryStringBehavior,
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
import { AccessScope } from "@guardian/cdk/lib/constants/access";
import { Stage } from "@guardian/cdk/lib/constants";

export class Typerighter extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const parameters = {
      MasterDBUsername: new GuParameter(this, "MasterDBUsername", {
        description: "Master DB username",
        default: "rule_manager",
        type: "String",
      }),
      CheckerCertificate: new GuArnParameter(
        this,
        "CheckerCloudfrontCertificate",
        {
          description:
            "The ARN of the certificate for the checker service Cloudfront distribution",
        }
      ),
    };

    const pandaAuthPolicy = new GuGetS3ObjectsPolicy(this, "PandaAuthPolicy", {
      bucketName: "pan-domain-auth-settings",
    });

    const checkerDomainCODE = "checker.typerighter.code.dev-gutools.co.uk";
    const checkerDomainPROD = "checker.typerighter.gutools.co.uk";
    const managerDomainCODE = "manager.typerighter.code.dev-gutools.co.uk";
    const managerDomainPROD = "manager.typerighter.gutools.co.uk";

    const lowercaseStage = this.withStageDependentValue({
      app: "typerighter",
      variableName: "lowercase",
      stageValues: {
        PROD: "prod",
        CODE: "code",
      },
    });

    const typerighterBucketName = `typerighter-app-${lowercaseStage}`;

    const dbPort = 5432;

    // Rule manager app

    const ruleManagerAppName = "typerighter-rule-manager";

    const ruleManagerDomain = this.withStageDependentValue({
      app: ruleManagerAppName,
      variableName: "ruleManagerDomain",
      stageValues: {
        PROD: managerDomainPROD,
        CODE: managerDomainCODE,
      },
    });

    const ruleManagerApp = new GuPlayApp(this, {
      app: ruleManagerAppName,
      instanceType: new InstanceType("t4g.micro"),
      userData: `#!/bin/bash -ev
        aws --quiet --region ${this.region} s3 cp s3://composer-dist/${this.stack}/${this.stage}/typerighter-rule-manager/typerighter-rule-manager.deb /tmp/package.deb
        dpkg -i /tmp/package.deb`,
      access: {
        scope: AccessScope.PUBLIC,
      },
      certificateProps: {
        CODE: { domainName: managerDomainCODE },
        PROD: { domainName: managerDomainPROD },
      },
      monitoringConfiguration: {
        noMonitoring: true,
      },
      roleConfiguration: {
        additionalPolicies: [pandaAuthPolicy],
      },
      scaling: {
        [Stage.CODE]: { minimumInstances: 1 },
        [Stage.PROD]: { minimumInstances: 3 },
      },
    });

    const ruleManagerDnsRecord = new GuDnsRecordSet(
      this,
      "manager-dns-records",
      {
        name: ruleManagerDomain,
        recordType: RecordType.CNAME,
        resourceRecords: [ruleManagerApp.loadBalancer.loadBalancerDnsName],
        ttl: Duration.minutes(60),
      }
    );

    // Checker app

    const checkerAppName = "typerighter-checker";

    const checkerDomain = this.withStageDependentValue({
      app: checkerAppName,
      variableName: "checkerDomain",
      stageValues: {
        PROD: checkerDomainPROD,
        CODE: checkerDomainCODE,
      },
    });

    const checkerApp = new GuPlayApp(this, {
      app: checkerAppName,
      instanceType: new InstanceType("t4g.small"),
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
        CODE: { domainName: checkerDomainCODE },
        PROD: { domainName: checkerDomainPROD },
      },
      monitoringConfiguration: {
        noMonitoring: true,
      },
      roleConfiguration: {
        additionalPolicies: [
          pandaAuthPolicy,
          new GuPutCloudwatchMetricsPolicy(this),
        ],
      },
      scaling: {
        [Stage.CODE]: { minimumInstances: 1 },
        [Stage.PROD]: { minimumInstances: 3 },
      },
    });

    const typerighterBucket = new GuS3Bucket(this, "typerighter-bucket", {
      bucketName: typerighterBucketName,
      app: ruleManagerAppName
    });
    typerighterBucket.grantReadWrite(checkerApp.autoScalingGroup);

    const cloudfrontBucket = new GuS3Bucket(this, "cloudfront-bucket", {
      app: ruleManagerAppName,
      lifecycleRules: [
        {
          expiration: Duration.days(90),
        },
      ],
    });

    const checkerCertificate = Certificate.fromCertificateArn(
      this,
      "CheckerCertificate",
      parameters.CheckerCertificate.valueAsString
    );

    const checkerCloudFrontDistro = new Distribution(
      this,
      "typerighter-cloudfront",
      {
        defaultBehavior: {
          origin: new LoadBalancerV2Origin(checkerApp.loadBalancer, {
            protocolPolicy: OriginProtocolPolicy.HTTPS_ONLY,
          }),
          allowedMethods: AllowedMethods.ALLOW_ALL,
          cachePolicy: new CachePolicy(
            this,
            "checker-cloudfront-cache-policy",
            {
              cookieBehavior: CacheCookieBehavior.all(),
              headerBehavior: CacheHeaderBehavior.allowList(
                "Host",
                "Origin",
                "Access-Control-Request-Headers",
                "Access-Control-Request-Method"
              ),
              queryStringBehavior: CacheQueryStringBehavior.all(),
            }
          ),
        },
        domainNames: [checkerDomain],
        logBucket: cloudfrontBucket,
        certificate: checkerCertificate,
      }
    );

    const checkerDnsRecord = new GuDnsRecordSet(this, "checker-dns-records", {
      name: checkerDomain,
      recordType: RecordType.CNAME,
      resourceRecords: [checkerCloudFrontDistro.domainName],
      ttl: Duration.minutes(60),
    });

    const ruleMetric = new Metric({
      metricName: "RulesNotFound",
      namespace: "Typerighter",
      period: Duration.minutes(60),
      statistic: "Sum",
    });

    const ruleProvisionerAlarm = new Alarm(this, "rule-provisioner-alarm", {
      alarmName: `Typerighter - ${this.stage} - issue provisioning rules`,
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
      vpcSubnets: { subnetType: SubnetType.PRIVATE_WITH_NAT },
      allocatedStorage: 50,
      allowMajorVersionUpgrade: true,
      autoMinorVersionUpgrade: true,
      backupRetention: Duration.days(10),
      engine: DatabaseInstanceEngine.postgres({
        version: PostgresEngineVersion.VER_13_3,
      }),
      instanceType: "t3.micro",
      instanceIdentifier: `typerighter-rule-manager-store-${this.stage}`,
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
