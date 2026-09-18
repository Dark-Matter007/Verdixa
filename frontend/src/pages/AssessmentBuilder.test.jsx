import {afterEach, beforeEach, expect, test, vi} from "vitest";
import {cleanup, fireEvent, render, screen, waitFor} from "@testing-library/react";
import {MemoryRouter, Route, Routes} from "react-router-dom";
import api from "../services/api";
import AssessmentBuilder from "./AssessmentBuilder";

vi.mock("../services/api", () => ({default:{get:vi.fn(), post:vi.fn(), put:vi.fn()}}));
vi.mock("../components/UserShell", () => ({default:({children}) => <main>{children}</main>}));
vi.mock("../components/WorkspacePageHeader", () => ({default:({title}) => <h1>{title}</h1>}));

function renderBuilder() {
  return render(<MemoryRouter initialEntries={["/assessments/studio/create"]}><Routes><Route path="/assessments/studio/create" element={<AssessmentBuilder/>}/></Routes></MemoryRouter>);
}
function renderEdit() {
  return render(<MemoryRouter initialEntries={["/assessments/studio/7/edit"]}><Routes><Route path="/assessments/studio/:id/edit" element={<AssessmentBuilder/>}/></Routes></MemoryRouter>);
}

beforeEach(() => {
  vi.clearAllMocks();
  api.get.mockImplementation((url, config) => {
    if (url !== "/problems/library") return Promise.resolve({data:{}});
    const page = config?.params?.page ?? 0;
    return Promise.resolve({data:page === 0 ? {content:[{problem:{id:1, title:"Array sum", difficulty:"EASY", tags:"arrays"}}], totalPages:2} : {content:[{problem:{id:2, title:"Graph paths", difficulty:"HARD", tags:"graphs"}}], totalPages:2}});
  });
});
afterEach(() => cleanup());

test("loads every page of active library problems and preserves selections when returning", async () => {
  renderBuilder();
  fireEvent.click(screen.getByRole("button", {name:/browse visible problems/i}));
  expect(await screen.findByText("Array sum")).toBeInTheDocument();
  expect(screen.getByText("Graph paths")).toBeInTheDocument();
  expect(api.get).toHaveBeenCalledWith("/problems/library", {params:{page:0, size:100, sort:"title"}});
  expect(api.get).toHaveBeenCalledWith("/problems/library", {params:{page:1, size:100, sort:"title"}});
  fireEvent.click(screen.getByText("Graph paths"));
  await waitFor(() => expect(screen.getByText((_, element) => element?.tagName === "P" && element.textContent === "1 problem ready for this assessment")).toBeInTheDocument());
  fireEvent.click(screen.getByRole("button", {name:/done selecting/i}));
  expect(screen.getByText((_, element) => element?.className === "vx-problem-set-summary" && element.textContent.includes("1selected problem"))).toBeInTheDocument();
  expect(screen.getByText("Graph paths")).toBeInTheDocument();
});

test("explains public and private visibility and reveals controlled private invite search", async () => {
  renderBuilder();
  expect(screen.getByText("Anyone with the link can verify their email and participate.")).toBeInTheDocument();
  expect(screen.getByText("Only email addresses you invite can verify and participate.")).toBeInTheDocument();
  fireEvent.click(screen.getByRole("radio", {name:/private/i}));
  expect(await screen.findByPlaceholderText("Search users by username")).toBeInTheDocument();
  expect(screen.getAllByText("0 selected").length).toBeGreaterThan(0);
});

test("loads an existing assessment and saves the mapped update request", async () => {
  api.get.mockImplementation((url) => {
    if (url === "/assessments/7") return Promise.resolve({data:{id:7,title:"Screen",organization:"Verdixa",description:"Before",instructions:"Read carefully",visibility:"PUBLIC",startAt:"2027-01-01T10:00:00",endAt:"2027-01-01T11:00:00",registrationDeadline:null,maxParticipants:20,fullscreenRequired:true,microphoneRequired:true,strictProctoring:true,problems:[{id:1,points:100}]}});
    if (url === "/assessments/7/participants") return Promise.resolve({data:[]});
    if (url === "/problems/library") return Promise.resolve({data:{content:[{problem:{id:1,title:"Array sum",difficulty:"EASY",tags:"arrays"}}],totalPages:1}});
    return Promise.resolve({data:{}});
  });
  api.put.mockResolvedValue({data:{id:7}});
  renderEdit();
  const description = await screen.findByDisplayValue("Before");
  fireEvent.change(description, {target:{value:"After"}});
  fireEvent.click(screen.getByRole("button", {name:"Save changes"}));
  await waitFor(() => expect(api.put).toHaveBeenCalledWith("/assessments/7", expect.objectContaining({title:"Screen",description:"After",problems:[{problemId:1,points:100}],inviteeIds:null})));
  expect(await screen.findByRole("status")).toHaveTextContent("Saved successfully");
});
