import {
  EuiButton,
  EuiCallOut,
  EuiFlexGroup,
  EuiFlexItem,
  EuiForm,
  EuiText,
  EuiToolTip
} from "@elastic/eui";
import React, {ReactElement, useEffect, useState} from "react"
import { RuleContent } from "./RuleContent";
import {DraftRule, RuleType, useRule} from "./hooks/useRule";
import {RuleHistory} from "./RuleHistory";
import styled from "@emotion/styled";
import {capitalize} from "lodash";
import { ReasonModal } from "./modals/Reason";
import {TagMap} from "./hooks/useTags";
import { useDebouncedValue } from "./hooks/useDebounce";
import {RuleStatus} from "./RuleStatus";
import {LineBreak} from "./LineBreak";
import {CategorySelector} from "./CategorySelector";
import {TagsSelector} from "./TagsSelector";
import {RuleFormSection} from "./RuleFormSection";

export type PartiallyUpdateRuleData = (partialReplacement: Partial<DraftRule>) => void;

export type FormError = { key: string; message: string };

export const baseForm = {
  ruleType: 'regex' as RuleType,
  tags: [] as number[],
  ignore: false,
} as DraftRule;

export const SpinnerOverlay = styled.div`
  display: flex;
  justify-content: center;
`

export const SpinnerOuter = styled.div`
  position: relative;
`;
export const SpinnerContainer = styled.div`
  position: absolute;
  top: 10px;
`;

const formDebounceMs = 1000;
export const emptyPatternFieldError = {key: 'pattern', message: 'A pattern is required'}

export const RuleForm = ({tags, ruleId, onClose, onUpdate}: {
        tags: TagMap,
        ruleId: number | undefined,
        onClose: () => void,
        onUpdate: (id: number) => void
    }) => {
    const [showErrors, setShowErrors] = useState(false);
    const { isLoading, errors, rule, isPublishing, publishRule, updateRule, createRule, validateRule, publishValidationErrors, resetPublishValidationErrors, archiveRule, unarchiveRule, unpublishRule, ruleState } = useRule(ruleId);
    const [ruleFormData, setRuleFormData] = useState(rule?.draft ?? baseForm)
    const debouncedFormData = useDebouncedValue(ruleFormData, 1000);
    const [formErrors, setFormErrors] = useState<FormError[]>([]);
    const [isReasonModalVisible, setIsReasonModalVisible] = useState(false);

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
        if (!errors && !formErrors.length) {
            setShowErrors(false);
        }
    }, [errors, formErrors])

    /**
     * Automatically save the form data when it changes. Debounces saves.
     */
    useEffect(() => {
      if (debouncedFormData === rule?.draft || debouncedFormData === baseForm) {
        return;
      }

      if (formErrors.length > 0 || errors) {
          setShowErrors(true);
          return;
      }

      ruleId ? updateRule(ruleFormData) : createRule(ruleFormData)
    }, [debouncedFormData]);

    useEffect(() => {
      rule?.draft.id && onUpdate(rule.draft.id);
    }, [rule?.draft.id]);

    const maybePublishRuleHandler = () => {
      if (rule?.live.length) {
        setIsReasonModalVisible(true);
      } else {
        publishRuleHandler("First published");
      }
    }

    const publishRuleHandler = async (reason: string) => {
      const isDraftOrLive = (ruleState === 'draft' || ruleState === 'live')
      if (!ruleId || !isDraftOrLive) {
        return;
      }
      await publishRule(ruleId, reason);
      if (isReasonModalVisible) {
        setIsReasonModalVisible(false);
      }
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

    const archiveRuleHandler = async () => {
        if (!ruleId || ruleState !== 'draft') {
          return;
        }

        await archiveRule(ruleId);
        onUpdate(ruleId);
    }

    const unarchiveRuleHandler = async () => {
        if (!ruleId || ruleState !== 'archived') {
            return;
        }

        await unarchiveRule(ruleId);
        onUpdate(ruleId);
    }

    const unpublishRuleHandler = async () => {
        if (!ruleId || ruleState !== 'live') {
            return;
        }

        await unpublishRule(ruleId);
        onUpdate(ruleId);
    }

    const hasUnsavedChanges = ruleFormData !== rule?.draft;
    const canEditRuleContent = ruleState === 'draft' || ruleState === 'live';

    return <EuiForm component="form">
        {<EuiFlexGroup gutterSize="m" direction="column">
            <RuleStatus ruleData={rule} />
            <RuleContent isLoading={isLoading} errors={errors} ruleData={rule} ruleFormData={ruleFormData}  partiallyUpdateRuleData={partiallyUpdateRuleData} showErrors={showErrors}/>
            <RuleFormSection title="RULE METADATA">
              <LineBreak/>
              <CategorySelector currentCategory={ruleFormData.category} partiallyUpdateRuleData={partiallyUpdateRuleData} />
              <TagsSelector tags={tags} selectedTagIds={ruleFormData.tags} partiallyUpdateRuleData={partiallyUpdateRuleData} />
            </RuleFormSection>

            {rule && <RuleHistory ruleHistory={rule.live} />}
            <EuiFlexGroup gutterSize="m">
            {
                canEditRuleContent &&
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
            }
            {
                ruleState === 'archived' &&
                    <EuiFlexItem>
                        <EuiButton onClick={unarchiveRuleHandler} color={"danger"} disabled={!ruleId || isLoading}>
                            Unarchive Rule
                        </EuiButton>
                    </EuiFlexItem>
            }
            {
                ruleState === 'draft' &&
                    <EuiFlexItem>
                        <EuiButton onClick={archiveRuleHandler} color={"danger"} disabled={!ruleId || isLoading}>
                            Archive Rule
                        </EuiButton>
                    </EuiFlexItem>
            }
            {
                ruleState === 'live' &&
                    <EuiFlexItem>
                        <EuiButton onClick={unpublishRuleHandler} color={"danger"} disabled={!ruleId || isLoading}>
                            Unpublish Rule
                        </EuiButton>
                    </EuiFlexItem>
            }
            {
                canEditRuleContent &&
                    <EuiFlexItem>
                        <PublishTooltip>
                            <EuiButton fill={true} disabled={!ruleId || isLoading || !!publishValidationErrors}
                                        isLoading={isPublishing}
                                        onClick={maybePublishRuleHandler}>{"Publish"}</EuiButton>
                        </PublishTooltip>
                    </EuiFlexItem>
            }
            </EuiFlexGroup>
            {showErrors ? <EuiCallOut title="Please resolve the following errors:" color="danger" iconType="error">
                {formErrors.map((error, index) => <EuiText key={index}>{`${error.message}`}</EuiText>)}
            </EuiCallOut> : null}
        </EuiFlexGroup>}
        {isReasonModalVisible && <ReasonModal onClose={() => setIsReasonModalVisible(false)} onSubmit={publishRuleHandler} isLoading={isPublishing} />}
    </EuiForm>
}
