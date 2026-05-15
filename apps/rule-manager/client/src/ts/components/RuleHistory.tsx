import { EuiFormRow, EuiIcon } from '@elastic/eui';
import React from 'react';
import { RuleFormSection } from './RuleFormSection';
import { RuleData } from './hooks/useRule';
import { maybeGetNameFromEmail } from '../utils/user';
import styled from '@emotion/styled';
import { LineBreak } from './LineBreak';
import { friendlyTimestampFormat } from '../utils/date';
import { Person } from './icons/person';
import { format } from 'date-fns';
import {
	Event,
	EventTimelineContainer,
	EventTimelinePersonContainer,
	EventDetails,
	EventDetailsHeader,
	EventDetailsBody,
} from './EventTimeline';

const SheetIconContainer = styled.div`
	padding: 7px 8px;
`;
const SheetIcon = () => (
	<SheetIconContainer>
		<EuiIcon type="pageSelect" />
	</SheetIconContainer>
);

export const RuleHistory = ({
	ruleHistory,
}: {
	ruleHistory: RuleData['live'];
}) => {
	const sortedHistory = ruleHistory
		.concat()
		.sort((a, b) => (a.revisionId > b.revisionId ? -1 : 1));
	return (
		<RuleFormSection title="PUBLICATION HISTORY">
			<LineBreak />
			<EuiFormRow>
				<>
					{!sortedHistory.length && 'This rule has not yet been published.'}
					{sortedHistory.map((rule, index) => {
						const isFirst = index === sortedHistory.length - 1;
						return (
							<Event key={rule.revisionId}>
								<EventTimelineContainer isFirst={isFirst}>
									<EventTimelinePersonContainer>
										{rule.updatedBy.includes('Google Sheet') ? (
											<SheetIcon />
										) : (
											<Person />
										)}
									</EventTimelinePersonContainer>
								</EventTimelineContainer>
								<EventDetails isFirst={isFirst}>
									<EventDetailsHeader>
										<strong>{maybeGetNameFromEmail(rule.updatedBy)}</strong>,{' '}
										{format(new Date(rule.updatedAt), friendlyTimestampFormat)}
									</EventDetailsHeader>
									<EventDetailsBody>{rule.reason}</EventDetailsBody>
								</EventDetails>
							</Event>
						);
					})}
				</>
			</EuiFormRow>
		</RuleFormSection>
	);
};
