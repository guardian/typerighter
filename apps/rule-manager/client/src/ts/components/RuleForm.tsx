import { EuiButton, EuiCallOut, EuiFlexGroup, EuiFlexItem, EuiForm, EuiSpacer, EuiText } from "@elastic/eui";
import React, { Dispatch, SetStateAction, useContext, useEffect, useState } from "react"
import { RuleContent } from "./RuleContent";
import { RuleType } from "./RuleType";
import { RuleMetadata } from "./RuleMetadata";
import { createRule } from "./api/createRule";
import { Rule } from "./RulesTable";
import { FeatureSwitchesContext } from "./context/featureSwitches";
import { updateRule } from "./api/updateRule";

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
    advisoryRule?: boolean,
    id?: number
}

export type PartiallyUpdateRuleData = (existing: RuleFormData, partialReplacement: Partial<RuleFormData>) => void;

export type FormError = { id: string; value: string }

export const baseForm = {
    ruleType: 'regex' as RuleType,
    tags: [] as string[],
    ignore: false,
}

export const RuleForm = ({onRuleUpdate, ruleData, setRuleData, createRuleFormOpen, setCreateRuleFormOpen, updateMode, setUpdateMode}: {
        onRuleUpdate: () => Promise<void>, 
        ruleData: RuleFormData,
        setRuleData: Dispatch<SetStateAction<RuleFormData>>,
        createRuleFormOpen: boolean, 
        setCreateRuleFormOpen: Dispatch<SetStateAction<boolean>>,
        updateMode: boolean, 
        setUpdateMode: Dispatch<SetStateAction<boolean>>
    }) => {
    const [showErrors, setShowErrors] = useState(false);
    const [errors, setErrors] = useState<FormError[]>([]);

    const openCreateRuleForm = () => {
        setRuleData(baseForm);
        setCreateRuleFormOpen(true);
        setUpdateMode(false);
    }
    
    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (existing, partialReplacement) => {
        setRuleData({...existing, ...partialReplacement});
    }

    useEffect(() => {
        const emptyPatternFieldError = {id: 'pattern', value: 'A pattern is required'}
        if(!ruleData.pattern) {
            setErrors([emptyPatternFieldError]);
        } else {
            setErrors([]);
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

        if (updateMode){
            updateRule(ruleData)
                .then(async response => {
                    return {
                        rule: await response.json(),
                        status: response.status,
                        statusText: response.statusText
                    }
                })
                .then(data => {
                    if (data.status === 200){
                        setRuleData(baseForm);
                        onRuleUpdate();
                        setCreateRuleFormOpen(false);
                    } else {
                        setErrors([...errors, {id: `${data.status} error`, value: `${data.statusText} - try again or contact the Editorial Tools team.`}])
                    }
                })
        } else {
            // Create new rule rather than updating existing rule
            createRule(ruleData)
                .then(async response => {
                    return {
                        rule: await response.json(),
                        status: response.status,
                        statusText: response.statusText
                    }
                })
                .then(data => {
                    if (data.status === 200) {
                        setRuleData(baseForm);
                        onRuleUpdate();
                    } else {
                        setErrors([...errors, {id: `${data.status} error`, value: `${data.statusText} - try again or contact the Editorial Tools team.`}])
                    }
                })
            setCreateRuleFormOpen(false);
        }
    }

    const { getFeatureSwitchValue } = useContext(FeatureSwitchesContext);

    return <EuiForm component="form">
        {getFeatureSwitchValue("create-and-edit") && <EuiButton isDisabled={createRuleFormOpen} onClick={openCreateRuleForm}>Create Rule</EuiButton>}
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
                    }}>{updateMode ? "Discard Changes" : "Discard Rule"}</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true} onClick={saveRuleHandler}>{updateMode ? "Update Rule" : "Save Rule"}</EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                { errors.map((error, index) => <EuiText key={index}>{`${error.value}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup> : null}
    </EuiForm>
}
