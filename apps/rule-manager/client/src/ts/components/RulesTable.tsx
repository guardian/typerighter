import React, {useState} from 'react';
import {
    EuiSearchBarProps,
    EuiBasicTableColumn,
    EuiInMemoryTable,
    EuiTitle,
    EuiFlexItem,
    EuiButton,
    EuiFlexGroup,
} from '@elastic/eui';
import {useRules} from "./hooks/rules-hook";
import { css } from "@emotion/css";

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

    const {location} = window;
    const [isRefreshing, setIsRefreshing] = React.useState(false);
    const {rules, isLoading, error, setRules} = useRules(`${location}rules`);
    // @ts-ignore
    const refreshRules = async () => {
        setIsRefreshing(true);
        try {
            const updatedRulesResponse = await fetch(`${location}refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
            });
            const {rules} = await updatedRulesResponse.json();
            setRules(rules);
            setIsRefreshing(false);
        } catch (e) {
            console.error(e);
            setIsRefreshing(false);
        }
    }

    const [incremental, _] = useState(false);

    const search: EuiSearchBarProps = {
        box: {
            incremental: incremental,
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
        {
            isLoading && <span css={css`padding: 8px`}>loading...</span>
        }
        {
            error && <span>Error: {`${error}`}</span>
        }
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
