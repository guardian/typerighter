#!/usr/bin/env node
import * as cdk from '@aws-cdk/core';
import { RuleManager } from '../lib/rule-manager/rule-manager';

const app = new cdk.App();
new RuleManager(app, 'CdkStack');
