import React from 'react';
import { RuleForm } from '../RuleForm';
import { useNavigate, useParams } from 'react-router-dom';
import { EuiFlexGroup, EuiFlexItem } from '@elastic/eui';

export const newRuleId = 'new-rule';

export const Rule = () => {
	const { id: ruleId } = useParams() as { id: string };
	const navigate = useNavigate();
	return (
		<EuiFlexGroup style={{ height: '100%' }}>
			<EuiFlexItem style={{ overflowY: 'scroll' }}>
				<RuleForm
					ruleId={ruleId === newRuleId ? undefined : parseInt(ruleId)}
					onClose={() => navigate('/')}
				/>
			</EuiFlexItem>
			<EuiFlexItem grow={2}></EuiFlexItem>
		</EuiFlexGroup>
	);
};
