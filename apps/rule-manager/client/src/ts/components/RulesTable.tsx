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
import {getRuleStatus, getRuleStatusColour} from "../utils/rule";
import {capitalize} from "lodash";
import {euiTextTruncate} from "@elastic/eui/src/global_styling/mixins/_typography";
import {TagMap, useTags} from "./hooks/useTags";
import {RuleFormBatchEdit} from "./RuleFormBatchEdit";

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

const TagWrapContainer = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 0 5px;
  width: 100%;
 `;

const createColumns = (tags: TagMap, editRule: (ruleId: number) => void): Array<EuiBasicTableColumn<DraftRule>> => {
  const hasEditPermissions = useCreateEditPermissions();
  return [
    {
      field: 'description',
      name: 'Description',
      truncateText: true,
      render: (value: string) => !!value ? value : '–',
      width: '21.4%'
    },
    {
      field: 'pattern',
      name: 'Pattern',
      truncateText: true,
      render: (value: string) => !!value ? value : '–',
      width: '21.4%'
    },
    {
      field: 'replacement',
      name: 'Replacement',
      render: (value: string) => !!value ? value : '–',
      width: '14.2%'
    },
    {
      field: 'category',
      name: 'Source',
      render: (value: string) => !!value ? value : '–',
      width: '14.2%'
    },
    {
      field: 'tags',
      name: 'Tags',
      render: (value: number[]) => value && value.length > 0 ?
        <TagWrapContainer>{value.map(tagId =>
          <span style={{width: '100%'}} key={tagId}>
            <EuiBadge>{tags[tagId.toString()]?.name ?? "Unknown tag"}</EuiBadge>
          </span>
        )}</TagWrapContainer> : <>–</>,
      width: '13.2%'
    },
    {
      name: 'Status',
      width: '8.1%',
      render: (rule: DraftRule) => {
        const state = capitalize(getRuleStatus(rule));
        return <EuiFlexGroup alignItems="center" justifyContent="flexStart" gutterSize="none">
          <EuiHealth color={getRuleStatusColour(rule)} />
          <EuiText css={css`${euiTextTruncate()}`}>{state}</EuiText>
          {rule.hasUnpublishedChanges &&
            <>&nbsp;&nbsp;<EuiToolTip content="This rule has unpublished changes"><EuiIcon type="warning" /></EuiToolTip></>}
        </EuiFlexGroup>
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
  const {tags, fetchTags, isLoading: isTagMapLoading } = useTags();
  const {rules, isLoading, error, refreshRules, isRefreshing, setError, fetchRules} = useRules();
  const [formMode, setFormMode] = useState<'closed' | 'create' | 'edit'>('closed');
  const [currentRuleId, setCurrentRuleId] = useState<number | undefined>(undefined)
  const [selectedRules, setSelectedRules] = useState<DraftRule[]>([]);
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

  const columns = createColumns(tags, openEditRulePanel);

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

  const getRowProps = useMemo(() => (rule: DraftRule) =>
    rule.id === currentRuleId && {
      isSelected: true
    }, [currentRuleId])

  const handleRefreshRules = async () => {
    await refreshRules();
    await fetchTags();
  }

  return <>
    <EuiFlexGroup direction="column" gutterSize="none" style={{ height: "100%"}}>
      <EuiFlexItem grow={0}>
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
      <EuiFlexItem grow={0} style={{ paddingBottom: "20px" }}>
        <EuiTitle>
          <h1>
            Current rules
            {
              getFeatureSwitchValue("enable-destructive-reload") ?
                  <>&nbsp;
                    <EuiButton size="s" fill={true} color={"danger"} onClick={handleRefreshRules} isLoading={isRefreshing}>
                      <strong>Destroy all rules in the manager and reload from the original Google Sheet</strong>
                    </EuiButton>
                  </>
                : null
            }
          </h1>
        </EuiTitle>
      </EuiFlexItem>
    </EuiFlexGroup>
      </EuiFlexItem>
      <EuiFlexGroup style={{ overflow: "hidden" }}>
        <EuiFlexItem style={{  overflowY: "scroll" }} grow={2}>
          {rules &&
            <EuiInMemoryTable
              rowProps={getRowProps}
              css={css`.euiTableRow.euiTableRow-isSelected { background-color: rgba(0, 119, 204, 0.1); }`}
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
          <EuiFlexItem>
            {selectedRules.length > 1
                ? <RuleFormBatchEdit
                    tags={tags}
                    isTagMapLoading={isTagMapLoading}
                    onClose={() => {
                      setFormMode('closed');
                      fetchRules()
                    }}
                    onUpdate={() => {
                      fetchRules();
                    }}
                    ruleIds={selectedRules.map(rule => rule.id) as number[]}
                  />
                : <RuleForm
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
                />}
          </EuiFlexItem>
        )}
      </EuiFlexGroup>
    </EuiFlexGroup>
  </>
}

export default RulesTable;
