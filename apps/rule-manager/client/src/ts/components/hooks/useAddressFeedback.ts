import useSWRMutation from 'swr/mutation';

type ActionFeedbackArg = {
	feedbackId: number;
	addressed: boolean;
	notes?: string;
};

async function addressFeedbackFetcher(
	_key: string,
	{ arg }: { arg: ActionFeedbackArg },
) {
	const { feedbackId, ...body } = arg;
	const response = await fetch(`/api/user-feedback/${feedbackId}/action`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify(body),
	});
	if (!response.ok) {
		throw new Error(
			`Failed to action feedback: ${response.status} ${response.statusText}`,
		);
	}
	return response.json();
}

export function useAddressFeedback() {
	return useSWRMutation('action-feedback', addressFeedbackFetcher);
}
