import {useEffect, useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";
import React from "react";
import { MetadataOption } from "./CategorySelector";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";

const existingTags = [
    "SG",
    "General",
    "US spelling",
    "Unnecessary contraction?",
    "Swearword!",
    "13?",
    "Offensive?",
    "US term",
    "Taste?",
    "Names",
    "Coronavirus",
    "Poss typo",
    "SG ",
    "Guardian convention",
    "Legal",
    "Tokyo 2020",
    "Names",
    "MP",
    "LanguageTool",
    "dates",
    "Semantics",
    "Typography",
    "Typos"
];

export const TagsSelector = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: RuleFormData,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    const options = existingTags.map(tag => {return {label: tag}});

    const [selectedTags, setSelectedTags] = useState<MetadataOption[]>([]);

    const onChange = (selectedTags) => {
        setSelectedTags(selectedTags);
    };

    useEffect(() => {
        if (selectedTags.length) {
            const newTags = selectedTags.map(tag => tag.label)
            partiallyUpdateRuleData(ruleData, {tags: newTags})
        } else {
            partiallyUpdateRuleData(ruleData, {tags: undefined})
        }
    }, [selectedTags])

    return (
        <EuiFormRow label='Tags'>
            <EuiComboBox
                options={options}
                selectedOptions={selectedTags}
                onChange={onChange}
                isClearable={true}
                isCaseSensitive
            />
        </EuiFormRow>
    );
}