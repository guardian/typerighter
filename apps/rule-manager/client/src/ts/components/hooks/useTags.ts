import { useEffect, useState } from 'react';
import { errorToString } from '../../utils/error';
import {
	ErrorIResponse,
	responseHandler,
	textResponseHandler,
} from '../../utils/api';

const defaultTags = {};

export type Tag = {
	id: number;
	name: string;
	ruleCount: number;
};

export type TagContent = Omit<Tag, 'ruleCount'>;

export type TagMap = Record<string, Tag>;

export function useTags() {
	const [tags, setTags] = useState<Record<number, Tag>>(defaultTags);
	const [isLoading, setIsLoading] = useState(false);
	const [isLoadingCreatedTag, setIsLoadingCreatedTag] = useState(false);
	const [error, setError] = useState<string | undefined>(undefined);

	const fetchTags = async (): Promise<void> => {
		setIsLoading(true);

		try {
			const response = await fetch(`${location.origin}/api/tags`);
			if (!response.ok) {
				throw new Error(
					`Failed to fetch rules: ${response.status} ${response.statusText}`,
				);
			}
			const tags: Tag[] = await response.json();
			const tagMap = {} as Record<string, Tag>;
			for (const tag of tags) {
				tagMap[tag.id] = tag;
			}
			setTags(tagMap);
		} catch (error) {
			setError(errorToString(error));
		}
		setIsLoading(false);
	};

	const updateTag = async (tagForm: TagContent) => {
		setIsLoading(true);

		// We would always expect the ruleForm to include an ID when updating a rule
		if (!tagForm.id)
			return {
				status: 'error',
				errorMessage: 'Update endpoint requires a tag ID',
			} as ErrorIResponse;

		try {
			const response = await fetch(
				`${location.origin}/api/tags/${tagForm.id}`,
				{
					method: 'POST',
					headers: {
						'Content-Type': 'application/json',
					},
					body: JSON.stringify(tagForm),
				},
			);

			const parsedResponse = await responseHandler<Tag>(response);
			if (parsedResponse.status === 'ok') {
				const updatedTag = parsedResponse.data;
				setTags({ ...tags, [`${updatedTag.id}`]: updatedTag });
			} else {
				setError(parsedResponse.errorMessage);
			}
		} catch (error) {
			setError(errorToString(error));
		} finally {
			setIsLoading(false);
		}
	};

	const createTag = async (tagName: string) => {
		setIsLoadingCreatedTag(true);
		try {
			const response = await fetch(`${location.origin}/api/tags`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
				},
				body: JSON.stringify({ name: tagName }),
			});

			const parsedResponse = await responseHandler<Tag>(response);
			if (parsedResponse.status === 'ok') {
				const updatedTag = parsedResponse.data;
				setTags({ ...tags, [`${updatedTag.id}`]: updatedTag });
			} else {
				setError(parsedResponse.errorMessage);
			}
		} catch (error) {
			setError(errorToString(error));
		} finally {
			setIsLoadingCreatedTag(false);
		}
	};

	const deleteTag = async (tagForm: Tag) => {
		setIsLoading(true);

		// We would always expect the ruleForm to include an ID when updating a rule
		if (!tagForm.id)
			return {
				status: 'error',
				errorMessage: 'Update endpoint requires a tag ID',
			} as ErrorIResponse;

		try {
			const response = await fetch(
				`${location.origin}/api/tags/${tagForm.id}`,
				{
					method: 'DELETE',
					headers: {
						'Content-Type': 'application/json',
					},
					body: JSON.stringify(tagForm),
				},
			);

			const parsedResponse = await textResponseHandler(response);

			if (parsedResponse.status === 'ok') {
				await fetchTags();
			} else {
				setError(parsedResponse.errorMessage);
			}
		} catch (error) {
			setError(errorToString(error));
		} finally {
			setIsLoading(false);
		}
	};

	useEffect(() => {
		fetchTags();
	}, []);

	return {
		tags,
		isLoading,
		error,
		fetchTags,
		updateTag,
		deleteTag,
		createTag,
		isLoadingCreatedTag,
	};
}
