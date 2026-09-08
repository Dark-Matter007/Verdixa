import { createContext, useCallback, useEffect, useMemo, useState } from "react";

export const ThemeContext = createContext({ theme: "light", toggleTheme: () => {} });

const applyTheme = (theme) => {
  document.documentElement.dataset.theme = theme;
  document.documentElement.style.colorScheme = theme;
  document.querySelector('meta[name="theme-color"]')?.setAttribute("content", theme === "dark" ? "#090a0c" : "#faf9f7");
};

const preferredTheme = () => String(localStorage.getItem("verdixa_theme") || "light").toLowerCase() === "dark" ? "dark" : "light";

export default function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(preferredTheme);

  useEffect(() => { applyTheme(theme); }, [theme]);

  const toggleTheme = useCallback(() => {
    const next = theme === "light" ? "dark" : "light";
    setTheme(next);
    localStorage.setItem("verdixa_theme", next);
  }, [theme]);

  const value = useMemo(() => ({ theme, toggleTheme }), [theme, toggleTheme]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
