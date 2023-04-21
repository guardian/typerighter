# Rule manager: implementing rule edit history

## Context

We'd like to store a history of rule edits. The user stories we'd like to satisfy with this feature are:

- As a rule owner, I'd like to be able to see the previous edits made to a rule, including
  - what changed, and
  - when.
- As a rule owner, I'd like to be able to restore a previous version of a rule.

There are a few ways we might model the data necessary to support these features in the database.

# Positions

### 1. Create a separate table, `rules_history`, populated with older versions of rules as rules are edited.

- (+) Reading and writing rules does not change – the `rules` table is canonical
- (-) We must maintain a duplicate of the rules table. The schema of that table must be kept in sync with the `rules` table.
- (-) The mechanism for writing rules into `rules_history` introduces complexity:
  1. Use a trigger. There's a [PL/pgSQL implementation of the temporal tables DB extension](https://github.com/nearform/temporal_tables) which looks well-used and tested, but future users may encounter edge-cases which are hard to debug.
  2. Write rows manually via the application. This behaviour is more explicit, but must be maintained manually. We'd need to be careful to make sure a row was written for every possible change.

### 2. Modify the existing table to make rules immutable. 

Updates to rules would create a new, 'active' version of the rule, and mark the preceding version as inactive. A view table would use this property to give a view of which rules were current.

Rule would need to have a UID that was stable across revisions, as opposed to the auto-incrementing ID they have at present, and a revisionId. The primary key would become a composite of these two columns, guaranteeing uniqueness per version.

A [partial index with a UNIQUE constraint](https://www.postgresql.org/docs/current/indexes-partial.html) could ensure that only one version of a rule was active at a time.

- (+) One table for rules. No cost of duplicating effort across schemas.
- (?) Lots of rules and edits means a larger table. We don't expect a corpus larger than 7 digits - with adequate indexes I don't anticipate problems.
- (-) Pattern perhaps less familiar to developers at first glance. 
- (-) Other tables that would like to reference a `rule` would have to take into account the active state.


## Decision

TBD.

## Consequences

TBD.
