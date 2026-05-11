import React from 'react';
import { EuiEmptyPrompt } from '@elastic/eui';

export const UserFeedback = () => (
	<EuiEmptyPrompt
		title={<h2>User Feedback</h2>}
		body={<p>No feedback yet.</p>}
	/>
);
