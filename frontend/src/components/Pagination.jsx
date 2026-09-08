import { ChevronLeft, ChevronRight } from "lucide-react";

export default function Pagination({ data, onChange }) {
  if (!data || data.totalPages <= 1) return null;

  return (
    <nav className="pagination" aria-label="Pagination">
      <button
        type="button"
        className="pagination-button pagination-button--previous"
        disabled={data.first}
        onClick={() => onChange(data.page - 1)}
      >
        <ChevronLeft size={15} aria-hidden="true" />
        Previous
      </button>
      <span className="pagination-status" aria-live="polite">
        Page {data.page + 1} of {data.totalPages}
      </span>
      <button
        type="button"
        className="pagination-button pagination-button--next"
        disabled={data.last}
        onClick={() => onChange(data.page + 1)}
      >
        Next
        <ChevronRight size={15} aria-hidden="true" />
      </button>
    </nav>
  );
}
