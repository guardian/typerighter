import { Port, SecurityGroup, SubnetType } from "@aws-cdk/aws-ec2";
import { Credentials, DatabaseInstanceEngine, PostgresEngineVersion, StorageType, SubnetGroup } from "@aws-cdk/aws-rds";
import type { App } from "@aws-cdk/core";
import { Fn } from "@aws-cdk/core";
import { Duration, RemovalPolicy, SecretValue, Tags } from "@aws-cdk/core";
import { GuSSMParameter, GuStackProps } from "@guardian/cdk/lib/constructs/core";
import { StringParameter } from '@aws-cdk/aws-ssm';
import {
  GuArnParameter,
  GuInstanceTypeParameter,
  GuParameter,
  GuStack,
  GuStringParameter,
  GuSubnetListParameter,
  GuVpcParameter,
} from "@guardian/cdk/lib/constructs/core";
import { GuSecurityGroup, GuVpc } from "@guardian/cdk/lib/constructs/ec2";
import { GuDatabaseInstance } from "@guardian/cdk/lib/constructs/rds";

export class RuleManagerDB extends GuStack {
  constructor(scope: App, id: string, props?: GuStackProps) {
    super(scope, id, props);

    const dbPort = 5432;

    const parameters = {
      VpcId: new GuSSMParameter(this, "VpcId", {
        description: "ID of the VPC onto which to launch the application eg. vpc-1234abcd",
        default: "/account/vpc/default/id"
      }),
      PrivateVpcSubnets: new GuSSMParameter(this, "PrivateVpcSubnets", {
        description: "Subnets to use in VPC for private EC2 instances eg. subnet-abcd1234",
        default: "/account/vpc/default/private.subnets"
      }),
      AccessSecurityGroupID: new GuParameter(this, "AccessSecurityGroupID", {
        description: "Id of the security group from which access to the DB will be allowed",
        type: "AWS::EC2::SecurityGroup::Id",
      }),
      KMSKey: new GuArnParameter(this, "KMSKey", {
        description: "ARN of the KMS Key to use to encrypt the database",
      }),
    };

    /* Resources */

    const vpc = GuVpc.fromId(this, "vpc", parameters.VpcId.valueAsString);

    const subnets = GuVpc.subnets(this, Fn.split(',', parameters.PrivateVpcSubnets.valueAsString));

    const dbSecurityGroup = new GuSecurityGroup(this, "DBSecurityGroup", {
      description: "DB security group servers",
      vpc,
      allowAllOutbound: true,
      overrideId: true,
    });
    
    dbSecurityGroup.connections.allowFrom(
      SecurityGroup.fromSecurityGroupId(this, "accessSecurityGroup", parameters.AccessSecurityGroupID.valueAsString),
      Port.tcp(dbPort)
    );

    const dbInstance = new GuDatabaseInstance(this, "RuleManagerRDS", {
      vpc,
      vpcSubnets: { subnetType: SubnetType.PRIVATE },
      allocatedStorage: 50,
      allowMajorVersionUpgrade: true,
      autoMinorVersionUpgrade: true,
      backupRetention: Duration.days(10),
      engine: DatabaseInstanceEngine.postgres({
        version: PostgresEngineVersion.VER_11_8,
      }),
      instanceType: "t3.micro",
      instanceIdentifier: `typerighter-rule-manager-db-${this.stage}`,
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
        vpc,
        vpcSubnets: { subnets },
        description: "Private subnet for typerighter rule-manager database",
      }),
      credentials: Credentials.fromPassword(
        // StringParameter.valueForStringParameter(this,`/${this.stage}/${this.stack}/typerighter-rule-manager/db.master.username`, 1),
        SecretValue.ssmSecure(`/${this.stage}/${this.stack}/typerighter-rule-manager/db.master.username`, "1").toString(),
        SecretValue.ssmSecure(`/${this.stage}/${this.stack}/typerighter-rule-manager/db.master.password`, "1")
      ),
      multiAz: true,
      port: dbPort,
      preferredBackupWindow: "02:00-02:30",
      preferredMaintenanceWindow: "Mon:06:30-Mon:07:00",
      securityGroups: [dbSecurityGroup],
      storageEncrypted: true,
      storageType: StorageType.GP2,
      removalPolicy: RemovalPolicy.SNAPSHOT
    });
  }
}