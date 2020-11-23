# Regex matcher capitalisation

## Context

At the moment, when we apply suggestions from regular expressions, there's sometimes an unexpected side effect: we overwrite casing in the matched text.

For example, with regex (?i)\\bmedia?eval (note the (?i) flag, which means it's case insensitive), we always suggest the word medieval.

This causes problems when words begin sentences, e.g. end of sentence. Medieval will produce a match suggesting medieval.

## Positions

### 1. Refactor the corpus to use capture groups for initial chars where appropriate

... for example, `((i?)m)edieaval` with replacement `$1edieval`.

- (+) Possible to arrive at correctness in this way
- (-) It'll take a long time, as our corpus is large and we haven't annotated which rules have case-sensitive replacements

### 2. Detect sentence starts and capitalise accordingly
 
- (+) No changes to our corpus
- (-) Possibly there are edge cases that we haven't yet considered

## Decision

We think detecting sentence starts is a better fit:
- We have a large corpus that will take a great deal of work to refactor, and it's not possible to automate part of this work, as we must distinguish between replacements that care about capitalisation (e.g. `WhatsApp`, `alsatian` – the dog is always lower case!) and replacements that don't (e.g. `medieval`).
- Detecting sentence starts allows us to make the correct suggestion when users have missed a capital at a sentence start and we're offering a replacement, which is a bonus.
- We shouldn't spend too long on the corpus, as the rules that benefit from this case will likely be replaced by a dictionary approach sooner rather than later.

## Consequences

- We don't need to change the corpus
- We may encounter edge cases as a result of the above approach, e.g. `ee cummings` at the start of a sentence, and as a result may have to a way of enforcing case at sentence starts.
