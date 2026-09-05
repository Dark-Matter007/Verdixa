import { AlertCircle, Inbox } from "lucide-react";

export function LoadingState({ label = "Loading" }) {
  return <div className="vx-skeleton-state" role="status" aria-live="polite"><span className="vx-skeleton-line vx-skeleton-title" /><span className="vx-skeleton-line" /><span className="vx-skeleton-line vx-skeleton-short" /><span className="sr-only">{label}</span></div>;
}

export function EmptyState({ title, children, icon: Icon = Inbox }) {
  return <div className="empty-state"><Icon aria-hidden="true" size={34} /><h3>{title}</h3>{children && <p>{children}</p>}</div>;
}

export function ErrorState({ message, onRetry }) {
  return <div className="vx-error-state" role="alert"><AlertCircle aria-hidden="true" size={18} /><span>{message}</span>{onRetry && <button className="secondary-button" onClick={onRetry}>Try again</button>}</div>;
}
