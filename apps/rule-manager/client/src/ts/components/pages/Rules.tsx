import React, { useContext, useEffect, useMemo, useState } from 'react';
import {
	EuiFlexItem,
	EuiButton,
	EuiFlexGroup,
	EuiButtonIcon,
	EuiToolTip,
	EuiSpacer,
} from '@elastic/eui';
import { SortColumns, useRules } from '../hooks/useRules';
import { newRuleId } from '../RuleForm';
import { PageContext } from '../../utils/window';
import { hasCreateEditPermissions } from '../helpers/hasCreateEditPermissions';
import { EuiFieldSearch } from '@elastic/eui/src/components/form/field_search';
import { PaginatedRulesTable } from '../table/PaginatedRulesTable';
import { useDebouncedValue } from '../hooks/useDebounce';
import { FullHeightContentWithFixedHeader } from '../layout/FullHeightContentWithFixedHeader';
import { matchPath, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { FeatureSwitchesContext } from '../context/featureSwitches';

// The data passed from the rules page to its child components.
export type RulesRouteContext = {
	// The rules currently selected, if any.
	ruleIds: number[] | undefined;
	// Called when a child component updates rule data, to allow the rules table to refresh in response.
	onUpdate: () => void;
};

export const useCreateEditPermissions = () => {
	const permissions = useContext(PageContext).permissions;
	// Do not recalculate permissions if the permissions list has not changed
	return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
};

export const Rules = () => {
	const navigate = useNavigate();
	const location = useLocation();
	const hasChildRoutes = !!matchPath('/rule/*', location.pathname);
	const [queryStr, setQueryStr] = useState<string>('');
	const { getFeatureSwitchValue } = useContext(FeatureSwitchesContext);
	const debouncedQueryStr = useDebouncedValue(queryStr, 200);
	const {
		ruleData,
		error,
		setError,
		fetchRules,
		refreshRules,
		isRefreshing,
		refreshDictionaryRules,
	} = useRules();
	const [pageIndex, setPageIndex] = useState(0);
	const [sortColumns, setSortColumns] = useState<SortColumns>([
		{ id: 'description', direction: 'asc' },
	]);
	const [rowSelection, setRowSelection] = useState<Set<number>>(new Set());
	const hasCreatePermissions = useCreateEditPermissions();

	const openEditRulePanel = (ruleId: number | typeof newRuleId) => {
		navigate(`/rule/${ruleId}`);
	};

	useEffect(() => {
		if (!rowSelection.size) {
			return;
		}

		if (rowSelection.size === 1) {
			openEditRulePanel(rowSelection.values().next().value);
		} else {
			navigate(`rule/batch`);
		}
	}, [rowSelection]);

	useEffect(() => {
		fetchRules(pageIndex, debouncedQueryStr, sortColumns);
	}, [pageIndex, debouncedQueryStr, sortColumns]);

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
					<EuiFieldSearch
						fullWidth
						placeholder="Search across description, pattern, replacement and category …"
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
							onClick={() => openEditRulePanel(newRuleId)}
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
			<EuiFlexItem grow={2} style={{ minWidth: 0 }}>
				<FullHeightContentWithFixedHeader
					header={tableHeader}
					content={tableContent}
				/>
			</EuiFlexItem>
			{hasChildRoutes && (
				<EuiFlexItem grow={1}>
					<Outlet
						context={{
							ruleIds: rowSelectionArray,
							onUpdate: () =>
								fetchRules(pageIndex, debouncedQueryStr, sortColumns),
						}}
					/>
				</EuiFlexItem>
			)}
		</EuiFlexGroup>
	);
};
