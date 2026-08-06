import { useEffect, useMemo, useState } from "react";
import "./App.css";

const sampleEvent = {
  eventType: "ACCOUNT_VIEWED",
  actorId: "ui-user",
  resourceType: "ACCOUNT",
  resourceId: "acct-ui",
  payload: { reason: "frontend-seed", ip: "10.2.0.1" },
  timestamp: new Date().toISOString()
};

function App() {
  const [baseUrl, setBaseUrl] = useState(window.location.origin);
  const [actorId, setActorId] = useState("fresh-demo-user");
  const [resourceType, setResourceType] = useState("");
  const [resourceId, setResourceId] = useState("");
  const [eventType, setEventType] = useState("");
  const [includeArchived, setIncludeArchived] = useState(false);
  const [searchText, setSearchText] = useState("");
  const [timeRange, setTimeRange] = useState("all");
  const [fromTime, setFromTime] = useState("");
  const [toTime, setToTime] = useState("");
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [records, setRecords] = useState([]);
  const [statusText, setStatusText] = useState("Ready.");
  const [loading, setLoading] = useState(false);

  const apiRoot = useMemo(() => baseUrl.replace(/\/$/, ""), [baseUrl]);

  async function request(path, options = {}) {
    try {
      const response = await fetch(`${apiRoot}${path}`, {
        headers: { "Content-Type": "application/json" },
        ...options
      });
      const text = await response.text();
      return { ok: response.ok, status: response.status, text };
    } catch (error) {
      return { ok: false, status: 0, text: String(error) };
    }
  }

  function toIsoOrBlank(value) {
    if (!value) {
      return "";
    }
    return new Date(value).toISOString();
  }

  function toLocalDateTime(date) {
    const pad = (value) => String(value).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  function applyPresetRange(range) {
    const now = new Date();
    const todayStart = toStartOfDay(now);
    const yesterdayStart = new Date(todayStart);
    yesterdayStart.setDate(yesterdayStart.getDate() - 1);
    const weekStart = new Date(todayStart);
    weekStart.setDate(weekStart.getDate() - 7);

    if (range === "today") {
      setFromTime(toLocalDateTime(todayStart));
      setToTime(toLocalDateTime(now));
      return;
    }
    if (range === "yesterday") {
      const yesterdayEnd = new Date(todayStart);
      yesterdayEnd.setMilliseconds(-1);
      setFromTime(toLocalDateTime(yesterdayStart));
      setToTime(toLocalDateTime(yesterdayEnd));
      return;
    }
    if (range === "7days") {
      setFromTime(toLocalDateTime(weekStart));
      setToTime(toLocalDateTime(now));
      return;
    }
    if (range === "all") {
      setFromTime("");
      setToTime("");
    }
  }

  function toStartOfDay(date) {
    const copy = new Date(date);
    copy.setHours(0, 0, 0, 0);
    return copy;
  }

  function formatCreationTime(timestamp) {
    const now = new Date();
    const input = new Date(timestamp);
    const todayStart = toStartOfDay(now);
    const yesterdayStart = new Date(todayStart);
    yesterdayStart.setDate(yesterdayStart.getDate() - 1);
    const dayLabel = input >= todayStart ? "Today" : input >= yesterdayStart ? "Yesterday" : input.toLocaleDateString();
    return `${dayLabel}, ${input.toLocaleTimeString()}`;
  }

  function operationLabel(eventType) {
    return eventType
      .toLowerCase()
      .split("_")
      .map((part) => part[0].toUpperCase() + part.slice(1))
      .join(" ");
  }

  function originLabel(record) {
    const ip = record?.payload?.ip;
    if (typeof ip === "string" && ip.length > 0) {
      return `${ip} (GUI)`;
    }
    return "audit-service (API)";
  }

  async function loadEvents() {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        includeArchived: String(includeArchived),
        page: "0",
        size: "100"
      });
      if (actorId.trim()) {
        params.set("actorId", actorId.trim());
      }
      if (resourceType.trim()) {
        params.set("resourceType", resourceType.trim());
      }
      if (resourceId.trim()) {
        params.set("resourceId", resourceId.trim());
      }
      if (eventType.trim()) {
        params.set("eventType", eventType.trim());
      }
      if (fromTime) {
        params.set("from", toIsoOrBlank(fromTime));
      }
      if (toTime) {
        params.set("to", toIsoOrBlank(toTime));
      }

      const response = await fetch(`${apiRoot}/audit/events?${params.toString()}`);
      const text = await response.text();
      if (!response.ok) {
        setStatusText(`Failed to load records (${response.status}).`);
        return;
      }
      const body = JSON.parse(text);
      setRecords(Array.isArray(body.items) ? body.items : []);
      setStatusText(`Loaded ${Array.isArray(body.items) ? body.items.length : 0} records.`);
    } catch (error) {
      setStatusText(`Request failed: ${String(error)}`);
    } finally {
      setLoading(false);
    }
  }

  async function seedOneRecord() {
    setLoading(true);
    const event = {
      ...sampleEvent,
      actorId,
      resourceId: `acct-ui-${Math.floor(Math.random() * 10000)}`,
      timestamp: new Date().toISOString()
    };
    try {
      const result = await request("/audit/events", { method: "POST", body: JSON.stringify(event) });
      if (!result.ok) {
        setStatusText(`Seed failed (${result.status}).`);
        return;
      }
      setStatusText("Seeded one record.");
      await loadEvents();
    } finally {
      setLoading(false);
    }
  }

  async function verifyChain() {
    setLoading(true);
    try {
      const result = await request("/audit/verify");
      if (!result.ok) {
        setStatusText(`Verify failed (${result.status}).`);
        return;
      }
      const body = JSON.parse(result.text);
      setStatusText(body.intact ? `Chain intact across ${body.checkedRecords} records.` : "Chain verification detected inconsistency.");
    } finally {
      setLoading(false);
    }
  }

  async function exportBundle() {
    setLoading(true);
    try {
      const actor = actorId.trim();
      const type = resourceType.trim();
      const resource = resourceId.trim();
      const hasActor = actor.length > 0;
      const hasResourcePair = type.length > 0 && resource.length > 0;
      if (!hasActor && !hasResourcePair) {
        setStatusText("Export requires actorId OR resourceType + resourceId.");
        return;
      }
      const params = new URLSearchParams({ includeArchived: "true" });
      if (hasActor) {
        params.set("actorId", actor);
      } else {
        params.set("resourceType", type);
        params.set("resourceId", resource);
      }
      const result = await request(`/audit/exports?${params.toString()}`);
      if (!result.ok) {
        setStatusText(`Export failed (${result.status}).`);
        return;
      }
      const bundle = JSON.parse(result.text);
      const fileName = `audit-export-${bundle.exportId ?? Date.now()}.json`;
      const blob = new Blob([JSON.stringify(bundle, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = fileName;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
      setStatusText(`Exported ${bundle.recordCount ?? 0} records to ${fileName}.`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadEvents();
  }, []);

  useEffect(() => {
    if (timeRange !== "custom") {
      applyPresetRange(timeRange);
    }
  }, [timeRange]);

  const filteredRows = useMemo(() => {
    return records.filter((record) => {
      if (!searchText.trim()) {
        return true;
      }
      const haystack = [
        String(record.sequenceNumber),
        record.eventType,
        record.actorId,
        record.resourceType,
        record.resourceId,
        JSON.stringify(record.payload ?? {})
      ]
        .join(" ")
        .toLowerCase();
      return haystack.includes(searchText.toLowerCase());
    });
  }, [records, searchText]);

  return (
    <div className="auditShell">
      <header className="titleBar">
        <h1>Audit Log System</h1>
        <span className="infoBadge" title="Audit trail console">i</span>
      </header>

      <section className="toolbar">
        <div className="toolbarLeft">
          <label className="inlineField">
            <span>Stored filters</span>
            <select>
              <option>All</option>
            </select>
          </label>
          <label className="inlineField">
            <span>Time range</span>
            <select value={timeRange} onChange={(e) => setTimeRange(e.target.value)}>
              <option value="all">All</option>
              <option value="today">Today</option>
              <option value="yesterday">Yesterday</option>
              <option value="7days">Last 7 days</option>
              <option value="custom">Custom</option>
            </select>
          </label>
        </div>

        <div className="toolbarRight">
          <input
            className="searchInput"
            placeholder="Search"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
          <button className="ghostBtn" type="button">Save as</button>
          <button className="ghostBtn" type="button" disabled>Delete</button>
          <button className="ghostBtn" type="button" onClick={() => setSearchText("")}>Reset</button>
          <button className="advancedToggle" type="button" onClick={() => setShowAdvanced((v) => !v)}>
            <span>{showAdvanced ? "▾" : "▸"}</span> Advanced filter
          </button>
        </div>
      </section>

      {showAdvanced ? (
        <section className="advancedPanel">
          <label>
            API base URL
            <input value={baseUrl} onChange={(e) => setBaseUrl(e.target.value)} />
          </label>
          <label>
            Actor
            <input value={actorId} onChange={(e) => setActorId(e.target.value)} />
          </label>
          <label>
            Resource type
            <input value={resourceType} onChange={(e) => setResourceType(e.target.value)} placeholder="ACCOUNT" />
          </label>
          <label>
            Resource ID
            <input value={resourceId} onChange={(e) => setResourceId(e.target.value)} placeholder="acct-1" />
          </label>
          <label>
            Event type
            <input value={eventType} onChange={(e) => setEventType(e.target.value)} placeholder="ACCOUNT_VIEWED" />
          </label>
          <label>
            From (ISO window)
            <input type="datetime-local" value={fromTime} onChange={(e) => setFromTime(e.target.value)} />
          </label>
          <label>
            To (ISO window)
            <input type="datetime-local" value={toTime} onChange={(e) => setToTime(e.target.value)} />
          </label>
          <label className="checkOption">
            <input
              type="checkbox"
              checked={includeArchived}
              onChange={(e) => setIncludeArchived(e.target.checked)}
            />
            Include archived
          </label>
          <div className="advancedActions">
            <button className="smallBtn" onClick={loadEvents} disabled={loading}>Refresh</button>
            <button className="smallBtn" onClick={seedOneRecord} disabled={loading}>Seed</button>
            <button className="smallBtn" onClick={verifyChain} disabled={loading}>Verify</button>
          </div>
        </section>
      ) : null}

      <div className="actionStrip">
        <button className="ghostBtn" onClick={exportBundle} disabled={loading}>Export</button>
      </div>

      <section className="gridWrap">
        <table>
          <thead>
            <tr>
              <th>Id...</th>
              <th>Creation Time</th>
              <th>Operation</th>
              <th>User</th>
              <th>Resource Type</th>
              <th>Resource ID</th>
              <th>Origin</th>
              <th>Extra Details</th>
            </tr>
          </thead>
          <tbody>
            {filteredRows.length === 0 ? (
              <tr>
                <td colSpan={8} className="emptyCell">{loading ? "Loading records..." : "No audit records."}</td>
              </tr>
            ) : filteredRows.map((record) => (
              <tr key={record.id}>
                <td>{record.sequenceNumber}</td>
                <td>{formatCreationTime(record.timestamp)}</td>
                <td>{operationLabel(record.eventType)}</td>
                <td>{record.actorId}</td>
                <td>{record.resourceType}</td>
                <td>{record.resourceId}</td>
                <td>{originLabel(record)}</td>
                <td className="truncate">{JSON.stringify(record.payload ?? {})}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <div className="statusBar">{loading ? "Working..." : statusText}</div>
    </div>
  );
}

export default App;
