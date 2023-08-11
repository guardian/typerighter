import { capitalize } from 'lodash';
import { FormError } from '../RuleForm';

const defaultErrors: FormError[] = [];

/**
 * Given a list of validation errors and a field name, return props that can be
 * spread in
 */
export const getErrorPropsForField = (
	fieldName: string,
	formErrors: FormError[] | undefined,
) => {
	const fieldErrors =
		formErrors?.filter(({ key }) => key === fieldName) || defaultErrors;

	return {
		isInvalid: !!fieldErrors?.length,
		error: fieldErrors?.map(({ message }) => capitalize(message)),
	};
};
