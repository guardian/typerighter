import {RuleFormSection} from "./RuleFormSection";
import {LineBreak} from "./LineBreak";
import {TagsSelector} from "./TagsSelector";
import {CategorySelector} from "./CategorySelector";
import React from "react";

export const RuleMetadata = () => {

    return <RuleFormSection title="RULE METADATA">
        <LineBreak/>
        <CategorySelector/>
        <TagsSelector/>
    </RuleFormSection>
}