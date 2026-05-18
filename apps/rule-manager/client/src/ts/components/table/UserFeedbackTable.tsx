import React, { useMemo, useState } from 'react';
import {
	EuiDataGrid,
	EuiDataGridColumn,
	EuiSkeletonText,
} from '@elastic/eui';
import styled from '@emotion/styled';
import { UserFeedbackItem } from '../hooks/useUserFeedback';
import { PaginatedResponse } from '../hooks/useRules';

const TableContainer = styled.div`
	width: 100%;
	min-height: 0;
	height: 100%;
`;

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
		id: 'externalRuleId',
		display: 'Rule ID',
	},
	{
		id: 'createdAt',
		display: 'Date',
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
	const [visibleColumns, setVisibleColumns] = useState(
		columns.map((_) => _.id),
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
