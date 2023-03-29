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
} from '@elastic/eui';
import {useRules} from "./hooks/rules-hook";
import {css} from "@emotion/css";

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
    category: Category;
    enabled: boolean;
    regex: string;
}

const columns: Array<EuiBasicTableColumn<Rule>> = [
    {
        field: '_type',
        name: 'Type',
        render: (_type: Rule['type']) => {
            return <>{_type.split('.').pop()}</>
        }
    },
    {
        field: 'id',
        name: 'ID',
        render: (id: Rule['id']) => {
            return <>{id}</>
        }
    },
    {
        field: 'category',
        name: 'Category',
        render: (category: Rule['category']) => {
            return <>{category.name}</>
        }
    },
    {
        field: 'regex',
        name: 'Match',
    },
    {
        field: 'description',
        name: 'Description'
    }
];

const RulesTable = () => {
    const {rules, isLoading, error, refreshRules, isRefreshing, setError} = useRules();
    const search: EuiSearchBarProps = {
        box: {
            incremental: true,
            schema: true,
        }
    };

    return <>
        <EuiFlexGroup>
            <EuiFlexItem grow={false} css={css`padding-bottom: 20px`}>
                <EuiTitle>
                    <h1>Current rules ({rules ? rules.length : 'loading...'})</h1>
                </EuiTitle>
            </EuiFlexItem>
            <EuiFlexItem grow={false}>
                <EuiButton fill={true} color={"primary"} onClick={() => refreshRules()} isLoading={isRefreshing}>
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
    </>
}

export default RulesTable;
