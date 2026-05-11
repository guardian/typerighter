import { FormError } from '../RuleForm';
import { getRuleStatus, RuleStatus } from '../../utils/rule';
import useSWR from 'swr';
import useSWRMutation from 'swr/mutation';

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

const fetchGET = (input: RequestInfo | URL, init?: RequestInit) =>
	fetch(input, init).then((res) => res.json());

const fetchPOST = (url: string, payload: unknown) =>
	fetch(url, {
		method: 'POST',
		body: JSON.stringify(payload),
	}).then((res) => res.json());

export const useRule = (ruleId: number) =>
	useSWR(`/api/rules/${ruleId}`, fetchGET);

export const usePublishRule = (ruleId: number) =>
	useSWRMutation(
		`/api/rules/${ruleId}`,
		async (url: string, { arg: { reason } }: { arg: { reason: string } }) => {
			await fetch(`${url}/publish`, {
				method: 'POST',
				headers: [['Content-Type', 'application/json']],
				body: JSON.stringify({ reason }),
			});
		},
	);

export const useArchiveRule = (ruleId: number) =>
	useSWRMutation(`/api/rules/${ruleId}`, async (url: string) =>
		fetch(`${url}/archive`, {
			method: 'POST',
		}),
	);

export const useUnarchiveRule = (ruleId: number) =>
	useSWRMutation(`/api/rules/${ruleId}`, async (url: string) =>
		fetch(`${url}/unarchive`, {
			method: 'POST',
		}),
	);

export const useUnpublishRule = (ruleId: number) =>
	useSWRMutation(`/api/rules/${ruleId}`, async (url: string) =>
		fetch(`${url}/unpublish`, {
			method: 'POST',
		}),
	);

const validateRule = async (ruleId: number) => {
	const response = await fetch(
		`${location.origin}/api/rules/${ruleId}/publish`,
	);
	if (response.status === 200) {
		const validationErrors: FormError[] = await response.json();
		const renamedValidationErrors = validationErrors.map(
			({ key, message }) => ({
				key: key.replace('invalid-', ''),
				message,
			}),
		);
	}
};

const useUpdateRule = (ruleForm: DraftRule) =>
	useSWRMutation(
		`/api/rules/${ruleForm.id}`,
		async (
			url: string,
			{ arg: { ruleForm } }: { arg: { ruleForm: DraftRule } },
		) => {
			fetch(`${url}`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify(ruleForm),
			});
		},
	);

const createRule = async (ruleForm: DraftRule) => {
	fetch(`${location.origin}/api/rules`, {
		method: 'POST',
		headers: {
			'Content-Type': 'application/json',
		},
		body: JSON.stringify(ruleForm),
	});
};

const discardRuleChanges = async (ruleId: number) => {
	fetch(`${location.origin}/api/rules/${ruleId}/discard-changes`, {
		method: 'POST',
	});
};
