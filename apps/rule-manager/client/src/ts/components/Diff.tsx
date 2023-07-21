import { EuiBadge, EuiFlexGroup, EuiFlexItem, EuiHorizontalRule, EuiSpacer, EuiText, useEuiTextDiff } from "@elastic/eui";
import { BaseRule, RuleData } from "./hooks/useRule";
import { useTags } from "./hooks/useTags";
import { css } from "@emotion/react";
import styled from "@emotion/styled";

type DivergentFields = {
    fieldName: string;
    live: FieldValue;
    draft: FieldValue;
}

type FieldValue = string | number | boolean | number[] | string[] | undefined

type FieldObject = {
    fieldName: string,
    value: FieldValue
}

const ComparisonPanel = styled.div`
    background-color: #F1F4FA;
    height: 100%;
    padding: 0.5rem;
`

const ComparisonPanelHeader = styled.div`
    padding: 0.25rem 0;
    color: #444;
`;

const diffWrapper = css`
    border: 1px solid #ddd;
    padding: 1rem;
    border-radius: 0.5rem;
`;

const textDiffFields = ["description", "pattern", "replacement"];
const comparisonDiffFields = ["ruleType", "category", "tags"];

const doValuesMatch = (draftValue?: FieldValue, liveValue?: FieldValue): boolean => {
    // The value could be an array or a primitive
    if (Array.isArray(draftValue) && Array.isArray(liveValue)){
        // This is a good enough comparison for our arrays of primitives
        return JSON.stringify(draftValue) === JSON.stringify(liveValue)
    }
    return draftValue === liveValue
}

const findNonIntersectingFields = (draft: FieldObject[], live: FieldObject[]) => {
    const uniqueDraftFields = draft.filter(draftField => !live.find(liveField => liveField.fieldName === draftField.fieldName))
    const uniqueLiveFields = live.filter(liveField => !draft.find(draftField => liveField.fieldName === draftField.fieldName))
    return [
        ...uniqueDraftFields.map(draftField => { return {draft: draftField.value, live: undefined, fieldName: draftField.fieldName}}), 
        ...uniqueLiveFields.map(liveField => { return {draft: undefined, live: liveField.value, fieldName: liveField.fieldName}})
    ]
}

const findFieldsWithDiffs = (rule: RuleData): DivergentFields[] => {
    const newestLiveRule = rule.live.reduce((acc, cur) => cur.revisionId > acc.revisionId ? cur : acc);
    const draftFieldObjects = Object.keys(rule.draft).map(fieldName => { return { fieldName, value: rule.draft[fieldName as keyof BaseRule]}});
    const liveFieldObjects = Object.keys(newestLiveRule).map(fieldName => { return { fieldName, value: newestLiveRule[fieldName as keyof BaseRule]}})

    const fieldsToProvideDiffFor = [...textDiffFields, ...comparisonDiffFields];
    // The rule data contains various fields we're not interested in providing diffs for
    const draftFieldsToDiff = draftFieldObjects.filter(draftField => fieldsToProvideDiffFor.includes(draftField.fieldName));
    const liveFieldsToDiff = liveFieldObjects.filter(liveField => fieldsToProvideDiffFor.includes(liveField.fieldName));
    
    // Add fields that only appear in one of live and draft
    const nonIntersectingFields = findNonIntersectingFields(draftFieldObjects, liveFieldObjects);

    // Compare fields that appear in both live and draft
    const divergentFields = draftFieldsToDiff.reduce((divergentFieldsArray, draftField) => {
        const equivalentLiveField = liveFieldsToDiff.find(liveField => liveField.fieldName === draftField.fieldName)
        if (equivalentLiveField ){
            const liveAndDraftValuesMatch = doValuesMatch(draftField.value, equivalentLiveField .value);
            if (!liveAndDraftValuesMatch){
                divergentFieldsArray.push({
                    fieldName: draftField.fieldName,
                    live: equivalentLiveField.value,
                    draft: draftField.value
                })
            }
        }
        return divergentFieldsArray
    }, nonIntersectingFields as DivergentFields[])
    return divergentFields
    // Find rows that don't match and return only those rows that aren't identical in both
};

