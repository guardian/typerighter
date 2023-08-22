import {
	EuiBadge,
	EuiFlexGroup,
	EuiFlexItem,
	EuiHorizontalRule,
	EuiSpacer,
	EuiText,
	useEuiTextDiff,
} from '@elastic/eui';
import { BaseRule, RuleData } from './hooks/useRule';
import { Tag, useTags } from './hooks/useTags';
import { css } from '@emotion/react';
import styled from '@emotion/styled';
import { ruleTypeOptions } from './RuleContent';
import { isEqual, startCase } from 'lodash';

type DivergentField = {
	fieldName: string;
	live: FieldValue;
	draft: FieldValue;
};

type FieldValue = string | number | boolean | number[] | string[] | undefined;

export type FieldObject = {
	fieldName: string;
	value: FieldValue;
};

const ComparisonPanel = styled.div`
	height: 100%;
`;

const ComparisonPanelHeader = styled.div`
	padding: 0.25rem 0;
	color: #444;
`;

const textDiffFields = ['description', 'pattern', 'replacement'];
const comparisonDiffFields = ['ruleType', 'category', 'tags'];

export const findNonIntersectingFields = (
	draft: FieldObject[],
	live: FieldObject[],
): DivergentField[] => {
	const uniqueDraftFields = draft.filter(
		(draftField) =>
			!live.find((liveField) => liveField.fieldName === draftField.fieldName),
	);
	const uniqueLiveFields = live.filter(
		(liveField) =>
			!draft.find((draftField) => liveField.fieldName === draftField.fieldName),
	);
	return [
		...uniqueDraftFields.map((draftField) => {
			return {
				draft: draftField.value,
				live: undefined,
				fieldName: draftField.fieldName,
			};
		}),
		...uniqueLiveFields.map((liveField) => {
			return {
				draft: undefined,
				live: liveField.value,
				fieldName: liveField.fieldName,
			};
		}),
	];
};

export const findFieldsWithDiffs = (rule: RuleData): DivergentField[] => {
	const newestLiveRule = rule.live.reduce((acc, cur) =>
		cur.revisionId > acc.revisionId ? cur : acc,
	);
	const draftFieldObjects = Object.keys(rule.draft).map((fieldName) => {
		return { fieldName, value: rule.draft[fieldName as keyof BaseRule] };
	});
	const liveFieldObjects = Object.keys(newestLiveRule).map((fieldName) => {
		return { fieldName, value: newestLiveRule[fieldName as keyof BaseRule] };
	});

	const fieldsToProvideDiffFor = [...textDiffFields, ...comparisonDiffFields];
	// The rule data contains various fields we're not interested in providing diffs for
	const draftFieldsToDiff = draftFieldObjects.filter((draftField) =>
		fieldsToProvideDiffFor.includes(draftField.fieldName),
	);
	const liveFieldsToDiff = liveFieldObjects.filter((liveField) =>
		fieldsToProvideDiffFor.includes(liveField.fieldName),
	);

	// Add fields that only appear in one of live and draft
	const nonIntersectingFields = findNonIntersectingFields(
		draftFieldsToDiff,
		liveFieldsToDiff,
	);

	// Compare fields that appear in both live and draft
	const divergentFields = draftFieldsToDiff.reduce(
		(divergentFieldsArray, draftField) => {
			const equivalentLiveField = liveFieldsToDiff.find(
				(liveField) => liveField.fieldName === draftField.fieldName,
			);
			if (equivalentLiveField) {
				const liveAndDraftValuesMatch = isEqual(
					draftField.value,
					equivalentLiveField.value,
				);
				if (!liveAndDraftValuesMatch) {
					divergentFieldsArray.push({
						fieldName: draftField.fieldName,
						live: equivalentLiveField.value,
						draft: draftField.value,
					});
				}
			}
			return divergentFieldsArray;
		},
		nonIntersectingFields,
	);
	return divergentFields;
};

export const transformToHumanReadableValues = (
	fields: DivergentField[],
	tags: Record<number, Tag>,
) => {
	return fields.map((field) => {
		// Use tag names instead of ids
		if (field.fieldName === 'tags') {
			if (Array.isArray(field.draft)) {
				field.draft = field.draft.map((field) =>
					tags[field as number] ? tags[field as number].name : field.toString(),
				);
			}
			if (Array.isArray(field.live)) {
				field.live = field.live.map((field) =>
					tags[field as number] ? tags[field as number].name : field.toString(),
				);
			}
		}
		// Use readable ruleType names
		if (field.fieldName === 'ruleType') {
			field.draft =
				ruleTypeOptions.find((option) => option.id === field.draft)?.label ||
				field.draft;
			field.live =
				ruleTypeOptions.find((option) => option.id === field.live)?.label ||
				field.live;
		}
		return field;
	});
};

