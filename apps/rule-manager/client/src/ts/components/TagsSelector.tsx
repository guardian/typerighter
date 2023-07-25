import React, { useEffect, useState } from 'react';
import { EuiComboBox, EuiFormRow, EuiLoadingSpinner } from '@elastic/eui';
import { DraftRule } from './hooks/useRule';
import { TagMap } from './hooks/useTags';
import { PartiallyUpdateRuleData } from './RuleForm';

type TagOption = { label: string; value?: number };

export const TagsSelector = ({
	tags,
	selectedTagIds,
	partiallyUpdateRuleData,
	isLoading,
}: {
	tags: TagMap;
	selectedTagIds: number[];
	isLoading: boolean;
	partiallyUpdateRuleData: PartiallyUpdateRuleData;
}) => {
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
