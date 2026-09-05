export default function StatusPill({ value, className = "" }) {
  const normalized = String(value || "unknown").toLowerCase().replaceAll("_", "-");
  return <span className={`vx-status vx-status--${normalized} ${className}`.trim()}>{String(value || "Unknown").replaceAll("_", " ")}</span>;
}
