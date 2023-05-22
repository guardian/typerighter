import {RuleFormSection} from "./RuleFormSection";
import {LineBreak} from "./LineBreak";
import {TagsSelector} from "./TagsSelector";
import {CategorySelector} from "./CategorySelector";
import React from "react";
import { PartiallyUpdateRuleData } from "./RuleForm";
import {DraftRule} from "./hooks/useRule";

export const RuleMetadata = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: DraftRule,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {

    return <RuleFormSection title="RULE METADATA">
        <LineBreak/>
        <CategorySelector ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData}/>
        <TagsSelector ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData}/>
    </RuleFormSection>
}
