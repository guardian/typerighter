import React, { LegacyRef, useEffect, useRef } from 'react';
import InfiniteLoader from 'react-window-infinite-loader';
import { PaginatedRuleData, useRules } from '../hooks/useRules';
import { TagMap } from '../hooks/useTags';
import { DraftRule } from '../hooks/useRule';
import {
	EuiBadge,
	EuiCheckbox,
	EuiDataGrid,
	EuiFlexGroup,
	EuiFlexItem,
	EuiHealth,
	EuiIcon,
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
			id: 'select',
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
			id: 'description',
			label: 'Description',
			truncateText: true,
			render: (rule: DraftRule) =>
				!!rule.description ? rule.description : '–',
			columns: 6,
		},
		{
			id: 'pattern',
			label: 'Pattern',
			truncateText: true,
			render: (rule: DraftRule) => (!!rule.pattern ? rule.pattern : '–'),
			columns: 4,
		},
		{
			id: 'replacement',
			label: 'Replacement',
			render: (rule: DraftRule) =>
				!!rule.replacement ? rule.replacement : '–',
			columns: 4,
		},
		{
			id: 'category',
			label: 'Source',
			render: (rule: DraftRule) => (!!rule.category ? rule.category : '–'),
			columns: 4,
		},
		{
			id: 'tags',
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
			id: 'status',
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
			id: 'actions',
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
	const columns = createColumns(
		tags,
		editRule,
		ruleData?.data.length || 0,
		selectedRules,
		onSelect,
		onSelectAll,
	);

	useEffect(() => {
		fetchRules(1, queryStr);
	}, [queryStr]);

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
    <div style={{width: "100%"}}>
		<EuiDataGrid
			aria-labelledby=""
			columnVisibility={{
				visibleColumns: columns.map((_) => _.id),
				setVisibleColumns: () => {},
			}}
			renderCellValue={({ rowIndex, columnId }) => ruleData.data[rowIndex][columnId] || ""}
			rowCount={ruleData.total}
			columns={columns}
			pagination={{
				pageIndex: 0,
				pageSize: ruleData.pageSize,
				onChangePage: (pageIndex) => fetchRules(pageIndex + 1, queryStr),
				onChangeItemsPerPage: () => {},
			}}
		/>
    </div>
	);
};
