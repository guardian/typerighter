import React, { createContext } from "react";

type PageData = {
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl: string;
};

const getPageData = (): PageData | undefined => {
  const scriptId = "data";
  const pageData = document.getElementById(scriptId);
  return JSON.parse(pageData?.innerHTML!);
};

export const PageContext = createContext<PageData | undefined>(undefined);

export const PageDataProvider: React.FC = ({ children }) => {
  const pageData = getPageData();
  return (
    <PageContext.Provider value={pageData}> {children}</PageContext.Provider>
  );
};
