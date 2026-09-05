import { useEffect, useState } from "react";
import api from "../services/api";
import AdminShell from "../components/AdminShell";
import { EmptyState } from "../components/PageState";
export default function AdminCollections() {
 const [items,setItems]=useState([]),[title,setTitle]=useState(""),[description,setDescription]=useState(""),[error,setError]=useState("");
 const load=()=>api.get("/collections").then(r=>setItems(r.data||[])).catch(()=>setError("Unable to load collections."));
 useEffect(()=>{load();},[]);
 const create=async e=>{e.preventDefault();try{await api.post("/collections",{title,description,published:false});setTitle("");setDescription("");load()}catch{setError("Unable to create collection.")}};
 const save=async c=>{try{await api.put(`/collections/${c.id}`,c);load()}catch{setError("Unable to update collection.")}};
 const remove=async id=>{if(!window.confirm("Delete this collection?"))return;try{await api.delete(`/collections/${id}`);load()}catch{setError("Unable to delete collection.")}};
 const add=async c=>{const id=window.prompt("Problem ID to add");if(id)try{await api.post(`/collections/${c.id}/problems/${id}`);load()}catch{setError("Unable to add problem.")}};
 const move=async(c,index,dir)=>{const ids=(c.items||[]).map(i=>i.problem.id),swap=index+dir;if(swap<0||swap>=ids.length)return;[ids[index],ids[swap]]=[ids[swap],ids[index]];try{await api.put(`/collections/${c.id}/problems/order`,ids);load()}catch{setError("Unable to reorder problems.")}};
 return <AdminShell title="Collections" description="Curate problem sets and control their publication lifecycle.">
  <form className="vx-admin-create" onSubmit={create}><label>Collection title<input required value={title} onChange={e=>setTitle(e.target.value)} placeholder="Collection title"/></label><label>Description<input value={description} onChange={e=>setDescription(e.target.value)} placeholder="What this collection teaches"/></label><button className="primary-button">Create</button></form>
  {error&&<p className="dashboard-error">{error}</p>}
  {items.length===0?<EmptyState title="No collections yet.">Create the first curated problem collection.</EmptyState>:<section className="vx-admin-editor-list">{items.map(c=><article className="vx-admin-editor" key={c.id}><div className="vx-admin-editor-head"><span>{c.published?"Published":"Draft"}</span><strong>{(c.items||[]).length} problems</strong></div><label>Title<input value={c.title} onChange={e=>setItems(items.map(x=>x.id===c.id?{...x,title:e.target.value}:x))}/></label><label>Description<textarea value={c.description||""} onChange={e=>setItems(items.map(x=>x.id===c.id?{...x,description:e.target.value}:x))}/></label><div className="vx-admin-actions"><button onClick={()=>save(c)}>Save</button><button onClick={()=>save({...c,published:!c.published})}>{c.published?"Unpublish":"Publish"}</button><button onClick={()=>add(c)}>Add problem</button><button className="danger-button" onClick={()=>remove(c.id)}>Delete</button></div><div className="vx-admin-items">{(c.items||[]).map((i,n)=><div key={i.id}><span><b>{String(n+1).padStart(2,"0")}</b>{i.problem?.title}</span><div><button aria-label={`Move ${i.problem?.title} up`} onClick={()=>move(c,n,-1)}>↑</button><button aria-label={`Move ${i.problem?.title} down`} onClick={()=>move(c,n,1)}>↓</button><button onClick={async()=>{await api.delete(`/collections/${c.id}/problems/${i.problem.id}`);load()}}>Remove</button></div></div>)}</div></article>)}</section>}
 </AdminShell>;
}
