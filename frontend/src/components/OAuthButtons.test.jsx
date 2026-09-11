import { render, screen } from "@testing-library/react";
import { expect, test } from "vitest";
import OAuthButtons from "./OAuthButtons";

test("renders accessible Google and GitHub continuation buttons", () => {
  render(<OAuthButtons />);
  expect(screen.getByRole("button", { name: "Continue with Google" })).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Continue with GitHub" })).toBeInTheDocument();
});
