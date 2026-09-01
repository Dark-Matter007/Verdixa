import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";

vi.mock("../services/api", () => ({ default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() } }));
import api from "../services/api";
import Dashboard from "./Dashboard";

const page = { content: [{ problem: { id: 2, title: "Reverse String", description: "Reverse a string.", difficulty: "EASY", tags: "String" }, status: "SOLVED", statistics: {} }], page: 0, size: 20, totalElements: 4, totalPages: 1, first: true, last: true };
const progress = { totalProblems: 4, solvedProblems: 1, currentStreak: 3, completionPercentage: 25 };

beforeEach(() => {
  localStorage.setItem("algosphere_token", "test");
  localStorage.setItem("algosphere_role", "USER");
  vi.clearAllMocks();
});

test("renders paginated library totals and progress fields", async () => {
  api.get.mockImplementation((url) => Promise.resolve({ data: url === "/problems/library" ? page : url === "/users/me/progress" ? progress : [] }));
  render(<MemoryRouter><Dashboard /></MemoryRouter>);
  expect(await screen.findByText("Reverse String")).toBeInTheDocument();
  expect(screen.getByText("Total Problems").parentElement).toHaveTextContent("4");
  expect(screen.getAllByText("Solved")[0].parentElement).toHaveTextContent("1");
  expect(screen.getByText("Current Streak").parentElement).toHaveTextContent("3 days");
  expect(screen.getByText("Completion").parentElement).toHaveTextContent("25%");
});

test("keeps core dashboard data when bookmarks fail", async () => {
  api.get.mockImplementation((url) => url === "/bookmarks" ? Promise.reject(new Error("offline")) : Promise.resolve({ data: url === "/problems/library" ? page : progress }));
  render(<MemoryRouter><Dashboard /></MemoryRouter>);
  expect(await screen.findByText("Reverse String")).toBeInTheDocument();
  expect(screen.getByText(/Unable to load bookmarks/)).toBeInTheDocument();
  expect(screen.queryByText("Unable to load dashboard data.")).not.toBeInTheDocument();
});

test("redirects on an expired session instead of showing zero-valued data", async () => {
  const expired = Object.assign(new Error("expired"), { response: { status: 401 } });
  api.get.mockRejectedValue(expired);
  render(<MemoryRouter initialEntries={["/dashboard"]}><Routes><Route path="/dashboard" element={<Dashboard />} /><Route path="/login" element={<p>Sign in again</p>} /></Routes></MemoryRouter>);
  expect(await screen.findByText("Sign in again")).toBeInTheDocument();
  expect(localStorage.getItem("algosphere_token")).toBeNull();
});
