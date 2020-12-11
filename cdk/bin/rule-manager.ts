#!/usr/bin/env node
import { App } from '@aws-cdk/core';
import { RuleManager } from '../lib/rule-manager/rule-manager';
import { RuleManagerDB } from '../lib/rule-manager/rule-manager-db';

const app = new App();
new RuleManager(app, 'rule-manager', { app: "rule-manager" });
new RuleManagerDB(app, 'rule-manager-db', { app: "rule-manager" })
