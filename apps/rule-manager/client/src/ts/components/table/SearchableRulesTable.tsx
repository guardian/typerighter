import React, {
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
	EuiToolTip,
	EuiSpacer,
} from '@elastic/eui';
import { useRules } from '../hooks/useRules';
import { RuleForm } from '../RuleForm';
import { PageContext } from '../../utils/window';
import { hasCreateEditPermissions } from '../helpers/hasCreateEditPermissions';
import { FeatureSwitchesContext } from '../context/featureSwitches';
import { DraftRule } from '../hooks/useRule';
import { useTags } from '../hooks/useTags';
import { RuleFormBatchEdit } from '../RuleFormBatchEdit';
import { EuiFieldSearch } from '@elastic/eui/src/components/form/field_search';
import { LazyLoadedRulesTable } from './LazyLoadedRulesTable';

export const useCreateEditPermissions = () => {
	const permissions = useContext(PageContext).permissions;
	// Do not recalculate permissions if the permissions list has not changed
	return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
};

const RulesTable = () => {
  const [queryStr, setQueryStr] = useState<string>("");
	const { tags, fetchTags, isLoading: isTagMapLoading } = useTags();
	const { ruleData, error, refreshRules, isRefreshing, setError, fetchRules } =
		useRules();
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
						<EuiFlexGroup style={{ flexGrow: 0 }}>
							<EuiFlexItem>
								<EuiFieldSearch fullWidth value={queryStr} onChange={e => setQueryStr(e.target.value)} />
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
								<LazyLoadedRulesTable
									fetchRules={index => fetchRules(index, queryStr)}
									ruleData={ruleData}
									tags={tags}
									editRule={openEditRulePanel}
									canEditRule={hasCreatePermissions}
									selectedRules={selectedRules}
									onSelect={onSelect}
									onSelectAll={onSelectAll}
                  queryStr={queryStr}
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
									onUpdate={fetchRules}
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
