import { EuiFlexItem, EuiTextColor } from '@elastic/eui';
import { css } from '@emotion/react';
import React from 'react';
import { SectionHeader } from './form/SectionHeader';
import { Title } from './form/Title';

export const RuleFormSection = ({
	title,
	additionalInfo,
	children,
}: {
	title?: string;
	additionalInfo?: React.ReactNode;
	children?: JSX.Element | JSX.Element[];
}) => {
	return (
		<EuiFlexItem
			css={css`
				background-color: #d3dae6;
				padding-top: 12px;
				padding-bottom: 12px;
				padding-left: 12px;
				padding-right: 12px;
				border-radius: 4px;
			`}
		>
			<SectionHeader>
				{title && <Title>{title}</Title>}
				{additionalInfo && <EuiTextColor>{additionalInfo}</EuiTextColor>}
			</SectionHeader>
			{children}
		</EuiFlexItem>
	);
};
