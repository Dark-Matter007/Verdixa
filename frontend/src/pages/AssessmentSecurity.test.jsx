import {afterEach,beforeEach,describe,expect,test,vi} from "vitest";
import {act,cleanup,fireEvent,render,screen,waitFor} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import {MemoryRouter,Route,Routes} from "react-router-dom";
import api from "../services/api";
import AssessmentDetail from "./AssessmentDetail";
import AssessmentStudio from "./AssessmentStudio";
import AssessmentTake from "./AssessmentTake";

vi.mock("../services/api",()=>({default:{get:vi.fn(),post:vi.fn(),defaults:{baseURL:"/api"}}}));
vi.mock("@monaco-editor/react",()=>({default:()=> <div data-testid="editor"/>}));

const assessment={id:7,title:"Security assessment",organization:"Verdixa",description:"Test",instructions:"Stay focused",visibility:"PRIVATE",status:"LIVE",startAt:new Date(Date.now()-60000).toISOString(),endAt:new Date(Date.now()+3600000).toISOString(),registered:true,fullscreenRequired:true,microphoneRequired:true,strictProctoring:true,host:"owner",problems:[{id:9,order:1,title:"Arrays",difficulty:"EASY",points:100}]};

function route(element,path="/assessments/7/take"){
 return render(<MemoryRouter initialEntries={[path]}><Routes><Route path="/assessments/:id/take" element={element}/><Route path="/assessments/:id" element={element}/><Route path="/assessments/studio" element={element}/></Routes></MemoryRouter>);
}

beforeEach(()=>{
 localStorage.clear();sessionStorage.clear();vi.clearAllMocks();
 Object.defineProperty(document,"hidden",{configurable:true,value:false});
 Object.defineProperty(document,"visibilityState",{configurable:true,value:"visible"});
 Object.defineProperty(document,"fullscreenElement",{configurable:true,value:null,writable:true});
 document.documentElement.requestFullscreen=vi.fn(async()=>{document.fullscreenElement=document.documentElement});
 document.exitFullscreen=vi.fn(async()=>{document.fullscreenElement=null});
 global.fetch=vi.fn(async()=>({ok:true}));
});
afterEach(()=>cleanup());

test("private assessment renders the backend unauthorized state",async()=>{
 api.get.mockRejectedValue({response:{data:{message:"This assessment is private."}}});
 route(<AssessmentDetail/>,"/assessments/7");
 expect(await screen.findByText("This assessment is private.")).toBeInTheDocument();
});

test("uninvited private users see metadata but no registration control",async()=>{
 api.get.mockResolvedValue({data:{...assessment,registered:false,invited:false,accessRestricted:true,status:"UPCOMING"}});
 route(<AssessmentDetail/>,"/assessments/7");
 expect(await screen.findByText("Access restricted")).toBeInTheDocument();
 expect(screen.getByText("You were not invited to this assessment.")).toBeInTheDocument();
 expect(screen.queryByRole("button",{name:/register now/i})).not.toBeInTheDocument();
});

test("public assessment directs participants to secure access instead of registration",async()=>{
  api.get.mockResolvedValue({data:{...assessment,visibility:"PUBLIC",registered:false,accessRestricted:false,status:"UPCOMING"}});
  route(<AssessmentDetail/>,"/assessments/7");
 const access=await screen.findByRole("link",{name:/access assessment/i});
 expect(access).toHaveAttribute("href","/assessment/7/access");
 expect(screen.queryByText(/registration confirmed/i)).not.toBeInTheDocument();
 expect(api.post).not.toHaveBeenCalled();
});

test("creator approval gates Assessment Studio",async()=>{
 api.get.mockResolvedValue({data:{status:"PENDING_ADMIN_REVIEW",approved:false}});
 route(<AssessmentStudio/>,"/assessments/studio");
 expect(await screen.findByText(/Application: PENDING ADMIN REVIEW/i)).toBeInTheDocument();
 expect(screen.queryByText("Create assessment")).not.toBeInTheDocument();
});

