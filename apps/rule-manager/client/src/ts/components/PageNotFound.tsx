import { EuiButton, EuiFlexGroup, EuiFlexItem, EuiTitle } from "@elastic/eui"
import { css } from "@emotion/react"
import { Link } from "react-router-dom"
import typey from "../../images/typey.gif"
import typeyArrow from "../../images/typey-arrow.gif"
import styled from "@emotion/styled"

const TypeyContainer = styled.div`
    display: flex;
    flex-direction: horizontal;
    font-family: Arial, Helvetica, sans-serif;
    font-size: 1.1rem;
    image-rendering: pixelated;
    font-smooth: never;
    -webkit-font-smoothing: none;
    line-height: 1.6rem;
    position: relative;
    margin-top: 20px;
`;

const TypeySpeechBubble = styled.div`
    position: relative;
    background-color: #FCFECC;
    display: block;
    height: fit-content;
    padding: 8px;
    border: 1px solid black;
    border-radius: 8px;
    margin-left: 8px;
`;

const TypeyButton = styled.button`
    font-family: Arial, Helvetica, sans-serif;
    border: 1px solid #C3C59E;
    padding: 3px 10px;
    border-radius: 4px;
    :hover {
        background-color: #F3F5C1;
    }
    margin-top: 4px;
`;

const TypeyText = styled.p`
    color: black;
`;

const TypeyArrow = styled.img`
    position: absolute;
    left: -13px;
    bottom: 11px;
    background: none;
    transform: scaleY(-1) rotate(90deg);
`;

export const PageNotFound = () => {
    return <>
        <EuiFlexItem grow={false} css={css`padding-bottom: 20px;`}>
            <EuiTitle>
                <h1>404 - Page not found</h1>
            </EuiTitle>
        </EuiFlexItem>
            <EuiFlexGroup>
            <EuiFlexItem></EuiFlexItem>
                <TypeyContainer>
                    <img src={typey}></img>
                    <TypeySpeechBubble>
                        <p><strong>Whoops!</strong></p>
                        <p>I couldn't find that page.</p>
                        <div><TypeyButton><Link to="/"><TypeyText>Return to the rules page</TypeyText></Link></TypeyButton></div>
                        <TypeyArrow src={typeyArrow}></TypeyArrow>
                    </TypeySpeechBubble>
                </TypeyContainer>
                <EuiFlexItem></EuiFlexItem>
            </EuiFlexGroup>
    </>
}