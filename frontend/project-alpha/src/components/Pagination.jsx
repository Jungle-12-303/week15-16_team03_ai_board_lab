export default function Pagination({ currentPage, totalPages, onPageChange }) {
  return (
    <div className="pagination">
      <button
        type="button"
        className="plain-button"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 1}
      >
        Previous
      </button>

      {Array.from({ length: totalPages }, (_, index) => {
        const pageNumber = index + 1;

        return (
          <button
            key={pageNumber}
            type="button"
            className="plain-button"
            onClick={() => onPageChange(pageNumber)}
            aria-current={currentPage === pageNumber ? 'page' : undefined}
          >
            {pageNumber}
          </button>
        );
      })}

      <button
        type="button"
        className="plain-button"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages}
      >
        Next
      </button>
    </div>
  );
}
