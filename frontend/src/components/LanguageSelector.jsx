import { useEffect, useId, useMemo, useRef, useState } from "react";
import { Check, ChevronDown, Languages, Search } from "lucide-react";
import { languageOptions } from "../i18n/languages";
import { useI18n } from "../i18n";

export default function LanguageSelector() {
  const { language, setLanguage } = useI18n();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [activeIndex, setActiveIndex] = useState(0);
  const root = useRef(null);
  const input = useRef(null);
  const optionRefs = useRef([]);
  const listboxId = useId();
  const options = useMemo(() => languageOptions(), []);
  const filtered = useMemo(() => options.filter((item) => `${item.native} ${item.english} ${item.code}`.toLowerCase().includes(query.toLowerCase())).slice(0, 100), [options, query]);
  const current = options.find((item) => item.code === language) || options.find((item) => item.code === "en");
  const currentMatch = filtered.find((item) => item.code === current.code);
  const visible = currentMatch ? [currentMatch, ...filtered.filter((item) => item.code !== currentMatch.code)] : filtered;

  const close = (restoreFocus = false) => {
    setOpen(false); setQuery("");
    if (restoreFocus) root.current?.querySelector("button")?.focus();
  };
  const openPopover = () => { setActiveIndex(Math.max(0, visible.findIndex((item) => item.code === current.code))); setOpen(true); };
  const select = (code) => { setLanguage(code); close(true); };

  useEffect(() => {
    if (!open) return undefined;
    input.current?.focus();
    const onPointerDown = (event) => { if (!root.current?.contains(event.target)) close(); };
    window.addEventListener("pointerdown", onPointerDown);
    return () => window.removeEventListener("pointerdown", onPointerDown);
  }, [open]);

  useEffect(() => { optionRefs.current[activeIndex]?.scrollIntoView?.({ block: "nearest" }); }, [activeIndex]);

  const onKeyDown = (event) => {
    if (!open && ["ArrowDown", "ArrowUp", "Enter", " "].includes(event.key)) { event.preventDefault(); openPopover(); return; }
    if (!open) return;
    if (event.key === "Escape") { event.preventDefault(); close(true); }
    if (event.key === "ArrowDown") { event.preventDefault(); setActiveIndex((index) => Math.min(index + 1, visible.length - 1)); }
    if (event.key === "ArrowUp") { event.preventDefault(); setActiveIndex((index) => Math.max(index - 1, 0)); }
    if (event.key === "Home") { event.preventDefault(); setActiveIndex(0); }
    if (event.key === "End") { event.preventDefault(); setActiveIndex(Math.max(visible.length - 1, 0)); }
    if (event.key === "Enter" && visible[activeIndex]) { event.preventDefault(); select(visible[activeIndex].code); }
  };

  return <div className="vx-language" ref={root} onKeyDown={onKeyDown}>
    <button className="vx-language-trigger" type="button" aria-haspopup="listbox" aria-controls={listboxId} aria-expanded={open} onClick={() => open ? close() : openPopover()}>
      <Languages size={16} aria-hidden="true" /><span>{current.english}</span><ChevronDown className={open ? "is-open" : ""} size={14} aria-hidden="true" />
    </button>
    {open && <section className="vx-language-popover" aria-label="Language selection">
      <header className="vx-language-popover-head"><strong>Language</strong><p>Choose your display language</p></header>
      <label className="vx-language-search"><Search size={15} aria-hidden="true" /><input ref={input} value={query} onChange={(event) => { setQuery(event.target.value); setActiveIndex(0); }} placeholder="Search languages…" aria-label="Search languages" autoComplete="off" /></label>
      <div id={listboxId} className="vx-language-options" role="listbox" tabIndex={-1} aria-label="Languages" aria-activedescendant={visible[activeIndex] ? `${listboxId}-${visible[activeIndex].code}` : undefined}>
        {currentMatch && <p className="vx-language-group">Current</p>}
        {visible.map((item, index) => <div key={item.code}>
          {index === (currentMatch ? 1 : 0) && <p className="vx-language-group">Languages</p>}
          <button ref={(element) => { optionRefs.current[index] = element; }} id={`${listboxId}-${item.code}`} type="button" role="option" aria-selected={item.code === language} className={index === activeIndex ? "is-active" : ""} onMouseMove={() => setActiveIndex(index)} onClick={() => select(item.code)} aria-label={`${item.native} ${item.english}`}>
            <span className="vx-language-row-copy"><strong>{item.native}</strong>{item.native !== item.english && <small>{item.english}</small>}</span>
            {item.code === language && <span className="vx-language-check"><Check size={14} aria-hidden="true" /></span>}
          </button>
        </div>)}
        {!visible.length && <p className="vx-language-empty">No languages found</p>}
      </div>
    </section>}
  </div>;
}
