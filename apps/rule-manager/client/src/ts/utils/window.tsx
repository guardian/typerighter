import React, { createContext } from "react";

export type Permission = {
  permission: string,
  active: boolean
}

type PageData = {
  user?: {
    firstName: string;
    lastName: string;
    email: string;
    avatarUrl: string;
  },
  permissions: Permission[]
};

const getPageData = (): PageData => {
  const scriptId = "data";
  const pageData = document.getElementById(scriptId);
  return JSON.parse(pageData?.innerHTML!);
};

export const PageContext = createContext<PageData>({permissions: []});

export const PageDataProvider: React.FC = ({ children }) => {
  const pageData = getPageData();
  return (
    <PageContext.Provider value={pageData}> {children}</PageContext.Provider>
  );
};
