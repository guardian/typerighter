import {useEffect, useState} from "react"
import {errorToString} from "../../utils/error";
import { ErrorIResponse, responseHandler } from "../../utils/api";

const defaultTags = {}

export type Tag = {
  id: number,
  name: string
}

type TagRuleCount = {
  tagId: number,
  ruleCount: number
}

type TagRuleCounts = {
  draft: TagRuleCount[],
  live: TagRuleCount[]
}

export type TagMap = Record<string, Tag>;

export function useTags() {
  const [tags, setTags] = useState<Record<number, Tag>>(defaultTags)
  const [tagRuleCounts, setTagRuleCounts] = useState<TagRuleCounts | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingTagRuleCounts, setIsLoadingTagRuleCounts] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  const fetchTags = async (): Promise<void> => {
    setIsLoading(true);

    try {
      const response = await fetch(`${location.origin}/api/tags`);
      if (!response.ok) {
        throw new Error(`Failed to fetch rules: ${response.status} ${response.statusText}`);
      }
      const tags: Tag[] = await response.json();
      const tagMap = {} as Record<string, Tag>;
      for (const tag of tags) {
        tagMap[tag.id] = tag
      }
      setTags(tagMap);
    } catch (error) {
      setError(errorToString(error));
    }
    setIsLoading(false);
  }

  const fetchTagRuleCounts = async (): Promise<void> => {
    setIsLoadingTagRuleCounts(true);

    try {
      const response = await fetch(`${location.origin}/api/tags/ruleCount`);
      if (!response.ok) {
        throw new Error(`Failed to fetch rules: ${response.status} ${response.statusText}`);
      }
      const tagsRuleCounts: TagRuleCounts = await response.json();
      setTagRuleCounts(tagsRuleCounts);
    } catch (error) {
      setError(errorToString(error));
    }
    setIsLoadingTagRuleCounts(false);
  }

  const updateTag = async (tagForm: Tag) => {
    setIsLoading(true);

    // We would always expect the ruleForm to include an ID when updating a rule
    if (!tagForm.id) return {status: 'error', errorMessage: "Update endpoint requires a tag ID"} as ErrorIResponse;

    try {
      const response = await fetch(`${location.origin}/api/tags/${tagForm.id}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(tagForm)
      })

      const parsedResponse = await responseHandler(response);
      if (parsedResponse.status === "ok") {
        fetchTags()
        //TODO: use the returned data instead
      } else {
        setError(parsedResponse.errorMessage);
      }
    } catch (error) {
      setError(errorToString(error));
    } finally {
      setIsLoading(false);
    }
  }

  const deleteTag = async (tagForm: Tag) => {
    setIsLoading(true);

    // We would always expect the ruleForm to include an ID when updating a rule
    if (!tagForm.id) return {status: 'error', errorMessage: "Update endpoint requires a tag ID"} as ErrorIResponse;

    try {
      const response = await fetch(`${location.origin}/api/tags/${tagForm.id}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(tagForm)
      })

      const parsedResponse = await responseHandler(response);
      if (parsedResponse.status === "ok") {
        fetchTags();
        //TODO: use the returned data instead
      } else {
        setError(parsedResponse.errorMessage);
      }
    } catch (error) {
      setError(errorToString(error));
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    fetchTags();
    fetchTagRuleCounts();
  }, [])

  return { tags, isLoading, error, fetchTags, fetchTagRuleCounts, tagRuleCounts, isLoadingTagRuleCounts, updateTag, deleteTag };
}
