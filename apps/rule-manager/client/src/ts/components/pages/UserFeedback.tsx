import React, { useState } from 'react';
import { EuiSpacer } from '@elastic/eui';
import { EuiFieldSearch } from '@elastic/eui/src/components/form/field_search';
import { useUserFeedback } from '../hooks/useUserFeedback';
import { useDebouncedValue } from '../hooks/useDebounce';
import { UserFeedbackTable } from '../table/UserFeedbackTable';
import { FullHeightContentWithFixedHeader } from '../layout/FullHeightContentWithFixedHeader';

export const UserFeedback = () => {
	const [queryStr, setQueryStr] = useState('');
	const [pageIndex, setPageIndex] = useState(0);
	const debouncedQueryStr = useDebouncedValue(queryStr, 200);

	const { data: feedbackData, isLoading } = useUserFeedback(
		pageIndex,
		debouncedQueryStr,
	);

	const header = (
		<>
			<EuiFieldSearch
				placeholder="Search feedback by message, email, matched text, or rule ID"
				fullWidth
				value={queryStr}
				onChange={(e) => {
					setQueryStr(e.target.value);
					setPageIndex(0);
				}}
			/>
			<EuiSpacer size="m" />
		</>
	);

	const content = feedbackData ? (
		<UserFeedbackTable
			feedbackData={feedbackData}
			isLoading={isLoading}
			pageIndex={pageIndex}
			setPageIndex={setPageIndex}
		/>
	) : null;

	return (
		<FullHeightContentWithFixedHeader header={header} content={content} />
	);
};
