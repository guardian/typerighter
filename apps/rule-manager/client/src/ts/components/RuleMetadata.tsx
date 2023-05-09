import {RuleFormSection} from "./RuleFormSection";
import {LineBreak} from "./LineBreak";
import {TagsSelector} from "./TagsSelector";
import {CategorySelector} from "./CategorySelector";
import React from "react";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";

export const RuleMetadata = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: RuleFormData,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {

    return <RuleFormSection title="RULE METADATA">
        <LineBreak/>
        <CategorySelector ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData}/>
        <TagsSelector ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData}/>
    </RuleFormSection>
}