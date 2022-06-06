import React, { useMemo } from "react";
import { useTable, usePagination } from "react-table";
import "./styles/Table.css";

export default function Table({ COLUMNS, DATA }) {
  const columns = useMemo(() => COLUMNS, [COLUMNS]);
  const data = useMemo(() => DATA, [DATA]);

  const tableInstance = useTable(
    {
      columns,
      data,
    },
    usePagination
  );

  const {
    getTableProps,
    getTableBodyProps,
    headerGroups,
    page,
    nextPage,
    previousPage,
    canNextPage,
    canPreviousPage,
    pageOptions,
    state,
    gotoPage,
    pageCount,
    prepareRow,
  } = tableInstance;

  const { pageIndex } = state;

  return (
    <>
      <table {...getTableProps()}>
        <thead>
          {headerGroups.map((headerGroup) => (
            <tr {...headerGroup.getHeaderGroupProps()}>
              {headerGroup.headers.map((column) => (
                <th {...column.getHeaderProps()}>{column.render("Header")}</th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody {...getTableBodyProps()}>
          {page.map((row) => {
            prepareRow(row);
            return (
              <tr {...row.getRowProps()}>
                {row.cells.map((cell) => {
                  return (
                    <td {...cell.getCellProps()}> {cell.render("Cell")} </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
      <div>
        <button
          className="tablebtn"
          onClick={() => {
            gotoPage(0);
          }}
          disabled={!canPreviousPage}>
          {"<<"}
        </button>
        <button
          className="tablebtn"
          onClick={() => {
            previousPage();
          }}
          disabled={!canPreviousPage}>
          Previous
        </button>
        <span>
          Page{" "}
          <strong>
            {" "}
            {pageIndex + 1} of {pageOptions.length}
          </strong>{" "}
        </span>
        <button
          className="tablebtn"
          onClick={() => {
            nextPage();
          }}
          disabled={!canNextPage}>
          Next
        </button>
        <button
          className="tablebtn"
          onClick={() => {
            gotoPage(pageCount - 1);
          }}
          disabled={!canNextPage}>
          {">>"}
        </button>
      </div>
    </>
  );
}
