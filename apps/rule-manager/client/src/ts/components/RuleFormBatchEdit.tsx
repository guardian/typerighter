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

import { ReasonModal } from "./modals/Reason";
import {useBatchRules} from "./hooks/useBatchRules";
import {RuleFormSection} from "./RuleFormSection";

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

// const emptyPatternFieldError = {key: 'pattern', message: 'A pattern is required'}

export const RuleFormBatchEdit = ({ruleIds, onClose, onUpdate}: {
    ruleIds: number[] | undefined,
    onClose: () => void,
    onUpdate: (ids: number) => void
}) => {
    const [showErrors, setShowErrors] = useState(false);
    const { isLoading, errors, rules, fetchRules, updateRules } = useBatchRules(ruleIds);
    const [ruleFormData, setRuleFormData] = useState(rules) // (RuleData | undefined)[]
    const [ formErrors, setFormErrors ] = useState<FormError[]>([]);
    const [isReasonModalVisible, setIsReasonModalVisible] = useState(false);

    const partiallyUpdateRuleData: PartiallyUpdateRuleData = (partialReplacement) => {
        setRuleFormData({ ...ruleFormData, ...partialReplacement});
    }

    useEffect(() => {
        if (rules) {
            setRuleFormData(rules);
            // rules.draft.id && validateRules(rules.draft.id);
        } else {
            setRuleFormData([]);
            // resetPublishValidationErrors();
        }
    }, [rules]);

    // useEffect(() => {
    //     if(!ruleFormData.pattern) {
    //         setFormErrors([emptyPatternFieldError]);
    //     } else {
    //         setFormErrors([]);
    //     }
    // }, [ruleFormData]);

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

        const response = await updateRules(ruleFormData);

        if (response.status === "ok" && response.data.id) {
            onUpdate(response.data.id);
        }
    };

    // const maybePublishRuleHandler = () => {
    //     if (rules?.live.length) {
    //         setIsReasonModalVisible(true);
    //     } else {
    //         publishRuleHandler("First published");
    //     }
    // }

    // const publishRuleHandler = async (reason: string) => {
    //     if (!ruleIds) {
    //         return;
    //     }
    //     await publishRule(ruleIds, reason);
    //     await fetchRule(ruleIds);
    //     if (isReasonModalVisible) {
    //         setIsReasonModalVisible(false);
    //     };
    //     onUpdate(ruleIds);
    // }
    //
    // const PublishTooltip: React.FC<{ children: ReactElement }> = ({ children }) => {
    //     if (!publishValidationErrors) {
    //         return <>{children}</>
    //     }
    //     return <EuiToolTip content={!!publishValidationErrors &&
    //         <span>
    //             This rule can't be published:
    //             <br/>
    //             <br/>
    //             {publishValidationErrors?.map(error => <span key={error.key}>{`${capitalize(error.key)}: ${error.message}`}<br/></span>)}
    //         </span>
    //     }>
    //         {children}
    //     </EuiToolTip>
    // }

    // const archiveRuleHandler = () => {
    //     if (!ruleFormData?.id) {
    //         return;
    //     }
    //
    //     archiveRule(ruleFormData.id).then(data => {
    //         if (data.status === 'ok'){
    //             setRuleFormData(baseForm);
    //             onClose();
    //         }
    //     })
    // }

    const hasUnsavedChanges = ruleFormData !== rules?.draft;


    return <EuiForm component="form">
        {isLoading && <SpinnerOverlay><SpinnerOuter><SpinnerContainer><EuiLoadingSpinner /></SpinnerContainer></SpinnerOuter></SpinnerOverlay>}
        {<EuiFlexGroup  direction="column">
            <RuleFormSection title="RULE CONTENT"/>
            <RuleMetadata ruleData={ruleFormData} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            {/*{rule && <RuleHistory ruleHistory={rule.live} />}*/}
            <EuiFlexGroup gutterSize="m">
                <EuiFlexItem>
                    <EuiButton onClick={() => {
                        const shouldClose = hasUnsavedChanges
                            ? window.confirm("Your rules have unsaved changes. Are you sure you want to discard them?")
                            : true;
                        if (!shouldClose) {
                            return;
                        }
                        onClose();
                        setRuleFormData(baseForm);
                    }}>Close</EuiButton>
                </EuiFlexItem>
                <EuiFlexItem>
                    <EuiButton fill={true} isDisabled={!hasUnsavedChanges} isLoading={isLoading} onClick={saveRuleHandler}>{ruleIds ? "Update Rule" : "Save Rule"}</EuiButton>
                </EuiFlexItem>
                {/*<EuiFlexItem>*/}
                {/*    <PublishTooltip>*/}
                {/*        <EuiButton fill={true} disabled={!ruleIds || isLoading || !!publishValidationErrors} isLoading={isPublishing} onClick={maybePublishRuleHandler}>{"Publish"}</EuiButton>*/}
                {/*    </PublishTooltip>*/}
                {/*</EuiFlexItem>*/}
            </EuiFlexGroup>
            {/*{*/}
            {/*    !ruleFormData.isArchived ? <EuiFlexItem grow={0}>*/}
            {/*        <EuiButton onClick={archiveRuleHandler} color={"danger"}>Archive Rule</EuiButton>*/}
            {/*    </EuiFlexItem> : null*/}
            {/*}*/}
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                {formErrors.map((error, index) => <EuiText key={index}>{`${error.message}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup>}
        {/*{isReasonModalVisible && <ReasonModal onClose={() => setIsReasonModalVisible(false)} onSubmit={publishRuleHandler} isLoading={isPublishing} />}*/}
    </EuiForm>
}
