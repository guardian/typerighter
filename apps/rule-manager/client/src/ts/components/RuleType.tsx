import { EuiFormRow, EuiRadioGroup, EuiSwitch, EuiSwitchEvent } from "@elastic/eui";
import React, { useState } from "react"
import { RuleFormSection } from "./RuleFormSection"
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";

export const RuleType = ({ruleData, partiallyUpdateRuleData}: {ruleData: RuleFormData, partiallyUpdateRuleData: PartiallyUpdateRuleData}) => {
    const [isAdvisory, setIsAdvisory] = useState(false);

    const onIsAdvisoryChange = (e: EuiSwitchEvent) => {
        setIsAdvisory(e.target.checked);
        partiallyUpdateRuleData(ruleData, {advisoryRule: e.target.checked})
    };
    
    return <RuleFormSection title="RULE TYPE">
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