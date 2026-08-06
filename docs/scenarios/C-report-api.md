# Scenario C - Concrete Compliance Report API Requirements

## Endpoint

1. `GET /audit/compliance/report`

## Required Query Parameters

1. `from` (ISO-8601 datetime)
2. `to` (ISO-8601 datetime)
3. At least one scope filter:
   - `actorId`, or
   - `resourceId`

## Optional Query Parameters

1. `includeArchived` (boolean, default `true`)
2. `page` (integer, default `0`)
3. `size` (integer, default `20`)

## Fixed Resource Scope

1. Prototype report scope is constrained to `resourceType = ACCOUNT`.
2. Non-account resources are excluded from this report endpoint.

## Response Requirements

1. Include generated timestamp and applied filter context.
2. Include paginated report items with:
   - sequenceNumber
   - eventType
   - actorId
   - resourceId
   - payload (redaction-safe view)
   - timestamp
   - recordHash
3. Include integrity snapshot:
   - sourceChainIntact
   - sourceCheckedRecords
   - firstSequenceNumber
   - lastSequenceNumber

## Validation Rules

1. `from` and `to` are both required.
2. `from` must be less than or equal to `to`.
3. Request without `actorId` and without `resourceId` is rejected.

## Error Contract

1. Invalid query combinations return `400` with:
   - `error = REQUEST_ERROR`
   - human-readable `message`

## Security and Evidence

1. Response payload uses the same redaction masking behavior as query/export endpoints.
2. Report includes `recordHash` for each item to support external traceability.
3. Chain integrity summary is included to indicate current global tamper-evidence state.
