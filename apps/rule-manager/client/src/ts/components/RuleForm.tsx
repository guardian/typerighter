import { EuiButton, EuiFlexGroup, EuiFlexItem, EuiForm, EuiSpacer } from "@elastic/eui";
import React, { useReducer, useState } from "react"
import { RuleContent } from "./RuleContent";
import { RuleType } from "./RuleType";
import {RuleMetadata} from "./RuleMetadata";

export const RuleForm = () => {
    const [createRuleFormOpen, setCreateRuleFormOpen] = useState(false);
    const openCreateRuleForm = () => {
        setCreateRuleFormOpen(true);
    }
    return <EuiForm component="form">
        <EuiButton isDisabled={createRuleFormOpen} onClick={openCreateRuleForm}>Create Rule</EuiButton>
        <EuiSpacer />
        {createRuleFormOpen ? <EuiFlexGroup  direction="column">   
            <RuleContent />
            <RuleType />
            <RuleMetadata />
            <EuiFlexGroup>
                <EuiFlexItem>
                    <EuiButton onClick={() => setCreateRuleFormOpen(false)}>Discard Rule</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true}>Save Rule</EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
        </EuiFlexGroup> : null}
    </EuiForm>
}