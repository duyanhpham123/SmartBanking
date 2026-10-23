export function AdminPagination({ page, totalPages, totalElements, onPageChange }) {
  if (totalPages <= 1 && totalElements <= 0) return null

  return (
    <div className="pagination">
      <div className="pagination-info">
        Trang {page + 1} / {Math.max(totalPages, 1)} ({totalElements} bản ghi)
      </div>
      <div className="pagination-pages">
        <button
          type="button"
          className="page-btn"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          aria-label="Trang trước"
        >
          ←
        </button>
        <button
          type="button"
          className="page-btn"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          aria-label="Trang sau"
        >
          →
        </button>
      </div>
    </div>
  )
}
