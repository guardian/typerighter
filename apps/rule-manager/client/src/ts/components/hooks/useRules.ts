import { useState } from 'react';
import { errorToString } from '../../utils/error';
import { DraftRule } from './useRule';
import useSWR from 'swr';
import { fetchGET } from '../../utils/api';

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

export const useRulesSWR = (
	pageIndex: number = 0,
	queryStr: string = '',
	sortColumns: SortColumns = [],
	tags: number[] = [],
	ruleTypes: string[] = [],
) => {
	const page = pageIndex + 1;
	const queryParams = new URLSearchParams({
		page: page.toString(),
		...(queryStr ? { queryStr } : {}),
	});

	tags.forEach((tag) => queryParams.append('tags', tag.toString()));
	ruleTypes.forEach((ruleType) => queryParams.append('ruleTypes', ruleType));

	sortColumns.forEach((colAndDir) =>
		queryParams.append(
			'sortBy',
			`${colAndDir.direction === 'asc' ? '+' : '-'}${colAndDir.id}`,
		),
	);

	return useSWR<PaginatedRuleData>(
		`${location.origin}/api/rules?${queryParams}`,
		fetchGET,
	);
};

export function useRules() {
	const { location } = window;
	const [ruleData, setRulesData] = useState<PaginatedRuleData | null>(null);
	const [isLoading, setIsLoading] = useState(false);
	const [error, setError] = useState<string | undefined>(undefined);
	const [isRefreshing, setIsRefreshing] = useState(false);

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
	};
}
