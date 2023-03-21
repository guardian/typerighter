import React from "react";
import styled from "@emotion/styled"
import {Logo} from "./Logo";
import {colors} from "../../constants/constants";

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

export const Header = () => <HeaderContainer>
  <HeaderLogo><Logo/></HeaderLogo>
</HeaderContainer>;
