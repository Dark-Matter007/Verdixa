import {afterEach, beforeEach, expect, test, vi} from "vitest";
import {cleanup, fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import api from "../services/api";
import AssessmentParticipants from "./AssessmentParticipants";

vi.mock("../services/api", () => ({default:{get:vi.fn(), post:vi.fn(), delete:vi.fn()}}));
vi.mock("../components/UserShell", () => ({default:({children}) => <main>{children}</main>}));
vi.mock("../components/WorkspacePageHeader", () => ({default:({title}) => <h1>{title}</h1>}));
vi.mock("../components/StatusPill", () => ({default:({value}) => <span>{value}</span>}));
vi.mock("../components/PageState", () => ({ErrorState:({message}) => <p role="alert">{message}</p>, LoadingState:() => <p>Loading</p>}));

const item = {id:7,title:"Private screen",visibility:"PRIVATE",status:"DRAFT"};
const people = [{id:3,username:"sam",status:"INVITED",score:0},{id:4,username:"lee",status:"SUBMITTED",score:100}];
function renderPage(){ return render(<MemoryRouter initialEntries={["/assessments/studio/7/participants"]}><Routes><Route path="/assessments/studio/:id/participants" element={<AssessmentParticipants/>}/></Routes></MemoryRouter>); }
beforeEach(() => { vi.clearAllMocks(); api.get.mockImplementation((url) => { if (url === "/assessments/7/participants") return Promise.resolve({data:people}); if (url === "/assessments/7") return Promise.resolve({data:item}); if (url === "/assessments/invitee-search") return Promise.resolve({data:[{id:8,username:"maya"}]}); return Promise.resolve({data:[]}); }); });
afterEach(cleanup);

test("selects a searched user, sends user IDs, and reports duplicate-safe results", async () => {
  api.post.mockResolvedValue({data:{invited:1,alreadyInvited:0}});
  renderPage();
  const search = await screen.findByRole("textbox", {name:"Search Verdixa users"});
  fireEvent.change(search, {target:{value:"ma"}});
  expect(await screen.findByText("maya")).toBeInTheDocument();
  fireEvent.click(screen.getByText("maya"));
  fireEvent.click(screen.getByRole("button", {name:/Send secure invitations/i}));
  await waitFor(() => expect(api.post).toHaveBeenCalledWith("/assessments/7/invitations", {userIds:[8],emails:[]}));
  expect(await screen.findByRole("status")).toHaveTextContent("1 participant invited");
});

test("filters the participation register using available participant status", async () => {
  renderPage();
  await screen.findByText("sam");
  fireEvent.click(screen.getByRole("button", {name:"COMPLETED"}));
  expect(screen.getByText("lee")).toBeInTheDocument();
  expect(screen.queryByText("sam")).not.toBeInTheDocument();
});
