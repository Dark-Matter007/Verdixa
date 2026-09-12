import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";
vi.mock("../services/api", () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn() } }));
vi.mock("../components/VerdixaAssistant", () => ({ default: () => null }));
import api from "../services/api";
import ProblemForm from "./ProblemForm";

const problem = { id: 7, title: "Function problem", description: "description", difficulty: "EASY", tags: "arrays", active: false, executionMode: "FUNCTION", starterCode: "{}", functionSignature: { functionName: "solve", returnType: "String", parameters: [{ name: "value", type: "String", order: 0 }] } };
const renderEdit = () => render(<MemoryRouter initialEntries={["/admin/problems/edit/7"]}><Routes><Route path="/admin/problems/edit/:id" element={<ProblemForm/>}/><Route path="/admin/problems" element={<div>Problems index</div>}/></Routes></MemoryRouter>);
const renderCreate = () => render(<MemoryRouter initialEntries={["/admin/problems/new"]}><Routes><Route path="/admin/problems/new" element={<ProblemForm/>}/><Route path="/admin/problems/edit/:id" element={<ProblemForm/>}/></Routes></MemoryRouter>);

beforeEach(() => {
  vi.clearAllMocks(); localStorage.setItem("algosphere_role", "ADMIN");
  api.get.mockImplementation((url) => {
    if (url === "/problems/7") return Promise.resolve({ data: problem });
    if (url.endsWith("/testcases/all") || url.endsWith("/hints/admin")) return Promise.resolve({ data: [] });
    if (url.endsWith("/editorial")) return Promise.reject({ response: { status: 404 } });
    return Promise.reject(new Error(`not configured: ${url}`));
  });
});

test("admin update submits FUNCTION parameters and reports success inline", async () => {
  api.put.mockResolvedValue({ data: problem }); renderEdit();
  expect(await screen.findByDisplayValue("Function problem")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Update Problem" }));
  await waitFor(() => expect(api.put).toHaveBeenCalledWith("/problems/7", expect.objectContaining({ executionMode: "FUNCTION", functionSignature: expect.objectContaining({ parameters: [expect.objectContaining({ name: "value", parameterOrder: 0 })] }) })));
  expect(await screen.findByRole("status")).toHaveTextContent("updated successfully");
});

test("admin update displays backend failure without alert", async () => {
  api.put.mockRejectedValue({ response: { status: 500, data: { message: "Update failed safely." } } }); renderEdit();
  await screen.findByDisplayValue("Function problem"); fireEvent.click(screen.getByRole("button", { name: "Update Problem" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("Update failed safely.");
});

test("create form renders every authoring section and validates required fields", () => {
  renderCreate();
  ["Basic information", "Problem statement", "Execution configuration", "Test cases", "Editorial & guidance"].forEach((name) => expect(screen.getByRole("heading", { name })).toBeInTheDocument());
  fireEvent.click(screen.getByRole("button", { name: "Create Problem" }));
  expect(screen.getByRole("alert")).toHaveTextContent("Enter a problem title");
  expect(api.post).not.toHaveBeenCalled();
});

test("test case drafts can be added and removed", () => {
  renderCreate(); fireEvent.click(screen.getByRole("button", { name: "Add test case" }));
  expect(screen.getByText("Visible sample")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name: "Remove test case 1" }));
  expect(screen.getByText("No test cases yet")).toBeInTheDocument();
});
