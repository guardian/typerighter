import { capitalize } from 'lodash';
import { DraftRule } from '../hooks/useRule';
import { getRuleStatus, getRuleStatusColour } from '../../utils/rule';
import {
	EuiFlexGroup,
	EuiHealth,
	EuiIcon,
	EuiText,
	EuiToolTip,
	euiTextTruncate,
} from '@elastic/eui';
import { css } from '@emotion/react';

export const ConciseRuleStatus = ({ rule }: { rule: DraftRule }) => {
	const state = capitalize(getRuleStatus(rule));
	return (
		<EuiFlexGroup
			alignItems="center"
			justifyContent="flexStart"
			gutterSize="none"
		>
			<EuiHealth color={getRuleStatusColour(rule)} />
			<EuiText
				css={css`
					${euiTextTruncate()}
				`}
			>
				{state}
			</EuiText>
			{rule.hasUnpublishedChanges && (
				<>
					&nbsp;&nbsp;
					<EuiToolTip content="This rule has unpublished changes">
						<EuiIcon type="warning" />
					</EuiToolTip>
				</>
			)}
		</EuiFlexGroup>
	);
};
