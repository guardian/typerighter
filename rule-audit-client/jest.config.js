module.exports = {
  moduleFileExtensions: ["ts", "tsx", "js"],
  modulePaths: ["<rootDir>/src"],
  testMatch: ["**/?(*.)+(spec|test).[jt]s?(x)"],
  transform: {
    "^.+\\.(ts|tsx|js)$": "ts-jest",
  },
};
