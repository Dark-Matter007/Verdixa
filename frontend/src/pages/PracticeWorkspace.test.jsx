import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";
import { LAYOUT_KEY } from "../components/solver/useWorkspaceLayout";
import ThemeProvider from "../components/ThemeProvider";
import { I18nProvider } from "../i18n";

vi.mock("@monaco-editor/react", () => ({ default: ({ value, onChange, theme }) => <textarea aria-label="Code editor" data-editor-theme={theme} value={value} onChange={(event) => onChange(event.target.value)}/> }));
vi.mock("../services/api", () => ({ default: Object.assign(vi.fn(), { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() }) }));
import api from "../services/api";
import ProblemSolver from "./ProblemSolver";

const problem = { id: 2, title: "Bracket Balance Check", description: "Check the brackets.", difficulty: "EASY", constraints: "1 <= s.length <= 1000", examples: 'Input: "()[]{}"\nOutput: true', tags: "stack,strings", executionMode: "FUNCTION", functionSignature: { functionName: "isBalanced", returnType: "boolean", parameters: [{ name: "s", type: "String" }] } };
let responses;
const open = async () => {
  const view = render(<MemoryRouter initialEntries={["/problems/2"]}><ThemeProvider><Routes><Route path="/problems/:id" element={<ProblemSolver/>}/><Route path="/problems" element={<p>Problem library</p>}/></Routes></ThemeProvider></MemoryRouter>);
  await screen.findByText(problem.title);
  return view;
};
beforeEach(() => {
  localStorage.clear(); localStorage.setItem("algosphere_token", "test");
  vi.clearAllMocks();
  responses = {
    "/problems/2": problem,
    "/problems/2/navigation": { previous: { id: 1 }, next: { id: 3 } },
    "/problems/2/hints": { total: 1, unlocked: 0, attempts: 3, hints: [{ id: 8, available: true, revealed: false, penaltyPoints: 10 }] },
    "/problems/2/editorial": { published: true, status: "AVAILABLE" },
    "/problems/2/note": { content: "Remember the empty stack." },
    "/bookmarks/2": false,
    "/users/me": { id: 1 },
    "/lists": [{ id: 4, name: "Practice later" }],
    "/submissions/user/1/problem/2": [{ id: 12, status: "WRONG_ANSWER", language: "java", passedTestCases: 3, totalTestCases: 5, submittedAt: "2026-09-18T10:00:00Z" }],
  };
  api.get.mockImplementation(async (url) => ({ data: responses[url] ?? (url.match(/^\/problems\/\d+$/) ? { ...problem, id: Number(url.split("/").at(-1)), title: `Problem ${url.split("/").at(-1)}` } : []) }));
  api.mockResolvedValue({ data: {} }); api.put.mockResolvedValue({ data: {} }); api.delete.mockResolvedValue({ data: {} });
  api.post.mockResolvedValue({ data: { status: "ACCEPTED", output: "true", executionTimeMs: 7 } });
});

test("one global language selector, one theme/profile, separate custom compiler picker", async () => {
  const { container } = await open();
  expect(container.querySelectorAll(".vx-language-trigger")).toHaveLength(1);
  expect(container.querySelectorAll(".vx-theme-toggle")).toHaveLength(1);
  expect(container.querySelectorAll(".vx-account-trigger")).toHaveLength(1);
  expect(container.querySelectorAll("select")).toHaveLength(0);
  const menu = screen.getByRole("button", { name: "Programming language" });
  fireEvent.keyDown(menu, { key: "ArrowDown" });
  fireEvent.keyDown(menu, { key: "End" });
  fireEvent.keyDown(menu, { key: "Enter" });
  expect(menu).toHaveTextContent("Python");
  expect(screen.getByLabelText("Code editor").value).toContain("def is_balanced");
  fireEvent.click(menu); fireEvent.keyDown(menu, { key: "Escape" });
  expect(screen.queryByRole("listbox", { name: "Programming language" })).not.toBeInTheDocument();
});

test.each([["Next problem", "3"], ["Previous problem", "1"]])("%s navigates to the real adjacent id", async (label, id) => {
  await open(); fireEvent.click(screen.getByRole("button", { name: label }));
  expect(await screen.findByText(`Problem ${id}`)).toBeInTheDocument(); expect(api.get).toHaveBeenCalledWith(`/problems/${id}`);
});

