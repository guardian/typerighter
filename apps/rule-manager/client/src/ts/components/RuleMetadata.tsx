import {RuleFormSection} from "./RuleFormSection";
import {LineBreak} from "./LineBreak";
import {TagsSelector} from "./TagsSelector";
import {CategorySelector} from "./CategorySelector";
import React from "react";
import { PartiallyUpdateRuleData } from "./RuleForm";
import {DraftRule} from "./hooks/useRule";
import {TagMap} from "./hooks/useTags";

export const RuleMetadata = ({tags, ruleData, partiallyUpdateRuleData}: {
    ruleData: DraftRule | DraftRule[],
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
    tags: TagMap
}) => {
    let combinedTags = new Set<string>();

    if (Array.isArray(ruleData)) {
        ruleData.forEach((rule, index) => {
            rule.tags.forEach(tag => combinedTags.add(tag))
        })
    }
    return <RuleFormSection title="RULE METADATA">
        <LineBreak/>
        <CategorySelector ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData}/>
        <TagsSelector ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData} combinedTags={combinedTags}/>
    </RuleFormSection>
}
