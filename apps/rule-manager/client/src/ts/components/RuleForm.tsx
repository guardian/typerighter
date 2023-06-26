import {
  EuiButton,
  EuiCallOut,
  EuiFlexGroup,
  EuiFlexItem,
  EuiForm,
  EuiLoadingSpinner,
  EuiText,
  EuiToolTip
} from "@elastic/eui";
import React, {ReactElement, useEffect, useState} from "react"
import { RuleContent } from "./RuleContent";
import { RuleMetadata } from "./RuleMetadata";
import {DraftRule, RuleType, useRule} from "./hooks/useRule";
import {RuleHistory} from "./RuleHistory";
import styled from "@emotion/styled";
import {capitalize} from "lodash";

export type PartiallyUpdateRuleData = (partialReplacement: Partial<DraftRule>) => void;

export type FormError = { key: string; message: string };

export const baseForm = {
  ruleType: 'regex' as RuleType,
  tags: [] as string[],
  ignore: false,
} as DraftRule;

const SpinnerOverlay = styled.div`
  display: flex;
  justify-content: center;
`

const SpinnerOuter = styled.div`
  position: relative;
`;
const SpinnerContainer = styled.div`
  position: absolute;
  top: 10px;
`;

const emptyPatternFieldError = {key: 'pattern', message: 'A pattern is required'}

export const RuleForm = ({ruleId, onClose, onUpdate}: {
        ruleId: number | undefined,
        onClose: () => void,
        onUpdate: (id: number) => void
    }) => {
    const [showErrors, setShowErrors] = useState(false);
    const { isLoading, errors, rule, isPublishing, publishRule, fetchRule, updateRule, createRule, validateRule, publishValidationErrors, resetPublishValidationErrors, archiveRule } = useRule(ruleId);
    const [ruleFormData, setRuleFormData] = useState(rule?.draft ?? baseForm)
    const [ formErrors, setFormErrors ] = useState<FormError[]>([]);

    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (partialReplacement) => {
      setRuleFormData({ ...ruleFormData, ...partialReplacement});
    }

    useEffect(() => {
        if (rule) {
          setRuleFormData(rule.draft);
          rule.draft.id && validateRule(rule.draft.id);
        } else {
          setRuleFormData(baseForm);
          resetPublishValidationErrors();
        }
    }, [rule]);

    useEffect(() => {
      if(!ruleFormData.pattern) {
        setFormErrors([emptyPatternFieldError]);
      } else {
        setFormErrors([]);
      }
    }, [ruleFormData]);

    useEffect(() => {
        if(errors?.length === 0) {
            setShowErrors(false);
        }
    }, [errors])

    // We need the errors at the form level, so that we can prevent save etc. when there are errors
    // We need to be able to change the errors depending on which fields are invalid
    // We need to be able to show errors on a field by field basis

    const saveRuleHandler = async () => {
      if(formErrors.length > 0 || errors && errors.length > 0) {
          setShowErrors(true);
          return;
      }

      const response = await (ruleId ? updateRule(ruleFormData) : createRule(ruleFormData));

      if (response.status === "ok" && response.data.id) {
        onUpdate(response.data.id);
      }
    };

    const publishRuleHandler = async () => {
      if (!ruleId) {
        return;
      }
      await publishRule(ruleId);
      await fetchRule(ruleId);
      onUpdate(ruleId);
    }

    const PublishTooltip: React.FC<{ children: ReactElement }> = ({ children }) => {
        if (!publishValidationErrors) {
          return <>{children}</>
        }
        return <EuiToolTip content={!!publishValidationErrors &&
            <span>
                This rule can't be published:
                <br/>
                <br/>
                {publishValidationErrors?.map(error => <span key={error.key}>{`${capitalize(error.key)}: ${error.message}`}<br/></span>)}
            </span>
        }>
          {children}
        </EuiToolTip>
    }

    const archiveRuleHandler = () => {
        if (!ruleFormData?.id) {
          return;
        }

        archiveRule(ruleFormData.id).then(data => {
            if (data.status === 'ok'){
                setRuleFormData(baseForm);
                onClose();
            }
        })
    }

    const hasUnsavedChanges = ruleFormData !== rule?.draft;


    return <EuiForm component="form">
        {isLoading && <SpinnerOverlay><SpinnerOuter><SpinnerContainer><EuiLoadingSpinner /></SpinnerContainer></SpinnerOuter></SpinnerOverlay>}
        {<EuiFlexGroup  direction="column">
            <RuleContent ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} errors={formErrors} showErrors={showErrors}/>
            <RuleMetadata ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            {rule && <RuleHistory ruleHistory={rule.live} />}
            <EuiFlexGroup gutterSize="m">
                <EuiFlexItem>
                    <EuiButton onClick={() => {
                        const shouldClose = hasUnsavedChanges
                          ? window.confirm("Your rule has unsaved changes. Are you sure you want to discard them?")
                          : true;
                        if (!shouldClose) {
                          return;
                        }
                        onClose();
                        setRuleFormData(baseForm);
                    }}>Close</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true} isDisabled={!hasUnsavedChanges} isLoading={isLoading} onClick={saveRuleHandler}>{ruleId ? "Update Rule" : "Save Rule"}</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                  <PublishTooltip>
                    <EuiButton disabled={!ruleId || isLoading || !!publishValidationErrors} isLoading={isPublishing} fill={true} onClick={publishRuleHandler}>{"Publish"}</EuiButton>
                  </PublishTooltip>
                </EuiFlexItem>
            </EuiFlexGroup>
            {
                !ruleFormData.isArchived ? <EuiFlexItem grow={0}>
                    <EuiButton onClick={archiveRuleHandler} color={"danger"}>Archive Rule</EuiButton>
                </EuiFlexItem> : null
            }
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                {formErrors.map((error, index) => <EuiText key={index}>{`${error.message}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup>}
    </EuiForm>
}
