import UserAccountMenu from "./UserAccountMenu";

/** Shared title-and-utility alignment for authenticated user workspaces. */
export default function WorkspacePageHeader({ eyebrow, title, description, actions, children }) {
  return <header className="vx-page-shell-header">
    <div className="vx-page-shell-copy">
      {eyebrow && <p className="vx-eyebrow">{eyebrow}</p>}
      <h1>{title}</h1>
      {description && <p>{description}</p>}
      {children}
    </div>
    <div className="vx-page-shell-actions">
      <UserAccountMenu />
      {actions && <div className="vx-page-shell-action-row">{actions}</div>}
    </div>
  </header>;
}
