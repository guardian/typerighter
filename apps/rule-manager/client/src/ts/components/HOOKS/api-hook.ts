import {useState, useEffect} from 'react';

export function useApi(endpoint, method, body) { // TODO - keep the endpoint, lose the method and  body - rename to something less general
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
                    'Csrf-Token': 'nocheck' // TODO - check is this needs to be here
                }
            })
            const {rules} = await response.json();
            // setData(rules.slice(0, 500));
            setData(rules);

        } catch (error) {
            console.log(error);
            setError(error);
        }
        setIsLoading(false);
    }

    useEffect(() => { // TODO - is this doing anything for us?
        fetchData();
    }, [endpoint, method, body]);

    return {data, isLoading, error, setData};
}
