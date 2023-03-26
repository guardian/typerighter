import React, {useState} from 'react';
import {
    formatDate,
    EuiBasicTable,
    EuiBasicTableColumn,
    EuiTitle, EuiFlexItem, EuiButton, EuiFlexGroup,
} from '@elastic/eui';
import {useApi} from "./HOOKS/api-hook";
import {css} from "@emotion/css";


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
    const [useElasticUi, setUseElasticUi] = useState(false);
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
            const {rules} = await updatedRulesResponse.json();
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

    const rowStyle = {
        display: "flex",
        width: "100%",
        minHeight: "35px",
        borderTop: "1px solid #D3DAE6",
        borderBottom: "1px solid #D3DAE6",
        alignItems: "center",
        backgroundColor: "white",
        padding: "8px",
    }
    const headerStyle = {
        display: "flex",
        position: "sticky",
        width: "100%",
        border: "1px solid #D3DAE6",
        alignItems: "center",
        alignContent: "center",
        justifyContent: "center",
        backgroundColor: "white",
        padding: "8px",
        top: 0
    }
    const headerCellStyle = {
        fontWeight: "bold",
    }

    const cellStyle = {
        display: "flex",
        width: "20%",
        backgroundColor: "white",
        padding: "2px",
    }
    return <>
        <EuiFlexGroup>
            <EuiFlexItem grow={false}>
                <div style={{paddingBottom: "20px"}}>
                    <EuiTitle>
                        <h1>Current rules ({data ? data.length : 'loading...'})</h1>
                    </EuiTitle>
                </div>

            </EuiFlexItem>
            <EuiFlexItem grow={false}>
                <EuiButton fill={true} color={"primary"} onClick={() => refreshRules()} isLoading={isRefreshing}>
                    Refresh{isRefreshing ? "ing" : ""} rules
                </EuiButton>
            </EuiFlexItem>
            <EuiFlexItem grow={false}>
                <EuiButton fill={true} color={"primary"} onClick={() => setUseElasticUi(!useElasticUi)} isLoading={isRefreshing}>
                    use {useElasticUi ? "Basic React" : "Elastic UI Table"}
                </EuiButton>
            </EuiFlexItem>
        </EuiFlexGroup>
        {
            isLoading && <p>Loading...</p>
        }
        {
            error && <p>Error: {`${error}`}</p>
        }
        {
            data && !useElasticUi &&
            <div style={{
                display: "flex",
                flexDirection: "column",
                justifyContent: "center",
                alignItems: "center"
            }}>
                <div style={headerStyle}>
                    <div style={{...headerCellStyle, ...cellStyle}}>Type</div>
                    <div style={{...headerCellStyle, ...cellStyle}}>ID</div>
                    <div style={{...headerCellStyle, ...cellStyle}}>Category</div>
                    <div style={{...headerCellStyle, ...cellStyle}}>Match</div>
                    <div style={{...headerCellStyle, ...cellStyle}}>Description</div>
                </div>
                {data.map(item => <React.Fragment>
                    <div style={rowStyle} key={item.id}>
                        <div style={cellStyle}>{item._type.split('.').pop()}</div>
                        <div style={cellStyle}>{item.id}</div>
                        <div style={cellStyle}>{item.category.name}</div>
                        <div style={cellStyle}>{item.regex}</div>
                        <div style={cellStyle}>{item.description}</div>
                    </div>
                </React.Fragment>)
                }
            </div>
        }
        {data && useElasticUi &&
            <EuiBasicTable
                columns={columns}
                items={data}
                rowHeader="id"
            />
        }
    </>
}

export default RulesTable;
