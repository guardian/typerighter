export const errorToString = (error: unknown): string =>
  error instanceof Error ? error.message : error ? (error as object).toString() : typeof error;
