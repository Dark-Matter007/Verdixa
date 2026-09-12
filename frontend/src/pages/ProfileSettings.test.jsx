import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";
vi.mock("../services/api", () => ({ default: { get: vi.fn(), post: vi.fn() } }));
vi.mock("../components/VerdixaAssistant", () => ({ default: () => null }));
import api from "../services/api";
import ProfileSettings from "./ProfileSettings";

const localProfile = { username: "ada", email: "ada@example.com", role: "USER", theme: "DARK", emailVerified: true, hasLocalPassword: true, connectedProviders: [] };
const renderPage = () => render(<MemoryRouter><ProfileSettings/></MemoryRouter>);

beforeEach(() => { vi.clearAllMocks(); localStorage.setItem("algosphere_role", "USER"); api.get.mockResolvedValue({ data: localProfile }); });

test("renders current profile without exposing internal security state", async () => {
  renderPage(); expect((await screen.findAllByText("ada@example.com"))[0]).toBeInTheDocument();
  expect(screen.getByText("Local password enabled")).toBeInTheDocument();
  expect(screen.queryByText(/auth.version|password hash|token/i)).not.toBeInTheDocument();
});

test("username request advances to OTP and successful verification requires a fresh sign in", async () => {
  const user = userEvent.setup();
  api.post.mockResolvedValueOnce({ data: { message: "Enter the 6-digit code sent to your registered email.", cooldownSeconds: 60 } }).mockResolvedValueOnce({ data: { message: "Username updated successfully. Please sign in again.", logoutRequired: true } });
  renderPage(); await screen.findAllByText("ada@example.com"); await user.click(screen.getByRole("button", { name: "Change username" })); await screen.findByRole("button", { name: "Close" });
  await user.type(await screen.findByRole("textbox", { name: /New username/ }), "ada_lovelace"); await user.click(screen.getByRole("button", { name: "Continue securely" }));
  expect(await screen.findByLabelText("Verification code")).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Resend in 60s" })).toBeDisabled();
  await user.type(screen.getByLabelText("Verification code"), "123456"); await user.click(screen.getByRole("button", { name: "Verify code" }));
  expect(await screen.findByText(/Username updated successfully/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Sign in again" })).toBeInTheDocument();
});

test("incorrect username OTP stays in the verification step and shows the backend error", async () => {
  const user = userEvent.setup();
  api.post.mockResolvedValueOnce({ data: { message: "Code sent.", cooldownSeconds: 60 } })
    .mockRejectedValueOnce({ response: { data: { message: "The verification code is incorrect." } } });
  renderPage(); await screen.findAllByText("ada@example.com"); await user.click(screen.getByRole("button", { name: "Change username" }));
  await user.type(await screen.findByRole("textbox", { name: /New username/ }), "ada_lovelace"); await user.click(screen.getByRole("button", { name: "Continue securely" }));
  await user.type(await screen.findByLabelText("Verification code"), "111111"); await user.click(screen.getByRole("button", { name: "Verify code" }));
  expect(await screen.findByRole("alert")).toHaveTextContent("The verification code is incorrect");
  expect(screen.getByLabelText("Verification code")).toBeInTheDocument();
});

test("email change presents old and new verification one at a time", async () => {
  api.post.mockResolvedValueOnce({ data: { message: "Confirm current", cooldownSeconds: 60 } }).mockResolvedValueOnce({ data: { message: "Verify new", cooldownSeconds: 60 } });
  renderPage(); await screen.findAllByText("ada@example.com"); fireEvent.click(screen.getByRole("button", { name: "Change email" }));
  fireEvent.change(await screen.findByLabelText("New email address"), { target: { value: "new@example.com" } }); fireEvent.change(document.getElementById("email-current-password"), { target: { value: "correct-password" } }); fireEvent.click(screen.getByRole("button", { name: "Confirm account" }));
  expect(await screen.findByLabelText("Code sent to your current email")).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText("Code sent to your current email"), { target: { value: "123456" } }); fireEvent.click(screen.getAllByRole("button", { name: "Verify code" })[0]);
  expect(await screen.findByLabelText("Code sent to new@example.com")).toBeInTheDocument();
  expect(screen.queryByLabelText("Code sent to your current email")).not.toBeInTheDocument();
});

test("incorrect current password is displayed in its security panel", async () => {
  const user = userEvent.setup();
  api.post.mockRejectedValue({ response: { data: { message: "Current password is incorrect." } } }); renderPage(); await screen.findAllByText("ada@example.com");
  await user.type(document.getElementById("current-password"), "wrong-password"); await user.type(screen.getByLabelText("New password"), "new-password"); await user.type(screen.getByLabelText("Confirm new password"), "new-password"); await user.click(screen.getByRole("button", { name: "Change password" }));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith("/profile/password/change", expect.any(Object)));
  expect(await screen.findByRole("alert")).toHaveTextContent("Current password is incorrect");
});

test("successful password change shows the fresh sign-in state", async () => {
  const user = userEvent.setup();
  api.post.mockResolvedValue({ data: { message: "Password updated successfully. Please sign in again.", logoutRequired: true } });
  renderPage(); await screen.findAllByText("ada@example.com");
  await user.type(document.getElementById("current-password"), "old-password"); await user.type(screen.getByLabelText("New password"), "new-password"); await user.type(screen.getByLabelText("Confirm new password"), "new-password"); await user.click(screen.getByRole("button", { name: "Change password" }));
  expect(await screen.findByText(/Password updated successfully/)).toBeInTheDocument();
  expect(screen.getByRole("button", { name: "Sign in again" })).toBeInTheDocument();
});

test("pending profile action disables duplicate submission", async () => {
  const user = userEvent.setup();
  api.post.mockReturnValue(new Promise(() => {}));
  renderPage(); await screen.findAllByText("ada@example.com"); await user.click(screen.getByRole("button", { name: "Change username" }));
  await user.type(await screen.findByRole("textbox", { name: /New username/ }), "ada_lovelace"); await user.click(screen.getByRole("button", { name: "Continue securely" }));
  expect(await screen.findByRole("button", { name: "Sending code…" })).toBeDisabled();
  expect(api.post).toHaveBeenCalledTimes(1);
});

test("OAuth-only accounts show password setup instead of a current-password form", async () => {
  api.get.mockResolvedValue({ data: { ...localProfile, hasLocalPassword: false, connectedProviders: ["GOOGLE"] } }); renderPage();
  expect(await screen.findByText("OAuth sign-in only")).toBeInTheDocument(); expect(screen.getByRole("button", { name: "Set a Verdixa password" })).toBeInTheDocument(); expect(screen.queryByLabelText("Current password", { selector: "input#current-password" })).not.toBeInTheDocument();
});
