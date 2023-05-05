import {useEffect, useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";
import React from "react";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";

export type MetadataOption = {label: string}
const singleSelectionOptions = { asPlainText: true };
export const CategorySelector = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: RuleFormData,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    const categories: MetadataOption[] = [
        {label: "Check this"},
        {label: "Guardian convention"},
        {label: "Style guide and names"},
        {label: "General"},
        {label: "Names"},
        {label: "Typos"},
        {label: "Tokyo 2020: General"},
        {label: "Tokyo 2020: Diving"},
        {label: "Tokyo 2020: Equestrian"},
        {label: "Tokyo 2020: Cycling"},
        {label: "Tokyo 2020: Rugby"},
        {label: "Tokyo 2020: Boxing"},
        {label: "Tokyo 2020: Football"},
        {label: "Tokyo 2020: Swimming"},
        {label: "Coronavirus"},
        {label: "Typography"},
        {label: "Dates"},
        {label: "Style Guide"}
    ]
    // This is an array in order to match the expected type for EuiComboBox, but 
    // it will never have more than one category selected
    const [selectedCategory, setSelectedCategory] = useState<MetadataOption[]>([]);

    const onChange = (selectedOption) => {
        setSelectedCategory(selectedOption);
    };

    useEffect(() => {
        const newCategory = selectedCategory.length ? selectedCategory[0].label : undefined;
        partiallyUpdateRuleData(ruleData, {category: newCategory})
    }, [selectedCategory])

    return (
        <EuiFormRow label='Category'>
            <EuiComboBox
                options={categories}
                singleSelection={singleSelectionOptions}
                selectedOptions={selectedCategory}
                onChange={onChange}
                isClearable={true}
                isCaseSensitive
            />
        </EuiFormRow>
    );
}