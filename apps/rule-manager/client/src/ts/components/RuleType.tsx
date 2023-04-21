import { EuiFormRow, EuiRadioGroup, EuiSwitch } from "@elastic/eui";
import { css } from "@emotion/react";
import React, { useState } from "react"
import { RuleFormSection } from "./RuleFormSection"

export const RuleType = () => {
    const ruleTypeOptions = [
        {
            id: "actionable",
            label: 'Actionable (Amend or OK)',
        },
        {
            id: "ambiguous",
            label: 'Ambiguous (Review)',
        },
    ]
    const [ruleTypeSelected, setRuleTypeSelected] = useState(ruleTypeOptions[0].id);
    const [isAdvisory, setIsAdvisory] = useState(false);

    const onIsAdvisoryChange = (e) => {
        setIsAdvisory(e.target.checked);
    };
    
    return <RuleFormSection title="RULE TYPE">
        <EuiFormRow
            label="Pattern"
        >
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
        </EuiFormRow>
        <EuiFormRow
            helpText="Flag only once per session"
        >
            <EuiSwitch
                label="Advisory rule"
                checked={isAdvisory}
                onChange={(e) => onIsAdvisoryChange(e)}
            />
        </EuiFormRow>
    </RuleFormSection>
}