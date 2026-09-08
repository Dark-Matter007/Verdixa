import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";

vi.mock("../services/api", () => ({ default: { get: vi.fn(), post: vi.fn() } }));

import api from "../services/api";
import AdminContests from "./AdminContests";

beforeEach(() => {
  vi.clearAllMocks();
  api.get.mockImplementation((url) => Promise.resolve({ data: url === "/contests" ? [] : { content: [
    { id: 11, title: "Two Sum", difficulty: "EASY" },
    { id: 12, title: "Three Sum", difficulty: "MEDIUM" },
  ] } }));
});

test("attaches selected contest problems in display order", async () => {
  api.post
    .mockResolvedValueOnce({ data: { id: 7 } })
    .mockResolvedValue({ data: {} });
  const { container } = render(<MemoryRouter><AdminContests /></MemoryRouter>);

  await screen.findByText("Two Sum");
  fireEvent.change(screen.getByLabelText("Title"), { target: { value: "Weekly" } });
  fireEvent.change(screen.getByLabelText("URL slug"), { target: { value: "weekly" } });
  fireEvent.change(screen.getByLabelText("Start time"), { target: { value: "2030-01-01T10:00" } });
  fireEvent.change(screen.getByLabelText("End time"), { target: { value: "2030-01-01T12:00" } });
  fireEvent.click(screen.getByRole("checkbox", { name: /two sum/i }));
  fireEvent.click(screen.getByRole("checkbox", { name: /three sum/i }));
  fireEvent.submit(container.querySelector("form"));

  await waitFor(() => expect(api.post).toHaveBeenCalledTimes(3));
  expect(api.post.mock.calls).toEqual([
    ["/contests", expect.objectContaining({ title: "Weekly" })],
    ["/contests/7/problems", { problemId: 11, points: 100 }],
    ["/contests/7/problems", { problemId: 12, points: 100 }],
  ]);
});
