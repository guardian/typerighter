import { EuiFormRow, EuiRadioGroup, EuiSwitch, EuiSwitchEvent } from "@elastic/eui";
import { css } from "@emotion/react";
import React, { useState } from "react"
import { RuleFormSection } from "./RuleFormSection"
import {LineBreak} from "./LineBreak";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";

export const RuleType = ({ruleData, partiallyUpdateRuleData}: {ruleData: RuleFormData, partiallyUpdateRuleData: PartiallyUpdateRuleData}) => {
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

    const onIsAdvisoryChange = (e: EuiSwitchEvent) => {
        setIsAdvisory(e.target.checked);
        partiallyUpdateRuleData(ruleData, {advisoryRule: e.target.checked})
    };
    
    return <RuleFormSection title="RULE TYPE">
        <LineBreak/>

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

        <LineBreak/>

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