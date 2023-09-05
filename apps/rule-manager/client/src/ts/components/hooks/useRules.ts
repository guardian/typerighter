import { useEffect, useState } from 'react';
import { errorToString } from '../../utils/error';
import { BaseRule, DraftRule } from './useRule';

export type PaginatedResponse<Data> = {
	data: Data[];
	page: number;
	pageSize: number;
	pages: number;
	total: number;
};

export type PaginatedRuleData = {
	data: DraftRule[];
	loadedRules: Set<number>;
	pageSize: number;
	total: number;
};

export type SortColumns = Array<{
	id: string;
	direction: 'asc' | 'desc';
}>;

const pageSize = 100;

export function useRules() {
	const { location } = window;
	const [ruleData, setRulesData] = useState<PaginatedRuleData | null>(null);
	const [isLoading, setIsLoading] = useState(false);
	const [error, setError] = useState<string | undefined>(undefined);
	const [isRefreshing, setIsRefreshing] = useState(false);
	const fetchRules = async (
		pageIndex: number = 0,
		queryStr: string = '',
		sortColumns: SortColumns = [],
	): Promise<void> => {
		setIsLoading(true);
		try {
			const page = pageIndex + 1;
			const queryParams = new URLSearchParams({
				page: page.toString(),
				...(queryStr ? { queryStr } : {}),
			});
			sortColumns.forEach((colAndDir) =>
				queryParams.append(
					'sortBy',
					`${colAndDir.direction === 'asc' ? '+' : '-'}${colAndDir.id}`,
				),
			);
			const response = await fetch(
				`${location.origin}/api/rules?${queryParams}`,
			);
			if (!response.ok) {
				throw new Error(
					`Failed to fetch rules: ${response.status} ${response.statusText}`,
				);
			}

			const incomingRuleData =
				(await response.json()) as PaginatedResponse<DraftRule>;

			setRulesData((currentRuleDataData) => {
				const loadedRules = new Set([
					...(currentRuleDataData?.loadedRules ?? []),
					...incomingRuleData.data.map(
						(_, index) => (page - 1) * pageSize + index,
					),
				]);

				const data: BaseRule[] = [];
				incomingRuleData.data.forEach((rule, index) => {
					const offsetIndex = (page - 1) * pageSize + index;
					data[offsetIndex] = rule;
				});

				return {
					data,
					loadedRules,
					pageSize: incomingRuleData?.pageSize ?? 0,
					total: incomingRuleData?.total ?? 0,
				};
			});
		} catch (error) {
			setError(errorToString(error));
		}
		setIsLoading(false);
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
