# Dependencies
We will need dev-nginx: `brew install dev-nginx`

# Set up

Get credentials for Composer profile.

Run `./script/setup` from the root of the project.

This will:
- Run the `dev-nginx` setup
- Set up pre-commit hooks
- Download our dictionary file for local use

# Basic developer run

Get credentials for Composer profile.

To launch the Checker, run `./script/start-checker` (`--debug` to run a debugger on port 5005).

To start the Rule Manager, run `./script/start-manager` (`--debug` to attach a debugger on port 5006) and visit [the locally running app](https://manager.typerighter.local.dev-gutools.co.uk) to check it is running.

To run everything, run `./script/start` (`--debug` will attach debuggers on both services in the respective ports above) and visit [the locally running app](https://manager.typerighter.local.dev-gutools.co.uk/) to confirm the service came up correctly.

In order to create, edit, and delete rules in Rule Manager, you first need to have the relevant permissions (see: `manage_rules`) enabled in the [permissions manager in CODE](https://permissions.code.dev-gutools.co.uk/).

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
