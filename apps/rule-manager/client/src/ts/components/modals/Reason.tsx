import {
  EuiButton,
  EuiButtonEmpty,
  EuiFieldText,
  EuiFlexGroup,
  EuiForm,
  EuiFormRow,
  EuiModal,
  EuiModalBody,
  EuiModalFooter,
  EuiModalHeader,
  EuiModalHeaderTitle,
  EuiSpacer,
  EuiText,
  useEuiTextDiff,
} from "@elastic/eui";
import { Label } from "../Label";
import { FormEventHandler, useEffect, useState } from "react";
import { RuleData } from "../hooks/useRule";

const modalFormId = "modal-form";

type DiffedValues<T> = {
  live: T;
  draft: T;
}

const findFieldsWithDiffs = (rule: RuleData): Record<string, DiffedValues<string | number | boolean>> => {
  const draftEntries = Object.entries(rule.draft);
  const liveEntries = Object.entries(rule.live.reduce((acc, cur) => cur.revisionId > acc.revisionId ? cur : acc))
  const diffValues = draftEntries.reduce<Record<string, any>>((diffObject, draftEntry) => {
    const [draftKey, draftValue] = draftEntry;
    // Fields are all optional
    const maybeEquivalentLiveEntry = liveEntries.find(([liveKey, liveValue]) => liveKey === draftKey)
    if (maybeEquivalentLiveEntry){
      const [liveKey, liveValue] = maybeEquivalentLiveEntry;
      const liveAndDraftValuesMatch = liveValue === draftValue;
      if (!liveAndDraftValuesMatch){
        diffObject[draftKey] = {
          live: liveValue,
          draft: draftValue
        }
      }
    }
    return diffObject
  }, {})
  return diffValues
  // Find rows that don't match and return only those rows that aren't identical in both
};

export const SingleTextDiff = ({draft, live, key}: {draft: string | number | boolean, live: string | number | boolean, key: string}) => {
  const [del, setDel] = useState(0);
  const [ins, setIns] = useState(0);
  const [rendered, textDiffObject] = useEuiTextDiff({ beforeText: live.toString(), afterText: draft.toString() })
  useEffect(() => {
    textDiffObject.forEach((el) => {
      if (el[0] === 1) {
        setIns((add) => add + 1);
      } else if (el[0] === -1) {
        setDel((sub) => sub + 1);
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return <EuiFlexGroup key={key}>
    {rendered}
  </EuiFlexGroup>
}

export const ReasonDiff = ({rule}: {rule: RuleData | undefined}) => {
  const diffedValues = rule ? findFieldsWithDiffs(rule) : null;
  console.log(diffedValues);
  return <>
    <EuiSpacer />
    <EuiFlexGroup>
      {diffedValues ? Object.entries(diffedValues).map(([key, diffedValue]) => <SingleTextDiff draft={diffedValue.draft} live={diffedValue.draft} key={key} />) : null}
      <EuiText>What's changed:</EuiText>
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