test("bookmark and list use the existing endpoints", async () => {
  await open(); fireEvent.click(screen.getByRole("button", { name: "Bookmark problem" }));
  expect(await screen.findByRole("button", { name: "Remove bookmark" })).toHaveAttribute("aria-pressed", "true");
  expect(api).toHaveBeenCalledWith({ method: "post", url: "/bookmarks/2" });
  fireEvent.click(screen.getByRole("button", { name: "Remove bookmark" }));
  await waitFor(() => expect(api).toHaveBeenCalledWith({ method: "delete", url: "/bookmarks/2" }));
  fireEvent.click(screen.getByRole("button", { name: "Add problem to list" })); fireEvent.click(screen.getByRole("option", { name: "Practice later" }));
  expect(api.post).toHaveBeenCalledWith("/lists/4/problems/2");
});

test("description protects examples and copies the exact original value", async () => {
  const writeText = vi.fn().mockResolvedValue(); Object.defineProperty(navigator, "clipboard", { configurable: true, value: { writeText } });
  await open(); expect(screen.getByText("Check the brackets.")).toBeInTheDocument();
  expect(screen.getByText(/Input:.*\(\)/)).toHaveAttribute("data-i18n-ignore");
  fireEvent.click(screen.getByRole("button", { name: "Copy example" }));
  expect(writeText).toHaveBeenCalledWith(problem.examples); expect(await screen.findByText("Copied")).toBeInTheDocument();
});

test("hint penalty confirmation does not reveal before confirmation", async () => {
  await open(); fireEvent.click(screen.getByRole("tab", { name: /Hints/ })); fireEvent.click(screen.getByRole("button", { name: "Reveal hint" }));
  const dialog = screen.getByRole("dialog", { name: "Confirm hint reveal" });
  expect(api.post).not.toHaveBeenCalled(); fireEvent.click(within(dialog).getByText("Cancel"));
  expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Reveal hint" })); fireEvent.click(within(screen.getByRole("dialog")).getByText("Reveal hint"));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith("/problems/2/hints/8/reveal"));
});

test("editorial preserves reveal endpoint and technical article", async () => {
  api.post.mockResolvedValue({ data: { published: true, status: "REVEALED", editorial: { title: "Use a stack", approach: "Track open brackets.", timeComplexity: "O(n)", spaceComplexity: "O(n)", javaSolution: "return true;" } } });
  await open(); fireEvent.click(screen.getByRole("tab", { name: "Editorial" })); fireEvent.click(screen.getByText("Reveal Editorial"));
  expect(await screen.findByText("Track open brackets.")).toBeInTheDocument(); expect(api.post).toHaveBeenCalledWith("/problems/2/editorial/reveal");
});

test("notes stay explicitly saved and deleted using existing APIs", async () => {
  await open(); fireEvent.click(screen.getByRole("tab", { name: "My Notes" }));
  expect(screen.getByLabelText("Private note")).toHaveValue("Remember the empty stack.");
  fireEvent.change(screen.getByLabelText("Private note"), { target: { value: "A new approach" } }); expect(api.put).not.toHaveBeenCalled();
  fireEvent.click(screen.getByText("Save Note")); expect(await screen.findByText("Saved privately to your account.")).toBeInTheDocument();
  expect(api.put).toHaveBeenCalledWith("/problems/2/note", { content: "A new approach" });
  fireEvent.click(screen.getByText("Delete Note")); await waitFor(() => expect(screen.getByLabelText("Private note")).toHaveValue(""));
  expect(api.delete).toHaveBeenCalledWith("/problems/2/note");
});

test("language drafts survive switching and Reset respects confirmation", async () => {
  await open(); const editor = screen.getByLabelText("Code editor");
  fireEvent.change(editor, { target: { value: "java draft" } });
  fireEvent.click(screen.getByRole("button", { name: "Programming language" })); fireEvent.click(screen.getByRole("option", { name: "Python" }));
  fireEvent.change(editor, { target: { value: "python draft" } });
  fireEvent.click(screen.getByRole("button", { name: "Programming language" })); fireEvent.click(screen.getByRole("option", { name: "Java" }));
  expect(editor).toHaveValue("java draft"); const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
  fireEvent.click(screen.getByLabelText("Reset code")); expect(editor).toHaveValue("java draft");
  confirm.mockReturnValue(true); fireEvent.click(screen.getByLabelText("Reset code")); expect(editor.value).toContain("public boolean isBalanced"); confirm.mockRestore();
});

