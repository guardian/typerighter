import React, {useCallback, useContext, useEffect, useMemo, useState} from 'react';
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
} from '@elastic/eui';
import { useRules } from './hooks/useRules';
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

export const useCreateEditPermissions = () => {
	const permissions = useContext(PageContext).permissions;
	// Do not recalculate permissions if the permissions list has not changed
	return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
};

const TagWrapContainer = styled.div`
	display: flex;
	flex-wrap: wrap;
	gap: 0 5px;
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
					<EuiCheckbox
						id={`rule-table-checkbox`}
						type="inList"
						checked={allRulesSelected}
            disabled={totalRules > 100}
						onChange={(e) => onSelectAll(e.target.checked)}
						title={'Select all rules in search'}
						aria-label={'Select all rules in search'}
					/>
			),
			render: (rule: DraftRule) => (
					<EuiCheckbox
						id={`rule-table-checkbox-${rule.id}`}
						type="inList"
						checked={selectedRules.has(rule)}
						onChange={(e) =>
							onSelect(rule, e.target.checked)
						}
						title={`Select rule ${rule.id}`}
						aria-label={`Select rule ${rule.id}`}
					/>
			),
			width: '30px',
      columns: 1
		},
		{
			field: 'description',
			label: 'Description',
			truncateText: true,
			render: (rule: DraftRule) =>
				!!rule.description ? rule.description : '–',
      columns: 3
		},
		{
			field: 'pattern',
			label: 'Pattern',
			truncateText: true,
			render: (rule: DraftRule) => (!!rule.pattern ? rule.pattern : '–'),
      columns: 3
		},
		{
			field: 'replacement',
			label: 'Replacement',
			render: (rule: DraftRule) =>
				!!rule.replacement ? rule.replacement : '–',
			columns: 2
		},
		{
			field: 'category',
			label: 'Source',
			render: (rule: DraftRule) => (!!rule.category ? rule.category : '–'),
      columns: 2
		},
		{
			field: 'tags',
			label: 'Tags',
			render: (rule: DraftRule) =>
				rule.tags && rule.tags.length > 0 ? (
					<TagWrapContainer>
						{rule.tags.map((tagId) => (
							<span style={{ width: '100%' }} key={tagId}>
								<EuiBadge>
									{tags[tagId.toString()]?.name ?? 'Unknown tag'}
								</EuiBadge>
							</span>
						))}
					</TagWrapContainer>
				) : (
					<>–</>
				),
      columns: 2
		},
		{
      field: 'status',
			label: 'Status',
      columns: 2,
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

const LazyRulesTableRow = styled.div`
	width: 100%;
	display: flex;
	align-content: center;
	border-bottom: 1px solid ${() => useEuiTheme().euiTheme.colors.lightShade};
`;

const LazyRulesTableColumn = styled.div`
	position: relative;
	overflow: hidden;
	display: -webkit-box;
	-webkit-line-clamp: 2;
	-webkit-box-orient: vertical;
	padding: ${() => useEuiTheme().euiTheme.base / 2}px;
	height: ${() => useEuiTheme().euiTheme.base * 2}px;
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
	display: inline-block;
	position: relative;
	padding: ${() => useEuiTheme().euiTheme.base / 2}px;
`;

const LazyRulesTableContainer = styled.div`
	background-color: white;
`;

const LazyRulesTable = ({
	rules,
	tags,
	editRule,
	canEditRule,
	columns,
}: {
	rules: DraftRule[];
	tags: TagMap;
	editRule: (ruleId: number) => void;
	canEditRule: boolean;
	columns: ReturnType<typeof createColumns>;
}) => {
  const totalColumns = columns
    .map(col => col.columns)
    .reduce((acc, cur) => acc + cur, 0);

	return (
		<LazyRulesTableContainer style={{ flex: '1 1 auto' }}>
			<LazyRulesTableHeader>
				{columns.map((column) => {
					return (
						<LazyRulesTableHeaderCell
							key={column.field}
							style={{ width: `${column.columns / totalColumns * 100}%` }}
						>
							<EuiText size="s">
								<strong>{column.label}</strong>
							</EuiText>
						</LazyRulesTableHeaderCell>
					);
				})}
			</LazyRulesTableHeader>
			<AutoSizer>
				{({ width, height }) => (
					<FixedSizeList
						itemSize={50}
						itemData={rules}
						height={height}
						width={width}
						itemCount={rules?.length || 0}
					>
						{({ style, index, data }) => (
								<LazyRulesTableRow style={style}>
									{columns.map((column) => {
                      const colWidth = column.columns / totalColumns * 100;
                      return <LazyRulesTableColumn
                        key={column.field}
                        style={{
                          minWidth: `${colWidth}%`,
                          maxWidth: `${colWidth}%`,
                        }}
                      >
                        <EuiText>
                          {column.render(data[index], canEditRule)}
                        </EuiText>
                      </LazyRulesTableColumn>
                    }
									)}
								</LazyRulesTableRow>
							)
						}
					</FixedSizeList>
				)}
			</AutoSizer>
		</LazyRulesTableContainer>
	);
};

const RulesTable = () => {
	const { tags, fetchTags, isLoading: isTagMapLoading } = useTags();
	const {
		rules,
		isLoading,
		error,
		refreshRules,
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

	const search = {
		box: {
			incremental: true,
			schema: true,
		},
		toolsRight: (
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
		),
	};

	const onSelect = useCallback((rule: DraftRule, selected: boolean) => {
		if (selected) {
      selectedRules.add(rule)
    } else {
      selectedRules.delete(rule);
    }

    const newSelectedRules = new Set([...selectedRules]);

    setSelectedRules(newSelectedRules);
    onSelectionChange(newSelectedRules);
	}, [rules, selectedRules]);

	const onSelectAll = (selected: boolean) => {
    const newSelectedRules = new Set(selected ? [...rules] : []);
    onSelectionChange(newSelectedRules);
	};

	const openEditRulePanel = (ruleId: number | undefined) => {
		setCurrentRuleId(ruleId);
		setFormMode(ruleId ? 'edit' : 'create');
	};

	const columns = createColumns(
		tags,
		openEditRulePanel,
		rules?.length || 0,
		selectedRules,
		onSelect,
		onSelectAll,
	);

	const onSelectionChange = (selectedRules: Set<DraftRule>) => {
		setSelectedRules(selectedRules);
		openEditRulePanel(Number(selectedRules[0]?.id));
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
										</>
									) : null}
								</h1>
							</EuiTitle>
						</EuiFlexItem>
					</EuiFlexGroup>
				</EuiFlexItem>
				<EuiFlexGroup style={{ overflow: 'hidden' }}>
					<EuiFlexItem grow={2}>
						{rules && (
							<LazyRulesTable
								rules={rules}
								columns={columns}
								tags={tags}
								editRule={openEditRulePanel}
								canEditRule={hasCreatePermissions}
							/>
						)}
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
