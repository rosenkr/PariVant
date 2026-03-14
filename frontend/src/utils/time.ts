// Utilities for formatting the backend's LocalDateTime strings (no timezone)
// into Swedish-looking UI strings, without accidental UTC shifts.

export function parseLocalDateTime(input: string): Date | null {
  // Expected: "2026-03-15T00:29:00" (no timezone)
  // We parse manually so browsers don't treat it as UTC.
  const m = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(input);
  if (!m) return null;

  const year = Number(m[1]);
  const month = Number(m[2]) - 1;
  const day = Number(m[3]);
  const hour = Number(m[4]);
  const minute = Number(m[5]);
  const second = m[6] ? Number(m[6]) : 0;

  return new Date(year, month, day, hour, minute, second);
}

function pad2(n: number): string {
  return String(n).padStart(2, "0");
}

export function formatRoundDateTime(input?: string): string | undefined {
  if (!input) return undefined;
  const d = parseLocalDateTime(input);
  if (!d) return input;

  const yyyy = d.getFullYear();
  const mm = pad2(d.getMonth() + 1);
  const dd = pad2(d.getDate());
  const hh = pad2(d.getHours());
  const mi = pad2(d.getMinutes());

  return `${yyyy}-${mm}-${dd} - ${hh}:${mi}`;
}

export function formatTimeOnly(input?: string): string | undefined {
  if (!input) return undefined;
  const d = parseLocalDateTime(input);
  if (!d) return input;

  const hh = pad2(d.getHours());
  const mi = pad2(d.getMinutes());
  return `${hh}:${mi}`;
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