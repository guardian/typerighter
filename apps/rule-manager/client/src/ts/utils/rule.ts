import { DraftRule, RuleData } from '../components/hooks/useRule';
import { IconColor } from '@elastic/eui/src/components/icon';

export type RuleStatus = 'live' | 'archived' | 'draft' | 'error';

export const getRuleStatus = (rule: DraftRule | undefined): RuleStatus => {
	if (!rule) {
		return 'draft';
	}

	if (rule.isPublished && rule.isArchived) {
		return 'error';
	}

	if (rule.isPublished) {
		return 'live';
	}

	if (rule.isArchived) {
		return 'archived';
	}

	return 'draft';
};

export const getRuleStatusColour = (rule: DraftRule | undefined): IconColor =>
	rule ? statusToColourMap[getRuleStatus(rule)] : statusToColourMap.draft;

const statusToColourMap: { [status in RuleStatus]: IconColor } = {
	error: 'danger',
	live: 'success',
	archived: 'danger',
	draft: '#DA8B45',
};

export const hasUnpublishedChanges = (ruleData: RuleData) =>
	ruleData.draft.isPublished &&
	!ruleData.live.some(
		(liveRule) => liveRule.revisionId === ruleData.draft.revisionId,
	);
