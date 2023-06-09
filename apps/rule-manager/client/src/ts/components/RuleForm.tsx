import {
  EuiButton,
  EuiCallOut,
  EuiFlexGroup,
  EuiFlexItem,
  EuiForm,
  EuiLoadingSpinner, EuiSpacer,
  EuiText, EuiToolTip
} from "@elastic/eui";
import React, {ReactElement, useEffect, useState} from "react"
import { RuleContent } from "./RuleContent";
import { RuleMetadata } from "./RuleMetadata";
import { createRule } from "./api/createRule";
import { updateRule } from "./api/updateRule";
import {DraftRule, RuleType, useRule} from "./hooks/useRule";
import {RuleHistory} from "./RuleHistory";
import styled from "@emotion/styled";
import {capitalize} from "lodash";

export type PartiallyUpdateRuleData = (partialReplacement: Partial<DraftRule>) => void;

export type FormError = { id: string; value: string };

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

const emptyPatternFieldError = {id: 'pattern', value: 'A pattern is required'}

export const RuleForm = ({ruleId, onClose}: {
        ruleId: number | undefined,
        onClose: () => void,
    }) => {
    const [showErrors, setShowErrors] = useState(false);
    const { isLoading, errors, rule, isPublishing, publishRule, fetchRule, validateRule, publishingErrors } = useRule(ruleId);
    const [ruleFormData, setRuleFormData] = useState(rule?.draft ?? baseForm)
    const [ formErrors, setFormErrors ] = useState<FormError[]>([]);

    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (partialReplacement) => {
      setRuleFormData({ ...ruleFormData, ...partialReplacement});
    }

    useEffect(() => {
        if (rule) {
          setRuleFormData(rule.draft);
          validateRule(rule.draft.id);
        } else {
          setRuleFormData(baseForm);
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

    const saveRuleHandler = () => {
        if(formErrors.length > 0 || errors?.length > 0) {
            setShowErrors(true);
            return;
        }

        (ruleId ? updateRule(ruleFormData) : createRule(ruleFormData))
            .then(data => {
                if (data.status === 'ok'){
                    setRuleFormData(baseForm);
                    onClose();
                } else {
                    setFormErrors([...formErrors, {id: `${data.status} error`, value: `${data.errorMessage} - try again or contact the Editorial Tools team.`}])
                }
            })
    }

    const publishRuleHandler = async () => {
      await publishRule(ruleId);
      await fetchRule(ruleId);
    }

    const PublishTooltip: React.FC<{ children: ReactElement }> = ({ children }) => {
        if (!publishingErrors) {
          return <>{children}</>
        }
        return <EuiToolTip content={!!publishingErrors &&
            <span>
                This rule can't be published:
                <br/>
                <br/>
                {publishingErrors?.map(error => <span>{`${capitalize(error.key)}: ${error.message}`}<br/></span>)}
            </span>
        }>
          {children}
        </EuiToolTip>
    }

    return <EuiForm component="form">
        {isLoading && <SpinnerOverlay><SpinnerOuter><SpinnerContainer><EuiLoadingSpinner /></SpinnerContainer></SpinnerOuter></SpinnerOverlay>}
        {<EuiFlexGroup  direction="column">
            <RuleContent ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} errors={formErrors} showErrors={showErrors}/>
            <RuleMetadata ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            {rule && <RuleHistory ruleHistory={rule.live} />}
            <EuiFlexGroup>
                <EuiFlexItem>
                    <EuiButton onClick={() => {
                        onClose();
                        setRuleFormData(baseForm);
                    }}>{ruleId ? "Discard Changes" : "Discard Rule"}</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true} onClick={saveRuleHandler}>{ruleId ? "Update Rule" : "Save Rule"}</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                  <PublishTooltip>
                    <EuiButton disabled={!ruleId || isLoading || !!publishingErrors} isLoading={isPublishing} fill={true} onClick={publishRuleHandler}>{"Publish"}</EuiButton>
                  </PublishTooltip>
                </EuiFlexItem>
            </EuiFlexGroup>
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                {formErrors.map((error, index) => <EuiText key={index}>{`${error.value}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup>}
    </EuiForm>
}
