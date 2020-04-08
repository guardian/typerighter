# Challenges

1. Mistakes in spelling, grammar and style exist in our published content.
2. Knowledge of what is 'correct' for all three of these categories is uneven. This knowledge changes and degrades over time.
3. The Guardian optimises for speed of publication, which increases the likelihood that we'll make mistakes in published content. The cost of correcting published errors is generally greater than catching them pre-publication.
4. Subediting in production includes mundane tasks that, we think, can be automated.

# Vision

The goal of the Typerighter project is to address the above challenges with a [universally available](#universal) document proofing service, via  an [easy-to-use](#easy-to-use) and [minimal interface](#minimal), that is [accurate](#accurate), [transparent](#transparent), and [responsive to change](#responsive-to-change).

## Universal

Proofing tools should be available to authors wherever they write.

Typerighter will be available for use across our internal toolset and beyond it, to check any text that is ultimately published and readable by the public. It will be as easy as possible to integrate into other applications, regardless of platform or publishing cadence. Its API will only care about text.

The initial candidate for adoption is Composer, starting with the Article edit view, with liveblogs and furniture fields (standfirst, image caption etc.) to follow. Beyond Composer, in a non-exhaustive and unordered list, Atom Workshop, Media Atom Maker, the Fronts tool, and the Grid are likely to be useful next steps.

#### KPIs

- Adoption across organisation
- Number of tools using the service

## Easy to use

A frictionless experience keeps users happy and drives adoption.

Typerighter will be easy to use, for writers and maintainers alike. The less friction our users experience, the more likely they are to use the service, and to recommend it to others.

### KPIs

- User feedback
- Adoption across organisation

## Minimal

Bloated tooling gets in the way.

For the user, Typerighter provides a simple service. We will ensure that the user interface reflects that simplicity.

### KPIs

- User feedback

## Accurate

Accurate tools gain their users' trust.

Typerighter will provide a service that values accuracy over volume. Broadly defined rules add noise to the signal users receive. Poorly defined rules are more likely to result in false positives or negatives. Both outcomes are likely to reduce users' trust in the service.

Typerighter will provide tooling that helps us audit our rules against our content at scale, to ensure that rule makers are best placed to understand the impact of their rules during proofing.

### KPIs

- High ratio of accepted suggestions where suggestions are provided
- User feedback

## Transparent

Transparent tooling is easier to reason about.

Typerighter will provide a service that is as transparent as possible, whether the user is checking text or writing rules. When Typerighter matches text, it will be clear why that match has occurred. When Typerighter has suggestions for changes to text, the user will be in charge of every single edit. And when rules are added or changed, it will be clear to users why that modification has taken place.

### KPIs

- Low frequency of feedback related to provenance of rules
- Feedback related to inaccurate rules is clearly actionable

## Responsive to change

Change is a permanent fact of language, and a proofing tool must be able to change with it.

Typerighter's rules will be as easy as possible to create and alter in a 24/7 news cycle. And when it's clear that a rule is not performing as it should, Typerighter will make it trivial for a user to let its maintainers know, with as much context as possible included by default in the feedback.

### KPIs

- Changes are submitted with high quality feedback
- Usage by editorial as an authoritative source for rule/style guide knowledge
