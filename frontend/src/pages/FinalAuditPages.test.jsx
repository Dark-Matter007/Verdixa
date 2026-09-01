import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";
vi.mock("../services/api",()=>({default:{get:vi.fn(),post:vi.fn(),put:vi.fn(),delete:vi.fn()}}));
import api from "../services/api";
import Collections from "./Collections";
import LearningPaths from "./LearningPaths";
import DailyChallenge from "./DailyChallenge";
import AdminDashboard from "./AdminDashboard";

beforeEach(()=>{vi.clearAllMocks();localStorage.setItem("algosphere_role","ADMIN");});
const wrapped=(Component)=><MemoryRouter><Component/></MemoryRouter>;

test("Collections provides dashboard return and problem navigation",async()=>{api.get.mockResolvedValue({data:[{id:1,title:"Core",description:"Practice",items:[{id:2,problem:{id:7,title:"Two Sum"}}]}]});render(wrapped(Collections));expect(await screen.findByText("Core")).toBeInTheDocument();expect(screen.getByRole("link",{name:/Back to Problems/})).toHaveAttribute("href","/dashboard");expect(screen.getByRole("link",{name:"Two Sum"})).toHaveAttribute("href","/problems/7");});

test("Learning Paths provides dashboard return and problem navigation",async()=>{api.get.mockResolvedValue({data:[{id:1,title:"Foundations",sections:[{id:2,title:"Arrays",items:[{id:3,problem:{id:8,title:"Array Sum",difficulty:"EASY"}}]}]}]});render(wrapped(LearningPaths));expect(await screen.findByText("Foundations")).toBeInTheDocument();expect(screen.getByRole("link",{name:/Back to Problems/})).toHaveAttribute("href","/dashboard");expect(screen.getByRole("link",{name:"Array Sum"})).toHaveAttribute("href","/problems/8");});

test("Daily Challenge provides return and challenge navigation",async()=>{api.get.mockImplementation((url)=>url==="/daily-challenges/today"?Promise.resolve({data:{challenge:{id:1},challengeDate:"2026-08-29",problem:{id:9,title:"Reverse String"},completed:false}}):url==="/daily-challenges/summary"?Promise.resolve({data:{completed:2,currentStreak:1,longestStreak:3}}):Promise.resolve({data:[]}));render(wrapped(DailyChallenge));expect(await screen.findByText("Reverse String")).toBeInTheDocument();expect(screen.getByRole("link",{name:/Back to Problems/})).toHaveAttribute("href","/dashboard");expect(screen.getByRole("link",{name:"Open Challenge"})).toHaveAttribute("href","/problems/9");});

test("Admin Dashboard renders database metrics and separated problem metadata",async()=>{api.get.mockImplementation((url)=>url==="/admin/analytics"?Promise.resolve({data:{publishedProblems:4,totalUsers:6,activeUsers:2,totalSubmissions:10,acceptedSubmissions:7,submissionsToday:1,submissionsLast7Days:5,submissionsLast30Days:9,acceptanceRate:70}}):Promise.resolve({data:[{id:2,title:"Reverse String",difficulty:"EASY",active:true,executionMode:"FUNCTION",functionSignature:{functionName:"reverseString",returnType:"String",parameters:[{name:"s",type:"String"}]}}]}));render(wrapped(AdminDashboard));expect(await screen.findByText("Published Problems")).toBeInTheDocument();expect(screen.getByText("70%")).toBeInTheDocument();expect(screen.getByText("Reverse String")).toBeInTheDocument();expect(screen.getByText("Problem #2 · Active")).toBeInTheDocument();expect(screen.getByText("Valid FUNCTION")).toBeInTheDocument();expect(screen.getByRole("button",{name:"Analytics"})).toBeInTheDocument();expect(screen.getByRole("button",{name:"Test Cases"})).toBeInTheDocument();});
