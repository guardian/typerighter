import React, { useEffect, useState } from 'react';
import {
	EuiButton,
	EuiButtonEmpty,
	EuiConfirmModal,
	EuiFormRow,
	EuiText,
	EuiTextArea,
} from '@elastic/eui';
import { RuleFormSection } from './RuleFormSection';
import { UserFeedback } from './hooks/useRule';
import { useAddressFeedback } from './hooks/useAddressFeedback';
import { useUpdateNotes } from './hooks/useUpdateNotes';
import { LineBreak } from './LineBreak';
import { format } from 'date-fns';
import { friendlyTimestampFormat } from '../utils/date';
import styled from '@emotion/styled';
import {
	Event,
	EventDetails,
	EventDetailsBody,
	EventDetailsHeader,
} from './EventTimeline';

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

const HeaderRow = styled.div`
	display: flex;
	justify-content: space-between;
	align-items: center;
`;

const getFeedbackRows = (feedback: UserFeedback): FeedbackRow[] => {
	const rows: FeedbackRow[] = [
		{ field: 'Message', value: feedback.feedbackMessage },
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

const getActionedRows = (feedback: UserFeedback): FeedbackRow[] => {
	const rows: FeedbackRow[] = [];
	if (feedback.lastAddressedAt) {
		rows.push({
			field: 'Addressed at',
			value: format(
				new Date(feedback.lastAddressedAt),
				friendlyTimestampFormat,
			),
		});
	}
	if (feedback.lastAddressedBy) {
		rows.push({ field: 'Addressed by', value: feedback.lastAddressedBy });
	}
	return rows;
};

const DebouncedNotesInput = ({
	feedbackId,
	initialNotes,
	onUpdateNotes,
}: {
	feedbackId: number;
	initialNotes: string;
	onUpdateNotes: (feedbackId: number, notes: string) => void;
}) => {
	const [notes, setNotes] = useState(initialNotes);

	useEffect(() => {
		if (notes === initialNotes) return;
		const timeout = setTimeout(() => {
			onUpdateNotes(feedbackId, notes);
		}, 500);
		return () => clearTimeout(timeout);
	}, [notes, feedbackId, onUpdateNotes, initialNotes]);

	return (
		<EuiFormRow label="Notes">
			<EuiTextArea
				value={notes}
				onChange={(e) => setNotes(e.target.value)}
				rows={2}
			/>
		</EuiFormRow>
	);
};

const NoteContainer = styled.div`
	padding: 8px 0;
`

const NotesSection = ({
	feedbackId,
	notes,
	onUpdateNotes,
}: {
	feedbackId: number;
	notes: string;
	onUpdateNotes: (feedbackId: number, notes: string) => void;
}) => {
	const [isOpen, setIsOpen] = useState(false);

	if (isOpen) {
		return (
			<NoteContainer>
				<DebouncedNotesInput
					feedbackId={feedbackId}
					initialNotes={notes}
					onUpdateNotes={onUpdateNotes}
				/>
				<EuiButtonEmpty flush='left' size="s" onClick={() => setIsOpen(false)}>
					Close notes
				</EuiButtonEmpty>
			</NoteContainer>
		);
	}

	if (notes) {
		return (
			<NoteContainer>
				<EuiText size="s">
					<strong>Notes:</strong> {notes}
				</EuiText>
				<EuiButtonEmpty flush='left' size="s" onClick={() => setIsOpen(true)}>
					Edit notes
				</EuiButtonEmpty>
			</NoteContainer>
		);
	}

	return (
		<EuiText color="subdued" size="s">
			No notes yet.{' '}
			<EuiButtonEmpty size="s" onClick={() => setIsOpen(true)}>
				Add notes
			</EuiButtonEmpty>
		</EuiText>
	);
};

const FeedbackItem = ({
	item,
	index,
	totalCount,
	onActionFeedback,
	onUnactionFeedback,
	onUpdateNotes,
	isActioning,
}: {
	item: UserFeedback;
	index: number;
	totalCount: number;
	onActionFeedback: (feedbackId: number, notes: string) => void;
	onUnactionFeedback: (feedbackId: number) => void;
	onUpdateNotes: (feedbackId: number, notes: string) => void;
	isActioning: boolean;
}) => {
	const [isUnactionModalOpen, setIsUnactionModalOpen] = useState(false);

	return (
		<>
			<Event>
				<EventDetails isFirst={index === totalCount - 1}>
					<EventDetailsHeader>
						<HeaderRow>
							<span>
								From&nbsp;
								<strong>{item.userEmail}</strong>,{' '}
								{format(new Date(item.createdAt), friendlyTimestampFormat)}
							</span>
							{item.id != null && (
								<EuiButton
									size="s"
									onClick={() =>
										item.addressed
											? setIsUnactionModalOpen(true)
											: onActionFeedback(item.id!, item.notes ?? '')
									}
									isLoading={isActioning}
								>
									{item.addressed ? 'Mark as unaddressed' : 'Mark as addressed'}
								</EuiButton>
							)}
						</HeaderRow>
						{item.id != null && (
							<NotesSection
								feedbackId={item.id}
								notes={item.notes ?? ''}
								onUpdateNotes={onUpdateNotes}
							/>
						)}
						<FeedbackTable>
							<tbody>
								{getActionedRows(item).map((row) => (
									<tr key={row.field}>
										<FieldNameCell>{row.field}</FieldNameCell>
										<ValueCell>{row.value}</ValueCell>
									</tr>
								))}
							</tbody>
						</FeedbackTable>
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
			{isUnactionModalOpen && item.id != null && (
				<EuiConfirmModal
					title="Mark as unaddressed"
					onCancel={() => setIsUnactionModalOpen(false)}
					onConfirm={() => {
						onUnactionFeedback(item.id!);
						setIsUnactionModalOpen(false);
					}}
					cancelButtonText="Cancel"
					confirmButtonText="Mark as unaddressed"
					buttonColor="danger"
					isLoading={isActioning}
				>
					<p>Are you sure you want to mark this feedback as unaddressed?</p>
				</EuiConfirmModal>
			)}
		</>
	);
};

export const RuleFeedback = ({
	feedback,
	ruleId,
	fetchRule,
}: {
	feedback: UserFeedback[];
	ruleId: number;
	fetchRule: (ruleId: number) => void;
}) => {
	const { trigger: triggerAddressFeedback, isMutating: isActioning } =
		useAddressFeedback();
	const { trigger: triggerUpdateNotes } = useUpdateNotes();

	const handleActionFeedback = async (feedbackId: number, notes: string) => {
		await triggerAddressFeedback({
			feedbackId,
			addressed: true,
			notes: notes || undefined,
		});
		fetchRule(ruleId);
	};

	const handleUnactionFeedback = async (feedbackId: number) => {
		await triggerAddressFeedback({
			feedbackId,
			addressed: false,
		});
		fetchRule(ruleId);
	};

	const handleUpdateNotes = async (feedbackId: number, notes: string) => {
		await triggerUpdateNotes({ feedbackId, notes });
		fetchRule(ruleId);
	};

	return (
		<RuleFormSection title="USER FEEDBACK">
			<LineBreak />
			<>
				{!feedback.length && 'No feedback has been submitted for this rule.'}
				{feedback.map((item, index) => (
					<FeedbackItem
						key={item.id ?? index}
						item={item}
						index={index}
						totalCount={feedback.length}
						onActionFeedback={handleActionFeedback}
						onUnactionFeedback={handleUnactionFeedback}
						onUpdateNotes={handleUpdateNotes}
						isActioning={isActioning}
					/>
				))}
			</>
		</RuleFormSection>
	);
};
