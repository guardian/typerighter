import React from 'react';
import { RuleForm } from '../RuleForm';
import { useNavigate, useParams } from 'react-router-dom';
import { EuiFlexGroup, EuiFlexItem } from '@elastic/eui';
import { useRule } from '../hooks/useRule';
import { TestRule } from './TestRule';

export const newRuleId = 'new-rule';

export const Rule = () => {
	const { id } = useParams() as { id: string };
	const ruleId = id === newRuleId ? undefined : parseInt(id);
	const ruleHooks = useRule(ruleId);
	const rule = ruleHooks.rule?.draft;
	const testPattern = ruleHooks.rule?.draft.pattern;
	const navigate = useNavigate();
	return (
		<EuiFlexGroup style={{ height: '100%' }}>
			<EuiFlexItem style={{ overflowY: 'scroll' }}>
				<RuleForm
					ruleId={ruleId}
					onClose={() => navigate('/')}
					{...ruleHooks}
				/>
			</EuiFlexItem>
			<EuiFlexItem grow={2}>
				{rule?.ruleType !== 'dictionary' ? (
					<TestRule pattern={testPattern} />
				) : (
					<h2>You cannot yet test dictionary rules.</h2>
				)}
			</EuiFlexItem>
		</EuiFlexGroup>
	);
};
