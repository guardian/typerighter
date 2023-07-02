import {
    EuiButton,
    EuiCallOut,
    EuiFlexGroup,
    EuiFlexItem,
    EuiForm,
    EuiLoadingSpinner,
    EuiText,
} from "@elastic/eui";
import React, {ReactElement, useEffect, useState} from "react"
import { RuleMetadata } from "./RuleMetadata";
import {DraftRule} from "./hooks/useRule";

import {useBatchRules} from "./hooks/useBatchRules";
import {RuleFormSection} from "./RuleFormSection";
import {TagMap} from "./hooks/useTags";
import {baseForm, FormError, PartiallyUpdateRuleData, SpinnerContainer, SpinnerOuter, SpinnerOverlay} from "./RuleForm";

export const RuleFormBatchEdit = ({tags, ruleIds, onClose, onUpdate}: {
    tags: TagMap,
    ruleIds: number[],
    onClose: () => void,
    onUpdate: (ids: number) => void
}) => {
    const [showErrors, setShowErrors] = useState(false);
    const { isLoading, errors, rules, fetchRules, updateRules } = useBatchRules(ruleIds);
    const [ruleFormData, setRuleFormData] = useState<DraftRule[]>([baseForm])
    const [ formErrors, setFormErrors ] = useState<FormError[]>([])

    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (partialReplacement) => {
        console.log('partial replacement', partialReplacement)
        const updatedRules = ruleFormData.map(rule => ({
            ...rule,
            ...partialReplacement
        })) as DraftRule[]
        setRuleFormData(updatedRules);
    };

    useEffect(() => {
        if (rules) {
            setRuleFormData(rules.map(rule => rule.draft) as DraftRule[]);
            // rules.draft.id && validateRules(rules.draft.id);
            }
    }, [rules]);

    useEffect(() => {
        if(errors?.length === 0) {
            setShowErrors(false);
        }
    }, [errors])

    // We need the errors at the form level, so that we can prevent save etc. when there are errors
    // We need to be able to change the errors depending on which fields are invalid
    // We need to be able to show errors on a field by field basis

    const saveRuleHandler = async () => {
        if(formErrors.length > 0 || errors && errors.length > 0) {
            setShowErrors(true);
            return;
        }

        const response = await updateRules(ruleFormData as DraftRule[]);

        if (response.status === "ok" && response.data.id) {
            onUpdate(response.data.id);
        }

        console.log('rule form data', ruleFormData.map(data => data.tags))
        console.log('rules', rules && rules.map(rule => rule.draft.tags))
    };

    const hasUnsavedChanges = rules && ruleFormData.some((data, index) => data !== rules[index].draft);

    return <EuiForm component="form">
        {isLoading && <SpinnerOverlay><SpinnerOuter><SpinnerContainer><EuiLoadingSpinner /></SpinnerContainer></SpinnerOuter></SpinnerOverlay>}
        {<EuiFlexGroup  direction="column">
            <RuleFormSection title="RULE CONTENT"/>
            <RuleMetadata tags={tags} ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
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
                    <EuiButton fill={true} isDisabled={!hasUnsavedChanges} isLoading={isLoading} onClick={saveRuleHandler}>{"Update Rule"}</EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                {formErrors.map((error, index) => <EuiText key={index}>{`${error.message}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup>}
    </EuiForm>
}
