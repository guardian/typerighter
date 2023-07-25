import React, { createContext, useCallback, useState } from 'react';

export type FeatureSwitch = {
	name: string;
	default: boolean;
	id: FeatureSwitchIds;
	overridden: boolean;
};

const allFeatureSwitches = [
	{
		name: 'Enable destructive reload from rules sheet',
		id: 'enable-destructive-reload',
		default: false,
	},
] as const;

type FeatureSwitchIds = (typeof allFeatureSwitches)[number]['id'];

const getFeatureOverride = (featureId: FeatureSwitchIds): boolean =>
	localStorage.getItem(`manager.typerighter.features.${featureId}`) === 'true';

const setFeatureOverride = (featureId: FeatureSwitchIds, override: boolean) =>
	localStorage.setItem(
		`manager.typerighter.features.${featureId}`,
		JSON.stringify(override),
	);

const getCurrentFeatureSwitchValues = () =>
	allFeatureSwitches.map((s) => ({
		...s,
		overridden: getFeatureOverride(s.id),
	}));

const initialFeatureSwitchValues = getCurrentFeatureSwitchValues();

export const FeatureSwitchesContext = createContext<{
	featureSwitches: FeatureSwitch[];
	toggleFeatureSwitch: (name: FeatureSwitchIds) => void;
	getFeatureSwitchValue: (name: FeatureSwitchIds) => boolean;
}>({
	featureSwitches: initialFeatureSwitchValues,
	// The setters and getters are a noop until defined by a context.
	toggleFeatureSwitch: (_: FeatureSwitchIds) => {},
	getFeatureSwitchValue: () => false,
});

/**
 * Provide a context to enable consumers to read and toggle feature switches.
 *
 * Example usage in parent component:
 *
 * <FeatureSwitchesProvider>
 *   {children}
 * </FeatureSwitchesProvider>
 *
 * Example usage in child component:
 *
 * const { featureSwitches, toggleFeatureSwitch, getFeatureSwitchValue } = useContext(FeatureSwitchesContext);
 */
export const FeatureSwitchesProvider: React.FC = ({ children }) => {
	const [featureSwitches, setFeatureSwitches] = useState(
		initialFeatureSwitchValues,
	);

	const withFeatureSwitch = <R extends unknown>(
		featureSwitches: FeatureSwitch[],
		featureId: FeatureSwitchIds,
		ifNotFound: R,
		callback: (featureSwitch: FeatureSwitch) => R,
	): R => {
		const featureSwitch = featureSwitches.find((_) => _.id === featureId);

		if (!featureSwitch) {
			console.warn(
				`[FeatureSwitchesProvider]: Cannot find feature switch with id ${featureId}`,
			);
			return ifNotFound;
		}

		return callback(featureSwitch);
	};

	const toggleFeatureSwitch = useCallback(
		(featureId: FeatureSwitchIds) => {
			withFeatureSwitch(
				featureSwitches,
				featureId,
				undefined,
				(featureSwitch) => {
					setFeatureOverride(featureId, !featureSwitch.overridden);
					// Read our own writes back into the context state
					setFeatureSwitches(getCurrentFeatureSwitchValues());
				},
			);
		},
		[featureSwitches],
	);

	const getFeatureSwitchValue = useCallback(
		(featureId: FeatureSwitchIds) => {
			return withFeatureSwitch(
				featureSwitches,
				featureId,
				false,
				(featureSwitch) =>
					featureSwitch.overridden
						? !featureSwitch.default
						: featureSwitch.default,
			);
		},
		[featureSwitches],
	);

	return (
		<FeatureSwitchesContext.Provider
			value={{ featureSwitches, toggleFeatureSwitch, getFeatureSwitchValue }}
		>
			{children}
		</FeatureSwitchesContext.Provider>
	);
};
