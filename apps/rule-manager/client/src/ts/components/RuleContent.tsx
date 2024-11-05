import {
	EuiButton,
	EuiFieldText,
	EuiFlexItem,
	EuiFormLabel,
	EuiFormRow,
	EuiIcon,
	EuiIconTip,
	EuiLink,
	EuiMarkdownFormat,
	EuiRadioGroup,
	EuiSpacer,
	EuiText,
	EuiTextArea,
} from '@elastic/eui';
import { css } from '@emotion/react';
import React, { useState } from 'react';
import { RuleFormSection } from './RuleFormSection';
import { LineBreak } from './LineBreak';
import { FormError, PartiallyUpdateRuleData } from './RuleForm';
import { Label } from './Label';
import { DraftRule, RuleData, RuleType } from './hooks/useRule';
import { RuleDataLastUpdated } from './RuleDataLastUpdated';
import { getErrorPropsForField } from './helpers/errors';

type RuleTypeOption = {
	id: RuleType;
	label: string;
};

export const ruleTypeOptions: RuleTypeOption[] = [
	{
		id: 'regex',
		label: 'Regex',
	},
	{
		id: 'dictionary',
		label: 'Dictionary',
	},
	{
		id: 'languageToolXML',
		label: 'LanguageTool',
	},
	{
		id: 'languageToolCore',
		label: 'LT built-in',
	},
];

const formDisplayRules = (ruleType: RuleType) => ({
	displayDescription: ruleType !== 'dictionary',
	displayDattern: ruleType !== 'dictionary' && ruleType !== 'languageToolCore',
	displayDeplacement:
		ruleType !== 'dictionary' && ruleType !== 'languageToolCore',
});

export const RuleContent = ({
	ruleData,
	ruleFormData,
	isLoading,
	partiallyUpdateRuleData,
	validationErrors,
	hasSaveErrors,
}: {
	ruleData: RuleData | undefined;
	ruleFormData: DraftRule;
	partiallyUpdateRuleData: PartiallyUpdateRuleData;
	validationErrors: FormError[] | undefined;
	hasSaveErrors: boolean;
	isLoading: boolean;
}) => {
	const TextField =
		ruleFormData.ruleType === 'languageToolXML' ? EuiTextArea : EuiFieldText;

	const [showMarkdownPreview, setShowMarkdownPreview] = useState(false);

	const handleButtonClick = () => {
		setShowMarkdownPreview(!showMarkdownPreview);
	};

	const patternErrors = getErrorPropsForField('pattern', validationErrors);
	const idErrors = getErrorPropsForField('externalId', validationErrors);
	const { ruleType } = ruleFormData;
	const displayDescription = ruleType !== 'dictionary';
	const displayPattern =
		ruleType !== 'dictionary' && ruleType !== 'languageToolCore';
	const displayReplacement =
		ruleType !== 'dictionary' && ruleType !== 'languageToolCore';
	const displayExternalId = ruleType === 'languageToolCore';

	return (
		<RuleFormSection
			title="RULE CONTENT"
			additionalInfo={
				ruleData && (
					<RuleDataLastUpdated
						ruleData={ruleData}
						isLoading={isLoading}
						hasErrors={hasSaveErrors}
					/>
				)
			}
		>
			<LineBreak />
			<EuiFlexItem>
				{displayDescription && (
					<EuiFormRow
						label={
							<div>
								Description&nbsp;
								<EuiIconTip
									content="Supports Markdown syntax for making text bold or italic"
									position="right"
									type="iInCircle"
									size="s"
								/>
							</div>
						}
						labelAppend={
							<EuiText size="xs">
								<EuiLink onClick={handleButtonClick}>
									Preview&nbsp;
									<EuiIcon
										type={showMarkdownPreview ? 'eyeClosed' : 'eye'}
										size="s"
									/>
								</EuiLink>
							</EuiText>
						}
						helpText="What will the users see in Composer?"
						fullWidth={true}
					>
						{!showMarkdownPreview ? (
							<EuiTextArea
								value={ruleFormData.description || ''}
								onChange={(_) =>
									partiallyUpdateRuleData({ description: _.target.value })
								}
								fullWidth={true}
								compressed={true}
							/>
						) : (
							<EuiMarkdownFormat
								textSize="relative"
								color="default"
								aria-label="Description editor"
								css={css`
									background-color: rgb(251 252 253 / 50%);
									padding: 8px;
								`}
							>
								{ruleFormData.description || ''}
							</EuiMarkdownFormat>
						)}
					</EuiFormRow>
				)}
				<EuiSpacer size="m" />
				<EuiFormLabel>Rule type</EuiFormLabel>
				<EuiRadioGroup
					options={ruleTypeOptions}
					idSelected={ruleFormData.ruleType}
					onChange={(ruleType) => {
						partiallyUpdateRuleData({ ruleType: ruleType as RuleType });
					}}
					css={css`
						display: flex;
						gap: 1rem;
						align-items: flex-end;
						white-space: nowrap;
					`}
				/>
				<EuiSpacer size="s" />
				{displayExternalId && (
					<EuiFormRow
						label={<Label text="LanguageTool ID" required={true} />}
						helpText="The ID of the built-in LanguageTool rule"
						fullWidth={true}
						{...idErrors}
					>
						<TextField
							value={ruleFormData.externalId || ''}
							onChange={(_) =>
								partiallyUpdateRuleData({ externalId: _.target.value })
							}
							required={true}
							fullWidth={true}
							{...patternErrors}
						/>
					</EuiFormRow>
				)}
				{displayPattern && (
					<EuiFormRow
						label={<Label text="Pattern" required={true} />}
						fullWidth={true}
						{...patternErrors}
					>
						<TextField
							value={ruleFormData.pattern || ''}
							onChange={(_) =>
								partiallyUpdateRuleData({ pattern: _.target.value })
							}
							required={true}
							fullWidth={true}
							{...patternErrors}
						/>
					</EuiFormRow>
				)}
				{displayReplacement && (
					<EuiFormRow
						label="Replacement"
						helpText="What is the ideal term as per the house style?"
						fullWidth={true}
					>
						<EuiFieldText
							value={ruleFormData.replacement || ''}
							onChange={(_) =>
								partiallyUpdateRuleData({ replacement: _.target.value })
							}
							fullWidth={true}
						/>
					</EuiFormRow>
				)}
			</EuiFlexItem>
		</RuleFormSection>
	);
};
