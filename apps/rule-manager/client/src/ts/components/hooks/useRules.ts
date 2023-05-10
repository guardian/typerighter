import {useEffect, useState} from 'react';

export function useRules() {
    const { location } = window;
    const [rules, setRules] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isRefreshing, setIsRefreshing] = useState(false);
    const fetchRules = async (): Promise<void> => {
        setIsLoading(true);
        try {
            const response = await fetch(`${location}rules`);
            if (!response.ok) {
                throw new Error(`Failed to fetch rules: ${response.status} ${response.statusText}`);
            }
            const rules = await response.json();
            setRules(rules);
        } catch (error) {
            setError(error);
        }
        setIsLoading(false);
    }

    const refreshRules = async (): Promise<void>  => {
        setIsRefreshing(true);
        try {
            const updatedRulesResponse = await fetch(`${location}refresh`, {
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
            setError(e);
        } finally {
            setIsRefreshing(false);
        }
    }

    useEffect(() => {
        fetchRules();
    }, []);

    return { rules, isLoading, error, refreshRules, isRefreshing, setError, fetchRules };
}
