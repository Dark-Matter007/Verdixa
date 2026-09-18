import { useEffect, useState } from "react";

export const LAYOUT_KEY = "verdixa.practice.layout.v1";
const defaults = { split: 43, consoleHeight: 238, consoleCollapsed: false, problemCollapsed: false };
const bounded = (value, min, max, fallback) => typeof value === "number" && Number.isFinite(value) ? Math.min(max, Math.max(min, value)) : fallback;
function readLayout() {
  try {
    const value = JSON.parse(localStorage.getItem(LAYOUT_KEY)) || {};
    return { split: bounded(value.split, 28, 62, defaults.split), consoleHeight: bounded(value.consoleHeight, 160, 480, defaults.consoleHeight), consoleCollapsed: value.consoleCollapsed === true, problemCollapsed: value.problemCollapsed === true };
  } catch { return defaults; }
}
export default function useWorkspaceLayout() {
  const [layout, setLayout] = useState(readLayout);
  useEffect(() => { try { localStorage.setItem(LAYOUT_KEY, JSON.stringify(layout)); } catch { /* Layout is usable when storage is unavailable. */ } }, [layout]);
  return [layout, (changes) => setLayout((previous) => ({ ...previous, ...changes })), () => setLayout({ ...defaults })];
}
