import {
  ApiRequest,
  ApiResponse as _ApiResponse,
  ApiError as _ApiError,
  ApiWorkComplete as _ApiWorkComplete,
} from "./types/api";

interface EnvelopeProperties {
  // We must add these properties, as we cannot currently
  // express union types in Scala.
  status: "OK" | "ERROR";
}

type ApiResponse = _ApiResponse &
  EnvelopeProperties & {
    type: "VALIDATOR_RESPONSE";
  };
type ApiError = _ApiError &
  EnvelopeProperties & {
    type: "VALIDATOR_ERROR";
  };
type ApiWorkComplete = _ApiWorkComplete &
  EnvelopeProperties & {
    type: "VALIDATOR_WORK_COMPLETE";
  };

/**
 * Fetch matches from the Typerighter API.
 */
export const fetchMatches = async (
  url: string,
  request: ApiRequest
): Promise<(ApiResponse | ApiError) & { statusCode: number }> => {
  const response = await fetch(`${url}/check`, {
    method: "POST",
    headers: new Headers({
      "Content-Type": "application/json",
    }),
    body: JSON.stringify(request),
  });

  const statusCode = response.status;
  const responseBody = await maybeParseResponse<ApiResponse | ApiError>(
    response
  );

  if (
    response.status !== 200 ||
    responseBody?.status !== "OK" ||
    !responseBody
  ) {
    return {
      type: "VALIDATOR_ERROR",
      status: "ERROR",
      statusCode,
      message:
        (isErrorResponse(responseBody) && responseBody.message) ||
        `Server returning error code ${response.status}: ${response.statusText}`,
    };
  }

  return {
    statusCode,
    ...responseBody,
  };
};

const maybeParseResponse = async <T>(
  response: Response
): Promise<T | undefined> => {
  try {
    return await response.json();
  } catch (e) {
    return undefined;
  }
};

const isErrorResponse = (
  response: ApiResponse | ApiError | ApiWorkComplete | undefined
): response is ApiError => !!response && response.type === "VALIDATOR_ERROR";
