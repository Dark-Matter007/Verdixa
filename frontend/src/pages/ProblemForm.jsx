import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  ArrowLeft,
  Save,
  Code2,
  Loader2,
} from "lucide-react";
import api from "../services/api";
import BrandLogo from "../components/BrandLogo";

function ProblemForm() {
  const navigate = useNavigate();
  const { id } = useParams();

  const isEditMode = Boolean(id);

  const [loading, setLoading] = useState(isEditMode);
  const [saving, setSaving] = useState(false);
  const [starterLanguage, setStarterLanguage] = useState("java");
  const [starterTemplates, setStarterTemplates] = useState({ java: "", cpp: "", python: "" });

  const [form, setForm] = useState({
    title: "",
    difficulty: "EASY",
    tags: "",
    description: "",
    inputFormat: "",
    outputFormat: "",
    constraints: "",
    examples: "",
    starterCode: "",
    active: false,
    executionMode: "STDIN",
    functionSignature: { functionName: "", returnType: "String", parameters: [] },
  });

  useEffect(() => {
    const role = localStorage.getItem("algosphere_role");

    if (role !== "ADMIN") {
      navigate("/dashboard");
      return;
    }

    if (isEditMode) {
      loadProblem();
    }
  }, [id, isEditMode, navigate]);

  const loadProblem = async () => {
    try {
      const response = await api.get(`/problems/${id}`);

      const problem = response.data;

      setForm({
        title: problem.title || "",
        difficulty: problem.difficulty || "EASY",
        tags: problem.tags || "",
        description: problem.description || "",
        inputFormat: problem.inputFormat || "",
        outputFormat: problem.outputFormat || "",
        constraints: problem.constraints || "",
        examples: problem.examples || "",
        starterCode: problem.starterCode || "",
        active: problem.active ?? false,
        executionMode: problem.executionMode || "STDIN",
        functionSignature: problem.functionSignature || { functionName: "", returnType: "String", parameters: [] },
      });
      try {
        const parsed = JSON.parse(problem.starterCode || "{}");
        if (parsed && typeof parsed === "object") setStarterTemplates((previous) => ({ ...previous, ...parsed }));
      } catch {
        setStarterTemplates((previous) => ({ ...previous, java: problem.starterCode || "" }));
      }
    } catch (error) {
      console.error("Failed to load problem:", error);
      alert("Unable to load problem.");
      navigate("/admin/problems");
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!form.title.trim()) {
      alert("Please enter a problem title.");
      return;
    }

    if (!form.description.trim()) {
      alert("Please enter a problem description.");
      return;
    }

    try {
      setSaving(true);

      const payload = {
        title: form.title.trim(),
        difficulty: form.difficulty,
        tags: form.tags.trim(),
        description: form.description.trim(),
        inputFormat: form.inputFormat.trim(),
        outputFormat: form.outputFormat.trim(),
        constraints: form.constraints.trim(),
        examples: form.examples.trim(),
        starterCode: JSON.stringify(starterTemplates),
        active: form.active,
        executionMode: form.executionMode,
        functionSignature: form.executionMode === "FUNCTION" ? {
          ...form.functionSignature,
          parameters: form.functionSignature.parameters.map((parameter, index) => ({ ...parameter, parameterOrder: index }))
        } : null,
      };

      if (isEditMode) {
        await api.put(`/problems/${id}`, payload);
        alert("Problem updated successfully.");
      } else {
        await api.post("/problems", payload);
        alert("Problem created successfully.");
      }

      navigate("/admin/problems");
    } catch (error) {
      console.error("Failed to save problem:", error);

      if (error.response?.status === 401) {
        alert("Your session has expired. Please login again.");
      } else if (error.response?.status === 403) {
        alert("Access denied. Admin privileges are required.");
      } else {
        alert(
          error.response?.data?.message ||
          "Unable to save problem."
        );
      }
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="dashboard-layout">
        <main className="dashboard-main">
          <div className="loading">
            <Loader2 size={20} className="spin" />
            Loading problem...
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="dashboard-layout">

      <aside className="sidebar">

        <div className="sidebar-brand">
          <BrandLogo compact />

          <div>
            <strong>Verdixa</strong>
            <span>ADMIN</span>
          </div>
        </div>

        <nav className="sidebar-nav">

          <button
            className="sidebar-item"
            onClick={() => navigate("/admin")}
          >
            Dashboard
          </button>

          <button
            className="sidebar-item active"
            onClick={() => navigate("/admin/problems")}
          >
            Problems
          </button>

        </nav>

        <button
          className="sidebar-logout"
          onClick={() => {
            localStorage.removeItem("algosphere_token");
            localStorage.removeItem("algosphere_role");
            localStorage.removeItem("algosphere_username");
            navigate("/login");
          }}
        >
          Logout
        </button>

      </aside>

      <main className="dashboard-main">

        <header className="dashboard-header">

          <div>

            <button
              className="back-button"
              onClick={() => navigate("/admin/problems")}
            >
              <ArrowLeft size={18} />
              Back to Problems
            </button>

            <h1>
              {isEditMode ? "Edit Problem" : "Create Problem"}
            </h1>

            <p>
              {isEditMode
                ? "Update the coding problem details."
                : "Create a new coding problem for Verdixa users."}
            </p>

          </div>

        </header>

        <section className="dashboard-section">

          <form
            className="problem-form"
            onSubmit={handleSubmit}
          >

            {/* BASIC INFORMATION */}

            <div className="form-section">

              <div className="form-section-header">
                <h2>Basic Information</h2>
                <p>Define the problem title and difficulty.</p>
              </div>

              <div className="form-grid">

                <div className="form-group form-full">
                  <label htmlFor="title">
                    Problem Title
                  </label>

                  <input
                    id="title"
                    name="title"
                    type="text"
                    placeholder="e.g. Two Sum"
                    value={form.title}
                    onChange={handleChange}
                    required
                  />
                </div>

                <div className="form-group">

                  <label htmlFor="difficulty">
                    Difficulty
                  </label>

                  <select
                    id="difficulty"
                    name="difficulty"
                    value={form.difficulty}
                    onChange={handleChange}
                  >
                    <option value="EASY">Easy</option>
                    <option value="MEDIUM">Medium</option>
                    <option value="HARD">Hard</option>
                  </select>

                </div>

                <div className="form-group">

                  <label htmlFor="tags">
                    Tags
                  </label>

                  <input
                    id="tags"
                    name="tags"
                    type="text"
                    placeholder="array,hashmap,two-pointers"
                    value={form.tags}
                    onChange={handleChange}
                  />

                  <small>
                    Separate tags using commas.
                  </small>

                </div>

              </div>

            </div>

            {/* DESCRIPTION */}

            <div className="form-section">

              <div className="form-section-header">
                <h2>Problem Description</h2>
                <p>Explain what the user needs to solve.</p>
              </div>

              <div className="form-group">

                <label htmlFor="description">
                  Description
                </label>

                <textarea
                  id="description"
                  name="description"
                  rows="7"
                  placeholder="Write the complete problem description..."
                  value={form.description}
                  onChange={handleChange}
                  required
                />

              </div>

            </div>

            {/* INPUT OUTPUT */}

            <div className="form-section">

              <div className="form-section-header">
                <h2>Input & Output</h2>
                <p>Define the expected input and output.</p>
              </div>

              <div className="form-grid">

                <div className="form-group">

                  <label htmlFor="inputFormat">
                    Input Format
                  </label>

                  <textarea
                    id="inputFormat"
                    name="inputFormat"
                    rows="5"
                    placeholder="Describe the input..."
                    value={form.inputFormat}
                    onChange={handleChange}
                  />

                </div>

                <div className="form-group">

                  <label htmlFor="outputFormat">
                    Output Format
                  </label>

                  <textarea
                    id="outputFormat"
                    name="outputFormat"
                    rows="5"
                    placeholder="Describe the output..."
                    value={form.outputFormat}
                    onChange={handleChange}
                  />

                </div>

              </div>

            </div>

            {/* CONSTRAINTS */}

            <div className="form-section">

              <div className="form-section-header">
                <h2>Constraints</h2>
                <p>Specify limits and conditions.</p>
              </div>

              <div className="form-group">

                <label htmlFor="constraints">
                  Constraints
                </label>

                <textarea
                  id="constraints"
                  name="constraints"
                  rows="5"
                  placeholder="e.g. 1 ≤ n ≤ 100000"
                  value={form.constraints}
                  onChange={handleChange}
                />

              </div>

            </div>

            {/* EXAMPLES */}

            <div className="form-section">

              <div className="form-section-header">
                <h2>Examples</h2>
                <p>Provide sample inputs and outputs.</p>
              </div>

              <div className="form-group">

                <label htmlFor="examples">
                  Examples
                </label>

                <textarea
                  id="examples"
                  name="examples"
                  rows="7"
                  placeholder={`Example 1:
Input: nums = [2,7,11,15]
Output: [0,1]

Example 2:
Input: nums = [3,2,4]
Output: [1,2]`}
                  value={form.examples}
                  onChange={handleChange}
                />

              </div>

            </div>

            <div className="form-section">
              <div className="form-section-header"><h2>Execution Contract</h2><p>Choose raw STDIN or a solution-only function.</p></div>
              <div className="form-group"><label>Execution Mode</label><select value={form.executionMode} onChange={(event) => setForm((previous) => ({...previous, executionMode: event.target.value}))}><option value="STDIN">STDIN</option><option value="FUNCTION">FUNCTION</option></select></div>
              {form.executionMode === "FUNCTION" && <div className="function-contract">
                <div className="form-grid"><div className="form-group"><label>Function name</label><input value={form.functionSignature.functionName} onChange={(event) => setForm((previous) => ({...previous, functionSignature:{...previous.functionSignature,functionName:event.target.value}}))} required /></div>
                <div className="form-group"><label>Return type</label><select value={form.functionSignature.returnType} onChange={(event) => setForm((previous) => ({...previous,functionSignature:{...previous.functionSignature,returnType:event.target.value}}))}>{["int","Integer","long","double","boolean","String","int[]","long[]","double[]","String[]"].map((type) => <option key={type}>{type}</option>)}</select></div></div>
                <h3>Ordered parameters</h3>
                {form.functionSignature.parameters.map((parameter,index) => <div className="function-parameter-row" key={index}><input aria-label={`Parameter ${index + 1} name`} placeholder="name" value={parameter.name} onChange={(event) => setForm((previous) => ({...previous,functionSignature:{...previous.functionSignature,parameters:previous.functionSignature.parameters.map((item,i)=>i===index?{...item,name:event.target.value}:item)}}))}/><select value={parameter.type} onChange={(event) => setForm((previous) => ({...previous,functionSignature:{...previous.functionSignature,parameters:previous.functionSignature.parameters.map((item,i)=>i===index?{...item,type:event.target.value}:item)}}))}>{["int","Integer","long","double","boolean","String","int[]","long[]","double[]","String[]"].map((type)=><option key={type}>{type}</option>)}</select><button type="button" onClick={() => setForm((previous) => ({...previous,functionSignature:{...previous.functionSignature,parameters:previous.functionSignature.parameters.filter((_,i)=>i!==index)}}))}>Remove</button></div>)}
                <button type="button" className="secondary-button" onClick={() => setForm((previous) => ({...previous,functionSignature:{...previous.functionSignature,parameters:[...previous.functionSignature.parameters,{name:"",type:"String",parameterOrder:previous.functionSignature.parameters.length}]}}))}>Add parameter</button>
              </div>}
            </div>

            {/* STARTER CODE */}

            <div className="form-section">

              <div className="form-section-header">
                <h2>Starter Code</h2>
                <p>Provide the initial code shown to users.</p>
              </div>

              <div className="form-group">

                <label htmlFor="starterCode">Language-specific starter code</label>

                <div className="starter-language-tabs" role="tablist" aria-label="Starter code language">
                  {["java", "cpp", "python"].map((item) => <button type="button" role="tab" key={item} className={starterLanguage === item ? "active" : ""} onClick={() => setStarterLanguage(item)}>{item === "cpp" ? "C++17" : item[0].toUpperCase() + item.slice(1)}</button>)}
                </div>

                <textarea
                  id="starterCode"
                  name="starterCode"
                  rows="10"
                  className="code-input"
                  placeholder={starterLanguage === "java" ? `public class Solution {
    public int[] twoSum(int[] nums, int target) {
        
    }
}` : starterLanguage === "cpp" ? "#include <iostream>\nusing namespace std;\n\nint main() {\n    return 0;\n}" : "def solve():\n    pass"}
                  value={starterTemplates[starterLanguage]}
                  onChange={(event) => setStarterTemplates((previous) => ({ ...previous, [starterLanguage]: event.target.value }))}
                />

              </div>

            </div>

            {/* STATUS */}

            <div className="form-section">

              <div className="form-section-header">
                <h2>Status</h2>
                <p>Control whether users can access this problem.</p>
              </div>

              <label className="checkbox-label">

                <input
                  type="checkbox"
                  name="active"
                  checked={form.active}
                  onChange={handleChange}
                />

                <span>
                  Problem is active
                </span>

              </label>

            </div>

            {/* ACTIONS */}

            <div className="form-actions">

              <button
                type="button"
                className="secondary-button"
                onClick={() => navigate("/admin/problems")}
                disabled={saving}
              >
                Cancel
              </button>

              <button
                type="submit"
                className="primary-button"
                disabled={saving}
              >

                {saving ? (
                  <>
                    <Loader2 size={18} className="spin" />
                    Saving...
                  </>
                ) : (
                  <>
                    <Save size={18} />
                    {isEditMode
                      ? "Update Problem"
                      : "Create Problem"}
                  </>
                )}

              </button>

            </div>

          </form>

        </section>

      </main>

    </div>
  );
}

export default ProblemForm;
