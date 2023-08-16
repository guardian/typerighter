import {
	EuiButton,
	EuiButtonEmpty,
	EuiForm,
	EuiModal,
	EuiModalBody,
	EuiModalFooter,
	EuiModalHeader,
	EuiModalHeaderTitle,
	EuiSpacer,
	EuiText,
} from '@elastic/eui';
import { FormEventHandler, useState } from 'react';
import { RuleData } from '../hooks/useRule';
import { Diff } from '../Diff';

const modalFormId = 'modal-form';

export const RevertModal = ({
	onClose,
	onSubmit,
	isLoading,
	rule,
}: {
	onClose: () => void;
	onSubmit: () => void;
	isLoading: boolean;
	rule: RuleData | undefined;
}) => {
	const handleSubmit: FormEventHandler<HTMLFormElement> = (e) => {
		e.preventDefault();
		onSubmit();
	};

	return (
		<EuiModal onClose={onClose} initialFocus="[name=popswitch]">
			<EuiModalHeader>
				<EuiModalHeaderTitle>Revert rule</EuiModalHeaderTitle>
			</EuiModalHeader>
			<EuiModalBody>
				<EuiForm id={modalFormId} component="form" onSubmit={handleSubmit}>
					<EuiText>Are you sure you want to discard these changes?</EuiText>
					<EuiSpacer />
				</EuiForm>
				<Diff
					rule={rule}
					beforeText={'Before changes:'}
					afterText={'After changes:'}
				/>
			</EuiModalBody>

			<EuiModalFooter>
				<EuiButtonEmpty onClick={onClose}>Cancel</EuiButtonEmpty>
				<EuiButton type="submit" form={modalFormId} fill isLoading={isLoading}>
					Discard changes
				</EuiButton>
			</EuiModalFooter>
		</EuiModal>
	);
};