test("custom tabs add, run one, run all and clear ordered JSON values", async () => {
  await open(); fireEvent.change(screen.getByPlaceholderText('"hello"'), { target: { value: '"()"' } });
  fireEvent.click(screen.getByLabelText("Add Custom Input")); expect(screen.getByRole("tab", { name: "Custom 2" })).toHaveAttribute("aria-selected", "true");
  fireEvent.change(screen.getByPlaceholderText('"hello"'), { target: { value: '"[]"' } });
  fireEvent.change(screen.getByPlaceholderText("true"), { target: { value: "true" } });
  fireEvent.click(screen.getByText("Run Input")); await screen.findByText("ACCEPTED");
  expect(api.post).toHaveBeenLastCalledWith("/execution-test/run", expect.objectContaining({ testCases: [{ arguments: ["[]"], expected: true }] }));
  fireEvent.click(screen.getByRole("tab", { name: "Test Cases" })); fireEvent.click(screen.getByText("Run All Inputs")); await screen.findByText("ACCEPTED");
  expect(api.post).toHaveBeenLastCalledWith("/execution-test/run", expect.objectContaining({ testCases: [{ arguments: ["()"] }, { arguments: ["[]"], expected: true }] }));
  fireEvent.click(screen.getByRole("tab", { name: "Test Cases" })); fireEvent.click(screen.getByText("Clear"));
  expect(screen.queryByRole("tab", { name: "Custom 2" })).not.toBeInTheDocument(); expect(screen.getByPlaceholderText('"hello"')).toHaveValue("");
});

test("invalid JSON is reported without sending an execution request", async () => {
  await open(); fireEvent.change(screen.getByPlaceholderText('"hello"'), { target: { value: "not JSON" } }); fireEvent.click(screen.getByText("Run Code"));
  expect(await screen.findByRole("alert")).toHaveTextContent("valid JSON"); expect(api.post).not.toHaveBeenCalled();
});

test("STDIN Run sends literal input without function test cases", async () => {
  responses["/problems/2"] = { ...problem, executionMode: "STDIN", functionSignature: null };
  await open(); fireEvent.change(screen.getByPlaceholderText("Example: hello"), { target: { value: "hello\n42" } });
  fireEvent.click(screen.getByText("Run Code")); await screen.findByText("ACCEPTED");
  const request = api.post.mock.calls[0][1]; expect(request.input).toBe("hello\n42"); expect(request).not.toHaveProperty("testCases");
});

test("an in-flight execution disables actions and ignores repeat shortcuts", async () => {
  let finish; api.post.mockImplementation(() => new Promise((resolve) => { finish = resolve; }));
  await open(); fireEvent.click(screen.getByText("Submit")); expect(screen.getByRole("button", { name: "Submitting…" })).toBeDisabled();
  fireEvent.keyDown(screen.getByLabelText("Code editor"), { key: "Enter", ctrlKey: true, shiftKey: true });
  expect(api.post).toHaveBeenCalledTimes(1); finish({ data: { status: "ACCEPTED" } }); await screen.findByText("ACCEPTED");
});

test("custom result shows only returned case data, including zero runtime", async () => {
  api.post.mockResolvedValue({ data: { testCases: [{ caseNumber: 1, passed: false, actual: false, expected: true, runtimeMs: 0 }, { caseNumber: 2, status: "RUNTIME_ERROR", message: "Execution failed" }] } });
  await open(); fireEvent.change(screen.getByPlaceholderText('"hello"'), { target: { value: '"()"' } }); fireEvent.click(screen.getByText("Run Input"));
  expect(await screen.findByText("Case 1 — FAILED")).toBeInTheDocument(); expect(screen.getByText("0 ms")).toBeInTheDocument();
  expect(screen.getByText("Execution failed")).toBeInTheDocument(); expect(screen.getByText("Case 2 — RUNTIME_ERROR")).toBeInTheDocument();
});

test.each(["broken JSON", JSON.stringify({ split: 999, consoleHeight: -5, consoleCollapsed: "true" })])("stored layout is validated: %s", async (stored) => {
  localStorage.setItem(LAYOUT_KEY, stored); await open();
  const saved = JSON.parse(localStorage.getItem(LAYOUT_KEY)); expect(saved.split).toBeLessThanOrEqual(62); expect(saved.consoleHeight).toBeGreaterThanOrEqual(160); expect(saved.consoleCollapsed).toBe(false);
});

test("keyboard Run and Submit retain their distinct request semantics", async () => {
  await open(); fireEvent.change(screen.getByPlaceholderText('"hello"'), { target: { value: '"()"' } });
  fireEvent.keyDown(screen.getByLabelText("Code editor"), { key: "Enter", ctrlKey: true }); await screen.findByText("ACCEPTED");
  expect(api.post).toHaveBeenLastCalledWith("/execution-test/run", expect.objectContaining({ problemId: 2, language: "java", testCases: [{ arguments: ["()"] }] }));
  fireEvent.keyDown(screen.getByLabelText("Code editor"), { key: "Enter", metaKey: true, shiftKey: true });
  await waitFor(() => expect(api.post).toHaveBeenLastCalledWith("/submissions?problemId=2&language=java", expect.any(String), { headers: { "Content-Type": "text/plain" } }));
});

