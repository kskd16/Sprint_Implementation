# SmartSure Security Audit — Issues Found & Fixed

## Architecture: Distributed JWT Authentication

All services share the same JWT secret and validation logic.
The filter is defined independently in each service (Spring Boot requires this per-service).
All filters are identical — JWT-only, no headers required from clients.

## JWT Flow
Client → API Gateway → Service → JwtAuthFilter validates token → sets SecurityContext
Principal = userId (String of Long)
Authority = ROLE_CUSTOMER or ROLE_ADMIN

---

## Issues Found & Fixed

### 1. policyService JwtAuthFilter — FIXED
Problem: Had X-User-Id + X-User-Role header fallback before JWT.
         If headers were present (e.g. from a proxy), they would override JWT.
         This is a security vulnerability — anyone could spoof their role via headers.
Fix: Removed header fallback entirely. JWT-only now.

### 2. PolicyController — FIXED
Problem: @RequestHeader("X-User-Id") on purchase, my, getPolicyById, cancel, renew, payPremium.
         Spring throws 400 MissingRequestHeaderException if header absent.
Fix: All replaced with @AuthenticationPrincipal String userId + Long.parseLong(userId).
     Role check for getPolicyById uses SecurityContextHolder.getAuthentication().getAuthorities().

### 3. AdminController — FIXED
Problem: @RequestHeader("X-Admin-Id") on review, approve, reject, cancelPolicy.
         Same 400 issue as PolicyController.
Fix: All replaced with @AuthenticationPrincipal String adminId + Long.parseLong(adminId).

### 4. ClaimController — FIXED
Problem: No @PreAuthorize on any endpoint — any authenticated user could:
         - Call GET /api/claims (all claims — should be ADMIN only)
         - Call PUT /{id}/status (approve/reject — should be ADMIN only)
         - Call GET /under-review (should be ADMIN only)
Fix: Added proper @PreAuthorize to every endpoint:
     - CUSTOMER: createClaim, uploadDocs, submit, deleteDraft, downloadDocs
     - ADMIN: getAllClaims, getUnderReview, moveToStatus
     - Both: getClaimById, getPolicyForClaim, downloadDocs

### 5. ClaimService.createClaim — FIXED
Problem: createClaim(ClaimRequest) ignored who the customer was.
         Any authenticated user could create a claim for any policyId.
Fix: createClaim(Long customerId, ClaimRequest) — customerId from JWT principal.
     Logged for audit trail.

### 6. ClaimController.createClaim — FIXED
Problem: Was not passing userId to service.
Fix: @AuthenticationPrincipal String userId passed to claimService.createClaim().

---

## Final Authorization Matrix

| Endpoint                              | CUSTOMER | ADMIN |
|---------------------------------------|----------|-------|
| POST /api/auth/register               | public   | public |
| POST /api/auth/login                  | public   | public |
| GET  /api/policy-types                | public   | public |
| POST /api/policies/calculate-premium  | public   | public |
| POST /api/policy-types                | ✗        | ✓ |
| PUT  /api/policy-types/{id}           | ✗        | ✓ |
| DELETE /api/policy-types/{id}         | ✗        | ✓ |
| POST /api/policies/purchase           | ✓        | ✗ |
| GET  /api/policies/my                 | ✓        | ✗ |
| GET  /api/policies/{id}               | ✓ (own)  | ✓ (any) |
| PUT  /api/policies/{id}/cancel        | ✓        | ✗ |
| POST /api/policies/renew              | ✓        | ✗ |
| GET  /api/policies/{id}/premiums      | ✓        | ✓ |
| GET  /api/policies/admin/all          | ✗        | ✓ |
| PUT  /api/policies/admin/{id}/status  | ✗        | ✓ |
| GET  /api/policies/admin/summary      | ✗        | ✓ |
| POST /api/payments/initiate           | ✓        | ✗ |
| POST /api/payments/confirm            | ✓        | ✗ |
| POST /api/payments/fail               | ✓        | ✗ |
| GET  /api/payments/my                 | ✓        | ✗ |
| GET  /api/payments/{id}               | ✓        | ✓ |
| GET  /api/payments/policy/{id}        | ✓        | ✓ |
| POST /api/claims                      | ✓        | ✗ |
| GET  /api/claims/{id}                 | ✓        | ✓ |
| GET  /api/claims                      | ✗        | ✓ |
| GET  /api/claims/under-review         | ✗        | ✓ |
| PUT  /api/claims/{id}/submit          | ✓        | ✗ |
| PUT  /api/claims/{id}/status          | ✗        | ✓ |
| POST /api/claims/{id}/upload/*        | ✓        | ✗ |
| GET  /api/claims/{id}/download/*      | ✓        | ✓ |
| DELETE /api/claims/{id}               | ✓        | ✗ |
| GET  /api/admin/**                    | ✗        | ✓ |
| PUT  /api/admin/claims/{id}/approve   | ✗        | ✓ |
| PUT  /api/admin/claims/{id}/reject    | ✗        | ✓ |

---

## How JWT Principal Works in Each Service

JwtAuthFilter sets:
  principal = userId (String)   ← use @AuthenticationPrincipal String userId
  authority = ROLE_CUSTOMER or ROLE_ADMIN

In controllers:
  @AuthenticationPrincipal String userId  → gives you the userId as String
  Long.parseLong(userId)                  → converts to Long for service calls
  @PreAuthorize("hasRole('CUSTOMER')")    → checks ROLE_CUSTOMER authority
  @PreAuthorize("hasRole('ADMIN')")       → checks ROLE_ADMIN authority
  @PreAuthorize("isAuthenticated()")      → any valid JWT

No X-User-Id, X-User-Role, or X-Admin-Id headers needed anywhere.
