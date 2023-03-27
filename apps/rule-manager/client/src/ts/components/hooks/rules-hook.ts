import {useState, useEffect} from 'react';

export function useRules(endpoint) {
    const [rules, setRules] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    // @ts-ignore
    const fetchData = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(endpoint);
            const { rules } = await response.json();
            setRules(rules);

        } catch (error) {
            console.log(error);
            setError(error);
        }
        setIsLoading(false);
    }

    useEffect(() => {
        fetchData();
    }, [endpoint]);

    return {rules, isLoading, error, setRules};
}
