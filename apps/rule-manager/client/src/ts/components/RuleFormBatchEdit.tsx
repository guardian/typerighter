import {
	EuiButton,
	EuiFlexGroup,
	EuiFlexItem,
	EuiForm,
	EuiLoadingSpinner,
} from '@elastic/eui';
import React, { useEffect, useState } from 'react';
import { DraftRule } from './hooks/useRule';

import { useBatchRules } from './hooks/useBatchRules';
import { RuleFormSection } from './RuleFormSection';
import {
	baseForm,
	PartiallyUpdateRuleData,
	SpinnerContainer,
	SpinnerOuter,
	SpinnerOverlay,
} from './RuleForm';
import { LineBreak } from './LineBreak';
import { CategorySelector } from './CategorySelector';
import { TagsSelector } from './TagsSelector';

import { useNavigate, useOutletContext } from 'react-router-dom';
import { RulesRouteContext } from './pages/Rules';

export const RuleFormBatchEdit = () => {
	const navigate = useNavigate();
	const { ruleIds, onUpdate } = useOutletContext() as RulesRouteContext;
	const { isLoading, rules, updateRules } = useBatchRules(ruleIds);
	const [ruleFormData, setRuleFormData] = useState<DraftRule[]>([baseForm]);

	const partiallyUpdateRuleData: PartiallyUpdateRuleData = (
		partialReplacement,
	) => {
		const updatedRules = ruleFormData.map((rule) => ({
			...rule,
			...partialReplacement,
		})) as DraftRule[];
		setRuleFormData(updatedRules);
	};

	useEffect(() => {
		if (rules) {
			setRuleFormData(rules.map((rule) => rule.draft) as DraftRule[]);
		}
	}, [rules]);

	useEffect(() => {
		if (!ruleIds || !ruleIds.length) navigate('/');
	}, [ruleIds]);

	const saveRuleHandler = async () => {
		const response = await updateRules(ruleFormData as DraftRule[]);

		if (
			response.status === 'ok' &&
			Array.isArray(response.data) &&
			response.data.every((rule) => rule.id)
		) {
			onUpdate();
		}
	};

	const categoryHasChanged = rules?.some(
		(rule, index) =>
			ruleFormData[index] &&
			rule.draft.category !== ruleFormData[index].category,
	);
	const tagsHaveChanged = rules?.some(
		(rule, index) =>
			ruleFormData[index] &&
			(rule.draft.tags.length !== ruleFormData[index].tags.length ||
				!rule.draft.tags.every(
					(tag, i) => tag === ruleFormData[index].tags[i],
				)),
	);
	const hasUnsavedChanges =
		!isLoading && (categoryHasChanged || tagsHaveChanged);

	const uniqueTagIds = [...new Set(ruleFormData.flatMap((rule) => rule.tags))];

	return (
		<EuiForm component="form">
			{isLoading && (
				<SpinnerOverlay>
					<SpinnerOuter>
						<SpinnerContainer>
							<EuiLoadingSpinner />
						</SpinnerContainer>
					</SpinnerOuter>
				</SpinnerOverlay>
			)}
			{
				<EuiFlexGroup gutterSize="m" direction="column">
					<RuleFormSection title="RULE CONTENT" />
					<RuleFormSection title="RULE METADATA">
						<LineBreak />
						<CategorySelector
							currentCategory={ruleFormData[0]?.category}
							partiallyUpdateRuleData={partiallyUpdateRuleData}
						/>
						<TagsSelector
							selectedTagIds={uniqueTagIds}
							partiallyUpdateRuleData={partiallyUpdateRuleData}
						/>
					</RuleFormSection>
					<EuiFlexGroup gutterSize="m">
						<EuiFlexItem>
							<EuiButton
								onClick={() => {
									const shouldClose = hasUnsavedChanges
										? window.confirm(
												'Your rules have unsaved changes. Are you sure you want to discard them?',
										  )
										: true;
									if (!shouldClose) {
										return;
									}
									navigate('/');
									setRuleFormData([baseForm]);
								}}
							>
								Close
							</EuiButton>
						</EuiFlexItem>
						<EuiFlexItem>
							<EuiButton
								fill={true}
								isDisabled={!hasUnsavedChanges}
								isLoading={isLoading}
								onClick={saveRuleHandler}
							>
								{'Update Rules'}
							</EuiButton>
						</EuiFlexItem>
					</EuiFlexGroup>
				</EuiFlexGroup>
			}
		</EuiForm>
	);
};
