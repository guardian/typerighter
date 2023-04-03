#!/bin/bash

awslocal s3 mb s3://typerighter-app-local

json='{"rules": []}'
echo "$json" > typerighter-rules.json
awslocal s3 cp typerighter-rules.json s3://typerighter-app-local/local/rules/typerighter-rules.json
