import {
  EuiFieldText,
  EuiFlexItem,
  EuiFormLabel,
  EuiFormRow,
  EuiMarkdownEditor,
  EuiRadioGroup,
  EuiSpacer,
  EuiTextArea,
  getDefaultEuiMarkdownPlugins,
} from "@elastic/eui";
import { css } from "@emotion/react";
import React from "react";
import { RuleFormSection } from "./RuleFormSection";
import { LineBreak } from "./LineBreak";
import { PartiallyUpdateRuleData } from "./RuleForm";
import { Label } from "./Label";
import { DraftRule, RuleType } from "./hooks/useRule";
import { LastUpdated } from "./LastUpdated";

type RuleTypeOption = {
  id: RuleType;
  label: string;
};

const { parsingPlugins, processingPlugins, uiPlugins } =
  getDefaultEuiMarkdownPlugins({ exclude: ["tooltip"] });

export const RuleContent = ({
  ruleData,
  partiallyUpdateRuleData,
  showErrors,
}: {
  ruleData: DraftRule;
  partiallyUpdateRuleData: PartiallyUpdateRuleData;
  showErrors: boolean;
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
  ];
  const TextField =
    ruleData.ruleType === "languageToolXML" ? EuiTextArea : EuiFieldText;

  return (
    <RuleFormSection
      title="RULE CONTENT"
      additionalInfo={
        ruleData.updatedAt && <LastUpdated lastUpdated={ruleData.updatedAt} />
      }
    >
      <LineBreak />
      <EuiFlexItem>
        <EuiFormLabel>Rule type</EuiFormLabel>
        <EuiRadioGroup
          options={ruleTypeOptions}
          idSelected={ruleData.ruleType}
          onChange={(ruleType) => {
            partiallyUpdateRuleData({ ruleType: ruleType as RuleType });
          }}
          css={css`
            display: flex;
            gap: 1rem;
            align-items: flex-end;
          `}
        />
        <EuiSpacer size="s" />
        <EuiFormRow
          label={<Label text="Pattern" required={true} />}
          isInvalid={showErrors && !ruleData.pattern}
          fullWidth={true}
        >
          <TextField
            value={ruleData.pattern || ""}
            onChange={(_) =>
              partiallyUpdateRuleData({ pattern: _.target.value })
            }
            required={true}
            isInvalid={showErrors && !ruleData.pattern}
            fullWidth={true}
          />
        </EuiFormRow>
        <EuiFormRow
          label="Replacement"
          helpText="What is the ideal term as per the house style?"
          fullWidth={true}
        >
          <EuiFieldText
            value={ruleData.replacement || ""}
            onChange={(_) =>
              partiallyUpdateRuleData({ replacement: _.target.value })
            }
            fullWidth={true}
          />
        </EuiFormRow>
        <EuiFormRow
          label="Description"
          helpText="What will the users see in Composer?"
          fullWidth={true}
        >
          <EuiMarkdownEditor
            value={ruleData.description || ""}
            onChange={(value) =>
              partiallyUpdateRuleData({ description: value })
            }
            aria-label={"Description editor"}
            parsingPluginList={parsingPlugins}
            processingPluginList={processingPlugins}
            uiPlugins={uiPlugins}
          />
        </EuiFormRow>
      </EuiFlexItem>
    </RuleFormSection>
  );
};
