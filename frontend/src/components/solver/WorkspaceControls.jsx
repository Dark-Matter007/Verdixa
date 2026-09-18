import { useEffect, useId, useRef, useState } from "react";
import { Check, ChevronDown } from "lucide-react";

/** Small listbox shared by the compiler and personal-list pickers. */
export function WorkspaceMenu({ label, value, options, onChange, disabled = false }) {
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const root = useRef(null);
  const trigger = useRef(null);
  const id = useId();
  const close = (restore = false) => { setOpen(false); if (restore) trigger.current?.focus(); };
  const show = () => { setActive(Math.max(0, options.findIndex((item) => item.value === value))); setOpen(true); };
  useEffect(() => {
    if (!open) return;
    const outside = (event) => { if (!root.current?.contains(event.target)) setOpen(false); };
    window.addEventListener("pointerdown", outside);
    return () => window.removeEventListener("pointerdown", outside);
  }, [open]);
  useEffect(() => { if (open) root.current?.querySelectorAll('[role="option"]')[active]?.focus(); }, [open, active]);
  const choose = (item) => { onChange(item.value); close(true); };
  const keyDown = (event) => {
    if (event.key === "Escape" && open) { event.stopPropagation(); event.preventDefault(); close(true); }
    if (event.key === "Tab") close();
    if (["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) {
      event.preventDefault();
      if (!open) { show(); return; }
      setActive((index) => event.key === "Home" ? 0 : event.key === "End" ? options.length - 1 : (index + (event.key === "ArrowDown" ? 1 : -1) + options.length) % options.length);
    }
    if (open && ["Enter", " "].includes(event.key)) { event.preventDefault(); choose(options[active]); }
  };
  return <div className="sw-menu" ref={root} onKeyDown={keyDown} onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) close(); }}>
    <button ref={trigger} type="button" aria-label={label} aria-haspopup="listbox" aria-expanded={open} aria-controls={id} onClick={() => open ? close() : show()} disabled={disabled}>
      <span>{options.find((item) => item.value === value)?.label || label}</span><ChevronDown size={14}/>
    </button>
    {open && <div className="sw-menu-options" role="listbox" id={id} aria-label={label}>
      {options.map((item, index) => <button key={item.value} id={`${id}-${index}`} role="option" tabIndex={-1} aria-selected={item.value === value} className={active === index ? "is-active" : ""} onPointerMove={() => setActive(index)} onClick={() => choose(item)}><span>{item.label}</span>{item.value === value && <Check size={14}/>}</button>)}
    </div>}
  </div>;
}

export function WorkspaceTabs({ label, items, value, onChange, idPrefix }) {
  const select = (event) => {
    const keys = ["ArrowLeft", "ArrowRight", "Home", "End"];
    if (!keys.includes(event.key)) return;
    event.preventDefault();
    const index = items.findIndex((item) => item.value === value);
    const next = event.key === "Home" ? 0 : event.key === "End" ? items.length - 1 : (index + (event.key === "ArrowRight" ? 1 : -1) + items.length) % items.length;
    onChange(items[next].value);
    event.currentTarget.querySelectorAll('[role="tab"]')[next]?.focus();
  };
  return <div className="sw-tabs" role="tablist" aria-label={label} onKeyDown={select}>
    {items.map((item) => <button key={item.value} role="tab" id={`${idPrefix}-${item.value}`} aria-controls={`${idPrefix}-panel-${item.value}`} aria-selected={value === item.value} tabIndex={value === item.value ? 0 : -1} onClick={() => onChange(item.value)}>{item.icon}{item.label}</button>)}
  </div>;
}

export function WorkspaceSplitter({ orientation, value, min, max, onChange, measure, label, controls }) {
  const dragging = useRef(false);
  const vertical = orientation === "vertical";
  const clamp = (next) => Math.min(max, Math.max(min, next));
  const stop = () => { dragging.current = false; };
  return <div className={`sw-splitter sw-splitter-${orientation}`} role="separator" tabIndex={0} aria-label={label} aria-orientation={orientation} aria-valuemin={min} aria-valuemax={max} aria-valuenow={Math.round(value)} aria-controls={controls}
    onPointerDown={(event) => { if (event.button !== 0) return; event.preventDefault(); dragging.current = true; event.currentTarget.setPointerCapture(event.pointerId); }}
    onPointerMove={(event) => { if (dragging.current) onChange(clamp(measure(event))); }} onPointerUp={stop} onPointerCancel={stop} onLostPointerCapture={stop}
    onKeyDown={(event) => {
      const positive = vertical ? "ArrowRight" : "ArrowUp", negative = vertical ? "ArrowLeft" : "ArrowDown";
      if (![positive, negative, "Home", "End"].includes(event.key)) return;
      event.preventDefault();
      onChange(event.key === "Home" ? min : event.key === "End" ? max : clamp(value + (event.key === positive ? 1 : -1) * (vertical ? 2 : 20)));
    }}><span/></div>;
}
