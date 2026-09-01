export default function Pagination({ data, onChange }) {
  if (!data || data.totalPages <= 1) return null;
  return <div className="pagination"><button disabled={data.first} onClick={() => onChange(data.page - 1)}>Previous</button><span>Page {data.page + 1} of {data.totalPages}</span><button disabled={data.last} onClick={() => onChange(data.page + 1)}>Next</button></div>;
}
