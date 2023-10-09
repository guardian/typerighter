import React from 'react';
import { useEffect, useState } from 'react';
import { EuiFormRow, EuiComboBox } from '@elastic/eui';
import { FormError, PartiallyUpdateRuleData } from './RuleForm';
import { existingCategories } from '../constants/constants';
import { getErrorPropsForField } from './helpers/errors';
import { Label } from './Label';

export type MetadataOption = { label: string };
const singleSelectionOptions = { asPlainText: true };
export const CategorySelector = ({
	currentCategory,
	partiallyUpdateRuleData,
	validationErrors,
	isDictionaryRule,
}: {
	currentCategory: string | undefined;
	partiallyUpdateRuleData: PartiallyUpdateRuleData;
	validationErrors?: FormError[];
	isDictionaryRule: boolean;
}) => {
	// This is an array in order to match the expected type for EuiComboBox, but
	// it will never have more than one category selected
	const [selectedCategory, setSelectedCategory] = useState<MetadataOption[]>(
		currentCategory ? [{ label: currentCategory }] : [],
	);
	const categories = existingCategories.map((category) => {
		return { label: category } as MetadataOption;
	});
	const onChange = (selectedOption: MetadataOption[]) => {
		setSelectedCategory(selectedOption);
	};

	useEffect(() => {
		const newCategory = selectedCategory.length
			? selectedCategory[0].label
			: undefined;
		partiallyUpdateRuleData({ category: newCategory });
	}, [selectedCategory]);

	const categoryErrors = getErrorPropsForField('category', validationErrors);

	const preSelectedCategory = (currentCategory: string | undefined) => {
		if (currentCategory) {
			return [{ label: currentCategory }];
		} else if (isDictionaryRule) {
			return [{ label: 'Guardian dictionary addition' }];
		} else {
			return [];
		}
	};
	return (
		<EuiFormRow
			label={<Label text="Source" required={true} />}
			fullWidth={true}
			{...categoryErrors}
		>
			<EuiComboBox
				options={categories}
				singleSelection={singleSelectionOptions}
				selectedOptions={preSelectedCategory(currentCategory)}
				onChange={onChange}
				isClearable={true}
				isCaseSensitive
				fullWidth={true}
				{...categoryErrors}
			/>
		</EuiFormRow>
	);
};
