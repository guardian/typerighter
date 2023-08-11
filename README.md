# Typerighter

<img width="1654" alt="image" src="https://github.com/guardian/typerighter/assets/34686302/db240fc4-cb2b-412d-b74b-02d6077a7258">

Typerighter is the server-side part of a service to check a document against a set of user-defined rules. It's designed to work like a spelling or grammar checker. It contains two services, the [checker](https://checker.typerighter.gutools.co.uk/) and the [rule manager](https://manager.typerighter.gutools.co.uk/) – see [architecture](#architecture) for more information.

We use it at the Guardian to check content against our style guide. Max Walker, the subeditor who inspired the creation of Typerighter, has written an introduction [here](https://www.theguardian.com/help/insideguardian/2020/nov/20/introducing-typerighter-making-life-easier-for-journalists-and-stories-better-for-readers).

To understand our goals for the tool, see the [vision document](./vision.md).

For setup, see [the docs directory](./docs/).

For an example of a Typerighter client (the part that presents the spellcheck-style interface to the user), see [prosemirror-typerighter](https://github.com/guardian/prosemirror-typerighter).

## How it works: an overview

The Typerighter Rule Manager produces a JSON artefact (stored in S3) which is ingested by the Checker service. This artefact represents all the rules in our system, currently including user-defined regex rules, user-defined Language Tool pattern rules (defined as XML) and Language Tool core rules (pre-defined rules from Language Tool). Historically, rules were derived from a Google Sheet, rather than the Rule Manager.

Each rule in the service corresponds to a `Matcher` that receives the document and passes back a list of `RuleMatch`. We have the following `Matcher` implementations:

- `RegexMatcher` uses regular expressions
- `LanguageToolMatcher` is powered by the [LanguageTool](https://languagetool.org/) project, and uses a combination of native LanguageTool rules and user-defined XML rules as its corpus

Matches contain the range that match applies to, a description of why the match has occurred, and any relevant suggestions – see the `RuleMatch` interface for the full description.

## Architecture

### Roles

- Rule owner: a person responsible for maintaining the rules that Typerighter consumes.
- Rule user: a person checking their copy with the checker service.

```mermaid
flowchart LR
  checker[Checker service]
  manager[Manager service]
  sheet[Google Sheet]
  client[Typerighter client]
  s3[(typerighter-rules.json)]
  db[(Postgres DB)]
  owner{{Rule owner role}}
  user{{Rule user role}}

  sheet--"Get rules"-->manager
  manager--"Write rules"-->db
  db--"Read rules"--> manager
  manager--"Write rule artefact"-->s3
  checker--"Read rule artefact"-->s3
  client--"Request matches"-->checker

  owner-."Force manager to re-fetch sheet".->manager
  user-."Request document check".->client
  owner-."Edit rules".->sheet
```

## Implementation

Both the Checker and Rule Manager services are built in Scala with the Play framework. Data in the Rule Manager is stored in a Postgres database, queried via ScalikeJDBC.

Google credentials are fetched from SSM using AWS Credentials or Instance Role.

## Integration

The [prosemirror-typerighter](https://github.com/guardian/prosemirror-typerighter) plugin provides an integration for the [Prosemirror](https://prosemirror.net) rich text editor.

If you'd like to provide your own integration, this service will function as a standalone REST platform, but you'll need to use [pan-domain-authentication](https://github.com/guardian/pan-domain-authentication) to provide a valid auth cookie with your requests.

## Upgrading LanguageTool

LanguageTool has core rules that we use, and as we upgrade LT, these could change underneath us.

There's a script to see if rules have changed as a result of an upgrade in ./script/js/compare-rule-xml.js.

## Formatting

### Prettier formatting

Prettier is installed in the client app using the Guardian's recommended [config](https://github.com/guardian/csnx/tree/main/libs/%40guardian/prettier). To format files you can run `npm run format:write`. A formatting check will run as part of CI.

To configure the IntelliJ Prettier plugin to format on save see the guide [here](https://www.jetbrains.com/help/idea/prettier.html#ws_prettier_configure). To configure the VS Code Prettier plugin see [here](https://github.com/prettier/prettier-vscode#format-on-save).

### Scala formatting
Typerighter uses [Scalafmt](https://scalameta.org/scalafmt/) to ensure consistent linting across all Scala files.

To lint all files you can run `sbt scalafmtAll`
To confirm all files are linted correctly, you can run `sbt scalafmtCheckAll`

You can configure your IDE to format scala files on save according to the linting rules defined in [.scalafmt.conf](.scalafmt.conf)

For intellij there is a guide to set up automated linting on save [here](https://www.jetbrains.com/help/idea/work-with-scala-formatter.html#scalafmt_config) and [here](https://scalameta.org/scalafmt/docs/installation.html). For visual studio code with metals see [here](https://scalameta.org/scalafmt/docs/installation.html#vs-code)

### Automatic formatting

The project contains a pre-commit hook which will automatically run the Scala formatter on all staged files. To enable this, run `./script/setup` from the root of the project.

## Developer how-tos

### Connecting to the rule-manager database in CODE or PROD

Sometimes it's useful to connect to the databases running in AWS to inspect the data locally.

We can use `ssm-scala` to create an SSH tunnel that exposes the remote database on a local port. For example, to connect to the CODE database, we can run:

```bash
ssm ssh -x -t typerighter-rule-manager,CODE -p composer --rds-tunnel 5000:rule-manager-db,CODE
```

You should then be able to connect the database on `localhost:5000`. You'll need to use the username and password specified in [AWS parameter store](https://eu-west-1.console.aws.amazon.com/systems-manager/parameters/?region=eu-west-1&tab=Table) at `/${STAGE}/flexible/typerighter-rule-manager/db.default.username` and `db.default.password`.

Don't forget to kill the connection once you're done! Here's a handy one-liner: `kill $(lsof -ti {PORT_NUMBER})`
