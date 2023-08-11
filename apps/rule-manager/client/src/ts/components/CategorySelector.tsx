import React from 'react';
import { useEffect, useState } from 'react';
import { EuiFormRow, EuiComboBox } from '@elastic/eui';
import { FormError, PartiallyUpdateRuleData } from './RuleForm';
import { existingCategories } from '../constants/constants';
import { hasErrorsForField } from './helpers/errors';

export type MetadataOption = { label: string };
const singleSelectionOptions = { asPlainText: true };
export const CategorySelector = ({
	currentCategory,
	partiallyUpdateRuleData,
	validationErrors,
}: {
	currentCategory: string | undefined;
	partiallyUpdateRuleData: PartiallyUpdateRuleData;
	validationErrors: FormError[] | undefined;
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

	return (
		<EuiFormRow
			label="Source"
			fullWidth={true}
			isInvalid={hasErrorsForField('category', validationErrors)}
		>
			<EuiComboBox
				options={categories}
				singleSelection={singleSelectionOptions}
				selectedOptions={currentCategory ? [{ label: currentCategory }] : []}
				onChange={onChange}
				isClearable={true}
				isCaseSensitive
				fullWidth={true}
				isInvalid={hasErrorsForField('category', validationErrors)}
			/>
		</EuiFormRow>
	);
};
