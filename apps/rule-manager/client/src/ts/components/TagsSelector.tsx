import React, {useEffect, useState} from "react";
import {EuiComboBox, EuiFormRow, EuiLoadingSpinner} from "@elastic/eui";
import {DraftRule} from "./hooks/useRule";
import {TagMap} from "./hooks/useTags";
import {PartiallyUpdateRuleData} from "./RuleForm";

type TagOption = { label: string, value?: number }

export const TagsSelector = ({tags, ruleData, partiallyUpdateRuleData}: {
    tags: TagMap,
    ruleData: DraftRule | DraftRule[],
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    if (Object.keys(tags).length === 0) {
      return <EuiFormRow label='Tags' fullWidth={true}><EuiLoadingSpinner/></EuiFormRow>
    }

    const options = tags ? Object.values(tags).map(tag => ({ label: tag.name, value: tag.id })) : [];

    const transformTags = (ruleData: DraftRule[]) => {
        let seenTags = new Set<number>();
        let uniqueTags = [];

        ruleData.map(rule => rule.tags.map(tag => {
            if (!seenTags.has(tag)) {
                seenTags.add(tag);
                uniqueTags.push(tag);
            }
        }))

        return uniqueTags.map(tag => ({label: tags[tag].name, value: tags[tag].id}));
    }

    const tagOptions = Array.isArray(ruleData) ? transformTags(ruleData) : ruleData.tags.map(tag => ({label: tags[tag].name, value: tags[tag].id}));
    const [selectedTags, setSelectedTags] = useState<TagOption[]>(tagOptions);

    useEffect(() => {
        const newTags = selectedTags.map(tag => tag.value!)
        partiallyUpdateRuleData({tags: newTags})
    }, [selectedTags])

    return (
        <EuiFormRow label='Tags' fullWidth={true}>
            <EuiComboBox<number>
                options={options}
                selectedOptions={tagOptions}
                onChange={options => setSelectedTags(options)}
                isClearable={true}
                isCaseSensitive
                fullWidth={true}
            />
        </EuiFormRow>
    );
}
