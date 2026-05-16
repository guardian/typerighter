import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';

type RuleSearchParams = {
	queryStr: string;
	tagIds: number[];
	ruleTypeOptions: string[];
};

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
	useEffect(() => {
		const incomingTagIds: number[] = [];
		const incomingRuleTypeOptions: string[] = [];
		searchParams.forEach((val, key) => {
			switch (key) {
				case 'tagId': {
					const tagId = Number(val);
					if (!isNaN(tagId)) {
						incomingTagIds.push(tagId);
					}
					break;
				}
				case 'ruleTypeOptions': {
					incomingRuleTypeOptions.push(val);
					break;
				}
			}
		});

		setQueryStr(searchParams.get('queryStr') ?? '');
		if (JSON.stringify(incomingTagIds) !== JSON.stringify(tagIds)) {
			setTagIds(incomingTagIds);
		}
		if (
			JSON.stringify(incomingRuleTypeOptions) !==
			JSON.stringify(ruleTypeOptions)
		) {
			setRuleTypeOptions(incomingRuleTypeOptions);
		}
	}, [searchParams]);

	const ruleSearchParams = useMemo(
		() => ({
			queryStr,
			tagIds,
			ruleTypeOptions,
		}),
		[queryStr, tagIds, ruleTypeOptions],
	);

	const setRuleSearchParams = useMemo(
		() => (rs: RuleSearchParams) => {
			const params = new URLSearchParams();
			params.append('queryStr', rs.queryStr);
			for (const tagId of rs.tagIds) {
				params.append('tagId', tagId.toString());
			}
			for (const ruleTypeOption of rs.ruleTypeOptions) {
				params.append('ruleTypeOptions', ruleTypeOption);
			}
			setSearchParams(params);
		},
		[setSearchParams],
	);

	return [ruleSearchParams, setRuleSearchParams];
};
