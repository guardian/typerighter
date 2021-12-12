# Typerighter

<img width="1232" alt="The Typerighter checker service frontend" src="https://user-images.githubusercontent.com/7767575/103550037-353f3200-4ea0-11eb-9ba5-9e4f7ecf2d1f.png">

Typerighter is the server-side part of a service to check a document against a set of user-defined rules. It's designed to work like a spelling or grammar checker.

We use it at the Guardian to check content against our style guide. Max Walker, the subeditor who inspired the creation of Typerighter, has written an introduction [here](https://www.theguardian.com/help/insideguardian/2020/nov/20/introducing-typerighter-making-life-easier-for-journalists-and-stories-better-for-readers).

To understand our goals for the tool, see the [vision document](./vision.md).

For setup, see [the docs directory](./docs/).

For an example of a Typerighter client (the part that presents the spellcheck-style interface to the user), see [prosemirror-typerighter](https://github.com/guardian/prosemirror-typerighter).

## How it works: an overview

The Typerighter checker service ingests user-defined rules from a `RuleResource`. This is a Google sheet, but the interface could be fulfilled from an arbitrary source.

Each rule in the service corresponds to a `Matcher` that receives the document and passes back a list of `RuleMatch`. We have the following `Matcher` implementations:

- `RegexMatcher` uses regular expressions
- `LanguageToolMatcher` is powered by the [LanguageTool](https://languagetool.org/) project, and uses a combination of native LanguageTool rules and user-defined XML rules as its corpus

Matches contain the range that match applies to, a description of why the match has occurred, and any relevant suggestions – see the `RuleMatch` interface for the full description.

## Implementation

Both the checker and management services are built in Scala with the Play framework. Data is currently stored in a Google Sheet.

Google credentials are fetched from SSM using AWS Credentials or Instance Role. 

It's worth noting that, at the moment, there are a fair few assumptions built into this repository that are Guardian-specific:
 - We assume the use of AWS cloud services, and default to the `eu-west-1` region. This is configurable on a [per-project](https://github.com/guardian/typerighter/blob/main/apps/checker/conf/application.conf) basis with the [configuration parameter `aws.region`](https://github.com/guardian/typerighter/blob/fa90ef260cd71e0f4fa1b893d7bba9b87ff828ef/apps/common-lib/src/main/scala/com/gu/typerighter/lib/CommonConfig.scala#L16).
 - Building and deployment is handled by riff-raff, [the Guardian's deployment platform](https://github.com/guardian/riff-raff)
 - Configuration is handled by [simple-configuration](https://github.com/guardian/simple-configuration)

We'd be delighted to consider any discussions or PRs that aimed to make Typerighter easier to use in a less institionally-specific context.

## Integration

The [prosemirror-typerighter](https://github.com/guardian/prosemirror-typerighter) plugin provides an integration for the [Prosemirror](https://prosemirror.net) rich text editor.

If you'd like to provide your own integration, this service will function as a standalone REST platform, but you'll need to use [pan-domain-authentication](https://github.com/guardian/pan-domain-authentication) to provide a valid auth cookie with your requests.
