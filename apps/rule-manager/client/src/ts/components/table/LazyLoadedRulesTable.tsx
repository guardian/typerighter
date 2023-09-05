import React, { useEffect, useMemo, useReducer, useState } from 'react';
import { PaginatedRuleData, SortColumns } from '../hooks/useRules';
import { TagMap } from '../hooks/useTags';
import { BaseRule, DraftRule } from '../hooks/useRule';
import {
	EuiBadge,
	EuiCheckbox,
	EuiDataGrid,
	EuiDataGridColumn,
	EuiDataGridControlColumn,
	EuiIcon,
	EuiSkeletonText,
	EuiToolTip,
} from '@elastic/eui';
import styled from '@emotion/styled';
import { ConciseRuleStatus } from '../rule/ConciseRuleStatus';

type EditRuleButtonProps = {
	editIsEnabled: boolean;
};
type RowState = Set<number>;
type RowAction =
	| { type: 'add'; id: number }
	| { type: 'delete'; id: number }
	| { type: 'set'; id: number }
	| { type: 'clear' }
	| { type: 'selectAll' };

const TagWrapContainer = styled.div`
	& > span {
		margin-right: 5px;
	}
	width: 100%;
`;

const EditRuleButton = styled.button<EditRuleButtonProps>((props) => ({
	width: '16px',
	cursor: props.editIsEnabled ? 'pointer' : 'not-allowed',
}));

// We use our own button rather than an EuiIconButton because that component won't allow us to
// show a tooltip on hover when the button is disabled
const EditRule = ({
	editIsEnabled,
	editRule,
	rule,
}: {
	editIsEnabled: boolean;
	editRule: (ruleId: number) => void;
	rule: DraftRule;
}) => {
	return (
		<EuiToolTip
			content={
				editIsEnabled
					? ''
					: 'You do not have the correct permissions to edit a rule. Please contact Central Production if you need to edit rules.'
			}
		>
			<EditRuleButton
				editIsEnabled={editIsEnabled}
				onClick={() => (editIsEnabled ? editRule(Number(rule.id)) : () => null)}
			>
				<EuiIcon type="pencil" />
			</EditRuleButton>
		</EuiToolTip>
	);
};

const columns: EuiDataGridColumn[] = [
	{
		id: 'description',
		display: 'Description',
		isSortable: true,
	},
	{
		id: 'pattern',
		display: 'Pattern',
		isSortable: true,
	},
	{
		id: 'replacement',
		display: 'Replacement',
		isSortable: true,
	},
	{
		id: 'category',
		isSortable: true,
		display: 'Source',
		initialWidth: 150,
	},
];

const inMemory = { level: 'enhancements' } as const;
const rowHeightsOptions = { defaultHeight: 'auto' } as const;
const rowCharMax = 300;

