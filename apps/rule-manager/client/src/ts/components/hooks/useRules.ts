import { useState } from 'react';
import { errorToString } from '../../utils/error';
import { DraftRule } from './useRule';

export type PaginatedResponse<Data> = {
	data: Data[];
	page: number;
	pageSize: number;
	pages: number;
	total: number;
};

export type PaginatedRuleData = {
	data: DraftRule[];
	pageSize: number;
	total: number;
};

export type SortColumns = Array<{
	id: string;
	direction: 'asc' | 'desc';
}>;

export function useRules() {
	const { location } = window;
	const [ruleData, setRulesData] = useState<PaginatedRuleData | null>(null);
	const [isLoading, setIsLoading] = useState(false);
	const [error, setError] = useState<string | undefined>(undefined);
	const [isRefreshing, setIsRefreshing] = useState(false);
	const [abortController, setAbortController] = useState<AbortController>();
	const fetchRules = async (
		pageIndex: number = 0,
		queryStr: string = '',
		sortColumns: SortColumns = [],
		tags: number[] = [],
		ruleTypes: string[] = [],
	): Promise<void> => {
		setIsLoading(true);

		abortController?.abort();
		const newAbortController = new AbortController();
		setAbortController(newAbortController);

		try {
			const page = pageIndex + 1;
			const queryParams = new URLSearchParams({
				page: page.toString(),
				...(queryStr ? { queryStr } : {}),
			});

			tags.forEach((tag) => queryParams.append('tags', tag.toString()));
			ruleTypes.forEach((ruleType) =>
				queryParams.append('ruleTypes', ruleType),
			);

			sortColumns.forEach((colAndDir) =>
				queryParams.append(
					'sortBy',
					`${colAndDir.direction === 'asc' ? '+' : '-'}${colAndDir.id}`,
				),
			);
			const response = await fetch(
				`${location.origin}/api/rules?${queryParams}`,
				{ signal: newAbortController.signal }
			);
			if (!response.ok) {
				throw new Error(
					`Failed to fetch rules: ${response.status} ${response.statusText}`,
				);
			}

			const incomingRuleData =
				(await response.json()) as PaginatedResponse<DraftRule>;

			setRulesData({
				data: incomingRuleData.data,
				pageSize: incomingRuleData?.pageSize ?? 0,
				total: incomingRuleData?.total ?? 0,
			});
		} catch (error) {
			// Do not expose aborts to the user
			if (!newAbortController.signal.aborted) {
				setError(errorToString(error));
			}
		}
		// If the request has been aborted, it has been superceded — do not reset the loading state
		if (!newAbortController.signal.aborted) {
			setIsLoading(false);
		}
	};

	const refreshRules = async (): Promise<void> => {
		setIsRefreshing(true);
		try {
			const updatedRulesResponse = await fetch(
				`${location.origin}/api/refresh`,
				{
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
					},
				},
			);
			if (!updatedRulesResponse.ok) {
				throw new Error(
					`Failed to refresh rules: ${updatedRulesResponse.status} ${updatedRulesResponse.statusText}`,
				);
			}
			const rules = await updatedRulesResponse.json();
			setRulesData(rules);
		} catch (e) {
			setError(errorToString(error));
		} finally {
			setIsRefreshing(false);
		}
	};

	const refreshDictionaryRules = async (): Promise<void> => {
		setIsRefreshing(true);
		try {
			const updatedRulesResponse = await fetch(
				`${location.origin}/api/refreshDictionary`,
				{
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
					},
				},
			);
			if (!updatedRulesResponse.ok) {
				throw new Error(
					`Failed to refresh rules: ${updatedRulesResponse.status} ${updatedRulesResponse.statusText}`,
				);
			}
			await updatedRulesResponse.json();
		} catch (e) {
			setError(errorToString(error));
		} finally {
			setIsRefreshing(false);
		}
	};

	return {
		ruleData,
		isLoading,
		error,
		refreshRules,
		refreshDictionaryRules,
		isRefreshing,
		setError,
		fetchRules,
	};
}
