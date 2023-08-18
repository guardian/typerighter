import React, {useEffect} from 'react';
import {
	EuiTableHeader,
	EuiTableHeaderCell,
	EuiTableRow,
	EuiTableRowCell,
} from '@elastic/eui/src/components/table';
import { EuiVirtualTable } from './VirtualTable';
import InfiniteLoader from 'react-window-infinite-loader';
import AutoSizer from 'react-virtualized-auto-sizer';
import { PaginatedRuleData, useRules } from '../hooks/useRules';
import { TagMap } from '../hooks/useTags';
import { DraftRule } from '../hooks/useRule';
import {
	EuiBadge,
	EuiCheckbox,
	EuiFlexGroup,
	EuiFlexItem,
	EuiHealth,
	EuiIcon,
	EuiSkeletonTitle,
	EuiText,
	EuiToolTip,
	euiTextTruncate,
} from '@elastic/eui';
import styled from '@emotion/styled';
import { css } from '@emotion/react';
import { getRuleStatus, getRuleStatusColour } from '../../utils/rule';
import { capitalize } from 'lodash';

const TagWrapContainer = styled.div`
	& > span {
		margin-right: 5px;
	}
	width: 100%;
`;

const tableHeaderHeight = 33;
const tableRowHeight = 41;

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

export const createColumns = (
	tags: TagMap,
	editRule: (ruleId: number) => void,
	totalRules: number,
	selectedRules: Set<DraftRule>,
	onSelect: (rule: DraftRule, value: boolean) => void,
	onSelectAll: (value: boolean) => void,
) => {
	const allRulesSelected = selectedRules.size === totalRules;

	return [
		{
			field: 'select',
			label: (
				<EuiFlexItem>
					<EuiCheckbox
						id={`rule-table-checkbox`}
						type="inList"
						checked={allRulesSelected}
						disabled={totalRules > 100}
						onChange={(e) => onSelectAll(e.target.checked)}
						title={'Select all rules in search'}
						aria-label={'Select all rules in search'}
					/>
				</EuiFlexItem>
			),
			render: (rule: DraftRule) => (
				<EuiCheckbox
					id={`rule-table-checkbox-${rule.id}`}
					type="inList"
					checked={selectedRules.has(rule)}
					onChange={(e) => onSelect(rule, e.target.checked)}
					title={`Select rule ${rule.id}`}
					aria-label={`Select rule ${rule.id}`}
				/>
			),
			width: '30px',
			columns: 1,
		},
		{
			field: 'description',
			label: 'Description',
			truncateText: true,
			render: (rule: DraftRule) =>
				!!rule.description ? rule.description : '–',
			columns: 6,
		},
		{
			field: 'pattern',
			label: 'Pattern',
			truncateText: true,
			render: (rule: DraftRule) => (!!rule.pattern ? rule.pattern : '–'),
			columns: 4,
		},
		{
			field: 'replacement',
			label: 'Replacement',
			render: (rule: DraftRule) =>
				!!rule.replacement ? rule.replacement : '–',
			columns: 4,
		},
		{
			field: 'category',
			label: 'Source',
			render: (rule: DraftRule) => (!!rule.category ? rule.category : '–'),
			columns: 4,
		},
		{
			field: 'tags',
			label: 'Tags',
			render: (rule: DraftRule) =>
				rule.tags && rule.tags.length > 0 ? (
					<TagWrapContainer>
						{rule.tags.map((tagId) => (
							<span key={tagId}>
								<EuiBadge>
									{tags[tagId.toString()]?.name ?? 'Unknown tag'}
								</EuiBadge>
							</span>
						))}
					</TagWrapContainer>
				) : (
					<>–</>
				),
			columns: 4,
		},
		{
			field: 'status',
			label: 'Status',
			columns: 4,
			render: (rule: DraftRule) => {
				const state = capitalize(getRuleStatus(rule));
				return (
					<EuiFlexGroup
						alignItems="center"
						justifyContent="flexStart"
						gutterSize="none"
					>
						<EuiHealth color={getRuleStatusColour(rule)} />
						<EuiText
							css={css`
								${euiTextTruncate()}
							`}
						>
							{state}
						</EuiText>
						{rule.hasUnpublishedChanges && (
							<>
								&nbsp;&nbsp;
								<EuiToolTip content="This rule has unpublished changes">
									<EuiIcon type="warning" />
								</EuiToolTip>
							</>
						)}
					</EuiFlexGroup>
				);
			},
		},
		{
			field: 'actions',
			label: <EuiIcon type="pencil" />,
			columns: 1,
			justify: 'right',
			render: (item: DraftRule, enabled: boolean) => (
				<EditRule editIsEnabled={enabled} editRule={editRule} rule={item} />
			),
		},
	];
};

