#!/usr/bin/env node
import { App } from "aws-cdk-lib/core";
import { Typerighter } from "../lib";

const app = new App();

const stack = "editorial-feeds";

const env = {
  region: "eu-west-1",
};

new Typerighter(app, "typerighter-CODE", {
  env,
  stack,
  stage: "CODE",
  instanceCount: 1,
  domainSuffix: "typerighter.code.dev-gutools.co.uk",
});

new Typerighter(app, "typerighter-PROD", {
  env,
  stack,
  stage: "PROD",
  instanceCount: 3,
  domainSuffix: "typerighter.gutools.co.uk",
});
