import {useState, useEffect} from 'react';

const rulesMock = [
    {
        id: "0170d0b7-193c-46c7-b64c-56d0130329ab",
        category: {
            id: "Style guide and names",
            name: "Style guide and names"
        },
        description: "**riverbed**",
        suggestions: [],
        replacement: {
            type: "TEXT_SUGGESTION",
            text: "riverbed"
        },
        regex: "(?i)\\briver-? ?bed",
        _type: "com.gu.typerighter.model.RegexRule"
    },
    {
        id: "b04d6180-aa72-425f-86d7-e1d518f9fca6",
        category: {
            id: "Style guide and names",
            name: "Style guide and names"
        },
        description: "**Royal National Institute of Blind People (no longer the Blind)**",
        suggestions: [],
        replacement: {
            type: "TEXT_SUGGESTION",
            text: "Royal National Institute of Blind People"
        },
        regex: "\\bRoyal National Institute of (the )?Blind( People)?",
        _type: "com.gu.typerighter.model.RegexRule"
    },
];

export function useApi(endpoint, method, body) {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    // @ts-ignore
    const fetchData = async () => {
        setIsLoading(true);
        try {
            const response = await fetch(endpoint, {
                method,
                headers: {
                    'Csrf-Token': 'nocheck'
                }
            })
            const {rules} = await response.json();
            setData(rules);

        } catch (error) {
            console.log(error);
            setError(error);
        }
        setIsLoading(false);
    }

    useEffect(() => {
        fetchData();
    }, [endpoint, method, body]);

    return {data, isLoading, error, setData};
}
