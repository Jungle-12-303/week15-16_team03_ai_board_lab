export default function Pagination({ currentPage, totalPages, onPageChange }) {
  const visiblePageNumbers = getVisiblePageNumbers(currentPage, totalPages);

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

      {visiblePageNumbers.map((pageNumber) => (
        <button
          key={pageNumber}
          type="button"
          className="plain-button"
          onClick={() => onPageChange(pageNumber)}
          aria-current={currentPage === pageNumber ? 'page' : undefined}
        >
          {pageNumber}
        </button>
      ))}

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

function getVisiblePageNumbers(currentPage, totalPages) {
  const pageCandidates = [
    1,
    currentPage - 1,
    currentPage,
    currentPage + 1,
    totalPages,
  ];

  return [...new Set(pageCandidates)].filter(
    (pageNumber) => pageNumber >= 1 && pageNumber <= totalPages,
  );
}
