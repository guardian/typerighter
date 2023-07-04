import {EuiFieldText, EuiFlexItem, EuiFormLabel, EuiFormRow, EuiRadioGroup, EuiSpacer, EuiTextArea} from "@elastic/eui"
import {css} from "@emotion/react";
import React from "react"
import {RuleFormSection} from "./RuleFormSection";
import {LineBreak} from "./LineBreak";
import {PartiallyUpdateRuleData} from "./RuleForm";
import {Label} from "./Label";
import {DraftRule, RuleData, RuleType} from "./hooks/useRule";
import { RuleDataLastUpdated } from "./RuleDataLastUpdated";

type RuleTypeOption = {
  id: RuleType,
  label: string,
}

export const RuleContent = ({ruleData, ruleFormData, isLoading, errors, partiallyUpdateRuleData, showErrors}: {
        ruleData: RuleData | undefined,
        ruleFormData: DraftRule,
        partiallyUpdateRuleData: PartiallyUpdateRuleData,
        showErrors: boolean,
        isLoading: boolean,
        errors: string | undefined
    }) => {

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
    const TextField = ruleFormData.ruleType === "languageToolXML" ? EuiTextArea : EuiFieldText;

    return <RuleFormSection
      title="RULE CONTENT"
      additionalInfo={ruleData && <RuleDataLastUpdated ruleData={ruleData} isLoading={isLoading} hasErrors={!!errors} />}>
        <LineBreak/>
        <EuiFlexItem>
          <EuiFormLabel>Rule type</EuiFormLabel>
            <EuiRadioGroup
                options={ruleTypeOptions}
                idSelected={ruleFormData.ruleType}
                onChange={(ruleType) => {
                    partiallyUpdateRuleData({ruleType: ruleType as RuleType});
                }}
                css={css`
                        display: flex;
                        gap: 1rem;
                        align-items: flex-end;
                    `}
            />
            <EuiSpacer size="s" />
            <EuiFormRow
                label={<Label text='Pattern' required={true}/>}
                isInvalid={showErrors && !ruleFormData.pattern}
                fullWidth={true}
            >
                <TextField
                    value={ruleFormData.pattern || ""}
                    onChange={(_ => partiallyUpdateRuleData({pattern: _.target.value}))}
                    required={true}
                    isInvalid={showErrors && !ruleFormData.pattern}
                    fullWidth={true}
                />
            </EuiFormRow>
            <EuiFormRow
                label="Replacement"
                helpText="What is the ideal term as per the house style?"
                fullWidth={true}
            >
                <EuiFieldText value={ruleFormData.replacement || ""}
                              onChange={(_ => partiallyUpdateRuleData({replacement: _.target.value}))}
                              fullWidth={true} />
            </EuiFormRow>
            <EuiFormRow
                label="Description"
                helpText="What will the users see in Composer?"
                fullWidth={true}
            >
                <EuiFieldText value={ruleFormData.description || ""}
                              onChange={(_ => partiallyUpdateRuleData({description: _.target.value}))}
                              fullWidth={true} />
            </EuiFormRow>
        </EuiFlexItem>
    </RuleFormSection>
}
