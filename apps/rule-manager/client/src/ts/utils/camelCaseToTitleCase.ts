export const camelCaseToTitleCase = (str: string) => {
    // Add space between strings
    const result = str.replace(/([A-Z])/g,' $1');
    return result.charAt(0).toUpperCase() + result.slice(1);
}