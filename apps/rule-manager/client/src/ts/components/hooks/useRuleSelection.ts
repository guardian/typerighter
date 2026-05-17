import { useReducer } from 'react';
import { PaginatedRuleData } from './useRules';

export type RowState = Set<number>;
export type RowAction =
	| { type: 'add'; id: number }
	| { type: 'delete'; id: number }
	| { type: 'set'; id: number }
	| { type: 'clear' }
	| { type: 'selectAll' };

export const useRuleSelection = (
	initialRules: Set<number>,
	ruleData: PaginatedRuleData | null,
) =>
	useReducer((selectedRows: RowState, action: RowAction): RowState => {
		switch (action.type) {
			case 'set': {
				return new Set([action.id]);
			}
			case 'add': {
				const nextRowSelection = new Set(selectedRows);
				nextRowSelection.add(action.id);
				return nextRowSelection;
			}
			case 'delete': {
				const nextRowSelection = new Set(selectedRows);
				nextRowSelection.delete(action.id);
				return nextRowSelection;
			}
			case 'clear': {
				return new Set();
			}
			case 'selectAll': {
				return !ruleData?.data || selectedRows.size === ruleData.data.length
					? new Set()
					: new Set(ruleData.data.map((rule) => rule.id as number));
			}
		}
	}, initialRules);
