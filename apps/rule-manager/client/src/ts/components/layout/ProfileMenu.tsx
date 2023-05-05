import { EuiContextMenu } from "@elastic/eui";
import React from "react";
import { FeatureSwitchTable } from "../FeatureSwitches";

export const ProfileMenu = () => {
  const panels = [
    {
      id: 0,
      items: [
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
      content: FeatureSwitchTable(),
    },
  ];

  return <EuiContextMenu initialPanelId={0} panels={panels} />;
};
