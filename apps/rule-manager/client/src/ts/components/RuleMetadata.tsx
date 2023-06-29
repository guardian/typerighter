import {RuleFormSection} from "./RuleFormSection";
import {LineBreak} from "./LineBreak";
import {TagsSelector} from "./TagsSelector";
import {CategorySelector} from "./CategorySelector";
import React from "react";
import { PartiallyUpdateRuleData } from "./RuleForm";
import {DraftRule} from "./hooks/useRule";
import {TagMap} from "./hooks/useTags";

export const RuleMetadata = ({tags, ruleData, partiallyUpdateRuleData}: {
    ruleData: DraftRule,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
    tags: TagMap
}) => {

    return <RuleFormSection title="RULE METADATA">
        <LineBreak/>
        <CategorySelector ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData}/>
        <TagsSelector tags={tags} ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData}/>
    </RuleFormSection>
}
