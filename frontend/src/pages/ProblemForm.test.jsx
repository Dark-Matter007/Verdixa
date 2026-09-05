import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, expect, test, vi } from "vitest";
vi.mock("../services/api",()=>({default:{get:vi.fn(),post:vi.fn(),put:vi.fn(),delete:vi.fn()}}));
import api from "../services/api";
import ProblemForm from "./ProblemForm";

const problem={id:7,title:"Function problem",description:"description",difficulty:"EASY",tags:"arrays",active:false,executionMode:"FUNCTION",starterCode:"{}",functionSignature:{functionName:"solve",returnType:"String",parameters:[{name:"value",type:"String",order:0}]}};
const renderForm=()=>render(<MemoryRouter initialEntries={["/admin/problems/edit/7"]}><Routes><Route path="/admin/problems/edit/:id" element={<ProblemForm/>}/><Route path="/admin/problems" element={<div>Problems index</div>}/></Routes></MemoryRouter>);

beforeEach(()=>{vi.clearAllMocks();localStorage.setItem("algosphere_role","ADMIN");vi.spyOn(window,"alert").mockImplementation(()=>{});api.get.mockImplementation((url)=>url==="/problems/7"?Promise.resolve({data:problem}):url.endsWith("/hints/admin")?Promise.resolve({data:[]}):Promise.reject(new Error("not configured")));});

test("admin update submits FUNCTION parameters and reports success",async()=>{api.put.mockResolvedValue({data:problem});renderForm();expect(await screen.findByDisplayValue("Function problem")).toBeInTheDocument();fireEvent.click(screen.getByRole("button",{name:"Update Problem"}));await waitFor(()=>expect(api.put).toHaveBeenCalledWith("/problems/7",expect.objectContaining({executionMode:"FUNCTION",functionSignature:expect.objectContaining({parameters:[expect.objectContaining({name:"value",parameterOrder:0})]})})));expect(window.alert).toHaveBeenCalledWith("Problem updated successfully.");});

test("admin update displays backend failure message",async()=>{api.put.mockRejectedValue({response:{status:500,data:{message:"Update failed safely."}}});renderForm();await screen.findByDisplayValue("Function problem");fireEvent.click(screen.getByRole("button",{name:"Update Problem"}));await waitFor(()=>expect(window.alert).toHaveBeenCalledWith("Update failed safely."));});
