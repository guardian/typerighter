import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { isEqual } from 'lodash';

type RuleSearchParams = {
	queryStr: string;
	tagIds: number[];
	ruleTypeOptions: string[];
	ruleSelection: Set<number>;
};

const queryStrParamKey = 'queryStr';
const tagIdsParamKey = 'tagIds';
const ruleTypeOptionsParamKey = 'ruleTypeOptions';
const ruleSelectionParamKey = 'selectedRules';

export const useRuleSearchParams = (): [
	RuleSearchParams,
	(rs: RuleSearchParams) => void,
] => {
	const [searchParams, setSearchParams] = useSearchParams();

	/**
	 * It's necessary to take care to preserve object identities here, as a
	 * change to the query string should not change the identity of the tag or
	 * ruleType options arrays.
	 */
	const [queryStr, setQueryStr] = useState('');
	const [tagIds, setTagIds] = useState([] as number[]);
	const [ruleTypeOptions, setRuleTypeOptions] = useState([] as string[]);
	const [ruleSelection, setRuleSelection] = useState(new Set<number>());
	useEffect(() => {
		const incomingTagIds: number[] = [];
		const incomingRuleTypeOptions: string[] = [];
		const incomingRuleSelection = new Set<number>();
		searchParams.forEach((val, key) => {
			switch (key) {
				case tagIdsParamKey: {
					const tagId = Number(val);
					if (!isNaN(tagId)) {
						incomingTagIds.push(tagId);
					}
					break;
				}
				case ruleTypeOptionsParamKey: {
					incomingRuleTypeOptions.push(val);
					break;
				}
				case ruleSelectionParamKey: {
					const ruleId = Number(val);
					if (!isNaN(ruleId)) {
						ruleSelection.add(ruleId);
					}
				}
			}
		});

		setQueryStr(searchParams.get('queryStr') ?? '');
		if (!isEqual(incomingTagIds, tagIds)) {
			setTagIds(incomingTagIds);
		}
		if (!isEqual(incomingRuleTypeOptions, ruleTypeOptions)) {
			setRuleTypeOptions(incomingRuleTypeOptions);
		}
		if (!isEqual(incomingRuleSelection, ruleSelection)) {
			setRuleSelection(incomingRuleSelection);
		}
	}, [searchParams]);

	const ruleSearchParams = useMemo(
		() => ({
			queryStr,
			tagIds,
			ruleTypeOptions,
			ruleSelection,
		}),
		[queryStr, tagIds, ruleTypeOptions, ruleSelection],
	);

	const setRuleSearchParams = useMemo(
		() => (rs: RuleSearchParams) => {
			const params = new URLSearchParams();
			params.append(queryStrParamKey, rs.queryStr);
			for (const tagId of rs.tagIds) {
				params.append(tagIdsParamKey, tagId.toString());
			}
			for (const ruleTypeOption of rs.ruleTypeOptions) {
				params.append(ruleTypeOptionsParamKey, ruleTypeOption);
			}
			for (const ruleId of rs.ruleSelection) {
				params.append(ruleSelectionParamKey, ruleId.toString());
			}
			setSearchParams(params);
		},
		[setSearchParams],
	);

	return [ruleSearchParams, setRuleSearchParams];
};
