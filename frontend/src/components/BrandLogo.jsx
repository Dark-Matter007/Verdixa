import verdixaLogo from "../assets/verdixa-logo-transparent.png";

/** Official transparent Verdixa artwork. The compact variant shows the emblem area in navigation. */
function BrandLogo({ compact = false, className = "" }) {
  return (
    <span className={`verdixa-logo ${compact ? "verdixa-logo--compact" : ""} ${className}`.trim()}>
      <img src={verdixaLogo} alt="Verdixa" />
    </span>
  );
}

export default BrandLogo;
