import React, { useContext, useEffect, useMemo, useState } from 'react';
import {
	EuiFlexItem,
	EuiButton,
	EuiFlexGroup,
	EuiButtonIcon,
	EuiFlexGrid,
	EuiToolTip,
	EuiSpacer,
} from '@elastic/eui';
import { SortColumns, useRules } from '../hooks/useRules';
import { RuleForm } from '../RuleForm';
import { PageContext } from '../../utils/window';
import { hasCreateEditPermissions } from '../helpers/hasCreateEditPermissions';
import { FeatureSwitchesContext } from '../context/featureSwitches';
import { RuleFormBatchEdit } from '../RuleFormBatchEdit';
import { EuiFieldSearch } from '@elastic/eui/src/components/form/field_search';
import { PaginatedRulesTable } from '../table/PaginatedRulesTable';
import { useDebouncedValue } from '../hooks/useDebounce';
import { FullHeightContentWithFixedHeader } from '../layout/FullHeightContentWithFixedHeader';

export const useCreateEditPermissions = () => {
	const permissions = useContext(PageContext).permissions;
	// Do not recalculate permissions if the permissions list has not changed
	return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
};

export const Rules = () => {
	const [queryStr, setQueryStr] = useState<string>('');
	const debouncedQueryStr = useDebouncedValue(queryStr, 200);
	const {
		ruleData,
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
	const [sortColumns, setSortColumns] = useState<SortColumns>([
		{ id: 'description', direction: 'asc' },
	]);
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
		fetchRules(pageIndex, debouncedQueryStr, sortColumns);
	}, [pageIndex, debouncedQueryStr, sortColumns]);

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
						onClick={handleRefreshRules}
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
					<EuiFieldSearch
						fullWidth
						value={queryStr}
						onChange={(e) => setQueryStr(e.target.value)}
					/>
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
						<RuleForm
							onClose={() => {
								setFormMode('closed');
								fetchRules(pageIndex, queryStr, sortColumns);
							}}
							onUpdate={(id) => {
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
