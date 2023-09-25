import React, {
	useContext,
	useEffect,
	useMemo,
	useReducer,
	useState,
} from 'react';
import { PaginatedRuleData, SortColumns } from '../hooks/useRules';
import { BaseRule, DraftRule } from '../hooks/useRule';
import {
	EuiBadge,
	EuiCheckbox,
	EuiDataGrid,
	EuiDataGridColumn,
	EuiDataGridControlColumn,
	EuiDataGridRowHeightsOptions,
	EuiIcon,
	EuiSkeletonText,
	EuiToolTip,
	withEuiTheme,
	WithEuiThemeProps,
} from '@elastic/eui';
import styled from '@emotion/styled';
import { ConciseRuleStatus } from '../rule/ConciseRuleStatus';
import { TagsContext } from '../context/tags';
import { EuiDataGridToolBarVisibilityOptions } from '@elastic/eui/src/components/datagrid/data_grid_types';
import { useEuiFontSize } from '@elastic/eui/src/global_styling/mixins/_typography';

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

const PaginatedRulesTableContainer = styled.div`
	width: 100%;
	minheight: 0;
	height: 100%;
`;

const ToolbarText = withEuiTheme(styled.span<WithEuiThemeProps>`
	vertical-align: middle;
	${({ theme }) => `padding-left: ${theme.euiTheme.base / 2}px;`}
	${() => useEuiFontSize('xs')}
`);

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
const rowHeightsOptions: EuiDataGridRowHeightsOptions = {
	defaultHeight: { lineCount: 2 },
} as const;

export const PaginatedRulesTable = ({
	ruleData,
	isLoading,
	canEditRule,
	pageIndex,
	setPageIndex,
	sortColumns,
	setSortColumns,
	onSelectionChanged,
	initialSelection,
}: {
	ruleData: PaginatedRuleData;
	isLoading: boolean;
	canEditRule: boolean;
	initialSelection: Set<number>;
	onSelectionChanged: (rows: RowState) => void;
	pageIndex: number;
	setPageIndex: (index: number) => void;
	sortColumns: SortColumns;
	setSortColumns: (columns: SortColumns) => void;
}) => {
	const { tags } = useContext(TagsContext);

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
		initialSelection,
	);

	const getRuleAtRowIndex = (rowIndex: number) =>
		ruleData.data[rowIndex - pagination.pageIndex * pagination.pageSize];

	const [visibleColumns, setVisibleColumns] = useState(
		columns.map((_) => _.id),
	);

	useEffect(() => {
		onSelectionChanged(rowSelection);
	}, [rowSelection]);

	const toolbarVisibility: EuiDataGridToolBarVisibilityOptions = useMemo(
		() => ({
			additionalControls: {
				left: {
					append: (
						<ToolbarText>
							{ruleData.data.length} of {ruleData.total.toLocaleString()} rules
							{rowSelection.size > 1 && (
								<span>, {rowSelection.size} selected</span>
							)}
						</ToolbarText>
					),
				},
			},
		}),
		[rowSelection, ruleData],
	);

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
					const rule = getRuleAtRowIndex(rowIndex);
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
					if (isLoading) return <EuiSkeletonText />;

					const value = getRuleAtRowIndex(rowIndex).tags;
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
				width: 80,
				headerCellRender: () => <>Status</>,
				rowCellRender: ({ rowIndex }) =>
					isLoading ? (
						<EuiSkeletonText />
					) : (
						<ConciseRuleStatus rule={getRuleAtRowIndex(rowIndex)} />
					),
			},
			{
				id: 'actions',
				width: 35,
				headerCellRender: () => <></>,
				rowCellRender: ({ rowIndex }) =>
					isLoading ? (
						<EuiSkeletonText />
					) : (
						<EditRule
							editIsEnabled={canEditRule}
							editRule={() =>
								setRowSelection({
									type: 'set',
									id: getRuleAtRowIndex(rowIndex).id!,
								})
							}
							rule={getRuleAtRowIndex(rowIndex)}
						/>
					),
			},
		],
		[isLoading, ruleData, setRowSelection, pageIndex, canEditRule, tags],
	);

	const renderCellValue = useMemo(
		() =>
			({ rowIndex, columnId }: { rowIndex: number; columnId: string }) => {
				const rule = getRuleAtRowIndex(rowIndex);
				if (!rule || isLoading) {
					return <EuiSkeletonText />;
				}
				return getRuleAtRowIndex(rowIndex)[columnId as keyof BaseRule] || '';
			},
		[ruleData, isLoading],
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

	const gridStyle = useMemo(() => {
		if (isLoading) {
			return { rowClasses: {} };
		}

		const rowClasses: Record<number, string> = {};
		rowSelection.forEach((ruleId) => {
			const rowIndex =
				ruleData.data.findIndex((rule) => rule?.id === ruleId) +
				pagination.pageIndex * pagination.pageSize;

			if (rowIndex !== -1) {
				rowClasses[rowIndex] = 'typerighter-euiDataGrid--selected-row';
			}
		});

		// Bit of an CSS escape hatch as EuiDataGrid is still reliant on classes – see
		// https://github.com/elastic/eui/issues/4401
		return {
			rowClasses,
		};
	}, [rowSelection, pagination, isLoading]);

	return (
		<PaginatedRulesTableContainer>
			<EuiDataGrid
				aria-label="Rules grid"
				inMemory={inMemory}
				toolbarVisibility={toolbarVisibility}
				columnVisibility={columnVisibility}
				renderCellValue={renderCellValue}
				leadingControlColumns={leadingColumns}
				trailingControlColumns={trailingColumns}
				rowHeightsOptions={rowHeightsOptions}
				sorting={sorting}
				rowCount={ruleData.total}
				columns={columns}
				pagination={pagination}
				gridStyle={gridStyle}
			/>
		</PaginatedRulesTableContainer>
	);
};
