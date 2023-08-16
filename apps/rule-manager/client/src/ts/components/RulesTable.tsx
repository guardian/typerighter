import React, {
  forwardRef,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import {
	EuiTitle,
	EuiFlexItem,
	EuiButton,
	EuiFlexGroup,
	EuiButtonIcon,
	EuiFlexGrid,
	EuiIcon,
	EuiToolTip,
	EuiHealth,
	EuiTableSelectionType,
	EuiBadge,
	EuiText,
	EuiSpacer,
	EuiSkeletonTitle,
} from '@elastic/eui';
import { PaginatedResponse, useRules } from './hooks/useRules';
import { css } from '@emotion/react';
import { RuleForm } from './RuleForm';
import { PageContext } from '../utils/window';
import { hasCreateEditPermissions } from './helpers/hasCreateEditPermissions';
import styled from '@emotion/styled';
import { FeatureSwitchesContext } from './context/featureSwitches';
import { DraftRule, RuleData } from './hooks/useRule';
import { getRuleStatus, getRuleStatusColour } from '../utils/rule';
import { capitalize, create, truncate } from 'lodash';
import { euiTextTruncate } from '@elastic/eui/src/global_styling/mixins/_typography';
import { TagMap, useTags } from './hooks/useTags';
import { RuleFormBatchEdit } from './RuleFormBatchEdit';
import { FixedSizeList } from 'react-window';
import AutoSizer from 'react-virtualized-auto-sizer';
import { useEuiTheme } from '@elastic/eui/src/services/theme';
import { EuiCheckbox } from '@elastic/eui/src/components/form/checkbox';
import { EuiFieldSearch } from '@elastic/eui/src/components/form/field_search';
import InfiniteLoader from 'react-window-infinite-loader';

export const useCreateEditPermissions = () => {
	const permissions = useContext(PageContext).permissions;
	// Do not recalculate permissions if the permissions list has not changed
	return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
};

const TagWrapContainer = styled.div`
	& > span {
		margin-right: 5px;
	}
	width: 100%;
`;

const createColumns = (
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
			render: (rule: DraftRule) => (
				<ClampText>{!!rule.description ? rule.description : '–'}</ClampText>
			),
			columns: 6,
		},
		{
			field: 'pattern',
			label: 'Pattern',
			truncateText: true,
			render: (rule: DraftRule) => (
				<ClampText>{!!rule.pattern ? rule.pattern : '–'}</ClampText>
			),
			columns: 4,
		},
		{
			field: 'replacement',
			label: 'Replacement',
			render: (rule: DraftRule) => (
				<ClampText>{!!rule.replacement ? rule.replacement : '–'}</ClampText>
			),
			columns: 4,
		},
		{
			field: 'category',
			label: 'Source',
			render: (rule: DraftRule) => (
				<ClampText>{!!rule.category ? rule.category : '–'}</ClampText>
			),
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

type EditRuleButtonProps = {
	editIsEnabled: boolean;
};

const EditRuleButton = styled.button<EditRuleButtonProps>((props) => ({
	width: '16px',
	cursor: props.editIsEnabled ? 'pointer' : 'not-allowed',
}));

const rowHeight = 64;

const LazyRulesTableRow = styled.div`
	width: 100%;
	display: flex;
	align-content: center;
	border-bottom: 1px solid ${() => useEuiTheme().euiTheme.colors.lightShade};
`;

const LazyRulesTableColumn = styled.div`
	display: flex;
	align-items: baseline;
	position: relative;
	padding: ${() => useEuiTheme().euiTheme.base / 2}px;
	height: ${rowHeight}px;
	overflow: hidden;
`;

const ClampText = styled.div<{ lines: number }>`
	display: -webkit-box;
	-webkit-line-clamp: ${({ lines = 2 }) => lines};
	-webkit-box-orient: vertical;
	overflow: hidden;
`;

const LazyRulesTableHeader = styled.div`
	height: 36px;
	position: relative;
	display: flex;
	align-content: center;
	width: 100%;
	border-bottom: 1px solid ${() => useEuiTheme().euiTheme.colors.lightShade};
`;

const LazyRulesTableHeaderCell = styled.div`
	display: flex;
	align-items: center;
	padding: ${() => useEuiTheme().euiTheme.base / 2}px;
`;

const LazyRulesTableContainer = styled.div`
	background-color: white;
	display: flex;
	flex: 1 1 auto;
	flex-flow: column;
`;

const LazyRulesTableBody = styled.div`
  display: 'flex';
  flex: 1;
  flexGrow: 2;
`;

const ellipsisOverflowStyles = {
	textOverflow: 'ellipsis',
	whiteSpace: 'nowrap',
	overflow: 'hidden',
};

const LazyLoadOuterBackground = styled.div`
  background: repeating-linear-gradient(transparent, transparent ${rowHeight - 1}px, ${(() => useEuiTheme().euiTheme.colors.lightShade)} ${rowHeight - 1}px, ${(() => useEuiTheme().euiTheme.colors.lightShade)} ${rowHeight}px);
`

const lazyLoadInner = forwardRef((props, ref) => (
  <LazyLoadOuterBackground ref={ref} {...props} />
))

const LazyRulesTable = ({
	ruleData,
	tags,
	editRule,
	canEditRule,
	columns,
	fetchRules,
}: {
	ruleData: PaginatedResponse<DraftRule>;
	tags: TagMap;
	editRule: (ruleId: number) => void;
	canEditRule: boolean;
	columns: ReturnType<typeof createColumns>;
	fetchRules: ReturnType<typeof useRules>['fetchRules'];
}) => {
	const totalColumns = columns
		.map((col) => col.columns)
		.reduce((acc, cur) => acc + cur, 0);

	return (
		<LazyRulesTableContainer>
			<LazyRulesTableHeader>
				{columns.map((column) => {
					return (
						<LazyRulesTableHeaderCell
							key={column.field}
							style={{
								width: `${(column.columns / totalColumns) * 100}%`,
								justifyContent: column.justify ?? 'inherit',
							}}
						>
							{typeof column.label === 'string' ? (
								<EuiText size="s" style={ellipsisOverflowStyles}>
									<strong>{column.label}</strong>
								</EuiText>
							) : (
								column.label
							)}
						</LazyRulesTableHeaderCell>
					);
				})}
			</LazyRulesTableHeader>
			<LazyRulesTableBody>
				<AutoSizer>
					{({ width, height }) => (
						<InfiniteLoader
							isItemLoaded={(index) => {
								return ruleData.loadedRules.has(index);
							}}
							itemCount={ruleData.total}
							loadMoreItems={(startIndex, stopIndex) => {
								console.log(`fetching ${startIndex}, ${stopIndex}`);
								fetchRules(startIndex);
							}}
							minimumBatchSize={1000}
              threshold={500}
						>
							{({ onItemsRendered, ref }) => (
								<FixedSizeList
									itemSize={rowHeight}
									itemData={ruleData.data}
									height={height}
									width={width}
									itemCount={ruleData?.total || 0}
									onItemsRendered={onItemsRendered}
                  innerElementType={lazyLoadInner}
									ref={ref}
								>
									{({ style, index, data }) => (
										<LazyRulesTableRow style={style}>
											{columns.map((column) => {
												const colWidth = (column.columns / totalColumns) * 100;
												return (
													<LazyRulesTableColumn
														key={column.field}
														style={{
															flexBasis: `${colWidth}%`,
															justifyContent: column.justify ?? 'inherit',
														}}
													>
														{ruleData.loadedRules.has(index) ? (
															<EuiText>
																{column.render(data[index], canEditRule)}
															</EuiText>
														) : (
															<div style={{ width: '100%' }}>
																<EuiSkeletonTitle
																	size="s"
																	isLoading={true}
																	contentAriaLabel={`Rule at index ${index}`}
																></EuiSkeletonTitle>
															</div>
														)}
													</LazyRulesTableColumn>
												);
											})}
										</LazyRulesTableRow>
									)}
								</FixedSizeList>
							)}
						</InfiniteLoader>
					)}
				</AutoSizer>
			</LazyRulesTableBody>
		</LazyRulesTableContainer>
	);
};

const RulesTable = () => {
	const { tags, fetchTags, isLoading: isTagMapLoading } = useTags();
	const {
		ruleData,
		isLoading,
		error,
		refreshRules,
		refreshDictionaryRules,
		isRefreshing,
		setError,
		fetchRules,
	} = useRules();
	const [formMode, setFormMode] = useState<'closed' | 'create' | 'edit'>(
		'closed',
	);
	const [currentRuleId, setCurrentRuleId] = useState<number | undefined>(
		undefined,
	);
	const [selectedRules, setSelectedRules] = useState<Set<DraftRule>>(new Set());
	const { getFeatureSwitchValue } = useContext(FeatureSwitchesContext);
	const hasCreatePermissions = useCreateEditPermissions();

	const onSelect = useCallback(
		(rule: DraftRule, selected: boolean) => {
			if (selected) {
				selectedRules.add(rule);
			} else {
				selectedRules.delete(rule);
			}

			const newSelectedRules = new Set([...selectedRules]);

			setSelectedRules(newSelectedRules);
			onSelectionChange(newSelectedRules);
		},
		[ruleData, selectedRules],
	);

	const onSelectAll = (selected: boolean) => {
		const newSelectedRules = new Set(selected ? ruleData?.data ?? [] : []);
		onSelectionChange(newSelectedRules);
	};

	const openEditRulePanel = (ruleId: number | undefined) => {
		setCurrentRuleId(ruleId);
		setFormMode(ruleId ? 'edit' : 'create');
	};

	const columns = createColumns(
		tags,
		openEditRulePanel,
		ruleData?.data.length || 0,
		selectedRules,
		onSelect,
		onSelectAll,
	);

	const onSelectionChange = (selectedRules: Set<DraftRule>) => {
		setSelectedRules(selectedRules);
		const firstRule = ruleData?.data.find((rule) => selectedRules.has(rule));
		if (firstRule?.id) {
			openEditRulePanel(firstRule?.id);
		}
	};

	useEffect(() => {
		if (selectedRules.size === 0) {
			setFormMode('closed');
		}
	}, [selectedRules]);

	const getRowProps = useMemo(
		() => (rule: DraftRule) =>
			rule.id === currentRuleId && {
				isSelected: true,
			},
		[currentRuleId],
	);

	const handleRefreshRules = async () => {
		await refreshRules();
		await fetchTags();
	};

	const handleRefreshDictionaryRules = async () => {
		await refreshDictionaryRules();
	};

	return (
		<>
			<EuiFlexGroup
				direction="column"
				gutterSize="none"
				style={{ height: '100%' }}
			>
				<EuiFlexItem grow={0}>
					<EuiFlexGrid>
						{error && (
							<EuiFlexItem
								grow={true}
								style={{
									display: 'flex',
									justifyContent: 'space-between',
									alignItems: 'center',
									width: '100%',
									backgroundColor: '#F8D7DA',
									color: '#721C24',
									flexDirection: 'row',
									padding: '10px',
									borderRadius: '5px',
									marginBottom: '10px',
									fontWeight: 'bold',
								}}
							>
								<div>{`${error}`}</div>
								<EuiButtonIcon
									onClick={() => setError(undefined)}
									iconType="cross"
								/>
							</EuiFlexItem>
						)}
					</EuiFlexGrid>
					<EuiFlexGroup>
						<EuiFlexItem grow={0} style={{ paddingBottom: '20px' }}>
							<EuiTitle>
								<h1>
									Current rules
									{getFeatureSwitchValue('enable-destructive-reload') ? (
										<>
											&nbsp;
											<EuiButton
												size="s"
												fill={true}
												color={'danger'}
												onClick={handleRefreshRules}
												isLoading={isRefreshing}
											>
												<strong>
													Destroy all rules in the manager and reload from the
													original Google Sheet
												</strong>
											</EuiButton>
											&nbsp;
											<EuiButton
												size="s"
												fill={true}
												color={'danger'}
												onClick={handleRefreshDictionaryRules}
												isLoading={isRefreshing}
											>
												<strong>
													Destroy all dictionary rules and reload from Collins
													XML wordlist
												</strong>
											</EuiButton>
										</>
									) : null}
								</h1>
							</EuiTitle>
						</EuiFlexItem>
					</EuiFlexGroup>
				</EuiFlexItem>
				<EuiFlexGroup style={{ overflow: 'hidden' }}>
					<EuiFlexItem grow={2}>
						<EuiFlexGroup style={{ flexGrow: 0 }}>
							<EuiFlexItem>
								<EuiFieldSearch fullWidth />
							</EuiFlexItem>
							<EuiFlexItem grow={0}>
								<EuiToolTip
									content={
										hasCreatePermissions
											? ''
											: 'You do not have the correct permissions to create a rule. Please contact Central Production if you need to create rules.'
									}
								>
									<EuiButton
										isDisabled={!hasCreatePermissions}
										onClick={() => openEditRulePanel(undefined)}
									>
										Create Rule
									</EuiButton>
								</EuiToolTip>
							</EuiFlexItem>
						</EuiFlexGroup>
						<EuiSpacer />
						<EuiFlexGroup>
							{ruleData && (
								<LazyRulesTable
									fetchRules={fetchRules}
									ruleData={ruleData}
									columns={columns}
									tags={tags}
									editRule={openEditRulePanel}
									canEditRule={hasCreatePermissions}
								/>
							)}
						</EuiFlexGroup>
					</EuiFlexItem>
					{formMode !== 'closed' && (
						<EuiFlexItem>
							{selectedRules.size > 1 ? (
								<RuleFormBatchEdit
									tags={tags}
									isTagMapLoading={isTagMapLoading}
									onClose={() => {
										setFormMode('closed');
										fetchRules();
									}}
									onUpdate={() => {
										fetchRules();
									}}
									ruleIds={
										[...selectedRules].map((rule) => rule.id) as number[]
									}
								/>
							) : (
								<RuleForm
									tags={tags}
									isTagMapLoading={isTagMapLoading}
									onClose={() => {
										setFormMode('closed');
										fetchRules();
									}}
									onUpdate={(id) => {
										fetchRules();
										setCurrentRuleId(id);
										if (formMode === 'create') {
											setFormMode('edit');
										}
									}}
									ruleId={currentRuleId}
								/>
							)}
						</EuiFlexItem>
					)}
				</EuiFlexGroup>
			</EuiFlexGroup>
		</>
	);
};

export default RulesTable;
