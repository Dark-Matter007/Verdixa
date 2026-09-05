export default function PageHeader({ eyebrow, title, description, actions }) {
  return <header className="page-heading">
    <div>
      {eyebrow && <p className="section-eyebrow">{eyebrow}</p>}
      <h1>{title}</h1>
      {description && <p>{description}</p>}
    </div>
    {actions && <div className="page-header-actions">{actions}</div>}
  </header>;
}
