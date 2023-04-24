import { EuiButton, EuiFlexGroup, EuiFlexItem, EuiForm, EuiSpacer } from "@elastic/eui";
import React, { useEffect, useState } from "react"
import { RuleContent } from "./RuleContent";
import { RuleType } from "./RuleType";
import {RuleMetadata} from "./RuleMetadata";

export type RuleType = 'regex' | 'languageTool';

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

export const RuleForm = () => {
    const [createRuleFormOpen, setCreateRuleFormOpen] = useState(false);
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
    useEffect(() => console.log(ruleData), [ruleData])


    return <EuiForm component="form">
        <EuiButton isDisabled={createRuleFormOpen} onClick={openCreateRuleForm}>Create Rule</EuiButton>
        <EuiSpacer />
        {createRuleFormOpen ? <EuiFlexGroup  direction="column">   
            <RuleContent ruleData={ruleData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            <RuleType />
            <RuleMetadata />
            <EuiFlexGroup>
                <EuiFlexItem>
                    <EuiButton onClick={() => {
                        setCreateRuleFormOpen(false);
                        setRuleData(baseForm);
                    }}>Discard Rule</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true}>Save Rule</EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
        </EuiFlexGroup> : null}
    </EuiForm>
}