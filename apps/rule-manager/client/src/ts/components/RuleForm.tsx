import {
  EuiButton,
  EuiCallOut,
  EuiFlexGroup,
  EuiFlexItem,
  EuiForm,
  EuiLoadingSpinner,
  EuiText
} from "@elastic/eui";
import React, { Dispatch, SetStateAction, useEffect, useState } from "react"
import { RuleContent } from "./RuleContent";
import { RuleType } from "./RuleType";
import { RuleMetadata } from "./RuleMetadata";
import { createRule } from "./api/createRule";
import { updateRule } from "./api/updateRule";
import {DraftRule, useRule} from "./hooks/useRule";
import {RuleHistory} from "./RuleHistory";

export type PartiallyUpdateRuleData = (partialReplacement: Partial<DraftRule>) => void;

export type FormError = { id: string; value: string };

export const baseForm = {
  ruleType: 'regex' as RuleType,
  tags: [] as string[],
  ignore: false,
} as DraftRule;

export const RuleForm = ({ruleId, setCurrentRuleId, onClose}: {
        ruleId: number | undefined,
        onClose: () => void,
        setCurrentRuleId: Dispatch<SetStateAction<number | null>>,
    }) => {
    const [showErrors, setShowErrors] = useState(false);
    const { isLoading, errors, rule } = useRule(ruleId);
    const [ruleFormData, setRuleFormData] = useState(rule?.draft ?? baseForm)
    const [ formErrors, setFormErrors ] = useState<FormError[]>([]);

    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (partialReplacement) => {
      setRuleFormData({ ...ruleFormData, ...partialReplacement});
    }

    useEffect(() => {
        const emptyPatternFieldError = {id: 'pattern', value: 'A pattern is required'}
        if (rule) {
          setRuleFormData(rule.draft);
        }
        if(!ruleFormData.pattern) {
          setFormErrors([emptyPatternFieldError]);
        } else {
          setFormErrors([]);
        }
    }, [rule])

    useEffect(() => {
        if(errors?.length === 0) {
            setShowErrors(false);
        }
    }, [errors])

    // We need the errors at the form level, so that we can prevent save etc. when there are errors
    // We need to be able to change the errors depending on which fields are invalid
    // We need to be able to show errors on a field by field basis

    const saveRuleHandler = () => {
        if(errors?.length > 0) {
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

    if (isLoading) {
      return <EuiLoadingSpinner />
    }

    return <EuiForm component="form">
        {<EuiFlexGroup  direction="column">
            <RuleContent ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} errors={formErrors} showErrors={showErrors}/>
            <RuleMetadata ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            {rule && <RuleHistory ruleHistory={rule.history} />}
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
            </EuiFlexGroup>
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                {formErrors.map((error, index) => <EuiText key={index}>{`${error.value}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup>}
    </EuiForm>
}
