import React, { useContext } from 'react';
import { EuiComboBox, EuiFormRow, EuiLoadingSpinner } from '@elastic/eui';
import { PartiallyUpdateRuleData } from './RuleForm';
import { TagsContext } from './context/tags';

export const TagsSelector = ({
	selectedTagIds,
	partiallyUpdateRuleData,
}: {
	selectedTagIds: number[];
	partiallyUpdateRuleData: PartiallyUpdateRuleData;
}) => {
	const { isLoading, tags } = useContext(TagsContext);

	if (isLoading) {
		return (
			<EuiFormRow label="Tags" fullWidth={true}>
				<EuiLoadingSpinner />
			</EuiFormRow>
		);
	}

	const options = tags
		? Object.values(tags).map((tag) => ({ label: tag.name, value: tag.id }))
		: [];
	const tagOptions = selectedTagIds.map((tag) => ({
		label: tags[tag].name,
		value: tags[tag].id,
	}));

	return (
		<EuiFormRow label="Tags" fullWidth={true}>
			<EuiComboBox<number>
				options={options}
				selectedOptions={tagOptions}
				onChange={(options) =>
					partiallyUpdateRuleData({ tags: options.map((tag) => tag.value!) })
				}
				isClearable={true}
				isCaseSensitive
				fullWidth={true}
			/>
		</EuiFormRow>
	);
};
