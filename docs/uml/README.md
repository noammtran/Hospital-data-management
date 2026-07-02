# UML Diagrams — Hospital Patient Management System

Source files cho diagram của báo cáo Chương 3 (System Analysis & Design).

## File listing

| File | Diagram type | Tham chiếu trong nháp |
|------|--------------|----------------------|
| `usecase.mermaid` | Use Case Diagram (D-01) | §10.1 |
| `../erd/erd.mermaid` | ERD (D-02) — đã có sẵn | §10.1 |
| `class-domain.mermaid` | Class Diagram domain model (D-03) | §10.1 |
| `state-patient.mermaid` | State Diagram — Patient status (D-04) | §10.1 |
| `state-admission.mermaid` | State Diagram — Admission status (D-05) | §10.1 |
| `seq-create-patient.mermaid` | Sequence — Create Patient + validate 2 layers (D-06) | §10.1 |
| `seq-edit-audit.mermaid` | Sequence — Edit Patient + @EntityListeners audit (D-07) | §10.1 |
| `seq-delete-archive.mermaid` | Sequence — Delete vs Archive (D-08) | §10.1 |
| `seq-search-filter.mermaid` | Sequence — Search/Filter via JPA Specifications (D-09) | §10.1 |
| `architecture.mermaid` | 3-tier MVC Architecture (D-10) | §10.1 |
| `activity-examination.mermaid` | Activity — Quy trình khám tổng thể (D-11) | §10.2 |
| `seq-login.mermaid` | Sequence — Login JWT (D-13) | §10.2 |
| `db-wiring.mermaid` | Block — Environment + App + DB at startup (Ch.4) | Ch.4 §Project Setup |

D-12 (Bulk delete) và D-14 (Deployment) chưa vẽ — sẽ thêm khi cần.

## Cách render sang PDF cho LaTeX

### Phương án 1: Mermaid CLI (khuyến nghị)
```bash
npm install -g @mermaid-js/mermaid-cli

# Render từng file
mmdc -i docs/uml/usecase.mermaid -o docs/report/figures/usecase.pdf -t neutral -b transparent
mmdc -i docs/uml/state-patient.mermaid -o docs/report/figures/state-patient.pdf
# ... lặp lại cho các file khác
```

### Phương án 2: Mermaid Live Editor (web)
- Mở https://mermaid.live
- Paste nội dung file `.mermaid`
- Export → SVG hoặc PNG → convert sang PDF qua Inkscape

### Phương án 3: Render trong VS Code
- Cài extension "Markdown Preview Mermaid Support" hoặc "Mermaid Preview"
- Mở file `.mermaid`, screenshot/export

## Nhúng vào LaTeX

```latex
\begin{figure}[H]
  \centering
  \includegraphics[width=0.9\textwidth]{usecase}
  \caption{Use Case Diagram of the Hospital Patient Management System.}
  \label{fig:usecase}
\end{figure}
```

## Quy ước

- File source ở `docs/uml/*.mermaid` — commit vào git.
- File output ở `docs/report/figures/*.pdf` — gitignore (sinh ra từ source).
- Mỗi diagram có header comment ghi: tên, ngày cập nhật, tham chiếu QT/UC.
- Khi sửa quy trình nghiệp vụ trong `business-process-draft.md`, đồng thời sửa diagram tương ứng để tránh lệch.
