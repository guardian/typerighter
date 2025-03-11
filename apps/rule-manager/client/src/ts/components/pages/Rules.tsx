import React, { useContext, useEffect, useMemo, useState } from 'react';
import {
	EuiFlexItem,
	EuiButton,
	EuiFlexGroup,
	EuiButtonIcon,
	EuiToolTip,
	EuiSpacer,
	EuiComboBox,
} from '@elastic/eui';
import { SortColumns, useRules } from '../hooks/useRules';
import { StandaloneRuleForm } from '../RuleForm';
import { PageContext } from '../../utils/window';
import { hasCreateEditPermissions } from '../helpers/hasCreateEditPermissions';
import { FeatureSwitchesContext } from '../context/featureSwitches';
import { RuleFormBatchEdit } from '../RuleFormBatchEdit';
import { EuiFieldSearch } from '@elastic/eui/src/components/form/field_search';
import { PaginatedRulesTable } from '../table/PaginatedRulesTable';
import { useDebouncedValue } from '../hooks/useDebounce';
import { FullHeightContentWithFixedHeader } from '../layout/FullHeightContentWithFixedHeader';
import { Tag, TagsContext } from '../context/tags';
import { ruleTypeOptions } from '../RuleContent';

export const useCreateEditPermissions = () => {
	const permissions = useContext(PageContext).permissions;
	// Do not recalculate permissions if the permissions list has not changed
	return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
};

const ruleTypeOptionsWithId = ruleTypeOptions.map((opt) => ({
	label: opt.label,
	value: opt.id,
}));

export const Rules = () => {
	const [queryStr, setQueryStr] = useState<string>('');
	const [selectedRuleTypeOptions, setSelectedRuleTypeOptions] = useState<
		{ label: string; value: string }[]
	>([]);
	const [selectedTags, setSelectedTags] = useState<
		{ label: string; value: number }[]
	>([]);
	const { tags } = useContext(TagsContext);
	const tagOptions = useMemo(
		() =>
			Object.values(tags).map((tag) => ({ label: tag.name, value: tag.id })),
		[tags],
	);
	const debouncedQueryStr = useDebouncedValue(queryStr, 200);
	const {
		ruleData,
		isLoading,
		error,
		refreshRules,
		isRefreshing,
		setError,
		fetchRules,
		refreshDictionaryRules,
	} = useRules();

	const [formMode, setFormMode] = useState<'closed' | 'create' | 'edit'>(
		'closed',
	);
	const [pageIndex, setPageIndex] = useState(0);
	const [sortColumns, setSortColumns] = useState<SortColumns>([]);
	const [currentRuleId, setCurrentRuleId] = useState<number | undefined>(
		undefined,
	);
	const [rowSelection, setRowSelection] = useState<Set<number>>(new Set());
	const { getFeatureSwitchValue } = useContext(FeatureSwitchesContext);
	const hasCreatePermissions = useCreateEditPermissions();

	const openEditRulePanel = (ruleId: number | undefined) => {
		setCurrentRuleId(ruleId);
		setFormMode(ruleId ? 'edit' : 'create');
	};

	useEffect(() => {
		fetchRules(
			pageIndex,
			debouncedQueryStr,
			sortColumns,
			selectedTags.map((_) => _.value),
			selectedRuleTypeOptions.map((_) => _.value),
		);
	}, [
		pageIndex,
		debouncedQueryStr,
		sortColumns,
		selectedTags,
		selectedRuleTypeOptions,
	]);

	useEffect(() => {
		if (rowSelection.size === 0) {
			setFormMode('closed');
		}
	}, [rowSelection]);

	const rowSelectionArray = useMemo(() => [...rowSelection], [rowSelection]);

	const tableHeader = (
		<div>
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
					<EuiButtonIcon onClick={() => setError(undefined)} iconType="cross" />
				</EuiFlexItem>
			)}
			{getFeatureSwitchValue('enable-destructive-reload') ? (
				<>
					&nbsp;
					<EuiButton
						size="s"
						fill={true}
						color={'danger'}
						onClick={refreshRules}
						isLoading={isRefreshing}
					>
						<strong>
							Destroy all rules in the manager and reload from the original
							Google Sheet
						</strong>
					</EuiButton>
					&nbsp;
					<EuiButton
						size="s"
						fill={true}
						color={'danger'}
						onClick={refreshDictionaryRules}
						isLoading={isRefreshing}
					>
						<strong>
							Destroy all dictionary rules and reload from Collins XML wordlist
						</strong>
					</EuiButton>
					<EuiSpacer />
				</>
			) : null}
			<EuiFlexGroup>
				<EuiFlexItem>
					<EuiFlexGroup gutterSize="s">
						<EuiFlexItem grow={3}>
							<EuiFieldSearch
								placeholder="Search tag description, pattern, or replacement"
								fullWidth
								value={queryStr}
								onChange={(e) => setQueryStr(e.target.value)}
							/>
						</EuiFlexItem>
						<EuiComboBox<string>
							style={{ maxWidth: '200px' }}
							placeholder="Filter by rule type"
							options={ruleTypeOptionsWithId}
							selectedOptions={selectedRuleTypeOptions}
							onChange={(ids) =>
								setSelectedRuleTypeOptions(
									ids as { label: string; value: string }[],
								)
							}
						/>
						<EuiComboBox<number>
							style={{ maxWidth: '200px' }}
							placeholder="Filter by tag"
							options={tagOptions}
							selectedOptions={selectedTags}
							onChange={(ids) =>
								setSelectedTags(ids as { label: string; value: number }[])
							}
						/>
					</EuiFlexGroup>
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
		</div>
	);

	const tableContent = (
		<>
			{ruleData && (
				<PaginatedRulesTable
					isLoading={isLoading}
					ruleData={ruleData}
					canEditRule={hasCreatePermissions}
					onSelectionChanged={(rows) => {
						setRowSelection(rows);
						if (rows.size === 1) {
							setCurrentRuleId([...rows].pop());
						}
						setFormMode('edit');
					}}
					pageIndex={pageIndex}
					setPageIndex={setPageIndex}
					sortColumns={sortColumns}
					setSortColumns={setSortColumns}
				/>
			)}
		</>
	);

	return (
		<EuiFlexGroup direction="row" style={{ height: '100%' }}>
			<FullHeightContentWithFixedHeader
				header={tableHeader}
				content={tableContent}
			/>
			{formMode !== 'closed' && (
				<EuiFlexItem>
					{rowSelection.size > 1 ? (
						<RuleFormBatchEdit
							onClose={() => {
								setFormMode('closed');
								fetchRules(pageIndex, queryStr, sortColumns);
							}}
							onUpdate={() => fetchRules(pageIndex, queryStr, sortColumns)}
							ruleIds={rowSelectionArray}
						/>
					) : (
						<StandaloneRuleForm
							onClose={() => {
								setFormMode('closed');
								fetchRules(pageIndex, queryStr, sortColumns);
							}}
							onUpdate={(id) => {
								if (id === currentRuleId) {
									return;
								}

								fetchRules(pageIndex, queryStr, sortColumns);
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
	);
};
