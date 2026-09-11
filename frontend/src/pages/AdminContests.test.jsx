import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";

vi.mock("../services/api", () => ({ default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() } }));
import api from "../services/api";
import AdminContests from "./AdminContests";

const contests = [
  { id:1, title:"Weekly Challenge", startTime:"2030-01-01T10:00:00", endTime:"2030-01-01T12:00:00", status:"UPCOMING", problemCount:5, registrationCount:1 },
  { id:2, title:"Monthly Sprint", startTime:"2030-02-01T10:00:00", endTime:"2030-02-01T12:00:00", status:"ENDED", problemCount:1, registrationCount:0 },
];
const detail = { id:1, title:"Weekly Challenge", description:"A focused weekly round.", startTime:"2030-01-01T10:00:00", endTime:"2030-01-01T12:00:00", status:"UPCOMING", problemCount:5, registrationCount:1, problems:[{ id:11, title:"Two Sum", difficulty:"EASY", tags:["Array", "Hash Map"], order:1 }], registrations:[{ userId:7, username:"ada", email:"ada@example.com", registeredAt:"2030-01-01T09:00:00", solvedCount:3, totalProblems:5 }] };

beforeEach(() => {
  vi.clearAllMocks();
  api.delete.mockResolvedValue({ data:{ id:1 } });
  api.get.mockImplementation((url) => {
    if (url === "/admin/contests") return Promise.resolve({ data:contests });
    if (url === "/admin/contests/1") return Promise.resolve({ data:detail });
    if (url === "/admin/contests/2") return Promise.resolve({ data:{ ...detail, id:2, title:"Monthly Sprint", problems:[], registrations:[] } });
    return Promise.resolve({ data:{ content:[{ id:11, title:"Two Sum", difficulty:"EASY" }] } });
  });
});

const renderPage = () => render(<MemoryRouter><AdminContests /></MemoryRouter>);

test("renders the contest list and automatically loads the first contest", async () => {
  renderPage();
  expect(await screen.findByText("Weekly Challenge")).toBeInTheDocument();
  expect(screen.getByText("Monthly Sprint")).toBeInTheDocument();
  expect(await screen.findByText("A focused weekly round.")).toBeInTheDocument();
  expect(api.get).toHaveBeenCalledWith("/admin/contests");
});

test("selecting a contest loads its detail view", async () => {
  renderPage();
  await screen.findByText("Weekly Challenge");
  fireEvent.click(screen.getByRole("listitem", { name:/monthly sprint/i }));
  expect(await screen.findByText("No problems have been assigned to this contest.")).toBeInTheDocument();
  expect(api.get).toHaveBeenCalledWith("/admin/contests/2");
});

test("renders problems and registered-user progress, then opens confirmation", async () => {
  renderPage();
  expect(await screen.findByRole("link", { name:"Two Sum" })).toHaveAttribute("href", "/admin/problems/edit/11");
  expect(screen.getByText("ada@example.com")).toBeInTheDocument();
  expect(screen.getByText("3 / 5 solved")).toBeInTheDocument();
  expect(screen.getByLabelText("3 of 5 solved")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("button", { name:/delete contest/i }));
  expect(await screen.findByRole("dialog")).toHaveTextContent("Users, global problems, and normal submission history will not be deleted.");
});

test("deleting the selected contest updates list and selection", async () => {
  renderPage();
  await screen.findByText("A focused weekly round.");
  fireEvent.click(screen.getByRole("button", { name:/delete contest/i }));
  fireEvent.click(screen.getAllByRole("button", { name:/delete contest/i })[1]);
  await waitFor(() => expect(api.delete).toHaveBeenCalledWith("/admin/contests/1"));
  await waitFor(() => expect(screen.queryByRole("listitem", { name:/weekly challenge/i })).not.toBeInTheDocument());
  expect(await screen.findByRole("listitem", { name:/monthly sprint/i })).toHaveAttribute("aria-pressed", "true");
});

test("renders registry error state", async () => {
  api.get.mockImplementation((url) => url === "/admin/contests" ? Promise.reject({ response:{ data:{ message:"Registry is unavailable" } } }) : Promise.resolve({ data:{ content:[] } }));
  renderPage();
  expect(await screen.findByRole("alert")).toHaveTextContent("Registry is unavailable");
});
