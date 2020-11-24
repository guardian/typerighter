# Typerighter rule management service architecture

## Status

Proposed

## Context

Typerighter requires a rule management system to serve rule owners (RO) (those managing the corpus of rules) and rule users (RU) (those receiving matches via the Typerighter service). As a summary, key user stories include:

-   Browsing, sorting, and filtering rules (RO)

-   CRUD for rules, including the ability to evaluate rules in a draft context, before applying them to the published corpus (RO)

-   Understanding the history of a rule, including seeing when and what was changed by whom (RO, RU)

-   Adding feedback to rules based on match performance (RU)

See the [user stories doc](../user-stories/001-rule-management-service.md) for a comprehensive list.

## Positions
------------

### 1. Backend

We'll need a backend to interact with the services that persist and publish our rules.

-   Typescript w/ e.g. Express or similar

    + (+) Potentially share models, code w/ a Typescript frontend, would we use one

    - (-) Cannot share models w/ checker service

    - (-) Issuing cookies not yet possible via [pan-domain-auth](https://github.com/guardian/pan-domain-authentication/#to-verify-login-in-nodejs) in Node

-   Scala w/ Play

-   + (+) Share models, code w/ checker service

-   - (-) Cannot share models, code w/ Typescript frontend

### 2. Frontend

There are a few options for rendering the front end of the application:

-   Server-side w/ play templates

    + (+) quicker to write (no models or state to pass)

    - (-) no interactivity, splicing javascript to manage changing UI not straightforward

-   JS managed w/ React

    + (+) interactivity

    + (+) integration with existing tooling, including prosemirror, Source design system

    - (-) more work (replicate models across client/srv boundary, fetch data, manage async state)

### 3. Version control

We can split our projects across repos, or maintain them in a single repository:

-   Split repos

    + (+) allows versioning of common dependencies

    - (-) adds release friction to anything maintained in the common project

-   Monorepo

    - (+) simpler to make changes to common dependencies

    - (-) potential of common projects becoming a 'bit bucket', with implications for coupling, comprehensibility of project

### 4. Persistence

We've got some data to persist. We think there'll be at least high single-figure no. of entities, with well-defined relationships:

-   Data integrity -- we'd like schema-on-write w/ integrity guarantees and joins, esp. to satisfy the 'rule history' requirement.

-   Availability -- we don't mind limited downtime -- two or three nines would be fine. The availability of ruleset once it's published is the bottleneck for our rule delivery service.

-   Durability -- we care a great deal if we lose data, as the time and effort we spend maintaining the corpus will be significant. We'll need backups on a regular schedule, with agreement with the rule owners as to what 'regular' means.

There are a few options:

-   NoSQL

    + (+) trivial to interface with

    + (+) well understood by department

    + (+) meets availability and durability requirements

    - (-) does not meet integrity requirements

    - (-) post-hoc indexing can be painful under certain circumstances, with the potential to limit the questions we can reasonably answer in a timely way as the corpus grows

-   RDS, e.g. postgres

    + (+) meets data integrity requirements, with the caveat that we'll need to set up backups

    + (+) meets availability and durability requirements

    + (+) flexible indexing means it's easier to answer questions we don't yet know we'll ask

    - (-) more effort to define schema and interactions in code

## Decisions
------------

### 1. Backend

We prefer Scala, as there'll be plenty to share with the checker service. Scala backends across Tools are generally speaking standard.

#### Consequences

See 2. Frontend.

### 2. Frontend

We prefer React as, in the face of an unknown amount of interactivity reqs. from the app in future, we cover that possibility, and we think the cost is not too great. We may be able to use code generation to manage syncing models across back- and front-ends.

#### Consequences

We'll need to invest time in either duplicating the API models across the client/server boundary, or coming up with a solution that generates them from code.

### 3. Version Control

We'll use a monorepo to enable dependency sharing across the project. A 'models' project will contain data structures common to the management and checker service. By avoiding loose naming for locations where code is shared (e.g. 'common', 'shared'), we hope to avoid these locations becoming general purpose code buckets, which can lead to unnecessary coupling and application structures that are difficult to understand.

#### Consequences

We'll need a 'models' project in the Typerighter repository, with clear guidelines as to what should be included.

### 4. Persistence

We'll use Postgres to persist application data. It's well used in other tooling, and satisfies our data integrity concerns.

#### Consequences

We'll need to make a decision as to how we interact with the DB via the Scala project. ScalikeJDBC seems to be the consensus within the Tools ecosystem.
