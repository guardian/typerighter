import React from 'react';
import { RuleData } from './hooks/useRule';
import { RuleFormSection } from './RuleFormSection';
import { capitalize } from 'lodash';
import {
	getRuleStatus,
	getRuleStatusColour,
	hasUnpublishedChanges,
} from '../utils/rule';
import {
	EuiFlexGroup,
	EuiFlexItem,
	EuiHealth,
	EuiIcon,
	EuiLink,
	EuiText,
} from '@elastic/eui';
import { css } from '@emotion/react';
import { euiTextTruncate } from '@elastic/eui/src/global_styling/mixins/_typography';
import styled from '@emotion/styled';
import { LineBreak } from './LineBreak';

const RuleStatusContainer = styled.div`
	display: flex;
	justify-content: space-between;
	align-items: center;
`;
const AnotherContainer = styled.div`
	display: flex;
`;

export const RuleStatus = ({
	ruleData,
	showRuleRevertModal,
}: {
	ruleData: RuleData | undefined;
	showRuleRevertModal: () => void;
}) => {
	const state = capitalize(getRuleStatus(ruleData?.draft));

	return (
		<RuleFormSection
			title="RULE STATUS"
			additionalInfo={
				!!ruleData &&
				hasUnpublishedChanges(ruleData) && (
					<EuiFlexGroup gutterSize="s">
						<EuiFlexItem>
							<EuiLink onClick={showRuleRevertModal} color={'warning'}>
								Discard unpublished changes
							</EuiLink>
						</EuiFlexItem>
						<EuiIcon type="warning" />
					</EuiFlexGroup>
				)
			}
		>
			<LineBreak />
			<RuleStatusContainer>
				<AnotherContainer>
					<EuiHealth
						textSize="m"
						color={getRuleStatusColour(ruleData?.draft)}
					/>
					<EuiText
						css={css`
							${euiTextTruncate()}
						`}
					>
						{state}
					</EuiText>
				</AnotherContainer>
			</RuleStatusContainer>
		</RuleFormSection>
	);
};
