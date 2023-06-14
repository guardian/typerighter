import React, {useContext, useMemo, useState} from 'react';
import {
  EuiSearchBarProps,
  EuiBasicTableColumn,
  EuiInMemoryTable,
  EuiTitle,
  EuiFlexItem,
  EuiButton,
  EuiFlexGroup,
  EuiButtonIcon,
  EuiFlexGrid,
  EuiIcon,
  EuiToolTip,
  EuiHealth
} from '@elastic/eui';
import {useRules} from "./hooks/useRules";
import {css} from "@emotion/react";
import {RuleForm} from './RuleForm';
import {PageContext} from '../utils/window';
import {hasCreateEditPermissions} from './helpers/hasCreateEditPermissions';
import styled from '@emotion/styled';
import {FeatureSwitchesContext} from "./context/featureSwitches";

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
      field: 'replacement',
      name: 'Replacement',
      width: '14.2%'
    },
    {
      field: 'description',
      name: 'Description',
      textOnly: true,
      truncateText: true,
      width: '21.4%'
    },
    {
      field: 'pattern',
      name: 'Match',
      truncateText: true,
      width: '21.4%'
    },
    {
      field: 'category',
      name: 'Rule source',
      width: '14.2%'
    },
    {
      field: 'ruleType',
      name: 'Type',
      width: '14.2%'
    },
    {
      field: 'isPublished',
      name: 'Status',
      render: (value: boolean) =>  <><EuiHealth color={value ? "success" : "warning"}/>{value ? "Live" : "Draft"}</>,
      width: '7.1%'
    },
    {
      name: <EuiIcon type="pencil"/>,
      width: '7.1%',
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
  ] as Array<EuiBasicTableColumn<Rule>>;
}

// We use our own button rather than an EuiIconButton because that component won't allow us to
// show a tooltip on hover when the button is disabled
const EditRule = ({
                    editIsEnabled,
                    editRule,
                    rule
                  }: { editIsEnabled: boolean, editRule: (ruleId: number) => void, rule: Rule }) => {
  return <EuiToolTip
    content={editIsEnabled ? "" : "You do not have the correct permissions to edit a rule. Please contact Central Production if you need to edit rules."}>
    <EditRuleButton editIsEnabled={editIsEnabled}
                    onClick={() => (editIsEnabled ? editRule(Number(rule.id)) : () => null)}>
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

  const [formMode, setFormMode] = useState<'closed' | 'create' | 'edit'>('closed');
  const [currentRuleId, setCurrentRuleId] = useState<number | undefined>(undefined)
  const { getFeatureSwitchValue } = useContext(FeatureSwitchesContext);
  const hasCreatePermissions = useCreateEditPermissions();

  const search: EuiSearchBarProps = {
    box: {
      incremental: true,
      schema: true,
    },
    toolsRight: getFeatureSwitchValue("create-and-edit") ?
    <EuiToolTip content={hasCreatePermissions ? "" : "You do not have the correct permissions to create a rule. Please contact Central Production if you need to create rules."}>
      <EuiButton
        isDisabled={!hasCreatePermissions}
        onClick={() => openEditRulePanel(undefined)}
      >Create Rule</EuiButton>
    </EuiToolTip> : <></>
};

  const openEditRulePanel = (ruleId: number | undefined) => {
    setCurrentRuleId(ruleId);
    setFormMode(ruleId ? 'edit' : 'create');
  }

  const columns = createColumns(openEditRulePanel);

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
            loading={isLoading}
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
      {formMode !== 'closed' && (
        <EuiFlexItem grow={1}>
          <RuleForm
            onClose={() => {
              setFormMode('closed');
              fetchRules();
            }}
            onUpdate={() => {
              fetchRules();
              if (formMode === 'create') {
                setFormMode('edit');
              }
            }}
            ruleId={currentRuleId}
          />
        </EuiFlexItem>
      )}
    </EuiFlexGroup>
  </>
}

export default RulesTable;
