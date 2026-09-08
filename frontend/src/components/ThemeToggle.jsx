import { useContext } from "react";
import { Moon, Sun } from "lucide-react";
import { ThemeContext } from "./ThemeProvider";

export default function ThemeToggle() {
  const { theme, toggleTheme } = useContext(ThemeContext);
  const dark = theme === "dark";
  return <button className="vx-theme-toggle" type="button" onClick={toggleTheme} aria-label={`Switch to ${dark ? "light" : "dark"} theme`} title={`Switch to ${dark ? "light" : "dark"} theme`}>
    {dark ? <Sun size={15} aria-hidden="true" /> : <Moon size={15} aria-hidden="true" />}
    <span>{dark ? "Light" : "Dark"}</span>
  </button>;
}