export const LazyLoadedRulesTable = ({
	ruleData,
	canEditRule,
	fetchRules,
	tags,
	editRule,
	selectedRules,
	onSelect,
	onSelectAll,
  queryStr
}: {
	ruleData: PaginatedRuleData;
	tags: TagMap;
	editRule: (ruleId: number) => void;
	canEditRule: boolean;
	fetchRules: ReturnType<typeof useRules>['fetchRules'];
	selectedRules: Set<DraftRule>;
	onSelect: (rule: DraftRule, isSelected: boolean) => void;
	onSelectAll: (selected: boolean) => void;
  queryStr: string
}) => {
	const columns = createColumns(
		tags,
		editRule,
		ruleData?.data.length || 0,
		selectedRules,
		onSelect,
		onSelectAll,
	);

	const totalColumns = columns
		.map((col) => col.columns)
		.reduce((acc, cur) => acc + cur, 0);

  useEffect(() => {
    fetchRules(0, queryStr)
  }, [queryStr])

	return (
		<AutoSizer>
			{({ width, height }) => (
				<InfiniteLoader
					isItemLoaded={(index) => ruleData.loadedRules.has(index)}
					itemCount={ruleData.total}
					loadMoreItems={startIndex => fetchRules(startIndex, queryStr)}
					minimumBatchSize={1000}
					threshold={500}
				>
					{({ onItemsRendered }) => (
						<EuiVirtualTable
							height={height}
							width={width}
							itemSize={tableRowHeight}
							itemData={ruleData.data}
							itemCount={ruleData?.total || 0}
							onItemsRendered={onItemsRendered}
							header={
								<EuiTableHeader style={{ backgroundColor: 'white' }}>
									{columns.map((column) => (
										<EuiTableHeaderCell
											style={{
												position: 'sticky',
												top: 0,
												width: `${(column.columns / totalColumns) * 100}%`,
												backgroundColor: 'white',
												zIndex: 1,
											}}
											key={column.field}
										>
											{column.label}
										</EuiTableHeaderCell>
									))}
								</EuiTableHeader>
							}
							row={({
								style: { top, position, ...otherStyles },
								index,
								data,
							}) => (
								<EuiTableRow
									style={{
										height: `${tableRowHeight}px`,
										position: 'absolute',
										display: 'flex',
										top: parseFloat(top?.toString() || '0') + tableHeaderHeight,
										...otherStyles,
									}}
								>
									{columns.map((column) => {
										const colWidth = (column.columns / totalColumns) * 100;
										return (
											<EuiTableRowCell
												key={column.field}
												style={{ width: `${colWidth}%`, borderBottom: 0 }}
												truncateText={true}
											>
												{ruleData.loadedRules.has(index) ? (
													column.render(data[index], canEditRule)
												) : (
													<EuiSkeletonTitle
														size="s"
														isLoading={true}
														contentAriaLabel={`Rule at index ${index}`}
														style={{ width: '300px' }}
													></EuiSkeletonTitle>
												)}
											</EuiTableRowCell>
										);
									})}
								</EuiTableRow>
							)}
						/>
					)}
				</InfiniteLoader>
			)}
		</AutoSizer>
	);
};