describe("proctored assessment events",()=>{
 let track,screenTrack;
 beforeEach(()=>{
  sessionStorage.setItem("verdixa_assessment_grant","verified-grant");
  track={kind:"audio",readyState:"live",stop:vi.fn(),addEventListener:vi.fn(),removeEventListener:vi.fn()};
  screenTrack={kind:"video",readyState:"live",stop:vi.fn(),addEventListener:vi.fn(),removeEventListener:vi.fn()};
  Object.defineProperty(navigator,"mediaDevices",{configurable:true,value:{getUserMedia:vi.fn(async()=>({getTracks:()=>[track]})),getDisplayMedia:vi.fn(async()=>({getTracks:()=>[screenTrack]}))}});
  api.get.mockResolvedValue({data:{assessment}});
  api.post.mockImplementation(async url=>url==="/assessment-access/7/start"?{data:{id:22,status:"ACTIVE",endsAt:assessment.endAt,assessment}}:{data:{status:"ACTIVE"}});
 });

 async function start(){route(<AssessmentTake/>);await screen.findByText("Security assessment");await userEvent.click(screen.getByRole("button",{name:"Check"}));await userEvent.click(screen.getByRole("button",{name:"Share"}));await userEvent.click(screen.getByRole("button",{name:"Enter"}));await userEvent.click(screen.getByRole("checkbox"));await userEvent.click(screen.getByRole("button",{name:/Enter secure assessment/i}));await screen.findByTestId("editor");await waitFor(()=>expect(api.post).toHaveBeenCalledWith("/assessment-sessions/22/arm"));await waitFor(()=>expect(track.addEventListener).toHaveBeenCalledWith("ended",expect.any(Function)))}

 test("fullscreen exit sends the violation and shows terminated UI",async()=>{
  await start();document.fullscreenElement=null;fireEvent(document,new Event("fullscreenchange"));
  expect(await screen.findByText("Assessment session ended")).toBeInTheDocument();
  expect(api.post).toHaveBeenCalledWith("/assessment-sessions/22/proctor-event",{type:"FULLSCREEN_EXIT"});
 });

 test("visibility change sends a tab-switch violation",async()=>{
  await start();Object.defineProperty(document,"visibilityState",{configurable:true,value:"hidden"});fireEvent(document,new Event("visibilitychange"));
  expect(await screen.findByText("Assessment session ended")).toBeInTheDocument();
  expect(api.post).toHaveBeenCalledWith("/assessment-sessions/22/proctor-event",{type:"TAB_SWITCH"});
 });

 test("window blur is audited while the strict proctored session stays active",async()=>{
  await start();fireEvent(window,new Event("blur"));
  await waitFor(()=>expect(api.post).toHaveBeenCalledWith("/assessment-sessions/22/proctor-event",{type:"WINDOW_BLUR"}));
  expect(screen.getByTestId("editor")).toBeInTheDocument();
  expect(screen.queryByText("Assessment session ended")).not.toBeInTheDocument();
 });

 test("running assessment sends a server heartbeat",async()=>{
  await start();
  await waitFor(()=>expect(api.post).toHaveBeenCalledWith("/assessment-sessions/22/heartbeat"));
 });

 test("first heartbeat completes before proctoring is armed",async()=>{
  await start();
  const heartbeat=api.post.mock.calls.findIndex(([url])=>url==="/assessment-sessions/22/heartbeat");
  const arm=api.post.mock.calls.findIndex(([url])=>url==="/assessment-sessions/22/arm");
  expect(heartbeat).toBeGreaterThan(-1);
  expect(arm).toBeGreaterThan(heartbeat);
 });

 test("startup fullscreen transitions do not emit a violation before arming",async()=>{
  route(<AssessmentTake/>);await screen.findByText("Security assessment");
  document.fullscreenElement=null;fireEvent(document,new Event("fullscreenchange"));
  expect(api.post).not.toHaveBeenCalledWith("/assessment-sessions/22/proctor-event",expect.anything());
 });

 test("preflight media cleanup does not emit a page-exit or microphone violation",async()=>{
  route(<AssessmentTake/>);await screen.findByText("Security assessment");
  await userEvent.click(screen.getByRole("button",{name:"Check"}));cleanup();
  expect(track.stop).toHaveBeenCalled();
  expect(api.post.mock.calls.filter(([url])=>url.includes("/proctor-event"))).toHaveLength(0);
 });

 test("component cleanup after activation does not report a synthetic violation",async()=>{
  await start();
  const before=api.post.mock.calls.filter(([url])=>url.includes("/proctor-event")).length;
  cleanup();
  expect(api.post.mock.calls.filter(([url])=>url.includes("/proctor-event"))).toHaveLength(before);
 });

 test("duplicate browser violations send only one termination request",async()=>{
  await start();Object.defineProperty(document,"visibilityState",{configurable:true,value:"hidden"});
  fireEvent(document,new Event("visibilitychange"));document.fullscreenElement=null;fireEvent(document,new Event("fullscreenchange"));
  await screen.findByText("Assessment session ended");
  expect(api.post.mock.calls.filter(([url,body])=>url==="/assessment-sessions/22/proctor-event"&&["TAB_SWITCH","FULLSCREEN_EXIT"].includes(body.type))).toHaveLength(1);
 });

 test("starting a proctored session keeps its permission streams alive",async()=>{
  await start();
  expect(track.stop).not.toHaveBeenCalled();
  expect(screenTrack.stop).not.toHaveBeenCalled();
 });

 test("microphone check never silently requests a webcam",async()=>{
  route(<AssessmentTake/>);await screen.findByText("Security assessment");await userEvent.click(screen.getByRole("button",{name:"Check"}));
  expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledWith({audio:true,video:false});
  expect(screen.getByText(/Live · audio is not recorded/i)).toBeInTheDocument();
 });

 test("microphone permission denial prevents session start",async()=>{
  navigator.mediaDevices.getUserMedia.mockRejectedValue(new DOMException("Denied","NotAllowedError"));
  route(<AssessmentTake/>);await screen.findByText("Security assessment");await userEvent.click(screen.getByRole("button",{name:"Check"}));
  expect(await screen.findByText("Microphone permission must be allowed before the assessment can start.")).toBeInTheDocument();
  expect(api.post).not.toHaveBeenCalledWith("/assessment-access/7/start");
 });

 test("microphone track ending terminates the assessment",async()=>{
  await start();const ended=track.addEventListener.mock.calls.find(([name])=>name==="ended")[1];await act(async()=>ended());
  expect(await screen.findByText("Assessment session ended")).toBeInTheDocument();
  expect(api.post).toHaveBeenCalledWith("/assessment-sessions/22/proctor-event",{type:"MICROPHONE_DISABLED"});
 });

 test("screen share ending after arming terminates the assessment",async()=>{
  await start();const ended=screenTrack.addEventListener.mock.calls.find(([name])=>name==="ended")[1];await act(async()=>ended());
  expect(await screen.findByText("Assessment session ended")).toBeInTheDocument();
  expect(api.post).toHaveBeenCalledWith("/assessment-sessions/22/proctor-event",{type:"SCREEN_SHARE_STOPPED"});
 });

 test("active workspace omits assistant and global display language controls",async()=>{
  await start();
  expect(screen.queryByRole("button",{name:/Open Verdixa Assistant/i})).not.toBeInTheDocument();
  expect(screen.queryByLabelText("Language selection")).not.toBeInTheDocument();
 });

 test("final submission opens a professional confirmation modal",async()=>{
  await start();await userEvent.click(screen.getByRole("button",{name:"Submit Assessment"}));
  expect(screen.getByRole("dialog",{name:"Submit assessment?"})).toBeInTheDocument();
  expect(screen.getByText("Unsolved work will be submitted as-is. After final submission you cannot continue editing.")).toBeInTheDocument();
 });
});
