import React from 'react';
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
    EuiForm,
    EuiFormRow,
    EuiFieldText,
} from '@elastic/eui';
import {useRules} from "./hooks/useRules";
import {css} from "@emotion/react";
import { RuleForm } from './RuleForm';

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

const columns: Array<EuiBasicTableColumn<Rule>> = [
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
    }
];

const RulesTable = () => {
    const {rules, isLoading, error, refreshRules, isRefreshing, setError, fetchRules} = useRules();
    const search: EuiSearchBarProps = {
        box: {
            incremental: true,
            schema: true,
        }
    };

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
                    />
                }
            </EuiFlexItem>
            <EuiFlexItem grow={1}>
                <RuleForm onRuleUpdate={fetchRules}/>
            </EuiFlexItem>
        </EuiFlexGroup>

    </>
}

export default RulesTable;
