import {useState} from "react";
import {EuiFormRow, EuiComboBox} from "@elastic/eui";

export const TagsSelector = () => {
    const [options, updateOptions] = useState([
        {
            label: 'Tag A',
        },
        {
            label: 'Tag B',
        }
    ]);

    const [selectedOptions, setSelected] = useState([]);

    const onChange = (selectedOptions) => {
        setSelected(selectedOptions);
    };

    const onCreateOption = (searchValue, flattenedOptions) => {
        const normalizedSearchValue = searchValue.trim().toLowerCase();

        if (!normalizedSearchValue) {
            return;
        }

        const newOption = {
            label: searchValue,
        };

        // Create the option if it doesn't exist.
        if (
            flattenedOptions.findIndex(
                (option) => option.label.trim().toLowerCase() === normalizedSearchValue
            ) === -1
        ) {
            updateOptions([...options, newOption]);
        }

        // Select the option.
        setSelected((prevSelected) => [...prevSelected, newOption]);
    };

    return (
        <EuiFormRow label='Tags'>
            <EuiComboBox
                options={options}
                selectedOptions={selectedOptions}
                onChange={onChange}
                onCreateOption={onCreateOption}
                isClearable={true}
                isCaseSensitive
            />
        </EuiFormRow>
    );
}