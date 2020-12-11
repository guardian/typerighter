import {
  GuStack,
  GuSubnetListParameter,
  GuVpcParameter,
  GuAmiParameter
} from "@guardian/cdk/lib/constructs/core";
import { App, StackProps } from '@aws-cdk/core';

export class RuleManager extends GuStack {
  constructor(scope: App, id: string, props?: StackProps) {
    super(scope, id, props);

    const parameters = {
      Subnets: new GuSubnetListParameter(this, "Subnets", {
        description: "The subnets where AMIable instances will run",
      }),
      VpcId: new GuVpcParameter(this, "VpcId", {
        description: "The VPC in which AMIable instances will run",
        default: "/account/vpc/default/id"
      }),
      AMI: new GuAmiParameter(this, "AMI", {
        description: "AMI to use for instances",
      }),
    }
  }
}
