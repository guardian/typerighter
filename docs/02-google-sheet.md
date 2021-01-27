# Getting the ID of the current google sheet

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

## The sheet format

An example rule sheet with a few instances of rules of different types is available [here](https://docs.google.com/spreadsheets/d/1n5xjfVnvRQBMfmjD_VzFX2ye4pLBmL98whdDqsJhtAs).

As currently implemented, only the first sheet is read for custom rules, and the sheet named `languagetoolRules` handles enabling rules that are a part of the default LanguageTool corpus.

Here's a snippet to give the [gist](https://en.wiktionary.org/wiki/gist), fields with asterisks are mandatory:

| Type* | Pattern*   | Replacement | Colour (deprecated) | Category* | Additional data (deprecated) | Description* | Tags | Ignore? | Notes | ID*                     |
|-------|------------|------------------------------------------------------------|---------------------|---------------------|------------------------------|----------------------------------------|-----------------------------|---------|--------------------------------------------------|--------------------------------------|
| regex | \b(j\|g)ist | gist                                                       | 00ffff              | Guardian convention |                              | gist, according to Guardian convention | Guardian convention         | FALSE   |                                                  | 658184fb-d1d4-4962-aba1-de3d31946cbc |




| Field | Description |
|-|-|
| Type | The type of matcher to use. At the moment, this is either `regex` or `lt`, where `lt` is the LanguageTool matcher. |
| Pattern | The pattern for the matcher. For `regex` matchers, this will be a regular expression. For `lt`, this will be the XML for a `rule` or `rulegroup`. |
| Replacement | Used by the `regex` matcher only. Can include capture groups, e.g `$1` |
| Colour | Deprecated |
| Category | The category of the match. Often used for display purposes.  |
| Additional data | Deprecated |
| Description | A description of the rule. Can use markdown syntax. Used when displaying matches to users. |
| Tags | Comma-separated tags to identify the rule in the corpus. Currently unused, but likely to be important in any future rule-management system. |
| Ignore? | Should this rule be ignored? (FALSE\|TRUE) |
| Notes | General notes for rule maintenance, not currently used by the system |
| ID | An id to uniquely identify the rule within the corpus. |


