import useSWRMutation from 'swr/mutation';

type UpdateNotesArg = {
	feedbackId: number;
	notes: string;
};

async function updateNotesFetcher(
	_key: string,
	{ arg }: { arg: UpdateNotesArg },
) {
	const { feedbackId, notes } = arg;
	const response = await fetch(`/api/user-feedback/${feedbackId}/notes`, {
		method: 'PUT',
		headers: { 'Content-Type': 'application/json' },
		body: JSON.stringify({ notes: notes || null }),
	});
	if (!response.ok) {
		throw new Error(
			`Failed to update notes: ${response.status} ${response.statusText}`,
		);
	}
	return response.json();
}

export function useUpdateNotes() {
	return useSWRMutation('update-notes', updateNotesFetcher);
}
