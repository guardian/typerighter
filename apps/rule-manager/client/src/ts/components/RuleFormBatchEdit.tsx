import {
    EuiButton,
    EuiCallOut,
    EuiFlexGroup,
    EuiFlexItem,
    EuiForm,
    EuiLoadingSpinner,
    EuiText,
} from "@elastic/eui";
import React, {useEffect, useState} from "react"
import {DraftRule} from "./hooks/useRule";

import {useBatchRules} from "./hooks/useBatchRules";
import {RuleFormSection} from "./RuleFormSection";
import {TagMap} from "./hooks/useTags";
import {baseForm, PartiallyUpdateRuleData, SpinnerContainer, SpinnerOuter, SpinnerOverlay} from "./RuleForm";
import {LineBreak} from "./LineBreak";
import {CategorySelector} from "./CategorySelector";
import {TagsSelector} from "./TagsSelector";

export const RuleFormBatchEdit = ({tags, ruleIds, onClose, onUpdate, isTagMapLoading}: {
    tags: TagMap,
    isTagMapLoading: boolean,
    ruleIds: number[],
    onClose: () => void,
    onUpdate: () => void
}) => {
    const { isLoading, rules, updateRules } = useBatchRules(ruleIds);
    const [ruleFormData, setRuleFormData] = useState<DraftRule[]>([baseForm])

    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (partialReplacement) => {
        const updatedRules = ruleFormData.map(rule => ({
            ...rule,
            ...partialReplacement
        })) as DraftRule[]
        setRuleFormData(updatedRules);
    };

    useEffect(() => {
        if (rules) {
            setRuleFormData(rules.map(rule => rule.draft) as DraftRule[]);
        }
    }, [rules]);

    // We need the errors at the form level, so that we can prevent save etc. when there are errors
    // We need to be able to change the errors depending on which fields are invalid
    // We need to be able to show errors on a field by field basis

    const saveRuleHandler = async () => {
        const response = await updateRules(ruleFormData as DraftRule[]);

        if (response.status === "ok" && Array.isArray(response.data) && response.data.every(rule => rule.id)) {
            onUpdate();
        }
    };

    const categoryHasChanged = rules?.some((rule, index) => ruleFormData[index] && rule.draft.category !== ruleFormData[index].category);
    const tagsHaveChanged = rules?.some((rule, index) => (
      ruleFormData[index] && (
      rule.draft.tags.length !== ruleFormData[index].tags.length ||
        !rule.draft.tags.every((tag, i) => tag === ruleFormData[index].tags[i]))
    ));
    const hasUnsavedChanges = !isLoading && (categoryHasChanged || tagsHaveChanged);

    const uniqueTagIds = [...new Set(ruleFormData.flatMap(rule => rule.tags))];

    return <EuiForm component="form">
        {isLoading && <SpinnerOverlay><SpinnerOuter><SpinnerContainer><EuiLoadingSpinner /></SpinnerContainer></SpinnerOuter></SpinnerOverlay>}
        {<EuiFlexGroup gutterSize="m" direction="column">
            <RuleFormSection title="RULE CONTENT" />
            <RuleFormSection title="RULE METADATA">
              <LineBreak/>
              <CategorySelector currentCategory={ruleFormData[0]?.category} partiallyUpdateRuleData={partiallyUpdateRuleData} />
              <TagsSelector tags={tags} isLoading={isTagMapLoading} selectedTagIds={uniqueTagIds} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            </RuleFormSection>
            <EuiFlexGroup gutterSize="m">
                <EuiFlexItem>
                    <EuiButton onClick={() => {
                        const shouldClose = hasUnsavedChanges
                            ? window.confirm("Your rules have unsaved changes. Are you sure you want to discard them?")
                            : true;
                        if (!shouldClose) {
                            return;
                        }
                        onClose();
                        setRuleFormData([baseForm]);
                    }}>Close</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true} isDisabled={!hasUnsavedChanges} isLoading={isLoading} onClick={saveRuleHandler}>{"Update Rules"}</EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
        </EuiFlexGroup>}
    </EuiForm>
}