export const Diff = ({
	rule,
	beforeHeading,
	afterHeading,
	modalType,
}: {
	rule: RuleData | undefined;
	beforeHeading: string;
	afterHeading: string;
	modalType: 'Reason' | 'Revert';
}) => {
	const { tags } = useTags();

	const diffedFields = rule
		? transformToHumanReadableValues(findFieldsWithDiffs(rule), tags)
		: [];
	const textDiffs = diffedFields.filter((diffedField) =>
		textDiffFields.includes(diffedField.fieldName),
	);
	const comparisonDiffs = diffedFields.filter((diffedField) =>
		comparisonDiffFields.includes(diffedField.fieldName),
	);

	return (
		<>
			<EuiSpacer />
			<EuiFlexGroup>
				<EuiFlexItem>
					<EuiFlexGroup>
						<EuiFlexItem
							css={css`
								width: 100px;
							`}
						>
							<strong>Field</strong>
						</EuiFlexItem>
						<EuiFlexItem grow>
							<strong>{beforeHeading}</strong>
						</EuiFlexItem>
						<EuiFlexItem grow>
							<strong>{afterHeading}</strong>
						</EuiFlexItem>
					</EuiFlexGroup>
					{diffedFields.map((diffedField) => (
						<FieldDiff
							draft={diffedField.draft}
							live={diffedField.live}
							name={diffedField.fieldName}
							key={diffedField.fieldName}
							modalType={modalType}
						/>
					))}
				</EuiFlexItem>
			</EuiFlexGroup>
		</>
	);
};

export const Comparison = ({
	left,
	right,
	fieldName,
}: {
	left: JSX.Element | JSX.Element[];
	right: JSX.Element | JSX.Element[];
	fieldName: string;
}) => {
	return (
		<EuiFlexGroup>
			<EuiFlexItem>
				<EuiHorizontalRule margin="xs" />
				<EuiFlexGroup>
					<EuiFlexItem
						css={css`
							width: 100px;
						`}
					>
						<ComparisonPanelHeader>{fieldName}</ComparisonPanelHeader>
					</EuiFlexItem>
					<EuiFlexItem grow>
						<ComparisonPanel>{left}</ComparisonPanel>
					</EuiFlexItem>
					<EuiFlexItem grow>
						<ComparisonPanel>{right}</ComparisonPanel>
					</EuiFlexItem>
				</EuiFlexGroup>
			</EuiFlexItem>
		</EuiFlexGroup>
	);
};

export const FieldDiff = ({
	draft,
	live,
	name,
	modalType,
}: {
	draft: FieldValue;
	live: FieldValue;
	name: string;
	modalType: 'Reason' | 'Revert';
}) => {
	const isTextField = textDiffFields.includes(name);
	const isComparisonField = comparisonDiffFields.includes(name);

	return (
		<>
			{isTextField ? (
				<TextDiff draft={draft} live={live} name={name} modalType={modalType} />
			) : null}
			{isComparisonField ? (
				<ComparisonDiff
					draft={draft}
					live={live}
					name={name}
					modalType={modalType}
				/>
			) : null}
		</>
	);
};

export const ComparisonDiff = ({
	draft,
	live,
	name,
	modalType,
}: {
	draft: FieldValue;
	live: FieldValue;
	name: string;
	modalType: 'Reason' | 'Revert';
}) => {
	return (
		<div>
			{modalType === 'Reason' && (
				<Comparison
					left={
						Array.isArray(live) ? (
							live.map((item) => <EuiBadge>{item}</EuiBadge>)
						) : (
							<EuiBadge>{live}</EuiBadge>
						)
					}
					right={
						Array.isArray(draft) ? (
							draft.map((item) => <EuiBadge>{item}</EuiBadge>)
						) : (
							<EuiBadge>{draft}</EuiBadge>
						)
					}
					fieldName={startCase(name)}
					key={name}
				/>
			)}
			{modalType === 'Revert' && (
				<Comparison
					left={
						Array.isArray(draft) ? (
							draft.map((item) => <EuiBadge>{item}</EuiBadge>)
						) : (
							<EuiBadge>{draft}</EuiBadge>
						)
					}
					right={
						Array.isArray(live) ? (
							live.map((item) => <EuiBadge>{item}</EuiBadge>)
						) : (
							<EuiBadge>{live}</EuiBadge>
						)
					}
					fieldName={startCase(name)}
					key={name}
				/>
			)}
		</div>
	);
};

export const TextDiff = ({
	draft,
	live,
	name,
	modalType,
}: {
	draft: FieldValue;
	live: FieldValue;
	name: string;
	modalType: 'Reason' | 'Revert';
}) => {
	const [rendered] = useEuiTextDiff({
		beforeText: live ? live.toString() : '',
		afterText: draft ? draft.toString() : '',
	});
	return (
		<div>
			{modalType === 'Reason' && (
				<Comparison
					left={<>{live}</>}
					right={<>{rendered}</>}
					fieldName={startCase(name)}
				/>
			)}
			{modalType === 'Revert' && (
				<Comparison
					left={<>{rendered}</>}
					right={<>{live}</>}
					fieldName={startCase(name)}
				/>
			)}
		</div>
	);
};
