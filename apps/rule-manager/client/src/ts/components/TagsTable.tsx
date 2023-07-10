import { EuiFlexGroup, EuiFlexItem, EuiInMemoryTable, EuiTitle } from "@elastic/eui"
import { css } from "@emotion/react"
import { useTags } from "./hooks/useTags";
import React from "react";

const createTagTableColumns = () => {
    return [
        {
            field: 'id',
            name: 'id'
        },{
            field: 'name',
            name: 'Name'
        }
    ]
}
export const TagsTable = () => {
    const {tags, fetchTags} = useTags();

    const fetchUsages = 
    return (<>
        <EuiFlexGroup>
            <EuiFlexItem/>
            <EuiFlexItem>
                <EuiFlexGroup>
                    <EuiFlexItem grow={false} css={css`padding-bottom: 20px;`}>
                        <EuiTitle>
                            <h1>Tags</h1>
                        </EuiTitle>
                    </EuiFlexItem>
                </EuiFlexGroup>
                <EuiFlexGroup>
                    <EuiFlexItem>
                        <EuiInMemoryTable
                            columns={createTagTableColumns()}
                            items={Object.values(tags)}
                            css={css`max-width: 500px`}
                        >
                        </EuiInMemoryTable>
                    </EuiFlexItem>
                </EuiFlexGroup>
            </ EuiFlexItem>
            <EuiFlexItem/>
        </EuiFlexGroup>
    </>)
}