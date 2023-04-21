import { EuiRadioGroup } from "@elastic/eui";
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
    
    return <RuleFormSection title="RULE TYPE">
        <EuiRadioGroup
            options={[
                {
                    id: "regex",
                    label: 'Regex',
                },
                {
                    id: "languageTool",
                    label: 'LanguageTool',
                },
            ]}
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