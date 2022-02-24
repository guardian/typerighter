#!/usr/bin/env node
import { App } from "@aws-cdk/core";
import { Typerighter } from "../lib";

const app = new App();

new Typerighter(app, "typerighter", {
  stack: "flexible",
});
