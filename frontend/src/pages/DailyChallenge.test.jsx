import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";
vi.mock("../services/api",()=>({default:{get:vi.fn()}}));
import api from "../services/api";
import DailyChallenge from "./DailyChallenge";

const renderPage=()=>render(<MemoryRouter><DailyChallenge/></MemoryRouter>);
beforeEach(()=>{vi.clearAllMocks();});

test("renders loading state while the current challenge is pending",()=>{api.get.mockReturnValue(new Promise(()=>{}));renderPage();expect(screen.getByRole("status")).toHaveTextContent("Loading daily challenge");});

test("renders current challenge data and completion state",async()=>{api.get.mockImplementation((url)=>url==="/daily-challenges/today"?Promise.resolve({data:{id:1,challengeDate:"2026-09-05",problem:{id:4,title:"Two Sum",difficulty:"EASY",tags:"arrays"},completed:true}}):url==="/daily-challenges/summary"?Promise.resolve({data:{completed:1,currentStreak:1,longestStreak:1}}):Promise.resolve({data:[]}));renderPage();expect(await screen.findByText("Two Sum")).toBeInTheDocument();expect(screen.getByText("Completed")).toBeInTheDocument();expect(screen.getByRole("link",{name:"Review Challenge"})).toHaveAttribute("href","/problems/4");});

test("renders clean empty state for a 204 response",async()=>{api.get.mockImplementation((url)=>url==="/daily-challenges/today"?Promise.resolve({status:204,data:""}):Promise.resolve({data:url.endsWith("summary")?{completed:0,currentStreak:0,longestStreak:0}:[]}));renderPage();expect(await screen.findByText("No challenge scheduled today")).toBeInTheDocument();expect(screen.queryByRole("alert")).not.toBeInTheDocument();});

test("renders retryable API error state",async()=>{api.get.mockImplementation((url)=>url==="/daily-challenges/today"?Promise.reject(new Error("network")):Promise.resolve({data:[]}));renderPage();expect(await screen.findByRole("alert")).toHaveTextContent("Today's challenge could not be retrieved.");expect(screen.getByRole("button",{name:"Try again"})).toBeInTheDocument();});
