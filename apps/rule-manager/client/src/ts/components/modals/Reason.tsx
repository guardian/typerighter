import {
  EuiBadge,
  EuiButton,
  EuiButtonEmpty,
  EuiCode,
  EuiFieldText,
  EuiFlexGroup,
  EuiFlexItem,
  EuiForm,
  EuiFormRow,
  EuiHorizontalRule,
  EuiModal,
  EuiModalBody,
  EuiModalFooter,
  EuiModalHeader,
  EuiModalHeaderTitle,
  EuiSpacer,
  EuiText,
  EuiTitle,
  useEuiTextDiff,
} from "@elastic/eui";
import { Label } from "../Label";
import { FormEventHandler, useEffect, useState } from "react";
import { RuleData } from "../hooks/useRule";
import { useTags } from "../hooks/useTags";
import { SectionHeader, Title } from "../RuleFormSection";
import styled from "@emotion/styled";
import { css } from "@emotion/react";

const modalFormId = "modal-form";

type DiffedFields = {
  field: string;
  live: DiffValue;
  draft: DiffValue;
}

type DiffValue = string | number | boolean | number[] | string[]

const textDiffFields = ["description", "pattern", "replacement"];
const comparisonDiffFields = ["ruleType", "category", "tags"];

const findFieldsWithDiffs = (rule: RuleData): DiffedFields[] => {
  const fieldsToProvideDiffFor = [...textDiffFields, ...comparisonDiffFields];
  const draftEntries = Object.entries(rule.draft).filter(([key]) => fieldsToProvideDiffFor.includes(key));
  const liveEntries = Object.entries(rule.live.reduce((acc, cur) => cur.revisionId > acc.revisionId ? cur : acc)).filter(([key]) => fieldsToProvideDiffFor.includes(key));

  const diffValues = draftEntries.reduce((diffArray, draftEntry) => {
    const [draftKey, draftValue] = draftEntry;
    // Fields are all optional
    const maybeEquivalentLiveEntry = liveEntries.find(([liveKey]) => liveKey === draftKey)
    if (maybeEquivalentLiveEntry){
      const [_, liveValue] = maybeEquivalentLiveEntry;
      const liveAndDraftValuesMatch = liveValue === draftValue;
      if (!liveAndDraftValuesMatch){
        diffArray.push({
          field: draftKey,
          live: liveValue,
          draft: draftValue
        })
      }
    }
    return diffArray
  }, [] as DiffedFields[])
  return diffValues
  // Find rows that don't match and return only those rows that aren't identical in both
};

export const camelCaseToTitleCase = (str: string) => {
  // Add space between strings
  const result = str.replace(/([A-Z])/g,' $1');
  // converting first character to uppercase and join it to the final string
  return result.charAt(0).toUpperCase() + result.slice(1);
}

export const TextDiff = ({draft, live, name}: {draft: DiffValue, live: DiffValue, name: string}) => {
  const [rendered] = useEuiTextDiff({ beforeText: live.toString(), afterText: draft.toString() })
  console.log({beforeText: live.toString(), afterText: draft.toString()})
  // TODO: Tidy tags - id => name
  // TODO: Tidy
  return <EuiFlexGroup>
      <EuiFlexItem>
        <EuiHorizontalRule margin="s" />
        <EuiFlexGroup>
        <SectionHeader>
            <Title>{camelCaseToTitleCase(name)}</Title>
        </SectionHeader>
        </EuiFlexGroup>
        <Comparison left={<>{live}</>} right={<>{rendered}</>}></Comparison>
      </EuiFlexItem>
  </EuiFlexGroup>
}

const ComparisonPanel = styled.div`
  background-color: #F1F4FA;
  padding: 0.5rem;
`

const ComparisonPanelHeader = styled.div`
  padding: 0.25rem 0;
  color: #444;
`

export const Comparison = ({left, right} : {left: JSX.Element | JSX.Element[], right: JSX.Element | JSX.Element[]}) => {
  return <EuiFlexGroup>
    <EuiFlexItem grow><ComparisonPanelHeader>Before:</ComparisonPanelHeader><ComparisonPanel>{left}</ComparisonPanel></EuiFlexItem>
    <EuiFlexItem grow><ComparisonPanelHeader>After:</ComparisonPanelHeader><ComparisonPanel>{right}</ComparisonPanel></EuiFlexItem>
  </EuiFlexGroup>
}

