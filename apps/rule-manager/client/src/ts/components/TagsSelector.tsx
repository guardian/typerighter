import {useEffect, useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";
import React from "react";
import { MetadataOption } from "./CategorySelector";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";

export const TagsSelector = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: RuleFormData,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    const [options, updateOptions] = useState([
        {
            label: 'Tag A',
        },
        {
            label: 'Tag B',
        }
    ]);

    const [selectedTags, setSelectedTags] = useState<MetadataOption[]>([]);

    const onChange = (selectedTags) => {
        setSelectedTags(selectedTags);
    };

    const onCreateOption = (searchValue, flattenedOptions) => {
        const normalizedSearchValue = searchValue.trim().toLowerCase();

        if (!normalizedSearchValue) {
            return;
        }

        const newOption = {
            label: searchValue,
        };

        // Create the option if it doesn't exist.
        if (
            flattenedOptions.findIndex(
                (option) => option.label.trim().toLowerCase() === normalizedSearchValue
            ) === -1
        ) {
            updateOptions([...options, newOption]);
        }

        // Select the option.
        setSelectedTags((prevSelected) => [...prevSelected, newOption]);
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
                onCreateOption={onCreateOption}
                isClearable={true}
                isCaseSensitive
            />
        </EuiFormRow>
    );
}