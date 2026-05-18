import useSWR from 'swr';
import { PaginatedResponse } from './useRules';

export type UserFeedbackItem = {
	id: number;
	app: string;
	stage: string;
	documentUrl: string;
	feedbackMessage: string;
	userEmail: string;
	matchId: string | null;
	externalRuleId: string | null;
	ruleId: number | null;
	documentId: string | null;
	matcherType: string | null;
	suggestion: string | null;
	matchIsMarkedAsCorrect: boolean | null;
	matchIsAdvisory: boolean | null;
	matchHasReplacement: boolean | null;
	matchedText: string | null;
	matchContext: string | null;
	createdAt: string;
};

const fetcher = (url: string) => fetch(url).then((res) => res.json());

export function useUserFeedback(pageIndex: number, queryStr: string) {
	const params = new URLSearchParams({ page: (pageIndex + 1).toString() });
	if (queryStr) {
		params.set('queryStr', queryStr);
	}

	return useSWR<PaginatedResponse<UserFeedbackItem>>(
		`/api/user-feedback?${params}`,
		fetcher,
	);
}
