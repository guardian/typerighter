import React, { LegacyRef, useEffect, useRef, useState } from 'react';
import InfiniteLoader from 'react-window-infinite-loader';
import { PaginatedRuleData, SortColumns, useRules } from '../hooks/useRules';
import { TagMap } from '../hooks/useTags';
import { DraftRule } from '../hooks/useRule';
import {
	EuiBadge,
	EuiCheckbox,
	EuiDataGrid,
	EuiDataGridColumn,
	EuiFlexGroup,
	EuiHealth,
	EuiIcon,
	EuiSkeletonText,
	EuiText,
	EuiToolTip,
	euiTextTruncate,
} from '@elastic/eui';
import styled from '@emotion/styled';
import { css } from '@emotion/react';
import { getRuleStatus, getRuleStatusColour } from '../../utils/rule';
import { capitalize } from 'lodash';
import { RuleStatus } from '../RuleStatus';
import { ConciseRuleStatus } from '../rule/ConciseRuleStatus';

const TagWrapContainer = styled.div`
	& > span {
		margin-right: 5px;
	}
	width: 100%;
`;

type EditRuleButtonProps = {
	editIsEnabled: boolean;
};

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
]

export const LazyLoadedRulesTable = ({
	ruleData,
	canEditRule,
	fetchRules,
	tags,
	editRule,
	selectedRules,
	onSelect,
	onSelectAll,
	queryStr,
}: {
	ruleData: PaginatedRuleData;
	tags: TagMap;
	editRule: (ruleId: number) => void;
	canEditRule: boolean;
	fetchRules: ReturnType<typeof useRules>['fetchRules'];
	selectedRules: Set<DraftRule>;
	onSelect: (rule: DraftRule, isSelected: boolean) => void;
	onSelectAll: (selected: boolean) => void;
	queryStr: string;
}) => {
	const [pageIndex, setPageIndex] = useState(0);
	const [sortColumns, setSortColumns] = useState<SortColumns>([
		{ id: 'description', direction: 'asc' },
	]);

	const [visibleColumns, setVisibleColumns] = useState(
		columns.map((_) => _.id),
	);

	useEffect(() => {
		fetchRules(1, queryStr, sortColumns);
	}, [queryStr, sortColumns]);

	const infiniteLoaderRef: LegacyRef<InfiniteLoader> | undefined = useRef(null);
	const hasMountedRef = useRef(false);

	// Invalidate the cache when the search string changes
	useEffect(() => {
		if (hasMountedRef.current && infiniteLoaderRef.current) {
			infiniteLoaderRef.current.resetloadMoreItemsCache();
		}
		hasMountedRef.current = true;
	}, [queryStr]);

	return (
		<div style={{ width: '100%' }}>
			<EuiDataGrid
				inMemory={{ level: 'enhancements' }}
				aria-labelledby=""
				columnVisibility={{
					visibleColumns,
					setVisibleColumns,
				}}
				renderCellValue={({ rowIndex, columnId }) =>
					ruleData.data[rowIndex] ? (
						ruleData.data[rowIndex][columnId] || ''
					) : (
						<EuiSkeletonText />
					)
				}
				leadingControlColumns={[
					{
						id: 'selection',
						width: 31,
						headerCellRender: () => (
							<div>
								<EuiCheckbox checked={false} onChange={() => {}} />
							</div>
						),
						headerCellProps: { className: 'eui-textCenter' },
						rowCellRender: () => (
							<div>
								<EuiCheckbox checked={false} onChange={() => {}} />
							</div>
						),
						footerCellRender: () => <span>Select a row</span>,
						footerCellProps: { className: 'eui-textCenter' },
					},
				]}
				trailingControlColumns={[
					{
						id: 'tags',
						width: 65,
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
						rowCellRender: ({ rowIndex }) => (
							<>
								{ruleData.data[rowIndex] ? (
									<ConciseRuleStatus rule={ruleData.data[rowIndex]} />
								) : (
									<EuiSkeletonText />
								)}
							</>
						),
					},
					{
						id: 'actions',
						width: 35,
						headerCellRender: () => <></>,
						rowCellRender: ({rowIndex}) => <EditRule editIsEnabled={true} editRule={editRule} rule={ruleData.data[rowIndex]} />,
					},
				]}
				sorting={{
					columns: sortColumns,
					onSort: (cols) => setSortColumns(cols),
				}}
				rowCount={ruleData.total}
				columns={columns}
				pagination={{
					pageIndex,
					pageSize: ruleData.pageSize,
					onChangePage: (pageIndex) => {
						setPageIndex(pageIndex);
						fetchRules(pageIndex + 1, queryStr, sortColumns);
					},
					onChangeItemsPerPage: () => {},
				}}
			/>
		</div>
	);
};
