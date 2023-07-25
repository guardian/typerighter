import React, { useContext } from 'react';
import {
	FeatureSwitch,
	FeatureSwitchesContext,
} from './context/featureSwitches';
import { EuiBasicTable, EuiBasicTableColumn, EuiSwitch } from '@elastic/eui';

export const FeatureSwitchTable = () => {
	const { featureSwitches, toggleFeatureSwitch, getFeatureSwitchValue } =
		useContext(FeatureSwitchesContext);

	const featureColumns: Array<EuiBasicTableColumn<FeatureSwitch>> = [
		{
			field: 'name',
			name: 'Feature',
		},
		{
			field: 'active',
			name: 'Active',
			render: (_, featureSwitch) =>
				getFeatureSwitchValue(featureSwitch.id).toString(),
		},
		{
			field: 'override',
			name: 'Override',
			render: (_, featureSwitch) => (
				<EuiSwitch
					label=""
					checked={featureSwitch.overridden}
					onChange={() => toggleFeatureSwitch(featureSwitch.id)}
				/>
			),
		},
	];

	return (
		<EuiBasicTable
			columns={featureColumns}
			items={featureSwitches}
			tableLayout="auto"
		/>
	);
};
