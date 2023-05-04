import { EuiButton, EuiCallOut, EuiFlexGroup, EuiFlexItem, EuiForm, EuiSpacer, EuiText } from "@elastic/eui";
import React, { useEffect, useState } from "react"
import { RuleContent } from "./RuleContent";
import { RuleType } from "./RuleType";
import {RuleMetadata} from "./RuleMetadata";
import { createRule, transformRuleFormData } from "./helpers/createRule";

export type RuleType = 'regex' | 'languageToolXML';

export type RuleFormData = {
    ruleType: RuleType,
    pattern?: string,
    replacement?: string,
    category?: string,
    tags: string[],
    description?: string,
    ignore: boolean,
    forceRedRule?: boolean,
    advisoryRule?: boolean
}

export type PartiallyUpdateRuleData = (existing: RuleFormData, partialReplacement: Partial<RuleFormData>) => void;

export type FormError = { id: string; value: string }

export const RuleForm = ({fetchRules}: {fetchRules: () => Promise<void>}) => {
    const [createRuleFormOpen, setCreateRuleFormOpen] = useState(false);
    const [showErrors, setShowErrors] = useState(false);
    const [errors, setErrors] = useState<FormError[]>([]);

    const openCreateRuleForm = () => {
        setCreateRuleFormOpen(true);
    }
    const baseForm = {
        ruleType: 'regex' as RuleType,
        tags: [] as string[],
        ignore: false,
    }
    const [ruleData, setRuleData] = useState<RuleFormData>(baseForm);
    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (existing, partialReplacement) => {
        setRuleData({...existing, ...partialReplacement});
    }

    useEffect(() => {
        const emptyPatternFieldError = {id: 'pattern', value: 'A pattern is required'}
        setErrors(errors.filter(error => !(error.id === emptyPatternFieldError.id && error.value === emptyPatternFieldError.value)));
        if(!ruleData.pattern) {
            setErrors([...errors, emptyPatternFieldError]);
        }
    }, [ruleData])

    useEffect(() => {
        if(errors.length === 0) {
            setShowErrors(false);
        }
    }, [errors])

    // We need the errors at the form level, so that we can prevent save etc. when there are errors
    // We need to be able to change the errors depending on which fields are invalid
    // We need to be able to show errors on a field by field basis

    const saveRuleHandler = () => {

        if(errors.length > 0) {
            setShowErrors(true);
            return;
        }

        createRule(transformRuleFormData(ruleData))
            .then(response => response.json())
            .then(data => {
                setRuleData(baseForm);
                fetchRules();
            })
        setCreateRuleFormOpen(false);
    }

    return <EuiForm component="form">
        <EuiButton isDisabled={createRuleFormOpen} onClick={openCreateRuleForm}>Create Rule</EuiButton>
        <EuiSpacer />
        {createRuleFormOpen ? <EuiFlexGroup  direction="column">   
            <RuleContent ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData} errors={errors} showErrors={showErrors}/>
            <RuleType ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            <RuleMetadata ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            <EuiFlexGroup>
                <EuiFlexItem>
                    <EuiButton onClick={() => {
                        setCreateRuleFormOpen(false);
                        setRuleData(baseForm);
                    }}>Discard Rule</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true} onClick={saveRuleHandler}>Save Rule</EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                { errors.map(error => <EuiText>{`${error.value}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup> : null}
    </EuiForm>
}