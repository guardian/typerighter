import React from "react";

export const Label = ({text, required}: {text: string, required: boolean}) => {
    return <>
        {text} {required ? <span style={{fontWeight: "normal"}}>(required)</span> : null}
    </>
}