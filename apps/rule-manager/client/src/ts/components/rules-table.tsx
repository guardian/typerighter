import React from 'react';
import {
    formatDate,
    EuiBasicTable,
    EuiBasicTableColumn,
    EuiTitle, EuiFlexItem, EuiButton, EuiFlexGroup,
} from '@elastic/eui';
import {useApi} from "./HOOKS/api-hook";
import {css} from "@emotion/css";
import DataTable from 'react-data-table-component';


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
    const {data, isLoading, error, setData} = useApi(`${location}rules`, 'GET', null);
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
            const { rules } = await updatedRulesResponse.json();
            setData(rules);
            setIsRefreshing(false);
        } catch (e) {
            console.error(e);
            setIsRefreshing(false);
        }
    }

    const cols = [
        {
            name: 'Type',
            selector: row => row.type.split('.').pop(),
        },
        {
            name: 'ID',
            selector: row => row.id,
        },
        {
            name: 'Category',
            selector: row => row.type
        },
        {
            name: 'Match',
            selector: row => row.regex,
        },
        {
            name: 'Description',
            selector: row => row.description,
        }
    ];

    return (<>
            <EuiFlexGroup>
                <EuiFlexItem grow={false}>
                    <EuiTitle>
                        <h1>Current rules</h1>
                    </EuiTitle>
                </EuiFlexItem>
                <EuiFlexItem grow={false}>
                    <EuiButton fill={true} color={"primary"} onClick={() => refreshRules()} isLoading={isRefreshing}>
                        Refresh{isRefreshing ? "ing" : ""} rules
                    </EuiButton>
                </EuiFlexItem>
            </EuiFlexGroup>
            <EuiTitle>
                <h2>Rules ({data ? data.length : '?'})</h2>
            </EuiTitle>
            {
                isLoading && <p>Loading...</p>
            }
            {
                error && <p>Error: {`${error}`}</p>
            }
            {
                data &&
                // <DataTable
                //     columns={cols}
                //     data={data}
                // />
                // (<table css={css`
                //       border: 1px solid black;
                //       padding: 4px;
                //       margin: 4px;
                //       table-layout: fixed;
                //       max-width: 300px;
                //       td {
                //           padding: 2px;
                //         max-width: 20%;
                //       }
                //       tr {
                //       border-bottom: 1px solid black;
                //       }
                // `}>
                //     {data.map((rule: Rule) => (<tr>
                //         <td>{`${rule._type.split('.').pop()}`}</td>
                //         <td >{`${rule.id}`}</td>
                //         <td>{`${rule.regex}`}</td>
                //         <td>{`${rule.description}`}</td>
                //     </tr>))}
                // </table>)

                <EuiBasicTable
                    columns={columns}
                    items={data}
                    rowHeader="id"
                />

            }
        </>
    )
}

export default RulesTable;