test("result omits unavailable runtime and does not expose hidden test details", async () => {
  api.post.mockResolvedValue({ data: { status: "WRONG_ANSWER", output: "", testCaseResults: [{ hidden: true, testNumber: 1, message: "Hidden test failed", arguments: ["SECRET"], expected: "SECRET", actual: "SECRET" }] } });
  await open(); fireEvent.click(screen.getByText("Submit")); await screen.findByText("WRONG ANSWER");
  const panel = screen.getByRole("tabpanel", { name: "Result" }); expect(within(panel).queryByText(/Runtime/)).not.toBeInTheDocument();
  expect(within(panel).queryByText(/SECRET/)).not.toBeInTheDocument(); expect(within(panel).getByText("Hidden test failed")).toBeInTheDocument();
});

test("history is only visible under Submissions and uses returned rows", async () => {
  await open(); expect(screen.queryByRole("table")).not.toBeInTheDocument(); fireEvent.click(screen.getByRole("tab", { name: "Submissions" }));
  expect(screen.getByRole("table")).toHaveTextContent("WRONG ANSWER"); expect(screen.getByRole("table")).toHaveTextContent("3 / 5");
  expect(screen.queryByRole("columnheader", { name: "Runtime" })).not.toBeInTheDocument();
});

test("split, console and problem collapse persist; Reset Layout restores defaults", async () => {
  const { unmount } = await open();
  fireEvent.keyDown(screen.getByRole("separator", { name: "Resize problem pane" }), { key: "ArrowRight" });
  fireEvent.keyDown(screen.getByRole("separator", { name: "Resize execution console" }), { key: "ArrowUp" });
  fireEvent.click(screen.getByLabelText("Collapse console")); fireEvent.click(screen.getByLabelText("Collapse problem pane"));
  expect(JSON.parse(localStorage.getItem(LAYOUT_KEY))).toEqual({ split: 45, consoleHeight: 258, consoleCollapsed: true, problemCollapsed: true });
  unmount(); await open(); expect(screen.getByLabelText("Restore problem pane")).toBeInTheDocument(); expect(screen.getByLabelText("Expand console")).toBeInTheDocument();
  fireEvent.click(screen.getByLabelText("Reset layout")); expect(screen.getByLabelText("Collapse console")).toBeInTheDocument(); expect(screen.getByRole("separator", { name: "Resize problem pane" })).toHaveAttribute("aria-valuenow", "43");
});

test("focus exits with Escape and preserves the user's panel choices", async () => {
  const { container } = await open(); fireEvent.click(screen.getByLabelText("Focus editor")); expect(container.querySelector(".practice-workspace")).toHaveClass("sw-focus");
  expect(screen.getByLabelText("Expand console")).toBeInTheDocument(); fireEvent.keyDown(window, { key: "Escape" });
  expect(container.querySelector(".practice-workspace")).not.toHaveClass("sw-focus"); expect(screen.getByLabelText("Collapse console")).toBeInTheDocument();
});

test("mobile workspace navigation and light/dark theme are explicit states", async () => {
  const { container } = await open(); const tabs = screen.getByRole("tablist", { name: "Mobile workspace" });
  fireEvent.click(within(tabs).getByText("Code")); expect(container.querySelector(".practice-workspace")).toHaveAttribute("data-mobile-view", "code");
  fireEvent.click(within(tabs).getByText("Tests")); expect(container.querySelector(".practice-workspace")).toHaveAttribute("data-mobile-view", "tests");
  fireEvent.click(within(tabs).getByText("Problem")); expect(container.querySelector(".practice-workspace")).toHaveAttribute("data-mobile-view", "problem");
  expect(screen.getByLabelText("Code editor")).toHaveAttribute("data-editor-theme", "verdixa-light"); fireEvent.click(screen.getByLabelText("Switch to dark theme"));
  expect(screen.getByLabelText("Code editor")).toHaveAttribute("data-editor-theme", "verdixa-dark");
});

test("translation excludes literal examples, custom values and editor source", async () => {
  const { unmount } = await open(); unmount();
  localStorage.setItem("verdixa_language", "hi");
  api.post.mockImplementation(async (url, body) => ({ data: { translations: body?.texts?.map((text) => `translated ${text}`) || [] } }));
  render(<MemoryRouter initialEntries={["/problems/2"]}><I18nProvider><Routes><Route path="/problems/:id" element={<ProblemSolver/>}/></Routes></I18nProvider></MemoryRouter>);
  await screen.findByLabelText("Code editor");
  await waitFor(() => expect(api.post.mock.calls.some(([url]) => url === "/i18n/translate-batch")).toBe(true));
  const translated = api.post.mock.calls.filter(([url]) => url === "/i18n/translate-batch").flatMap(([, body]) => body.texts);
  expect(translated).not.toContain(problem.examples); expect(translated).not.toContain(screen.getByLabelText("Code editor").value);
});
