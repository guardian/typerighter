import {EuiFieldText, EuiFlexItem, EuiFormRow, EuiRadioGroup} from "@elastic/eui"
import {css} from "@emotion/react";
import React, {useState} from "react"
import {RuleFormSection} from "./RuleFormSection";
import {LineBreak} from "./LineBreak";
import {FormError, PartiallyUpdateRuleData, RuleFormData, RuleType} from "./RuleForm";
import {Label} from "./Label";

export const RuleContent = ({ruleData, partiallyUpdateRuleData, errors, showErrors}: {
        ruleData: RuleFormData,
        partiallyUpdateRuleData: PartiallyUpdateRuleData,
        errors: FormError[],
        showErrors: boolean
    }) => {
    type RuleTypeOption = {
        id: RuleType,
        label: string,
    }
    const ruleTypeOptions: RuleTypeOption[] = [
        {
            id: "regex",
            label: "Regex",
        },
        {
            id: "languageToolXML",
            label: "LanguageTool",
        },
    ]
    const [ruleTypeSelected, setRuleTypeSelected] = useState(ruleTypeOptions[0].id);

    return <RuleFormSection title="RULE CONTENT">
        <LineBreak/>
        <EuiFlexItem>
            <EuiFormRow
                label={<Label text='Pattern' required={true}/>}
                isInvalid={showErrors && !ruleData.pattern}
            >
                <EuiFieldText
                    value={ruleData.pattern}
                    onChange={(_ => partiallyUpdateRuleData(ruleData, {pattern: _.target.value}))}
                    required={true}
                    isInvalid={showErrors && !ruleData.pattern}
                />
            </EuiFormRow>
            <EuiFormRow
                label="Replacement"
                helpText="What is the ideal term as per the house style?"
            >
                <EuiFieldText value={ruleData.replacement}
                              onChange={(_ => partiallyUpdateRuleData(ruleData, {replacement: _.target.value}))}/>
            </EuiFormRow>
            <EuiFormRow
                label="Description"
                helpText="What will the users see in Composer?"
            >
                <EuiFieldText value={ruleData.description}
                              onChange={(_ => partiallyUpdateRuleData(ruleData, {description: _.target.value}))}/>
            </EuiFormRow>
            <EuiRadioGroup
                options={ruleTypeOptions}
                idSelected={ruleTypeSelected}
                onChange={(ruleType) => {
                    setRuleTypeSelected(ruleType as RuleType);
                    partiallyUpdateRuleData(ruleData, {ruleType: ruleType as RuleType});
                }}
                css={css`
                        flex-direction: row;
                        display: flex;
                        gap: 1rem;
                        align-items: flex-end;
                        margin-top: 8px;
                    `}
            />
        </EuiFlexItem>
    </RuleFormSection>
}