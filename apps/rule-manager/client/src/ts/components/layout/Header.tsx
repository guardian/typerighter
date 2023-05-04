import React, { useState } from "react";
import styled from "@emotion/styled";
import { Logo } from "./Logo";
import { colors } from "../../constants/constants";
import { withEuiTheme, WithEuiThemeProps } from "@elastic/eui";
import { DownChevron } from "../icons/downChevron";
import { ProfileMenu } from "./ProfileMenu";
import { EuiPopover } from "@elastic/eui";

const HeaderContainer = styled.div`
  display: flex;
  height: 50px;
  background-color: white;
`;

const HeaderLogo = styled.div`
  height: 50px;
  width: 50px;
  margin-right: auto;
  background-color: ${colors.backgroundColorDark};
  display: flex;
  align-items: center;
  justify-content: center;
`;

const UserActionMenu = withEuiTheme(styled.div<WithEuiThemeProps>`
  height: 50px;
  line-height: 50px;
  padding: 0 ${({ theme }) => theme.euiTheme.base}px;
  cursor: pointer;
`);

export const Header = () => {
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(false);

  const toggleProfileMenu = () => setIsProfileMenuOpen(!isProfileMenuOpen);

  const ProfileMenuButton = (
    <UserActionMenu onClick={toggleProfileMenu}>
      Jonathon Herbert &nbsp;
      <DownChevron />
    </UserActionMenu>
  );

  return (
    <HeaderContainer>
      <HeaderLogo>
        <Logo />
      </HeaderLogo>
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
