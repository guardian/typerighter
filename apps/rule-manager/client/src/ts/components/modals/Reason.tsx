import {
  EuiButton,
  EuiButtonEmpty,
  EuiFieldText,
  EuiForm,
  EuiFormRow,
  EuiModal,
  EuiModalBody,
  EuiModalFooter,
  EuiModalHeader,
  EuiModalHeaderTitle,
  EuiSpacer,
  EuiText,
} from "@elastic/eui";
import { Label } from "../Label";
import { FormEventHandler, useState } from "react";

const modalFormId = "modal-form";

export const ReasonModal = ({
                              onClose,
                              onSubmit,
                              isLoading
                            }: {
  onClose: () => void;
  onSubmit: (reason: string) => void;
  isLoading: boolean;
}) => {
  const [reason, setReason] = useState<string>("");
  const handleSubmit: FormEventHandler<HTMLFormElement> = e => {
    e.preventDefault();
    onSubmit(reason);
  }

  return (
    <EuiModal onClose={onClose} initialFocus="[name=popswitch]">
      <EuiModalHeader>
        <EuiModalHeaderTitle>Republish rule</EuiModalHeaderTitle>
      </EuiModalHeader>
      <EuiModalBody>
        <EuiForm id={modalFormId} component="form" onSubmit={handleSubmit}>
          <EuiText>Why is this rule being revised? This helps us understand the rule's publication history.</EuiText>
          <EuiSpacer />
          <EuiFormRow
            label={<Label text="Reason" required={true} />}
            isInvalid={!reason}
          >
            <EuiFieldText
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              required={true}
              isInvalid={!reason}
            />
          </EuiFormRow>
        </EuiForm>
      </EuiModalBody>

      <EuiModalFooter>
        <EuiButtonEmpty onClick={onClose}>Cancel</EuiButtonEmpty>
        <EuiButton
          type="submit"
          form={modalFormId}
          fill
          isLoading={isLoading}
        >
          Publish
        </EuiButton>
      </EuiModalFooter>
    </EuiModal>
  );
};
