import { useEffect, useState } from 'react';
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
	loadedRules: Set<number>;
	pageSize: number;
	total: number;
};

const pageSize = 1000;

export function useRules() {
	const { location } = window;
	const [ruleData, setRulesData] = useState<PaginatedRuleData | null>(null);
	const [isLoading, setIsLoading] = useState(false);
	const [error, setError] = useState<string | undefined>(undefined);
	const [isRefreshing, setIsRefreshing] = useState(false);
	const fetchRules = async (startIndex: number = 0, queryStr?: string): Promise<void> => {
		setIsLoading(true);
		const page = Math.floor(startIndex / pageSize) + 1;
		try {
			const response = await fetch(`${location.origin}/api/rules?page=${page}${queryStr ? `&queryStr=${queryStr}` : ''}`);
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

				const data = [...(currentRuleDataData?.data || [])];
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

	useEffect(() => {
		fetchRules();
	}, []);

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
