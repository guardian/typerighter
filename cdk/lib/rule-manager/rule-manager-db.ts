import { Port, SecurityGroup, SubnetType } from "@aws-cdk/aws-ec2";
import { Credentials, DatabaseInstanceEngine, PostgresEngineVersion, StorageType, SubnetGroup } from "@aws-cdk/aws-rds";
import type { App } from "@aws-cdk/core";
import { Duration, RemovalPolicy, SecretValue, Tags } from "@aws-cdk/core";
import { GuStackProps } from "@guardian/cdk/lib/constructs/core";
import {
  GuParameter,
  GuStack
} from "@guardian/cdk/lib/constructs/core";
import { GuSecurityGroup, GuVpc } from "@guardian/cdk/lib/constructs/ec2";
import { GuDatabaseInstance } from "@guardian/cdk/lib/constructs/rds";
export class RuleManagerDB extends GuStack {
  constructor(scope: App, id: string, props: GuStackProps) {
    super(scope, id, props);

    const dbPort = 5432;

    const parameters = {
      VpcId: new GuParameter(this, "VPC", {
        type: "AWS::SSM::Parameter::Value<AWS::EC2::VPC::Id>",
        description: "Virtual Private Cloud to run EC2 instances within",
        default: "/account/vpc/default/id"
      }),
      MasterDBUsername: new GuParameter(this, "MasterDBUsername", {
        description: "Master DB username",
        default: "rule_manager",
        type: "String"
      }),
      PrivateVpcSubnets: new GuParameter(this, "PrivateSubnets", {
        type: "AWS::SSM::Parameter::Value<List<AWS::EC2::Subnet::Id>>",
        description: "Subnets to run the ASG and instances within",
        default: "/account/vpc/default/private.subnets"
      }),
      AccessSecurityGroupID: new GuParameter(this, "AccessSecurityGroupID", {
        description: "Id of the security group from which access to the DB will be allowed",
        type: "AWS::EC2::SecurityGroup::Id",
      })
    };

    /* Resources */

    const vpc = GuVpc.fromId(this, "Vpc", { vpcId: parameters.VpcId.valueAsString });

    const subnets = GuVpc.subnets(this, parameters.PrivateVpcSubnets.valueAsList);

    const dbSecurityGroup = new GuSecurityGroup(this, "DBSecurityGroup", {
      description: "DB security group servers",
      vpc,
      allowAllOutbound: true,
      overrideId: true,
    });

    dbSecurityGroup.connections.allowFrom(
      SecurityGroup.fromSecurityGroupId(this, "AccessSecurityGroup", parameters.AccessSecurityGroupID.valueAsString),
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
        version: PostgresEngineVersion.VER_11,
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
        parameters.MasterDBUsername.valueAsString,
        SecretValue.ssmSecure(`/${this.stage}/${this.stack}/typerighter-rule-manager/db.default.password`, "1")
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
