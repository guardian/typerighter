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

## The sheet format.

An example rule sheet with a few instances of rules of different types is available [here](https://docs.google.com/spreadsheets/d/1n5xjfVnvRQBMfmjD_VzFX2ye4pLBmL98whdDqsJhtAs/edit#gid=0). Here's a snippet to give the [gist](https://en.wiktionary.org/wiki/gist):

| Type* | Pattern*   | Replacement (regex type only – can include capture groups) | Colour (deprecated) | Category            | Additional data (deprecated) | Description (can use Markdown syntax)* | Tags (separate with commas) | Ignore? | Notes (not processed, just for maintainance use) | ID (any string)*                     |
|-------|------------|------------------------------------------------------------|---------------------|---------------------|------------------------------|----------------------------------------|-----------------------------|---------|--------------------------------------------------|--------------------------------------|
| regex | \b(j\|g)ist | gist                                                       | 00ffff              | Guardian convention |                              | gist, according to Guardian convention | Guardian convention         | FALSE   |                                                  | 658184fb-d1d4-4962-aba1-de3d31946cbc |

As currently implemented, only the first sheet is read for custom rules, and the sheet named `languagetoolRules` handles enabling rules that are a part of the default LanguageTool corpus.


