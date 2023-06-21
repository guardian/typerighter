import React, {useContext, useEffect, useMemo, useState} from 'react';
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
  EuiHealth,
  EuiTableSelectionType,
  EuiBadge,
  EuiText
} from '@elastic/eui';
import {useRules} from "./hooks/useRules";
import {css} from "@emotion/react";
import {RuleForm} from './RuleForm';
import {PageContext} from '../utils/window';
import {hasCreateEditPermissions} from './helpers/hasCreateEditPermissions';
import styled from '@emotion/styled';
import {FeatureSwitchesContext} from "./context/featureSwitches";
import {DraftRule} from "./hooks/useRule";
import {getRuleState, getRuleStateColour} from "../utils/rule";
import {capitalize} from "lodash";
import {euiTextTruncate} from "@elastic/eui/src/global_styling/mixins/_typography";

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

export const useCreateEditPermissions = () => {
  const permissions = useContext(PageContext).permissions;
  // Do not recalculate permissions if the permissions list has not changed
  return useMemo(() => hasCreateEditPermissions(permissions), [permissions]);
}

const createColumns = (editRule: (ruleId: number) => void): Array<EuiBasicTableColumn<DraftRule>> => {
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
      field: 'tags',
      name: 'Tags',
      render: (value: string) => value ? value.split(',').map(tagName => <EuiBadge key={tagName}>{tagName}</EuiBadge>) : undefined,
      width: '13.2%'
    },
    {
      name: 'State',
      width: '8.1%',
      render: (rule: DraftRule) => {
        const state = capitalize(getRuleState(rule));
        return <>
          <EuiHealth color={getRuleStateColour(rule)} />
          <EuiText css={css`${euiTextTruncate()}`}>{state}</EuiText>
        </>
      }
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
        enabled: () => hasEditPermissions,
        'data-test-subj': 'action-edit',
      }]
    }
  ] as Array<EuiBasicTableColumn<DraftRule>>;
}

// We use our own button rather than an EuiIconButton because that component won't allow us to
// show a tooltip on hover when the button is disabled
const EditRule = ({
                    editIsEnabled,
                    editRule,
                    rule
                  }: { editIsEnabled: boolean, editRule: (ruleId: number) => void, rule: DraftRule }) => {
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

  const [selectedRules, setSelectedRules] = useState<DraftRule[]>([]);

  const onSelectionChange = (selectedRules: DraftRule[]) => {
    setSelectedRules(selectedRules);
    openEditRulePanel(Number(selectedRules[0]?.id));
  }

  const selection: EuiTableSelectionType<DraftRule> = {
    selectable: () => hasCreatePermissions,
    selectableMessage: (selectable, rule) => !selectable ? "You don't have edit permissions" : '',
    onSelectionChange,
    initialSelected: [],
  }

  useEffect(() => {
    if (selectedRules.length === 0) {
        setFormMode('closed')
    }
  }, [selectedRules])

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
            itemId="id"
            columns={columns}
            pagination={true}
            sorting={sorting}
            search={search}
            hasActions={true}
            selection={selection}
            isSelectable={true}
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
            onUpdate={(id) => {
              fetchRules();
              setCurrentRuleId(id);
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
