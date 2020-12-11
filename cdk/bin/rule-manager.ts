#!/usr/bin/env node
import { App, StackProps } from '@aws-cdk/core';
import { RuleManager } from '../lib/rule-manager/rule-manager';

const app = new App();
new RuleManager(app, 'rule-manager');
