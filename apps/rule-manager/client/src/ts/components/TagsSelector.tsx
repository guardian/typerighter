import {useEffect, useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";
import React from "react";
import { MetadataOption } from "./CategorySelector";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";
import { existingTags } from "../constants/constants";

export const TagsSelector = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: RuleFormData,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    const options = existingTags.map(tag => {return {label: tag}});

    const [selectedTags, setSelectedTags] = useState<MetadataOption[]>(ruleData.tags ? ruleData.tags.map(tag => ({label: tag})) : []);

    const onChange = (selectedTags) => {
        setSelectedTags(selectedTags);
    };

    useEffect(() => {
        const newTags = selectedTags.map(tag => tag.label)
        partiallyUpdateRuleData(ruleData, {tags: newTags})
    }, [selectedTags])

    return (
        <EuiFormRow label='Tags'>
            <EuiComboBox
                options={options}
                selectedOptions={ruleData.tags ? ruleData.tags.map(tag => ({label: tag})) : undefined}
                onChange={onChange}
                isClearable={true}
                isCaseSensitive
            />
        </EuiFormRow>
    );
}