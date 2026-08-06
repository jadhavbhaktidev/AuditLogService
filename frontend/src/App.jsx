import { useEffect, useMemo, useState } from "react";
import "./App.css";

const defaultPayload = "{\n  \"reason\": \"support\",\n  \"ip\": \"10.2.0.1\"\n}";

function App() {
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState("Ready.");

  const [eventType, setEventType] = useState("ACCOUNT_VIEWED");
  const [actorId, setActorId] = useState("fresh-demo-user");
  const [resourceType, setResourceType] = useState("ACCOUNT");
  const [resourceId, setResourceId] = useState("acct-ui-1");
  const [payloadText, setPayloadText] = useState(defaultPayload);

  const [queryActorId, setQueryActorId] = useState("");
  const [queryResourceType, setQueryResourceType] = useState("");
  const [queryResourceId, setQueryResourceId] = useState("");
  const [queryEventType, setQueryEventType] = useState("");
  const [queryFrom, setQueryFrom] = useState("");
  const [queryTo, setQueryTo] = useState("");
  const [queryIncludeArchived, setQueryIncludeArchived] = useState(false);
  const [queryPage, setQueryPage] = useState(0);
  const [querySize, setQuerySize] = useState(20);

  const [allRecords, setAllRecords] = useState([]);
  const [queryResult, setQueryResult] = useState(null);
  const [verifyResult, setVerifyResult] = useState(null);
  const [retentionDays, setRetentionDays] = useState("30");
  const [retentionResult, setRetentionResult] = useState(null);

  const [redactSequenceNumber, setRedactSequenceNumber] = useState("");
  const [redactFields, setRedactFields] = useState("email,token");
  const [redactReason, setRedactReason] = useState("PII policy");
  const [redactApprovedBy, setRedactApprovedBy] = useState("compliance-approver");
  const [redactionResult, setRedactionResult] = useState(null);

  const [exportBundleText, setExportBundleText] = useState("");
  const [exportVerifyResult, setExportVerifyResult] = useState(null);

  const apiRoot = useMemo(() => window.location.origin.replace(/\/$/, ""), []);

  function toIso(value) {
    return value ? new Date(value).toISOString() : "";
  }

  async function callApi(path, options = {}) {
    const response = await fetch(`${apiRoot}${path}`, {
      headers: { "Content-Type": "application/json" },
      ...options
    });
    const text = await response.text();
    let json = null;
    try {
      json = text ? JSON.parse(text) : null;
    } catch {
      json = null;
    }
    return { ok: response.ok, status: response.status, statusText: response.statusText, text, json };
  }

  async function runAction(action) {
    setBusy(true);
    try {
      await action();
    } catch (error) {
      setStatus(`Request failed: ${String(error)}`);
    } finally {
      setBusy(false);
    }
  }

  function setRequestStatus(result, successMessage) {
    if (result.ok) {
      setStatus(successMessage);
    } else {
      setStatus(`${result.status} ${result.statusText}`);
    }
  }

  async function loadFilterOptions() {
    const result = await callApi("/audit/events?includeArchived=true&page=0&size=300");
    if (result.ok && result.json && Array.isArray(result.json.items)) {
      setAllRecords(result.json.items);
    }
  }

  async function createEvent() {
    const payload = JSON.parse(payloadText);
    const body = {
      eventType: eventType.trim(),
      actorId: actorId.trim(),
      resourceType: resourceType.trim(),
      resourceId: resourceId.trim(),
      payload,
      timestamp: new Date().toISOString()
    };
    const result = await callApi("/audit/events", { method: "POST", body: JSON.stringify(body) });
    setRequestStatus(result, "Event created.");
    if (result.ok) {
      setQueryPage(0);
      await queryEvents(0);
      await loadFilterOptions();
    }
  }

  function buildQueryParams(pageNumber) {
    const params = new URLSearchParams({
      includeArchived: String(queryIncludeArchived),
      page: String(pageNumber),
      size: String(querySize)
    });
    if (queryActorId.trim()) {
      params.set("actorId", queryActorId.trim());
    }
    if (queryResourceType.trim()) {
      params.set("resourceType", queryResourceType.trim());
    }
    if (queryResourceId.trim()) {
      params.set("resourceId", queryResourceId.trim());
    }
    if (queryEventType.trim()) {
      params.set("eventType", queryEventType.trim());
    }
    if (queryFrom) {
      params.set("from", toIso(queryFrom));
    }
    if (queryTo) {
      params.set("to", toIso(queryTo));
    }
    return params;
  }

  async function queryEvents(pageNumber = queryPage) {
    const params = buildQueryParams(pageNumber);
    const result = await callApi(`/audit/events?${params.toString()}`);
    setRequestStatus(result, `Query loaded (page ${pageNumber}).`);
    if (result.ok && result.json) {
      setQueryResult(result.json);
      setQueryPage(pageNumber);
    }
  }

  async function verifyChain() {
    const result = await callApi("/audit/verify");
    setRequestStatus(result, "Verification loaded.");
    if (result.ok && result.json) {
      setVerifyResult(result.json);
    }
  }

  async function runRetention() {
    const days = retentionDays.trim();
    const query = days ? `?days=${encodeURIComponent(days)}` : "";
    const result = await callApi(`/audit/retention/run${query}`, { method: "POST" });
    setRequestStatus(result, "Retention completed.");
    if (result.ok && result.json) {
      setRetentionResult(result.json);
      await queryEvents(0);
      await loadFilterOptions();
    }
  }

  async function applyRedaction() {
    const sequenceNumber = Number(redactSequenceNumber);
    const redactedFields = redactFields
      .split(",")
      .map((item) => item.trim())
      .filter(Boolean);

    const body = {
      sequenceNumber,
      redactedFields,
      reason: redactReason.trim(),
      approvedBy: redactApprovedBy.trim()
    };
    const result = await callApi("/audit/redactions", { method: "POST", body: JSON.stringify(body) });
    setRequestStatus(result, "Redaction applied.");
    if (result.ok && result.json) {
      setRedactionResult(result.json);
      await queryEvents(queryPage);
      await loadFilterOptions();
    }
  }

  async function exportBundleFromQuery() {
    const actor = queryActorId.trim();
    const type = queryResourceType.trim();
    const resource = queryResourceId.trim();
    const hasActor = actor.length > 0;
    const hasResourcePair = type.length > 0 && resource.length > 0;
    if (!hasActor && !hasResourcePair) {
      setStatus("Export requires actorId OR resourceType + resourceId.");
      return;
    }

    const params = new URLSearchParams({ includeArchived: String(queryIncludeArchived) });
    if (hasActor) {
      params.set("actorId", actor);
    } else {
      params.set("resourceType", type);
      params.set("resourceId", resource);
    }
    const result = await callApi(`/audit/exports?${params.toString()}`);
    setRequestStatus(result, "Export bundle generated.");
    if (result.ok && result.json) {
      const bundleText = JSON.stringify(result.json, null, 2);
      setExportBundleText(bundleText);

      const fileName = `audit-export-${result.json.exportId ?? Date.now()}.json`;
      const blob = new Blob([bundleText], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = fileName;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      URL.revokeObjectURL(url);
    }
  }

  async function verifyExportBundle() {
    const bundle = JSON.parse(exportBundleText);
    const body = bundle.bundle ? bundle : { bundle };
    const result = await callApi("/audit/exports/verify", { method: "POST", body: JSON.stringify(body) });
    setRequestStatus(result, "Export verification complete.");
    if (result.ok && result.json) {
      setExportVerifyResult(result.json);
    }
  }

  const items = queryResult?.items ?? [];
  const totalPages = queryResult?.totalPages ?? 0;
  const canPrev = queryPage > 0;
  const canNext = queryPage + 1 < totalPages;

  const actorOptions = useMemo(
    () => Array.from(new Set(allRecords.map((row) => row.actorId).filter(Boolean))).sort(),
    [allRecords]
  );

  const resourceTypeOptions = useMemo(
    () => Array.from(new Set(allRecords.map((row) => row.resourceType).filter(Boolean))).sort(),
    [allRecords]
  );

  const resourceIdOptions = useMemo(() => {
    const candidates = allRecords.filter((row) => {
      if (!row.resourceId) {
        return false;
      }
      if (!queryResourceType) {
        return true;
      }
      return row.resourceType === queryResourceType;
    });
    return Array.from(new Set(candidates.map((row) => row.resourceId))).sort();
  }, [allRecords, queryResourceType]);

  const eventTypeOptions = useMemo(
    () => Array.from(new Set(allRecords.map((row) => row.eventType).filter(Boolean))).sort(),
    [allRecords]
  );

  useEffect(() => {
    runAction(async () => {
      await queryEvents(0);
      await loadFilterOptions();
    });
  }, []);

  return (
    <div className="page">
      <h1>Audit Log UI</h1>

      <section className="panel">
        <h2>Create Event</h2>
        <div className="grid cols4">
          <label>Event Type<input value={eventType} onChange={(e) => setEventType(e.target.value)} /></label>
          <label>Actor ID<input value={actorId} onChange={(e) => setActorId(e.target.value)} /></label>
          <label>Resource Type<input value={resourceType} onChange={(e) => setResourceType(e.target.value)} /></label>
          <label>Resource ID<input value={resourceId} onChange={(e) => setResourceId(e.target.value)} /></label>
        </div>
        <label>
          Payload JSON
          <textarea rows={6} value={payloadText} onChange={(e) => setPayloadText(e.target.value)} />
        </label>
        <button onClick={() => runAction(createEvent)} disabled={busy}>Create</button>
      </section>

      <section className="panel">
        <h2>Query Events</h2>
        <div className="grid cols4">
          <label>
            Actor ID
            <select value={queryActorId} onChange={(e) => setQueryActorId(e.target.value)}>
              <option value="">All</option>
              {actorOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
          <label>
            Resource Type
            <select
              value={queryResourceType}
              onChange={(e) => {
                const nextType = e.target.value;
                setQueryResourceType(nextType);
                if (nextType && queryResourceId) {
                  const hasMatch = allRecords.some(
                    (row) => row.resourceType === nextType && row.resourceId === queryResourceId
                  );
                  if (!hasMatch) {
                    setQueryResourceId("");
                  }
                }
              }}
            >
              <option value="">All</option>
              {resourceTypeOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
          <label>
            Resource ID
            <select value={queryResourceId} onChange={(e) => setQueryResourceId(e.target.value)}>
              <option value="">All</option>
              {resourceIdOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
          <label>
            Event Type
            <select value={queryEventType} onChange={(e) => setQueryEventType(e.target.value)}>
              <option value="">All</option>
              {eventTypeOptions.map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </label>
          <label>From<input type="datetime-local" value={queryFrom} onChange={(e) => setQueryFrom(e.target.value)} /></label>
          <label>To<input type="datetime-local" value={queryTo} onChange={(e) => setQueryTo(e.target.value)} /></label>
          <label>Page<input type="number" min={0} value={queryPage} onChange={(e) => setQueryPage(Number(e.target.value))} /></label>
          <label>Size<input type="number" min={1} max={100} value={querySize} onChange={(e) => setQuerySize(Number(e.target.value))} /></label>
        </div>
        <label className="inline">
          <input type="checkbox" checked={queryIncludeArchived} onChange={(e) => setQueryIncludeArchived(e.target.checked)} />
          Include Archived
        </label>
        <div className="row">
          <button onClick={() => runAction(() => queryEvents(queryPage))} disabled={busy}>Search</button>
          <button onClick={() => runAction(exportBundleFromQuery)} disabled={busy}>Export</button>
          <button onClick={() => runAction(() => queryEvents(queryPage - 1))} disabled={busy || !canPrev}>Previous</button>
          <button onClick={() => runAction(() => queryEvents(queryPage + 1))} disabled={busy || !canNext}>Next</button>
        </div>

        <div className="tableWrap">
          <table>
            <thead>
              <tr>
                <th>Seq</th>
                <th>Event Type</th>
                <th>Actor</th>
                <th>Resource Type</th>
                <th>Resource ID</th>
                <th>Timestamp</th>
                <th>Record Hash</th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 ? (
                <tr><td colSpan={7} className="empty">No records</td></tr>
              ) : items.map((item) => (
                <tr key={`${item.sequenceNumber}-${item.id}`}>
                  <td>{item.sequenceNumber}</td>
                  <td>{item.eventType}</td>
                  <td>{item.actorId}</td>
                  <td>{item.resourceType}</td>
                  <td>{item.resourceId}</td>
                  <td>{item.timestamp}</td>
                  <td className="mono truncate">{item.recordHash}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <p className="meta">Page {queryResult?.page ?? 0} / {Math.max((queryResult?.totalPages ?? 1) - 1, 0)} | Total {queryResult?.totalElements ?? 0}</p>
      </section>

      <section className="panel">
        <h2>Verify Chain</h2>
        <button onClick={() => runAction(verifyChain)} disabled={busy}>Verify</button>
        {verifyResult ? (
          <div className="result">
            <p>Intact: {String(verifyResult.intact)}</p>
            <p>Checked Records: {verifyResult.checkedRecords}</p>
            <p>Violation Type: {verifyResult.violationType ?? "-"}</p>
            <p>First Bad Sequence: {verifyResult.firstBadSequenceNumber ?? "-"}</p>
          </div>
        ) : null}
      </section>

      <section className="panel">
        <h2>Retention and Redaction</h2>
        <div className="row blockTop">
          <label>Retention Days<input value={retentionDays} onChange={(e) => setRetentionDays(e.target.value)} /></label>
          <button onClick={() => runAction(runRetention)} disabled={busy}>Run Retention</button>
        </div>
        {retentionResult ? (
          <p className="meta">Archived: {retentionResult.archivedCount} | Retention Days: {retentionResult.retentionDays}</p>
        ) : null}

        <div className="grid cols4 blockTop">
          <label>Sequence Number<input value={redactSequenceNumber} onChange={(e) => setRedactSequenceNumber(e.target.value)} /></label>
          <label>Fields (comma-separated)<input value={redactFields} onChange={(e) => setRedactFields(e.target.value)} /></label>
          <label>Reason<input value={redactReason} onChange={(e) => setRedactReason(e.target.value)} /></label>
          <label>Approved By<input value={redactApprovedBy} onChange={(e) => setRedactApprovedBy(e.target.value)} /></label>
        </div>
        <button onClick={() => runAction(applyRedaction)} disabled={busy}>Apply Redaction</button>
        {redactionResult ? (
          <div className="result">
            <p>Sequence Number: {redactionResult.sequenceNumber}</p>
            <p>State: {redactionResult.redactionState}</p>
            <p>Fields: {(redactionResult.redactedFields ?? []).join(", ")}</p>
          </div>
        ) : null}
      </section>

      <section className="panel">
        <h2>Verify Export Bundle</h2>
        <label>
          Export Bundle JSON
          <textarea rows={8} value={exportBundleText} onChange={(e) => setExportBundleText(e.target.value)} />
        </label>
        <button onClick={() => runAction(verifyExportBundle)} disabled={busy}>Verify Export Bundle</button>
        {exportVerifyResult ? (
          <div className="result">
            <p>Valid: {String(exportVerifyResult.valid)}</p>
            <p>Checked Records: {exportVerifyResult.checkedRecords}</p>
            <p>Violation Type: {exportVerifyResult.violationType ?? "-"}</p>
          </div>
        ) : null}
      </section>

      <div className="status">{busy ? "Working..." : status}</div>
    </div>
  );
}

export default App;
