import {useEffect, useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";
import React from "react";
import { PartiallyUpdateRuleData, RuleFormData } from "./RuleForm";
import { existingCategories } from "../constants/constants";

export type MetadataOption = {label: string};
const singleSelectionOptions = { asPlainText: true };
export const CategorySelector = ({ruleData, partiallyUpdateRuleData}: {
    ruleData: RuleFormData,
    partiallyUpdateRuleData: PartiallyUpdateRuleData,
}) => {
    // This is an array in order to match the expected type for EuiComboBox, but 
    // it will never have more than one category selected
    const [selectedCategory, setSelectedCategory] = useState<MetadataOption[]>(ruleData.category ? [{label: ruleData.category}] : []);
    const categories = existingCategories.map(category => {return {label: category} as MetadataOption});
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
                selectedOptions={ruleData.category ? [{label: ruleData.category}] : []}
                onChange={onChange}
                isClearable={true}
                isCaseSensitive
            />
        </EuiFormRow>
    );
}