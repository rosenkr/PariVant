import {Paper, Stack, Typography, type PaperProps} from "@mui/material";
import { Page } from "../components/layout/Page";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";

import type {ReactNode} from "react";
import {MenuBook} from "@mui/icons-material";

type TextProps = {
  children: ReactNode;
};

type AboutPaperProps = {
  children: ReactNode;
} & PaperProps;

function AboutPaper({children, ...paperProps}: AboutPaperProps) {
  return (
    <Paper {...paperProps}>
      <Stack padding={2} spacing={2}>
        {children}
      </Stack>
    </Paper>
  );
}

function LargeHeader({ children }: TextProps) {
  return (
      <Typography
          variant="h6"
          sx={(theme) => ({
            color: theme.appColors.text.primary,
            fontWeight: 800,
            lineHeight: 1.15,
          })}
      >
        {children}
      </Typography>
  );
}

function MediumHeader({ children }: TextProps) {
  return (
      <Typography
          variant="subtitle1"
          sx={(theme) => ({
            color: theme.appColors.text.primary,
            fontWeight: 700,
            lineHeight: 1.25,
          })}
      >
        {children}
      </Typography>
  );
}

function RegularText({ children }: TextProps) {
  return (
      <Typography
          variant="body1"
          sx={(theme) => ({
            color: theme.appColors.text.secondary,
            lineHeight: 1.7,
          })}
      >
        {children}
      </Typography>
  );
}

function HeroPaper() {
  return (
      <AboutPaper>
        <Stack direction={"row"} spacing={0.5}>
          <InfoOutlinedIcon></InfoOutlinedIcon>
          <MediumHeader>What is Parivant?</MediumHeader>
        </Stack>

        <RegularText>Parivant is a project intending to be used as a support tool for pari-mutuel betting on Svenskaspel products.
          It combines an internal model for automating mathematically sane decisions based on a wisdom-of-the-crowd mindset,
          together with a subjective approach by aggregating fresh news about matches to facilitate the domain knowledge of the
          bettor.</RegularText>

        <Stack spacing={0.5}>
          <LargeHeader>Motivation Behind This Project</LargeHeader>
          <RegularText>This project emerged from a personal interest in pari-mutuel products for soccer.
            While Svenskaspel provides some information about matches, it is often not enough
            for making confident picks. As a soccer fan, I wanted to create a tool that collects
            relevant information about matches of for example Stryktipset in one place.  </RegularText>
        </Stack>
      </AboutPaper>
  );
}

function MethodologyPaper() {
  return (
      <AboutPaper>
        <Stack spacing={0.5}>
          <Stack direction={"row"} spacing={0.5}>
            <MenuBook></MenuBook>
            <MediumHeader>Methodology</MediumHeader>
          </Stack>
          The V1 model internal workings.
        </Stack>

        <LargeHeader>V1 model</LargeHeader>
        <RegularText>
          Given a budget, the model first selects the most valuable picks for every match using the KL-divergence.
          It then proceeds to rank all the matches by uncertainty, and applies as many half-guards as the budget
          allows to most uncertain matches. While this implementation is a V1 draft, the intuition is that this
          two-phase selection tries to combine value with risk-averse measures.
        </RegularText>

        <AboutPaper>
          <MediumHeader>Mathematical Formulas</MediumHeader>
          <Stack direction={"row"} spacing={1}>
            <AboutPaper>
              <MediumHeader>KL-based scoring against public picks</MediumHeader>
              <RegularText>Base pick scoring uses a per-outcome KL contribution. The model picks the outcome with the highest score
              .</RegularText>
              <RegularText>score_i = p_i * log(p_i / q_i) where p_i is the model’s internal probability and q_i is the public probability for that outcome.</RegularText>
            </AboutPaper>
            <AboutPaper>
              <MediumHeader>uncertainty ranking for half-guards</MediumHeader>
              <RegularText>Matches with no dominant outcome are treated as more uncertain and get half-guards first.</RegularText>
              <RegularText>Uncertainty = 1 - max(home, draw, away)</RegularText>
            </AboutPaper>
            <AboutPaper>
              <MediumHeader>Equal weight ensemble averaging</MediumHeader>
              <RegularText>Internal probabilities are built as an equal-weight ensemble average of market probabilities and all provider probabilities:  </RegularText>
              <RegularText>p = (market + sum(providers)) / (n + 1)</RegularText>
            </AboutPaper>
          </Stack>

        </AboutPaper>
      </AboutPaper>
  );
}

export function AboutPage() {
  return (
    <Page maxWidth={"lg"}>
      <Stack padding={6} spacing={4}>

        <HeroPaper></HeroPaper>

        <MethodologyPaper></MethodologyPaper>

      </Stack>
    </Page>
  );
}
