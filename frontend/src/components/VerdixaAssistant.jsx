import { useEffect, useRef, useState } from "react";
import { Bot, ChevronRight, MessageCircle, Send, X } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import api from "../services/api";

const prompts = {
  public: ["What is Verdixa?", "Why should I use Verdixa?", "What can I do here?", "How do contests work?"],
  authenticated: ["How am I doing?", "How many problems have I solved?", "What is my certificate progress?", "What contests am I registered for?"],
  admin: ["How is the platform doing?", "Show contest analytics", "Create a balanced contest", "Suggest contest problems"],
};

export default function VerdixaAssistant({ mode = "authenticated" }) {
  const [open, setOpen] = useState(false); const [text, setText] = useState(""); const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([]); const bottom = useRef(null); const location = useLocation(); const navigate = useNavigate();
  useEffect(() => { bottom.current?.scrollIntoView({ block: "end" }); }, [messages, loading]);
  useEffect(() => { const close = (event) => event.key === "Escape" && setOpen(false); window.addEventListener("keydown", close); return () => window.removeEventListener("keydown", close); }, []);
  const send = async (value = text) => { const message = value.trim(); if (!message || loading) return; setText(""); setMessages((items) => [...items, { role:"user", text:message }]); setLoading(true); try {
    const match = location.pathname.match(/\/(problems|contests)\/(\d+)/); const endpoint = mode === "public" ? "/assistant/public/chat" : mode === "admin" ? "/admin/assistant/chat" : "/assistant/chat"; const pageType = mode === "public" ? "LANDING" : mode === "admin" ? "ADMIN" : match?.[1]?.toUpperCase() || "DASHBOARD"; const response = await api.post(endpoint, { message, pageType, pageId: match ? Number(match[2]) : null });
    setMessages((items) => [...items, { role:"assistant", text:response.data.message, route:response.data.route }]);
  } catch (error) { setMessages((items) => [...items, { role:"assistant", text:error.response?.data?.message || "Verdixa Assistant is temporarily unavailable. Please try again shortly." }]); } finally { setLoading(false); } };
  return <div className="vx-assistant">
    {open && <section className="vx-assistant-panel" aria-label="Verdixa Assistant" role="dialog">
      <header><div><Bot size={18}/><span><strong>Verdixa Assistant</strong><small>{mode === "public" ? "Platform help" : mode === "admin" ? "Administration guide" : "Your workspace guide"}</small></span></div><button aria-label="Close assistant" onClick={() => setOpen(false)}><X size={18}/></button></header>
      <div className="vx-assistant-body">{messages.length === 0 && <div className="vx-assistant-welcome"><p>{mode === "public" ? "Ask about Verdixa, account access, practice, contests, or certificates." : mode === "admin" ? "Ask about platform analytics or create a safe, reviewable contest draft." : "Ask about your Verdixa progress or how the workspace works."}</p><div>{prompts[mode].map((prompt) => <button key={prompt} onClick={() => send(prompt)}>{prompt}</button>)}</div></div>}{messages.map((item, index) => <article className={`vx-assistant-message ${item.role}`} key={`${item.text}-${index}`}><p>{item.text}</p>{item.route && <button onClick={() => { setOpen(false); navigate(item.route); }}>Open section <ChevronRight size={14}/></button>}</article>)}{loading && <article className="vx-assistant-message assistant"><p className="vx-assistant-typing">Thinking<span>.</span><span>.</span><span>.</span></p></article>}<div ref={bottom}/></div>
      <form onSubmit={(event) => { event.preventDefault(); send(); }}><textarea aria-label="Message Verdixa Assistant" value={text} maxLength="1000" onChange={(event) => setText(event.target.value)} onKeyDown={(event) => { if(event.key === "Enter" && !event.shiftKey) { event.preventDefault(); send(); } }} placeholder="Ask Verdixa Assistant…" rows="1"/><button aria-label="Send message" disabled={!text.trim() || loading}><Send size={17}/></button></form>
    </section>}
    <button className="vx-assistant-launcher" aria-label={open ? "Close Verdixa Assistant" : "Open Verdixa Assistant"} onClick={() => setOpen((value) => !value)}><MessageCircle size={22}/><span>Assistant</span></button>
  </div>;
}
