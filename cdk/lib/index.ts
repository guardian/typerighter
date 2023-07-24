import {
  App,
  Duration,
  RemovalPolicy,
  SecretValue,
} from "aws-cdk-lib";
import { Certificate } from "aws-cdk-lib/aws-certificatemanager";
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
import { InstanceType, Port, SubnetType } from "aws-cdk-lib/aws-ec2";
import { GuS3Bucket } from "@guardian/cdk/lib/constructs/s3";
import {
  AllowedMethods,
  CacheCookieBehavior,
  CacheHeaderBehavior,
  CachePolicy,
  CacheQueryStringBehavior,
  Distribution,
  OriginProtocolPolicy,
} from "aws-cdk-lib/aws-cloudfront";
import { LoadBalancerV2Origin } from "aws-cdk-lib/aws-cloudfront-origins";
import {
  Alarm,
  ComparisonOperator,
  Metric,
  TreatMissingData,
} from "aws-cdk-lib/aws-cloudwatch";
import { GuDatabaseInstance } from "@guardian/cdk/lib/constructs/rds";
import {
  Credentials,
  DatabaseInstanceEngine,
  PostgresEngineVersion,
  StorageType,
  SubnetGroup,
} from "aws-cdk-lib/aws-rds";
import { GuArnParameter, GuParameter } from "@guardian/cdk/lib/constructs/core";
import { AccessScope } from "@guardian/cdk/lib/constants/access";
import {Secret} from "aws-cdk-lib/aws-secretsmanager";

export interface TyperighterStackProps extends GuStackProps {
  domainSuffix: string;
  instanceCount: number;
}

export class Typerighter extends GuStack {
  constructor(scope: App, id: string, props: TyperighterStackProps) {
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

    const permissionsFilePolicyStatement = new GuGetS3ObjectsPolicy(this, "PermissionsPolicy", {
      bucketName: "permissions-cache",
      paths: [`${this.stage}/*`]
    });

    const lowercaseStage = this.stage.toLowerCase();

    const typerighterBucketName = `typerighter-app-${lowercaseStage}`;

    // Checker app

    const checkerAppName = "typerighter-checker";

    const checkerDomain = `checker.${props.domainSuffix}`

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
        domainName: checkerDomain
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
        minimumInstances: props.instanceCount,
      },
      applicationLogging: {
        enabled: true,
        systemdUnitName: "typerighter-checker"
      }
    });

    // Rule manager app

    const dbPort = 5432;

    const ruleManagerAppName = "typerighter-rule-manager";

    const ruleManagerDomain = `manager.${props.domainSuffix}`

    const ruleManagerApp = new GuPlayApp(this, {
      app: ruleManagerAppName,
      instanceType: new InstanceType("t4g.micro"),
      userData: `#!/bin/bash -ev
        aws --quiet --region ${this.region} s3 cp s3://composer-dist/${this.stack}/${this.stage}/typerighter-rule-manager/typerighter-rule-manager.deb /tmp/package.deb
        dpkg -i /tmp/package.deb

        mkdir /etc/gu
cat > /etc/gu/typerighter-rule-manager.conf << 'EOF'
typerighter.checkerServiceUrl = "https://${checkerDomain}"
EOF
        `,
      access: {
        scope: AccessScope.PUBLIC,
      },
      certificateProps: {
        domainName: ruleManagerDomain,
      },
      monitoringConfiguration: {
        noMonitoring: true,
      },
      roleConfiguration: {
        additionalPolicies: [
          pandaAuthPolicy,
          permissionsFilePolicyStatement
        ],
      },
      scaling: {
        minimumInstances: props.instanceCount,
      },
      applicationLogging: {
        enabled: true,
        systemdUnitName: "typerighter-rule-manager"
      }
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

    const typerighterBucket = new GuS3Bucket(this, "typerighter-bucket", {
      bucketName: typerighterBucketName,
      app: ruleManagerAppName
    });
    typerighterBucket.grantReadWrite(checkerApp.autoScalingGroup);
    typerighterBucket.grantReadWrite(ruleManagerApp.autoScalingGroup);

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
              cachePolicyName: `checker-cloudfront-cache-policy-${this.stage}`,
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

    // Box-to-box communication

    const hmacSecret = new Secret(this, "hmacSecret", {
      description: "Shared secret for HMAC-based communication between manager and checker services",
      secretName: `/${this.stage}/flexible/typerighter/hmacSecretKey`
    });

    hmacSecret.grantRead(checkerApp.autoScalingGroup.role);
    hmacSecret.grantRead(ruleManagerApp.autoScalingGroup.role);

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
      allowMajorVersionUpgrade: false,
      autoMinorVersionUpgrade: true,
      backupRetention: Duration.days(10),
      engine: DatabaseInstanceEngine.postgres({
        version: PostgresEngineVersion.VER_13,
      }),
      instanceType: "db.t4g.micro",
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
      multiAz: this.stage === "PROD",
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
