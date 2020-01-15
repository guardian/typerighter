# Getting the google sheet.

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
