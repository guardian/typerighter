import React, { useMemo, useState } from 'react';
import {
	EuiDataGrid,
	EuiDataGridColumn,
	EuiIcon,
	EuiSkeletonText,
	EuiToolTip,
} from '@elastic/eui';
import styled from '@emotion/styled';
import { useNavigate } from 'react-router-dom';
import { UserFeedbackItem } from '../hooks/useUserFeedback';
import { PaginatedResponse } from '../hooks/useRules';

const TableContainer = styled.div`
	width: 100%;
	min-height: 0;
	height: 100%;
`;

const EditRuleButton = styled.button<{ editIsEnabled: boolean }>((props) => ({
	width: '16px',
	cursor: props.editIsEnabled ? 'pointer' : 'not-allowed',
	color: props.editIsEnabled ? 'inherit' : 'gray',
}));

const columns: EuiDataGridColumn[] = [
	{
		id: 'userEmail',
		display: 'User',
	},
	{
		id: 'feedbackMessage',
		display: 'Feedback',
	},
	{
		id: 'matchedText',
		display: 'Matched text',
	},
	{
		id: 'matchContext',
		display: 'Match context',
	},
	{
		id: 'createdAt',
		display: 'Date',
	},
	{
		id: 'externalRuleId',
		display: 'External Rule ID',
	},
	{
		id: 'edit',
		display: 'Edit',
		initialWidth: 60,
	},
];

export const UserFeedbackTable = ({
	feedbackData,
	isLoading,
	pageIndex,
	setPageIndex,
}: {
	feedbackData: PaginatedResponse<UserFeedbackItem>;
	isLoading: boolean;
	pageIndex: number;
	setPageIndex: (index: number) => void;
}) => {
	const navigate = useNavigate();
	const [visibleColumns, setVisibleColumns] = useState(
		columns.filter(({ id }) => id !== 'ruleId').map((_) => _.id),
	);

	const getFeedbackAtRowIndex = (rowIndex: number) =>
		feedbackData.data[rowIndex - pagination.pageIndex * pagination.pageSize];

	const pagination = useMemo(
		() => ({
			pageIndex,
			pageSize: feedbackData.pageSize,
			onChangePage: (newPageIndex: number) => setPageIndex(newPageIndex),
			onChangeItemsPerPage: () => {},
		}),
		[pageIndex, feedbackData],
	);

	const columnVisibility = useMemo(
		() => ({
			visibleColumns,
			setVisibleColumns,
		}),
		[visibleColumns, setVisibleColumns],
	);

	const renderCellValue = useMemo(
		() =>
			({ rowIndex, columnId }: { rowIndex: number; columnId: string }) => {
				const item = getFeedbackAtRowIndex(rowIndex);
				if (!item || isLoading) {
					return <EuiSkeletonText />;
				}

				if (columnId === 'edit') {
					const editIsEnabled = item.ruleId != null;
					return (
						<EuiToolTip
							content={
								editIsEnabled
									? 'Edit rule'
									: 'No rule associated with this feedback'
							}
						>
							<EditRuleButton
								editIsEnabled={editIsEnabled}
								onClick={() =>
									editIsEnabled ? navigate(`/rule/${item.ruleId}`) : undefined
								}
							>
								<EuiIcon type="pencil" />
							</EditRuleButton>
						</EuiToolTip>
					);
				}

				const value = item[columnId as keyof UserFeedbackItem];

				if (columnId === 'createdAt' && typeof value === 'string') {
					return new Date(value).toLocaleString();
				}

				return value ?? '';
			},
		[feedbackData, isLoading],
	);

	return (
		<TableContainer>
			<EuiDataGrid
				aria-label="User feedback grid"
				columnVisibility={columnVisibility}
				renderCellValue={renderCellValue}
				rowCount={feedbackData.total}
				columns={columns}
				pagination={pagination}
				toolbarVisibility={{
					showColumnSelector: true,
					showFullScreenSelector: false,
				}}
			/>
		</TableContainer>
	);
};
