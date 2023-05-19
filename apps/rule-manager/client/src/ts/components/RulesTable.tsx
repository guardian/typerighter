import React, { useContext, useMemo, useState } from 'react';
import {
    EuiSearchBarProps,
    EuiBasicTableColumn,
    EuiInMemoryTable,
    EuiTitle,
    EuiFlexItem,
    EuiButton,
    EuiFlexGroup,
    EuiLoadingSpinner,
    EuiButtonIcon,
    EuiFlexGrid,
    EuiIcon,
    EuiToolTip
} from '@elastic/eui';
import { useRules } from "./hooks/useRules";
import { css } from "@emotion/react";
import { baseForm, RuleForm, RuleFormData } from './RuleForm';
import { getRule } from './api/getRule';
import { PageContext, PageDataProvider, Permission } from '../utils/window';
import { hasCreateEditPermissions } from './helpers/hasCreateEditPermissions';
import styled from '@emotion/styled';

const sorting = {
    sort: {
        field: 'description',
        direction: 'desc' as const,
    },
};

type Category = {
    name: string;
    id: string;
}

export type Rule = {
    type: string;
    id: string;
    description: string;
    replacement: {
      type: 'TEXT_SUGGESTION',
      text: string,
    };
    category: Category;
    enabled: boolean;
    regex: string;
}

export const useCreateEditPermissions = () => {
    const permissions = useContext(PageContext).permissions;
    // Do not recalculate permissions if the permissions list has not changed
    return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
}

const createColumns = (editRule: (ruleId: number) => void): Array<EuiBasicTableColumn<Rule>> => {
    const hasEditPermissions = useCreateEditPermissions();
    return [
        {
            field: 'ruleType',
            name: 'Type',
        },
        {
            field: 'externalId',
            name: 'ID'
        },
        {
            field: 'category',
            name: 'Category',
        },
        {
            field: 'pattern',
            name: 'Match',
        },
        {
        field: 'replacement',
        name: 'Replacement',
        },
        {
            field: 'description',
            name: 'Description'
        },
        {
            name: <EuiIcon type="pencil"/>,
            actions: [{
                name: 'Edit',
                render: (item, enabled) => <EditRule editIsEnabled={enabled} editRule={editRule} rule={item}/>,
                isPrimary: true,
                description: 'Edit this rule',
                onClick: (rule) => {
                    editRule(Number(rule.id))
                },
                enabled: (item) => hasEditPermissions,
                'data-test-subj': 'action-edit',
            }]
    }
]
}

// We use our own button rather than an EuiIconButton because that component won't allow us to
// show a tooltip on hover when the button is disabled
const EditRule = ({editIsEnabled, editRule, rule}: {editIsEnabled: boolean, editRule: (ruleId: number) => void, rule: Rule}) => {
    return <EuiToolTip content={editIsEnabled ? "" : "You do not have the correct permissions to edit a rule. Please contact Central Production if you need to edit rules."}>
        <EditRuleButton editIsEnabled={editIsEnabled} onClick={() => (editIsEnabled ? editRule(Number(rule.id)) : () => null)}>
            <EuiIcon type="pencil"/>
        </EditRuleButton>
    </EuiToolTip>
}

type EditRuleButtonProps = {
    editIsEnabled: boolean;
}

const EditRuleButton = styled.button<EditRuleButtonProps>(props => ({
    width: '16px',
    cursor: props.editIsEnabled ? 'pointer' : 'not-allowed',
}))

const RulesTable = () => {
    const {rules, isLoading, error, refreshRules, isRefreshing, setError, fetchRules} = useRules();
    const search: EuiSearchBarProps = {
        box: {
            incremental: true,
            schema: true,
        }
    };
    const openEditRulePanel = (ruleId: number) => {
        getRule(ruleId)
            .then(data => {
                if (data.status === 'ok'){
                    setUpdateMode(true);
                    setCreateRuleFormOpen(true);
                    setRuleData(data.rule);
                } else {
                    console.error(data);
                }
            })
    }
    const columns = createColumns(openEditRulePanel);
    const [ruleData, setRuleData] = useState<RuleFormData>(baseForm);
    const [createRuleFormOpen, setCreateRuleFormOpen] = useState(false);
    const [updateMode, setUpdateMode] = useState(false);

    return <>
        <EuiFlexGroup>
            <EuiFlexItem grow={false} css={css`padding-bottom: 20px;`}>
                <EuiTitle>
                    <h1>Current rules</h1>
                </EuiTitle>
            </EuiFlexItem>
            <EuiFlexItem grow={false}>
                <EuiButton size="s" fill={true} color={"primary"} onClick={() => refreshRules()} isLoading={isRefreshing}>
                    Refresh{isRefreshing ? "ing" : ""} rules
                </EuiButton>
            </EuiFlexItem>
        </EuiFlexGroup>
        <EuiFlexGrid>
            {isLoading &&
                <EuiFlexItem grow={true} css={css`
                  align-content: center;
                  display: flex;
                  justify-content: center;
                  width: 100%;
                  padding-bottom: 20px;
                `}>
                    <EuiLoadingSpinner size="m"/>
                </EuiFlexItem>

            }
            {error &&
                <EuiFlexItem grow={true} style={{
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
                    fontWeight: 'bold'
                }}>
                    <div>{`${error}`}</div>
                    <EuiButtonIcon
                        onClick={() => setError(undefined)}
                        iconType="cross"/></EuiFlexItem>

            }
        </EuiFlexGrid>

        <EuiFlexGroup>
            <EuiFlexItem grow={2}>
                {rules &&
                    <EuiInMemoryTable
                        tableCaption="Demo of EuiInMemoryTable"
                        items={rules}
                        columns={columns}
                        pagination={true}
                        sorting={sorting}
                        search={search}
                        hasActions={true}
                    />
                }
            </EuiFlexItem>
            <EuiFlexItem grow={1}>
                <RuleForm 
                    onRuleUpdate={fetchRules}
                    ruleData={ruleData}
                    setRuleData={setRuleData}
                    createRuleFormOpen={createRuleFormOpen}
                    setCreateRuleFormOpen={setCreateRuleFormOpen}
                    updateMode={updateMode}
                    setUpdateMode={setUpdateMode}
                />
            </EuiFlexItem>
        </EuiFlexGroup>

    </>
}

export default RulesTable;
