#!/bin/bash

awslocal s3 mb s3://typerighter-app-local

touch typerighter-rules-seq.json
awslocal s3 cp typerighter-rules-seq.json s3://typerighter-app-local/local/rules/typerighter-rules-seq.json
# Copying from the location in the localstack instance that Docker writes the dictionary file to
awslocal s3 cp /etc/gu/typerighter/collins-dictionary.xml s3://typerighter-app-local/local/dictionary/collins-dictionary.xml
awslocal s3 cp /etc/gu/typerighter/collins-lemmatised-list.xml s3://typerighter-app-local/local/dictionary/collins-lemmatised-list.xml
awslocal s3 cp /etc/gu/typerighter/words-to-not-publish.json s3://typerighter-app-local/local/dictionary/words-to-not-publish.json
