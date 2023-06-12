import {startCase} from "lodash";

export const maybeGetNameFromEmail = (email: string) => {
  // Include the first name, and last name, separated by a dot, but nothing else (e.g. a trailing `.casual`).
  const groups = email.match(/(?<firstName>.*?)\.(?<lastName>.*?(\.|\@))/)?.groups;
  if (!groups) {
    return email;
  }
  const { firstName, lastName } = groups;

  return startCase(`${firstName} ${lastName}`);
}
