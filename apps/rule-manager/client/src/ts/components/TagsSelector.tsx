import {useEffect, useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";
import React from "react";
import { MetadataOption } from "./CategorySelector";
import { PartiallyUpdateRuleData } from "./RuleForm";
import { existingTags } from "../constants/constants";
import {DraftRule} from "./hooks/useRule";

export const TagsSelector = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: DraftRule,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    const options = existingTags.map(tag => {return {label: tag}});

    const [selectedTags, setSelectedTags] = useState<MetadataOption[]>(ruleData.tags ? ruleData.tags.map(tag => ({label: tag})) : []);

    const onChange = (selectedTags: MetadataOption[]) => {
        setSelectedTags(selectedTags);
    };

    useEffect(() => {
        const newTags = selectedTags.map(tag => tag.label)
        partiallyUpdateRuleData({tags: newTags})
    }, [selectedTags])

    return (
        <EuiFormRow label='Tags' fullWidth={true}>
            <EuiComboBox
                options={options}
                selectedOptions={ruleData.tags ? ruleData.tags.map(tag => ({label: tag})) : undefined}
                onChange={onChange}
                isClearable={true}
                isCaseSensitive
                fullWidth={true}
            />
        </EuiFormRow>
    );
}
