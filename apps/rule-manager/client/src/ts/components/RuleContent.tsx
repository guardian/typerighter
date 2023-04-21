import { EuiFieldText, EuiFlexItem, EuiForm, EuiFormRow, EuiRadioGroup } from "@elastic/eui"
import { css } from "@emotion/react";
import React, { useState } from "react"
import { RuleFormSection } from "./RuleFormSection";

export const RuleContent = () => {
    const ruleTypeOptions = [
        {
            id: "regex",
            label: 'Regex',
        },
        {
            id: "languageTool",
            label: 'LanguageTool',
        },
    ]
    const [ruleTypeSelected, setRuleTypeSelected] = useState(ruleTypeOptions[0].id);

    return <RuleFormSection title="RULE CONTENT">
            <EuiFormRow
                label="Replacement"
                helpText="What is the ideal term as per the house style?"
            >
                <EuiFieldText/>
            </EuiFormRow>
            <EuiFormRow
                label="Description"
                helpText="What will the users see in Composer?"
            >
                <EuiFieldText/>
            </EuiFormRow>
            <EuiFormRow
                label="Pattern"
                helpText=""
            >
                <EuiFieldText/>
            </EuiFormRow>
            <EuiRadioGroup
                options={ruleTypeOptions}
                idSelected={ruleTypeSelected}
                onChange={(id)=> {
                    setRuleTypeSelected(id)
                }}
                css={css`
                    flex-direction: row;
                    display: flex;
                    gap: 1rem;
                    align-items: flex-end;
                `}
            />
    </RuleFormSection>
}