export const camelCaseToTitleCase = (str: string) => {
    // Add space between strings
    const result = str.replace(/([A-Z])/g,' $1');
    // converting first character to uppercase and join it to the final string
    return result.charAt(0).toUpperCase() + result.slice(1);
}

export const Diff = ({rule}: {rule: RuleData | undefined}) => {
    const {tags} = useTags();
    const diffedFields = rule ? findFieldsWithDiffs(rule) : null;
    const textDiffs = diffedFields?.filter(diffedField => textDiffFields.includes(diffedField.fieldName))
    const comparisonDiffs = diffedFields?.filter(diffedField => comparisonDiffFields.includes(diffedField.fieldName))
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
      <EuiFlexGroup css={diffWrapper}>
        <EuiFlexItem>
            <EuiText><h4>What's changed:</h4></EuiText>
            <EuiSpacer size="m" />
            <EuiFlexGroup>
                <EuiFlexItem css={css`width: 100px;`}><strong>Field</strong></EuiFlexItem>
                <EuiFlexItem grow><strong>Before:</strong></EuiFlexItem>
                <EuiFlexItem grow><strong>After:</strong></EuiFlexItem>
            </EuiFlexGroup>
                {textDiffs?.map(diffedField => <TextDiff draft={diffedField.draft} live={diffedField.live} name={diffedField.fieldName} key={diffedField.fieldName} />)}
                {comparisonDiffs?.map(diffedField => 
                <ComparisonDiff draft={diffedField.draft} live={diffedField.live} name={diffedField.fieldName} key={diffedField.fieldName} />
            )}
        </EuiFlexItem>
      </EuiFlexGroup>
    </>
  }

  export const ComparisonDiff = ({draft, live, name}: {draft: FieldValue, live: FieldValue, name: string}) => {
    // Transform tag ids to tag names
    return <EuiFlexGroup>
        <EuiFlexItem>
          <EuiHorizontalRule margin="s" />
            <Comparison 
                left={Array.isArray(live) ? live.map(item => <EuiBadge>{item}</EuiBadge>) : <EuiBadge>{live}</EuiBadge>}
                right={Array.isArray(draft) ? draft.map(item => <EuiBadge>{item}</EuiBadge>) : <EuiBadge>{draft}</EuiBadge>}
                fieldName={camelCaseToTitleCase(name)}
            />
        </EuiFlexItem>
    </EuiFlexGroup>
  }
  
  export const TextDiff = ({draft, live, name}: {draft: FieldValue, live: FieldValue, name: string}) => {
    const [rendered] = useEuiTextDiff({ beforeText: live ? live.toString() : "", afterText: draft ? draft.toString() : "" })
    // TODO: Tidy tags - id => name
    // TODO: Tidy
    return <EuiFlexGroup>
        <EuiFlexItem>
          <EuiHorizontalRule margin="xs" />
          <Comparison left={<>{live}</>} right={<>{rendered}</>} fieldName={camelCaseToTitleCase(name)}></Comparison>
        </EuiFlexItem>
    </EuiFlexGroup>
  }

  export const Comparison = ({left, right, fieldName} : {left: JSX.Element | JSX.Element[], right: JSX.Element | JSX.Element[], fieldName: string}) => {
    return <EuiFlexGroup>
      <EuiFlexItem css={css`width: 100px;`}><ComparisonPanelHeader>{fieldName}</ComparisonPanelHeader></EuiFlexItem>
      <EuiFlexItem grow><ComparisonPanel>{left}</ComparisonPanel></EuiFlexItem>
      <EuiFlexItem grow><ComparisonPanel>{right}</ComparisonPanel></EuiFlexItem>
    </EuiFlexGroup>
  }
  