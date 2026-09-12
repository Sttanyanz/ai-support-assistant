import { useState } from "react";

export default function App() {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);

  async function sendMessage(e) {
    e.preventDefault();

    if (!message.trim() || loading) return;

    const userMessage = message.trim();

    setMessages((prev) => [
      ...prev,
      { role: "user", content: userMessage },
    ]);

    setMessage("");
    setLoading(true);

    try {
      const response = await fetch("/api/messages", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          source:"SMS",
          userContact:"+79001112233",
          message: userMessage,
        }),
      });

      if (!response.ok) {
        throw new Error("Request failed");
      }

      const data = await response.json();
      if(data.action=="ASK_CLARIFICATION"){
        setMessages((prev) => [
          ...prev,
          { role: "assistant", content: data.answer },
        ]);
      }else if (data.action=="CREATE_TICKET"){
        setMessages((prev) => [
          ...prev,
          { role: "assistant", content: "Ваша заявка отправлена в техподдержку." },
        ]);
      }
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          role: "assistant",
          content: "Извините, что-то пошло не так.",
        },
      ]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={styles.page}>
      <div style={styles.chat}>
        <h2>Техподдержка</h2>

        <div style={styles.messages}>
          {messages.map((msg, index) => (
            <div
              key={index}
              style={{
                ...styles.message,
                ...(msg.role === "user"
                  ? styles.userMessage
                  : styles.assistantMessage),
              }}
            >
              {msg.content}
            </div>
          ))}

          {loading && (
            <div style={{ ...styles.message, ...styles.assistantMessage }}>
              Thinking...
            </div>
          )}
        </div>

        <form onSubmit={sendMessage} style={styles.form}>
          <input
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Введите сообщение..."
            style={styles.input}
            disabled={loading}
          />

          <button type="submit" style={styles.button} disabled={loading}>
            Send
          </button>
        </form>
      </div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    background: "#f5f5f5",
    display: "flex",
    justifyContent: "center",
    alignItems: "center",
    padding: "20px",
    fontFamily: "Arial, sans-serif",
  },

  chat: {
    width: "100%",
    maxWidth: "700px",
    height: "80vh",
    background: "white",
    borderRadius: "12px",
    display: "flex",
    flexDirection: "column",
    padding: "20px",
    boxSizing: "border-box",
    boxShadow: "0 4px 20px rgba(0,0,0,0.1)",
  },

  messages: {
    flex: 1,
    overflowY: "auto",
    padding: "20px 0",
    display: "flex",
    flexDirection: "column",
    gap: "12px",
  },

  message: {
    padding: "10px 14px",
    borderRadius: "10px",
    maxWidth: "75%",
    lineHeight: "1.5",
  },

  userMessage: {
    alignSelf: "flex-end",
    background: "#007bff",
    color: "white",
  },

  assistantMessage: {
    alignSelf: "flex-start",
    background: "#eee",
    color: "#222",
  },

  form: {
    display: "flex",
    gap: "10px",
    borderTop: "1px solid #ddd",
    paddingTop: "15px",
  },

  input: {
    flex: 1,
    padding: "12px",
    border: "1px solid #ccc",
    borderRadius: "8px",
    fontSize: "16px",
  },

  button: {
    padding: "12px 20px",
    border: "none",
    borderRadius: "8px",
    background: "#007bff",
    color: "white",
    cursor: "pointer",
  },
};