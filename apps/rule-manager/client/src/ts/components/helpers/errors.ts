import { FormError } from '../RuleForm';

export const hasErrorsForField = (
	fieldName: string,
	formErrors: FormError[] | undefined,
) => !!formErrors?.some(({ key }) => key === fieldName);
