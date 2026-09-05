import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { expect, test } from "vitest";
import Landing from "./Landing";

test("renders a public Verdixa landing page with judge language tabs", () => {
  render(<MemoryRouter><Landing /></MemoryRouter>);
  expect(screen.getByRole("heading", { name: /Build sharper.*algorithmic instincts/i })).toBeInTheDocument();
  expect(screen.getByRole("link", { name: /Start solving/i })).toHaveAttribute("href", "/register");
  fireEvent.click(screen.getByRole("tab", { name: "Python" }));
  expect(screen.getByText(/def two_sum/)).toBeInTheDocument();
  expect(screen.getByRole("heading", { name: /Turn each attempt.*better judgment/i })).toBeInTheDocument();
});
