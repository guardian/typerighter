import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { isEqual, identity } from 'lodash';

export const useStringParam = (paramKey: string) => {
	const [searchParams, setSearchParams] = useSearchParams();

	const paramValue = useMemo(
		() => searchParams.get(paramKey) || '',
		[searchParams],
	);

	const setParamValue = useMemo(
		() => (value: string) => {
			searchParams.set(paramKey, value);
			setSearchParams(searchParams);
		},
		[searchParams],
	);

	return [paramValue, setParamValue] as const;
};

const getArrayValuesFromSearchParams = <T>(
	searchParams: URLSearchParams,
	paramKey: string,
	transformer: (paramVal: string) => T,
) => {
	const incomingValue: T[] = [];
	searchParams.forEach((val, key) => {
		if (key === paramKey) {
			incomingValue.push(transformer(val));
		}
	});
	return incomingValue;
};

const getSetValuesFromSearchParams = <T>(
	searchParams: URLSearchParams,
	paramKey: string,
	transformer: (paramVal: string) => T,
) => {
	const incomingValue = new Set<T>();
	searchParams.forEach((val, key) => {
		if (key === paramKey) {
			incomingValue.add(transformer(val));
		}
	});
	return incomingValue;
};

const useCollectionParam = <I extends Set<T> | Array<T>, T = string>(
	paramKey: string,
	transformIn: (paramVal: string) => T = identity,
	transformOut: (paramVal: T) => string = identity,
	getValuesFromSearchParams: (
		searchParams: URLSearchParams,
		paramKey: string,
		transformer: (paramVal: string) => T,
	) => I,
) => {
	const [searchParams, setSearchParams] = useSearchParams();
	const [localParamValue, setLocalParamValue] = useState<I>(() =>
		getValuesFromSearchParams(searchParams, paramKey, transformIn),
	);

	useEffect(() => {
		const incomingValue = getValuesFromSearchParams(
			searchParams,
			paramKey,
			transformIn,
		);

		/**
		 * We take care to preserve object identities here, as a change to the
		 * query string should not change the identity of the tag or ruleType
		 * options arrays.
		 */
		if (!isEqual(incomingValue, localParamValue)) {
			setLocalParamValue(incomingValue);
		}
	}, [searchParams]);

	const setParamValue = useMemo(
		() => (newValue: I) => {
			searchParams.delete(paramKey);
			for (const value of newValue) {
				searchParams.append(paramKey, transformOut(value));
			}
			setSearchParams(searchParams);
		},
		[searchParams],
	);

	return [localParamValue, setParamValue] as const;
};

export const useArrayParam = <T = string>(
	paramKey: string,
	transformIn: (paramVal: string) => T = identity,
	transformOut: (paramVal: T) => string = identity,
) =>
	useCollectionParam(
		paramKey,
		transformIn,
		transformOut,
		getArrayValuesFromSearchParams,
	);

export const useSetParam = <T = string>(
	paramKey: string,
	transformIn: (paramVal: string) => T = identity,
	transformOut: (paramVal: T) => string = identity,
) =>
	useCollectionParam(
		paramKey,
		transformIn,
		transformOut,
		getSetValuesFromSearchParams,
	);
