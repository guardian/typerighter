import React from 'react';
import { RuleFormSection } from './RuleFormSection';
import { UserFeedback } from './hooks/useRule';
import { LineBreak } from './LineBreak';
import { format } from 'date-fns';
import { friendlyTimestampFormat } from '../utils/date';
import styled from '@emotion/styled';
import {
	Event,
	EventDetails,
	EventDetailsBody,
	EventDetailsHeader,
	EventTimelineContainer,
	EventTimelinePersonContainer,
} from './EventTimeline';
import { Person } from './icons/person';

type FeedbackRow = {
	field: string;
	value: string;
};

const FeedbackTable = styled.table`
	width: 100%;
	border-collapse: collapse;
`;

const FieldNameCell = styled.td`
	font-weight: bold;
	padding: 4px 0;
`;

const ValueCell = styled.td`
	padding: 4px 0;
`;

const getFeedbackRows = (feedback: UserFeedback): FeedbackRow[] => {
	const rows: FeedbackRow[] = [
		{
			field: 'Date',
			value: format(new Date(feedback.createdAt), friendlyTimestampFormat),
		},
		{ field: 'Message', value: feedback.feedbackMessage },
		{ field: 'User', value: feedback.userEmail },
	];

	if (feedback.matchedText) {
		rows.push({ field: 'Matched text', value: feedback.matchedText });
	}
	if (feedback.matchContext) {
		rows.push({ field: 'Context', value: feedback.matchContext });
	}
	if (feedback.suggestion) {
		rows.push({ field: 'Suggestion', value: feedback.suggestion });
	}
	if (feedback.matchIsMarkedAsCorrect !== undefined) {
		rows.push({
			field: 'Marked as correct',
			value: feedback.matchIsMarkedAsCorrect ? 'Yes' : 'No',
		});
	}
	if (feedback.documentUrl) {
		rows.push({ field: 'Document URL', value: feedback.documentUrl });
	}

	return rows;
};

export const RuleFeedback = ({ feedback }: { feedback: UserFeedback[] }) => {
	return (
		<RuleFormSection title="USER FEEDBACK">
			<LineBreak />
			<>
				{!feedback.length && 'No feedback has been submitted for this rule.'}
				{feedback.map((item, index) => (
					<React.Fragment key={item.id ?? index}>
						<Event>

							<EventDetails isFirst={index === feedback.length - 1}>
								<EventDetailsHeader>
									<strong>{item.userEmail}</strong>,{' '}
									{format(new Date(item.createdAt), friendlyTimestampFormat)}
								</EventDetailsHeader>
								<EventDetailsBody>
									<FeedbackTable>
										<tbody>
											{getFeedbackRows(item).map((row) => (
												<tr key={row.field}>
													<FieldNameCell>{row.field}</FieldNameCell>
													<ValueCell>{row.value}</ValueCell>
												</tr>
											))}
										</tbody>
									</FeedbackTable>
								</EventDetailsBody>
							</EventDetails>
						</Event>
					</React.Fragment>
				))}
			</>
		</RuleFormSection>
	);
};
