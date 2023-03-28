import React, {useEffect, useState} from 'react';

export function useRules() {
    const { location } = window;
    const [rules, setRules] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [isRefreshing, setIsRefreshing] = useState(false);
    // @ts-ignore
    const fetchRules = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(`${location}rules`);
            if (!response.ok) {
                throw new Error('Failed to fetch rules');
            }
            const { rules } = await response.json();
            setRules(rules);
        } catch (error) {
            console.log(error);
            setError(error);
        }
        setIsLoading(false);
    }

    // @ts-ignore
    const refreshRules = async () => {
        setIsRefreshing(true);
        try {
            const updatedRulesResponse = await fetch(`${location}refresh`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
            });
            if (!updatedRulesResponse.ok) {
                throw new Error('Failed to refresh rules');
            }
            const {rules} = await updatedRulesResponse.json();
            setRules(rules);
            setIsRefreshing(false);
        } catch (e) {
            setError(e);
            setIsRefreshing(false);
        }
    }

    useEffect(() => {
        fetchRules();
    }, []);

    return { rules, isLoading, error, refreshRules, isRefreshing, setError };
}
