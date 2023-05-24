import {startCase} from "lodash";

export const maybeGetNameFromEmail = (email: string) => {
  const groups = email.match(/(?<firstName>.*)\.(?<lastName>.*)\@/)?.groups;
  if (!groups) {
    return email;
  }
  const { firstName, lastName } = groups;
  return startCase(`${firstName} ${lastName}`);
}
