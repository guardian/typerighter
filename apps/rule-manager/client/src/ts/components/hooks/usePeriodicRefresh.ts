import { useEffect, useState } from "react";

/**
 * Force a re-render every given interval.
 */
export const usePeriodicRefresh = (periodInMs: number) => {
  const [_, setTime] = useState(Date.now());

  useEffect(() => {
    const interval = setInterval(() => setTime(Date.now()), periodInMs);
    return () => {
      clearInterval(interval);
    };
  }, []);
};
