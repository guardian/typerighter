import {useEffect, useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";
import React from "react";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";

export const CategorySelector = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: RuleFormData,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    type Category = {label: string}
    const categories: Category[] = [
        {
            label: 'Category A',
        },
        {
            label: 'Category B',
        }
    ]
    const [options, updateOptions] = useState(categories);
    // This is an array in order to match the expected type for EuiComboBox, but 
    // it will never have more than one category selected
    const [selectedCategory, setSelectedCategory] = useState<Category[]>([]);

    const onChange = (selectedOption) => {
        setSelectedCategory(selectedOption);
    };

    const onCreateOption = (searchValue: string, flattenedOptions) => {
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
        setSelectedCategory([newOption]);
    };

    useEffect(() => {
        const newCategory = selectedCategory.length ? selectedCategory[0].label : undefined;
        partiallyUpdateRuleData(ruleData, {category: newCategory})
    }, [selectedCategory])

    return (
        <EuiFormRow label='Categories'>
            <EuiComboBox
                options={options}
                singleSelection={{ asPlainText: true }}
                selectedOptions={selectedCategory}
                onChange={onChange}
                onCreateOption={onCreateOption}
                isClearable={true}
                isCaseSensitive
            />
        </EuiFormRow>
    );
}