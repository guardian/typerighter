import { EuiFlexItem, EuiTextColor } from "@elastic/eui"
import { css } from "@emotion/react"
import styled from "@emotion/styled"
import React from "react"

export const SectionHeader = styled.div`
  display: flex;
  justify-content: space-between;
`;

export const Title = styled.h2`
  font-family: 'Guardian Agate Sans';
  color: #1A1C21;
  font-weight: 700;
`

export const RuleFormSection = ({title, additionalInfo, children} : {title?: string, additionalInfo?: React.ReactNode, children?: JSX.Element | JSX.Element[]}) => {
    return <EuiFlexItem css={css`
            background-color: #D3DAE6;
            padding-top: 12px;
            padding-bottom: 12px;
            padding-left: 12px;
            padding-right: 12px;
            border-radius: 4px;
        `}>
        <SectionHeader>
            {title && <Title>{title}</Title>}
            {additionalInfo && <EuiTextColor >{additionalInfo}</EuiTextColor>}
        </SectionHeader>
        {children}
    </EuiFlexItem>
}
