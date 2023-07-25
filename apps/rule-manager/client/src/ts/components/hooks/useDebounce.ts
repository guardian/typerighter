import React, { useState, useEffect } from 'react';

/**
 * Debounce changes to the given value, returning the debounced value.
 */
export const useDebouncedValue = <T>(value: T, timeoutMs: number): T => {
	const [state, setState] = useState(value);

	useEffect(() => {
		const handler = setTimeout(() => setState(value), timeoutMs);

		return () => clearTimeout(handler);
	}, [value, timeoutMs]);

	return state;
};
