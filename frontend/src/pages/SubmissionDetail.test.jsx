import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { vi } from "vitest";
vi.mock("@monaco-editor/react", () => ({ default: ({ value }) => <pre data-testid="source">{value}</pre> }));
vi.mock("../services/api", () => ({ default:{ get:vi.fn(() => Promise.resolve({data:{id:1,problemId:2,problemTitle:"Reverse String",status:"ACCEPTED",language:"java",executionTimeMs:3,passedTestCases:6,totalTestCases:6,submittedAt:"2026-01-01T00:00:00",sourceCode:"public String reverseString(String s){return s;}"}})) } }));
import SubmissionDetail from "./SubmissionDetail";
test("renders submitted source and safe verdict metadata", async () => { render(<MemoryRouter initialEntries={["/submissions/1"]}><Routes><Route path="/submissions/:id" element={<SubmissionDetail/>}/></Routes></MemoryRouter>); expect(await screen.findByText("Reverse String")).toBeInTheDocument(); expect(screen.getByText("ACCEPTED")).toBeInTheDocument(); expect(screen.getByTestId("source")).toHaveTextContent("reverseString"); });
