import { addDays, format, isWithinInterval, startOfDay } from 'date-fns';
import { usePeriodicRefresh } from './hooks/usePeriodicRefresh';
import { RuleData } from './hooks/useRule';
import { EuiFlexGroup, EuiIcon, EuiLoadingSpinner } from '@elastic/eui';
import React from 'react';

export const RuleDataLastUpdated: React.FC<{
	ruleData: RuleData;
	isLoading: boolean;
	hasErrors: boolean;
}> = ({ isLoading, ruleData, hasErrors }) => {
	usePeriodicRefresh(10000);

	const latestUpdatedAt = Math.max(
		new Date(ruleData.draft.updatedAt).getTime(),
		new Date(ruleData.live[0]?.updatedAt || 0).getTime(),
	);

	const now = new Date();
	const isLessThanADayOld = isWithinInterval(new Date(latestUpdatedAt), {
		start: startOfDay(now),
		end: now,
	});
	const formatStr = isLessThanADayOld ? 'HH:mm:ss' : 'do LLL yyyy';

	const iconColor = hasErrors ? 'danger' : 'success';
	const iconName = hasErrors ? 'error' : 'checkInCircleFilled';
	const text = hasErrors
		? 'Error saving changes'
		: `Saved ${format(latestUpdatedAt, formatStr)}`;

	return (
		<EuiFlexGroup gutterSize="s" alignItems="center">
			<span>{text}</span>
			{isLoading ? (
				<EuiLoadingSpinner size="s" />
			) : (
				<EuiIcon color={iconColor} type={iconName} size="s" />
			)}
		</EuiFlexGroup>
	);
};
