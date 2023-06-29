import { formatDistance } from "date-fns";
import { usePeriodicRefresh } from "./hooks/usePeriodicRefresh";

/**
 * Return the
 * @param lastUpdated
 * @returns
 */
export const LastUpdated: React.FC<{ lastUpdated: string }> = ({
  lastUpdated,
}) => {
  usePeriodicRefresh(10000);
  return (
    <>{`Updated ${formatDistance(new Date(), new Date(lastUpdated), {
      includeSeconds: true,
    })} ago`}</>
  );
};
