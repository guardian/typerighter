# Typerighter

A tool to check if what you typed is right.

## Implementation

Standard play service, with data currently stored in a google sheet.
Google credentials are fetched from ssm using AWS Credentials or Instance Role. 

## Integration

The service is intended to be run as a plugin to ProseMirror but will function as a standalone REST platform.

## Basic developer run

`sbt run` and visit `localhost:9000` to confirm the service came up correctly.

## Demonstration of null request

Each request has the following format, of zero or more text blocks, and zero or more rule sets to check.

```
cat <<EOF | curl -s -X POST localhost:9000/check -H 'Content-Type: application/json' -d@- | jq '.'
{
  "requestId": "1", 
  "documentId": "2", 
  "blocks": [], 
  "categoryIds": []
}
EOF

```

## Demonstration of sample realistic request

The following demonstrates the structure of a text block.  As many of these are
are needed can be provided.

```
cat <<EOF | curl -s -X POST localhost:9000/check -H 'Content-Type: application/json' -d@- | jq '.'
{
  "requestId": "1", 
  "documentId": "2", 
  "blocks": [
    {
      "id": "1",
      "text": "This is a block of text from the Grauniad, for testing.",
      "from": 0,
      "to": 1000
    }
  ], 
  "categoryIds": [
    "General Election, 2019"
  ]
}
EOF

```

Assuming you are running a CODE environment, the response should indicate that the word 'Grauniad' is misspelled.

## Getting the google sheet.

Get the identity of the google sheet with the following:
```
STAGE=CODE #replace as applicable
aws ssm --profile composer --region eu-west-1 get-parameter --name "/$STAGE/flexible/typerighter/typerighter.sheetId"
```

You can then open the sheet at:
```
https://docs.google.com/document/d/<ID>/edit
```

Note that there are different sheets for different environments.

## Interpreting the Google sheet.

As currently implemented, the first sheet only is read.

The columns are:
 * Unused
 * Correct Value
 * Regex (search string)
 * Colour
 * Category ID
 * Unused
 * Description
