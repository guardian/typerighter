import { useEffect, useState } from 'react';
import { ErrorIResponse, responseHandler } from '../../utils/api';
import { errorToString } from '../../utils/error';
import { FormError } from '../RuleForm';
import { getRuleStatus, RuleStatus } from '../../utils/rule';

export type RuleType =
	| 'regex'
	| 'languageToolXML'
	| 'languageToolCore'
	| 'dictionary';

export type BaseRule = {
	ruleType: RuleType;
	pattern?: string;
	replacement?: string;
	category?: string;
	tags: number[];
	description?: string;
	ignore: boolean;
	forceRedRule?: boolean;
	advisoryRule?: boolean;
	revisionId: number;
	createdBy: string;
	createdAt: string;
	updatedBy: string;
	updatedAt: string;
	id?: number;
	externalId?: string;
	isArchived: boolean;
	isPublished: boolean;
	hasUnpublishedChanges: boolean;
};

export type DraftRule = BaseRule;
export type LiveRule = BaseRule & {
	reason: string;
};

export type RuleData = {
	draft: DraftRule;
	live: LiveRule[];
};

export function useRule(ruleId: number | undefined) {
	const [isLoading, setIsLoading] = useState(false);
	const [isPublishing, setIsPublishing] = useState(false);
	const [isValidating, setIsValidating] = useState(false);
	const [publishValidationErrors, setPublishValidationErrors] = useState<
		FormError[] | undefined
	>(undefined);
	const [errors, setErrors] = useState<string | undefined>(undefined);
	const [ruleData, setRuleData] = useState<RuleData | undefined>(undefined);
	const [ruleStatus, setRuleStatus] = useState<RuleStatus>('draft');
	const [isDiscarding, setIsDiscarding] = useState(false);

	const setRuleDataAndClearErrors = (ruleData: RuleData) => {
		setRuleData(ruleData);
		setErrors(undefined);
	};

	const fetchRule = async (ruleId: number) => {
		setIsLoading(true);
		setIsValidating(true); // Mark the rule as pending validation until the server tells us otherwise

		try {
			const response = await fetch(`${location.origin}/api/rules/${ruleId}`);
			if (!response.ok) {
				throw new Error(
					`Failed to fetch rules: ${response.status} ${response.statusText}`,
				);
			}
			const rules: RuleData = await response.json();
			setRuleDataAndClearErrors(rules);
		} catch (error) {
			setErrors(errorToString(error));
		}

		setIsLoading(false);
	};

	const publishRule = async (ruleId: number, reason: string) => {
		setIsPublishing(true);

		try {
			const response = await fetch(
				`${location.origin}/api/rules/${ruleId}/publish`,
				{
					method: 'POST',
					headers: [['Content-Type', 'application/json']],
					body: JSON.stringify({ reason }),
				},
			);
			if (!response.ok) {
				throw new Error(
					`Failed to publish rules: ${response.status} ${response.statusText}`,
				);
			}

			const rules: RuleData = await response.json();
			setRuleDataAndClearErrors(rules);
		} catch (error) {
			setErrors(errorToString(error));
		} finally {
			setIsPublishing(false);
		}
	};

	const archiveRule = async (ruleId: number) => {
		setIsLoading(true);

		try {
			const response = await fetch(
				`${location.origin}/api/rules/${ruleId}/archive`,
				{
					method: 'POST',
				},
			);

			if (!response.ok) {
				throw new Error(
					`Failed to archive rule: ${response.status} ${response.statusText}`,
				);
			}

			const rules = await response.json();
			setRuleDataAndClearErrors(rules);
		} catch (error) {
			setErrors(errorToString(error));
		} finally {
			setIsLoading(false);
		}
	};

	const unarchiveRule = async (ruleId: number) => {
		setIsLoading(true);

		try {
			const response = await fetch(
				`${location.origin}/api/rules/${ruleId}/unarchive`,
				{
					method: 'POST',
				},
			);

			if (!response.ok) {
				throw new Error(
					`Failed to unarchive rule: ${response.status} ${response.statusText}`,
				);
			}

			const rules: RuleData = await response.json();
			setRuleDataAndClearErrors(rules);
		} catch (error) {
			setErrors(errorToString(error));
		} finally {
			setIsLoading(false);
		}
	};

	const unpublishRule = async (ruleId: number) => {
		setIsLoading(true);

		try {
			const response = await fetch(
				`${location.origin}/api/rules/${ruleId}/unpublish`,
				{
					method: 'POST',
				},
			);

			if (!response.ok) {
				throw new Error(
					`Failed to unpublish rule: ${response.status} ${response.statusText}`,
				);
			}

			const rules: RuleData = await response.json();
			setRuleDataAndClearErrors(rules);
		} catch (error) {
			setErrors(errorToString(error));
		} finally {
			setIsLoading(false);
		}
	};

	const validateRule = async (ruleId: number) => {
		setIsValidating(false);

		try {
			const response = await fetch(
				`${location.origin}/api/rules/${ruleId}/publish`,
			);
			if (response.status === 200) {
				const validationErrors: FormError[] = await response.json();
				if (validationErrors.length > 0) {
					setPublishValidationErrors(
						validationErrors.map(({ key, message }) => ({
							key: key.replace('invalid-', ''),
							message,
						})),
					);
				} else {
					setPublishValidationErrors(undefined);
				}
				return;
			}
			setPublishValidationErrors(undefined);
		} catch (error) {
			setPublishValidationErrors([
				{
					key: 'Error contacting server',
					message: 'The server could not be contacted',
				},
			]);
		} finally {
			setIsValidating(false);
		}
	};

	const resetPublishValidationErrors = () =>
		setPublishValidationErrors(undefined);

	const updateRule = async (ruleForm: DraftRule) => {
		setIsLoading(true);

		// We would always expect the ruleForm to include an ID when updating a rule
		if (!ruleForm.id)
			return {
				status: 'error',
				errorMessage: 'Update endpoint requires a rule ID',
			} as ErrorIResponse;

		try {
			const response = await fetch(
				`${location.origin}/api/rules/${ruleForm.id}`,
				{
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
					},
					body: JSON.stringify(ruleForm),
				},
			);

			const parsedResponse = await responseHandler<DraftRule>(response);
			if (parsedResponse.status === 'ok') {
				setRuleDataAndClearErrors({
					...(ruleData || { live: [] }),
					draft: parsedResponse.data,
				});
			} else {
				setErrors(parsedResponse.errorMessage);
			}
		} catch (error) {
			setErrors(errorToString(error));
		} finally {
			setIsLoading(false);
		}
	};

	const createRule = async (ruleForm: DraftRule) => {
		setIsLoading(true);

		try {
			const createRuleResponse = await fetch(`${location.origin}/api/rules`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify(ruleForm),
			});

			const parsedResponse = await responseHandler<DraftRule>(
				createRuleResponse,
			);
			if (parsedResponse.status === 'ok') {
				setRuleDataAndClearErrors({
					...(ruleData || { live: [] }),
					draft: parsedResponse.data,
				});
			} else {
				setErrors(parsedResponse.errorMessage);
			}
		} catch (error) {
			setErrors(errorToString(error));
		} finally {
			setIsLoading(false);
		}
	};

	const discardRuleChanges = async (ruleId: number) => {
		setIsDiscarding(true);

		try {
			const response = await fetch(
				`${location.origin}/api/rules/${ruleId}/discard-changes`,
				{
					method: 'POST',
				},
			);

			if (!response.ok) {
				throw new Error(
					`Failed to discard changes: ${response.status} ${response.statusText}`,
				);
			}

			const rules: RuleData = await response.json();
			setRuleDataAndClearErrors(rules);
		} catch (error) {
			setErrors(errorToString(error));
		} finally {
			setIsDiscarding(false);
		}
	};

	useEffect(() => {
		if (ruleId) {
			fetchRule(ruleId);
		} else {
			setRuleData(undefined);
		}
	}, [ruleId]);

	useEffect(() => {
		setRuleStatus(getRuleStatus(ruleData?.draft));
	}, [ruleData]);

	return {
		fetchRule,
		updateRule,
		createRule,
		isLoading,
		errors,
		rule: ruleData,
		publishRule,
		isPublishing,
		validateRule,
		isValidating,
		publishValidationErrors,
		resetPublishValidationErrors,
		archiveRule,
		unarchiveRule,
		unpublishRule,
		ruleStatus,
		isDiscarding,
		discardRuleChanges,
	};
}
