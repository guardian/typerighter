import {useEffect, useState} from "react"
import {errorToString} from "../../utils/error";

const defaultTags = {}

export type Tag = {
  id: number,
  name: string
}

export type TagMap = Record<string, Tag>;

export function useTags() {
  const [tags, setTags] = useState<Record<number, Tag>>(defaultTags)
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | undefined>(undefined);

  const fetchTags = async (): Promise<void> => {
    setIsLoading(true);

    try {
      const response = await fetch(`${location}api/tags`);
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

  useEffect(() => {
    fetchTags()
  }, [])

  return { tags, isLoading, error, fetchTags };
}
