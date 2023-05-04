import {
  EuiBasicTable,
  EuiBasicTableColumn,
  EuiContextMenu,
  EuiSwitch,
} from "@elastic/eui";
import React, { useContext } from "react";
import {
  FeatureSwitch,
  FeatureSwitchesContext,
} from "../context/featureSwitches";
import { PageContext } from "../../utils/window";

export const ProfileMenu = () => {
  const { featureSwitches, toggleFeatureSwitch, getFeatureSwitchValue } =
    useContext(FeatureSwitchesContext);

  const featureColumns: Array<EuiBasicTableColumn<FeatureSwitch>> = [
    {
      field: "name",
      name: "Feature",
    },
    {
      field: "active",
      name: "Active",
      render: (_, featureSwitch) =>
        getFeatureSwitchValue(featureSwitch.id).toString(),
    },
    {
      field: "override",
      name: "Override",
      render: (_, featureSwitch) => (
        <EuiSwitch
          label=""
          checked={featureSwitch.overridden}
          onChange={() => toggleFeatureSwitch(featureSwitch.id)}
        />
      ),
    },
  ];
  const panels = [
    {
      id: 0,
      items: [
        {
          name: "Logout",
          icon: "exit",
          onClick: () => (window.location.href = "/logout"),
        },
        {
          name: "Feature switches",
          icon: "starEmptySpace",
          panel: 1,
        },
      ],
    },
    {
      id: 1,
      title: "Feature switches",
      width: 400,
      content: (
        <EuiBasicTable columns={featureColumns} items={featureSwitches} tableLayout="auto" />
      ),
    },
  ];

  return <EuiContextMenu initialPanelId={0} panels={panels} />;
};