export const ComparisonDiff = ({draft, live, name}: {draft: DiffValue, live: DiffValue, name: string}) => {
  // Transform tag ids to tag names
  return <EuiFlexGroup>
      <EuiFlexItem>
        <EuiHorizontalRule margin="s" />
        <EuiFlexGroup> 
          <SectionHeader>
              <Title>{camelCaseToTitleCase(name)}</Title>
          </SectionHeader>
        </EuiFlexGroup>
          <Comparison 
            left={Array.isArray(draft) ? draft.map(item => <EuiBadge>{item}</EuiBadge>) : <EuiBadge>{draft}</EuiBadge>}
            right={Array.isArray(live) ? live.map(item => <EuiBadge>{item}</EuiBadge>) : <EuiBadge>{live}</EuiBadge>}
          />
      </EuiFlexItem>
  </EuiFlexGroup>
}

export const ReasonDiff = ({rule}: {rule: RuleData | undefined}) => {
  const {tags} = useTags();
  const diffedFields = rule ? findFieldsWithDiffs(rule) : null;
  const textDiffs = diffedFields?.filter(diffedField => textDiffFields.includes(diffedField.field))
  const comparisonDiffs = diffedFields?.filter(diffedField => comparisonDiffFields.includes(diffedField.field))
  // if (diffedValues?.tags){
  //   // Render tags as their names rather than their IDs
  //   const diffedTags = diffedValues.tags;
  //   diffedTags.live = (diffedTags.live as number[]).map(tagId => tags[tagId]?.name)
  //   diffedTags.draft = (diffedTags.draft as number[]).map(tagId => tags[tagId]?.name)
  //   //(diffedTags.draft as number[]).map(tagId => tags[tagId]);
  //   console.log(tags)
  // }

  
  return <>
    <EuiSpacer />
    <EuiFlexGroup css={css`border: 1px solid #ddd;
    padding: 1rem;
    border-radius: 0.5rem;`}>
      <EuiFlexItem>
        <EuiText>What's changed:</EuiText>
        {textDiffs?.map(diffedField => <TextDiff draft={diffedField.draft} live={diffedField.live} name={diffedField.field} key={diffedField.field} />)}
        {comparisonDiffs?.map(diffedField => <ComparisonDiff draft={diffedField.draft} live={diffedField.live} name={diffedField.field} key={diffedField.field} />)}
      </EuiFlexItem>
    </EuiFlexGroup>
  </>
}

export const ReasonModal = ({
                              onClose,
                              onSubmit,
                              isLoading,
                              rule
                            }: {
  onClose: () => void;
  onSubmit: (reason: string) => void;
  isLoading: boolean;
  rule: RuleData | undefined;
}) => {
  const [reason, setReason] = useState<string>("");
  const handleSubmit: FormEventHandler<HTMLFormElement> = e => {
    e.preventDefault();
    onSubmit(reason);
  }

  return (
    <EuiModal onClose={onClose} initialFocus="[name=popswitch]">
      <EuiModalHeader>
        <EuiModalHeaderTitle>Republish rule</EuiModalHeaderTitle>
      </EuiModalHeader>
      <EuiModalBody>
        <EuiForm id={modalFormId} component="form" onSubmit={handleSubmit}>
          <EuiText>Why is this rule being revised? This helps us understand the rule's publication history.</EuiText>
          <EuiSpacer />
          <EuiFormRow
            label={<Label text="Reason" required={true} />}
            isInvalid={!reason}
          >
            <EuiFieldText
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required={true}
              isInvalid={!reason}
            />
          </EuiFormRow>
        </EuiForm>
        <ReasonDiff rule={rule}/>
      </EuiModalBody>
      <EuiModalFooter>
        <EuiButtonEmpty onClick={onClose}>Cancel</EuiButtonEmpty>
        <EuiButton
          type="submit"
          form={modalFormId}
          fill
          isLoading={isLoading}
        >
          Publish
        </EuiButton>
      </EuiModalFooter>
    </EuiModal>
  );
};
