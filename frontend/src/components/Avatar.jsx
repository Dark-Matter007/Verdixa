export default function Avatar({ name = "User", className = "" }) {
  const initials = name.trim().split(/\s+/).map((part) => part[0]).join("").slice(0, 2).toUpperCase();
  return <span className={`vx-avatar ${className}`.trim()} aria-hidden="true">{initials || "U"}</span>;
}