export const LazyLoadedRulesTable = ({
	ruleData,
	canEditRule,
	pageIndex,
	setPageIndex,
	sortColumns,
	setSortColumns,
	tags,
	onSelectionChanged,
}: {
	ruleData: PaginatedRuleData;
	tags: TagMap;
	canEditRule: boolean;
	onSelectionChanged: (rows: RowState) => void;
	pageIndex: number;
	setPageIndex: (index: number) => void;
	sortColumns: SortColumns;
	setSortColumns: (columns: SortColumns) => void;
}) => {
	const [rowSelection, setRowSelection] = useReducer(
		(selectedRows: RowState, action: RowAction): RowState => {
			switch (action.type) {
				case 'set': {
					return new Set([action.id]);
				}
				case 'add': {
					const nextRowSelection = new Set(selectedRows);
					nextRowSelection.add(action.id);
					return nextRowSelection;
				}
				case 'delete': {
					const nextRowSelection = new Set(selectedRows);
					nextRowSelection.delete(action.id);
					return nextRowSelection;
				}
				case 'clear': {
					return new Set();
				}
				case 'selectAll': {
					return selectedRows.size === ruleData.data.length
						? new Set()
						: new Set(ruleData.data.map((rule) => rule.id as number));
				}
			}
		},
		new Set<number>(),
	);

	const [visibleColumns, setVisibleColumns] = useState(
		columns.map((_) => _.id),
	);

	useEffect(() => {
		onSelectionChanged(rowSelection);
	}, [rowSelection]);

	const leadingColumns: EuiDataGridControlColumn[] = useMemo(
		() => [
			{
				id: 'selection',
				width: 31,
				headerCellRender: () => (
					<EuiCheckbox
						id="select-all"
						checked={rowSelection.size === ruleData.data.length}
						onChange={() => setRowSelection({ type: 'selectAll' })}
					/>
				),
				headerCellProps: { className: 'eui-textCenter' },
				rowCellRender: ({ rowIndex }) => {
					const rule = ruleData.data[rowIndex];
					if (!rule) {
						return <EuiSkeletonText />;
					}

					const isSelected = !!rule.id && rowSelection.has(rule.id);
					const type = isSelected ? 'delete' : 'add';
					return (
						<EuiCheckbox
							id={`select-${rule.id}`}
							checked={isSelected}
							onChange={() => rule.id && setRowSelection({ type, id: rule.id })}
						/>
					);
				},
				footerCellRender: () => <span>Select a row</span>,
				footerCellProps: { className: 'eui-textCenter' },
			},
		],
		[ruleData, rowSelection, setRowSelection],
	);

	const trailingColumns: EuiDataGridControlColumn[] = useMemo(
		() => [
			{
				id: 'tags',
				width: 90,
				headerCellRender: () => <>Tags</>,
				rowCellRender: ({ rowIndex }) => {
					const value = ruleData.data[rowIndex]?.tags;
					return value && value.length > 0 ? (
						<TagWrapContainer>
							{value.map((tagId) => (
								<span style={{ width: '100%' }} key={tagId}>
									<EuiBadge>
										{tags[tagId.toString()]?.name ?? 'Unknown tag'}
									</EuiBadge>
								</span>
							))}
						</TagWrapContainer>
					) : (
						<></>
					);
				},
			},
			{
				id: 'status',
				width: 65,
				headerCellRender: () => <>Status</>,
				rowCellRender: ({ rowIndex }) =>
					ruleData.data[rowIndex] ? (
						<ConciseRuleStatus rule={ruleData.data[rowIndex]} />
					) : (
						<EuiSkeletonText />
					),
			},
			{
				id: 'actions',
				width: 35,
				headerCellRender: () => <></>,
				rowCellRender: ({ rowIndex }) =>
					ruleData.data[rowIndex] ? (
						<EditRule
							editIsEnabled={canEditRule}
							editRule={() =>
								setRowSelection({
									type: 'set',
									id: ruleData.data[rowIndex].id!,
								})
							}
							rule={ruleData.data[rowIndex]}
						/>
					) : (
						<EuiSkeletonText />
					),
			},
		],
		[ruleData, setRowSelection, canEditRule, tags],
	);

	const renderCellValue = useMemo(
		() =>
			({ rowIndex, columnId }: { rowIndex: number; columnId: string }) => {
				const rule = ruleData.data[rowIndex];
				if (!rule) {
					return <EuiSkeletonText />;
				}
				const data = (ruleData.data[rowIndex][columnId as keyof BaseRule] ||
					'') as string;
				return data.length > rowCharMax
					? data.slice(0, rowCharMax) + '…'
					: data;
			},
		[ruleData],
	);

	const columnVisibility = useMemo(
		() => ({
			visibleColumns,
			setVisibleColumns,
		}),
		[visibleColumns, setVisibleColumns],
	);

	const sorting = useMemo(
		() => ({
			columns: sortColumns,
			onSort: setSortColumns,
		}),
		[sortColumns, setSortColumns],
	);

	const pagination = useMemo(
		() => ({
			pageIndex,
			pageSize: ruleData.pageSize,
			onChangePage: (pageIndex: number) => setPageIndex(pageIndex),
			onChangeItemsPerPage: () => {},
		}),
		[pageIndex, ruleData],
	);

	return (
		<div style={{ width: '100%' }}>
			<EuiDataGrid
				aria-label="Rules grid"
				inMemory={inMemory}
				columnVisibility={columnVisibility}
				renderCellValue={renderCellValue}
				leadingControlColumns={leadingColumns}
				trailingControlColumns={trailingColumns}
				rowHeightsOptions={rowHeightsOptions}
				sorting={sorting}
				rowCount={ruleData.total}
				columns={columns}
				pagination={pagination}
			/>
		</div>
	);
};
