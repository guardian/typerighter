import { format } from "date-fns";

export const formatTimestampTZ = (timestamptz: string, formatStr: string = "do MMMM yyyy HH:mm:ss") => format(new Date(timestamptz.replace(/\[.*\]/, "")), formatStr)
