import React, { useContext, useState } from "react";
import styled from "@emotion/styled";
import { Logo } from "./Logo";
import { colors } from "../../constants/constants";
import { EuiHeaderLink, EuiHeaderLinks, withEuiTheme, WithEuiThemeProps } from "@elastic/eui";
import { DownChevron } from "../icons/downChevron";
import { ProfileMenu } from "./ProfileMenu";
import { EuiPopover } from "@elastic/eui";
import { PageContext } from "../../utils/window";
import { Link, useLocation } from "react-router-dom";
import { FeatureSwitchesContext } from "../context/featureSwitches";

export const headerHeight = "50px";

const HeaderContainer = styled.div`
  position: fixed;
  display: flex;
  height: ${headerHeight};
  width: 100%;
  background-color: white;
  z-index: 10;
`;

const NavContainer = styled.div`
  display: flex;
  margin-right: auto;
`;

const HeaderLogo = styled.div`
  height: ${headerHeight};
  width: ${headerHeight};
  margin-right: auto;
  background-color: ${colors.backgroundColorDark};
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
`;

const UserActionMenu = withEuiTheme(styled.div<WithEuiThemeProps>`
  height: ${headerHeight};
  line-height: ${headerHeight};
  padding: 0 ${({ theme }) => theme.euiTheme.base}px;
  cursor: pointer;
`);

export const Header = () => {
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);
  const pageData = useContext(PageContext)
  const { getFeatureSwitchValue } = useContext(FeatureSwitchesContext);

  const toggleProfileMenu = () => setIsProfileMenuOpen(!isProfileMenuOpen);

  const ProfileMenuButton = (
    <UserActionMenu onClick={toggleProfileMenu}>
      {pageData.user?.firstName} {pageData.user?.lastName} &nbsp;
      <DownChevron />
    </UserActionMenu>
  );

  return (
    <HeaderContainer>
      <NavContainer>
        <HeaderLogo>
          <Link to="/">
            <Logo />
          </Link>
        </HeaderLogo>
        {
        getFeatureSwitchValue('show-tags-page') ?
          <EuiHeaderLinks>
            <Link to="/">
              <EuiHeaderLink isActive={useLocation().pathname === "/"}>Rules</EuiHeaderLink>
            </Link>
            <Link to="/tags">
              <EuiHeaderLink isActive={useLocation().pathname === "/tags"}>Tags</EuiHeaderLink>
            </Link>
          </EuiHeaderLinks> : null
        }
      </NavContainer>
      <EuiPopover
        button={ProfileMenuButton}
        isOpen={isProfileMenuOpen}
        closePopover={() => setIsProfileMenuOpen(false)}
        panelPaddingSize="none"
      >
        <ProfileMenu />
      </EuiPopover>
    </HeaderContainer>
  );
};
