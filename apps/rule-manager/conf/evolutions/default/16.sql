-- !Ups

UPDATE rules_draft SET description = external_id WHERE rule_type = 'languageToolCore'

-- !Downs

UPDATE rules_draft SET description = '' WHERE rule_type = 'languageToolCore'
