import { useEffect, useState } from 'react';
import { errorToString } from '../../utils/error';
import {DraftRule} from "./useRule";

export function useRules() {
    const { location } = window;
    const [rules, setRules] = useState<DraftRule[] | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | undefined>(undefined);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const fetchRules = async (): Promise<void> => {
        setIsLoading(true);
        try {
            const response = await fetch(`${location.origin}/api/rules`);
            if (!response.ok) {
                throw new Error(`Failed to fetch rules: ${response.status} ${response.statusText}`);
            }
            const rules = await response.json();
            setRules(rules);
        } catch (error) {
            setError(errorToString(error));
        }
        setIsLoading(false);
    }

    const refreshRules = async (): Promise<void>  => {
        setIsRefreshing(true);
        try {
            const updatedRulesResponse = await fetch(`${location.origin}/api/refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
            });
            if (!updatedRulesResponse.ok) {
                throw new Error(`Failed to refresh rules: ${updatedRulesResponse.status} ${updatedRulesResponse.statusText}`);
            }
            const rules = await updatedRulesResponse.json();
            setRules(rules);
        } catch (e) {
            setError(errorToString(error));
        } finally {
            setIsRefreshing(false);
        }
    }

    useEffect(() => {
        fetchRules();
    }, []);

    return { rules, isLoading, error, refreshRules, isRefreshing, setError, fetchRules };
}
