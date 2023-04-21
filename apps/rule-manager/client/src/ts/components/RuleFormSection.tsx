import { EuiFlexItem } from "@elastic/eui"
import { css } from "@emotion/react"
import React from "react"
import { ReactElement } from "react"

export const RuleFormSection = ({title, children} : {title?: string, children: JSX.Element | JSX.Element[]}) => {
    return <EuiFlexItem css={css`
            background-color: #D3DAE6;
            padding-top: 12px;
            padding-bottom: 12px;
            padding-left: 12px;
            padding-right: 48px;
            border-radius: 4px;
        `}>
        {
            title ? <h2 style={{
                fontFamily: 'Open Sans',
                color: '#1A1C21',
                fontWeight: '700',
                paddingBottom: '20px'
            }}>{title}</h2> : null
        }
        {children}
    </EuiFlexItem>
}