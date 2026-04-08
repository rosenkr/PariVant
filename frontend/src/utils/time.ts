// Utilities for formatting backend ISO instant strings (for example "2026-04-08T18:45:00Z")
// into Swedish-looking UI strings.

const SWEDEN_TIME_ZONE = "Europe/Stockholm";

export function parseLocalDateTime(input: string): Date | null {
  if (!input) return null;

  const d = new Date(input);
  return Number.isNaN(d.getTime()) ? null : d;
}

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

function getSwedenParts(date: Date): {
  year: number;
  month: number;
  day: number;
  hour: number;
  minute: number;
} {
  const formatter = new Intl.DateTimeFormat("sv-SE", {
    timeZone: SWEDEN_TIME_ZONE,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  });

  const parts = formatter.formatToParts(date);

  const get = (type: Intl.DateTimeFormatPartTypes): number => {
    const value = parts.find((p) => p.type === type)?.value;
    return value ? Number(value) : 0;
  };

  return {
    year: get("year"),
    month: get("month"),
    day: get("day"),
    hour: get("hour"),
    minute: get("minute"),
  };
}

export function formatRoundDateTime(input?: string): string | undefined {
  if (!input) return undefined;

  const d = parseLocalDateTime(input);
  if (!d) return input;

  const parts = getSwedenParts(d);
  const yyyy = parts.year;
  const mm = pad2(parts.month);
  const dd = pad2(parts.day);
  const hh = pad2(parts.hour);
  const mi = pad2(parts.minute);

  return `${yyyy}-${mm}-${dd} - ${hh}:${mi}`;
}

export function formatTimeOnly(input?: string): string | undefined {
  if (!input) return undefined;

  const d = parseLocalDateTime(input);
  if (!d) return input;

  const parts = getSwedenParts(d);
  return `${pad2(parts.hour)}:${pad2(parts.minute)}`;
}

export function formatCountdownTo(startIsoLocal?: string, now = new Date()): string | undefined {
  if (!startIsoLocal) return undefined;

  const start = parseLocalDateTime(startIsoLocal);
  if (!start) return undefined;

  const ms = start.getTime() - now.getTime();
  if (ms <= 0) return "Starting now";

  const totalMinutes = Math.floor(ms / 60000);
  const days = Math.floor(totalMinutes / (60 * 24));
  const hours = Math.floor((totalMinutes % (60 * 24)) / 60);
  const minutes = totalMinutes % 60;

  const parts: string[] = [];
  if (days > 0) parts.push(`${days}d`);
  if (hours > 0 || days > 0) parts.push(`${hours}h`);
  parts.push(`${minutes}m`);

  return `Starting in ${parts.join(" ")}`;
}