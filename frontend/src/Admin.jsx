import { useEffect, useState } from "react";

export default function Admin() {
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  async function loadTickets() {
    try {
      setLoading(true);

      const response = await fetch("/api/tickets");

      if (!response.ok) {
        throw new Error("Не удалось загрузить тикеты");
      }

      const data = await response.json();
      print(data)

      setTickets(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadTickets();
  }, []);

  return (
    <div style={styles.page}>
      <div style={styles.container}>

        <div style={styles.header}>
          <h1>Тикеты техподдержки</h1>

          <div>
            <button
              onClick={loadTickets}
              style={styles.refreshButton}
            >
              Обновить
            </button>

            <button
              onClick={() => window.location.href = "/"}
              style={styles.backButton}
            >
              ← Чат
            </button>
          </div>
        </div>

        {loading && <p>Загрузка...</p>}

        {error && (
          <div style={styles.error}>
            {error}
          </div>
        )}

        {!loading && !error && (
          <div style={styles.tableWrapper}>
            <table style={styles.table}>
              <thead>
                <tr>
                  <th>Время создания</th>
                  <th>Описание</th>
                  <th>Категория</th>
                  <th>Приоритет</th>
                  <th>Телефон</th>
                </tr>
              </thead>

              <tbody>
                {tickets.map((ticket, index) => (
                  <tr key={ticket.id ?? index}>
                    <td>
                      {formatDate(ticket.createdAt)}
                    </td>

                    <td>
                      {ticket.text}
                    </td>

                    <td>
                      {ticket.category}
                    </td>

                    <td>
                      <span
                        style={{
                          ...styles.priority,
                          ...getPriorityStyle(ticket.priority),
                        }}
                      >
                        {ticket.priority}
                      </span>
                    </td>

                    <td>
                      {ticket.userContact}
                    </td>
                  </tr>
                ))}

                {tickets.length === 0 && (
                  <tr>
                    <td colSpan="5" style={styles.empty}>
                      Тикетов пока нет
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

function formatDate(date) {
  if (!date) return "—";

  try {
    return new Date(date).toLocaleString("ru-RU");
  } catch {
    return date;
  }
}

function getPriorityStyle(priority) {
  switch (String(priority).toUpperCase()) {
    case "HIGH":
    case "ВЫСОКИЙ":
      return {
        background: "#ffe0e0",
        color: "#c00",
      };

    case "MEDIUM":
    case "СРЕДНИЙ":
      return {
        background: "#fff0c2",
        color: "#996600",
      };

    case "LOW":
    case "НИЗКИЙ":
      return {
        background: "#e0f5e0",
        color: "#287a28",
      };

    default:
      return {
        background: "#eee",
        color: "#333",
      };
  }
}

const styles = {
  page: {
    minHeight: "100vh",
    background: "#f5f5f5",
    padding: "30px",
    fontFamily: "Arial, sans-serif",
    boxSizing: "border-box",
  },

  container: {
    maxWidth: "1200px",
    margin: "0 auto",
  },

  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: "25px",
  },

  refreshButton: {
    padding: "10px 16px",
    border: "none",
    borderRadius: "6px",
    background: "#007bff",
    color: "white",
    cursor: "pointer",
    marginRight: "10px",
  },

  backButton: {
    padding: "10px 16px",
    border: "1px solid #ccc",
    borderRadius: "6px",
    background: "white",
    cursor: "pointer",
  },

  tableWrapper: {
    background: "white",
    borderRadius: "10px",
    overflow: "auto",
    boxShadow: "0 2px 10px rgba(0,0,0,0.08)",
  },

  table: {
    width: "100%",
    borderCollapse: "collapse",
  },

  priority: {
    display: "inline-block",
    padding: "5px 9px",
    borderRadius: "12px",
    fontSize: "13px",
    fontWeight: "bold",
  },

  error: {
    background: "#ffe0e0",
    color: "#b00000",
    padding: "15px",
    borderRadius: "8px",
  },

  empty: {
    textAlign: "center",
    padding: "30px",
    color: "#777",
  },
};