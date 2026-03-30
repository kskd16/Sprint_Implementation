# 🚀 SmartSure Insurance - Postman API Test Guide

## 📥 Import Collection

1. Open **Postman**
2. Click **Import** → Select **SmartSure_Postman_Collection.json**
3. Collection will be imported with all environment variables pre-configured

---

## 🔑 TOKENS QUICK REFERENCE

### **CUSTOMER TOKEN** (After Login)
```
Token Type: Bearer
Use For: Customer operations (buy policies, file claims, pay premiums)
Set By: Request "1.3 Login Customer - Get CUSTOMER_TOKEN"
Stored In: {{CUSTOMER_TOKEN}}
```

### **ADMIN TOKEN** (After Login)
```
Token Type: Bearer
Use For: Admin operations (review claims, manage policies, audit logs)
Set By: Request "1.4 Login Admin - Get ADMIN_TOKEN"
Stored In: {{ADMIN_TOKEN}}
```

---

## 📋 COMPLETE TEST WORKFLOW

### **STEP 1: SETUP (Section 1️⃣)**

```
1.1 Register Customer (use REAL email: customer@gmail.com)
    ↓
1.2 Register Admin (admin@smartsure.com)
    ↓
1.3 Login Customer → Saves {{CUSTOMER_TOKEN}} & {{CUSTOMER_ID}}
    ↓
1.4 Login Admin → Saves {{ADMIN_TOKEN}} & {{ADMIN_ID}}
    ↓
1.5 Health Check (verify services running)
    ↓
1.6 Add Customer Info
    ↓
1.7 Get Customer Info
    ↓
1.8 Add Customer Address
```

### **STEP 2: BROWSE POLICIES (Section 2️⃣)**

```
2.1 Browse All Policy Types (NO TOKEN)
    ↓
2.2 Get Policy Type by ID
    ↓
2.3 Calculate Premium (NO TOKEN) - Check cost before buying
    ↓
2.4 Purchase Policy → Saves {{POLICY_ID}}
    ↓
2.5 Get My Policies
    ↓
2.6 Get Policy Details
    ↓
2.7 Get Premium Schedule
```

### **STEP 3: FILE INSURANCE CLAIM (Section 3️⃣)**

```
3.1 Create DRAFT Claim → Saves {{CLAIM_ID}}
    ↓
3.2 Upload Claim Form Document (PDF)
    ↓
3.3 Upload Aadhaar Card (JPG/PNG)
    ↓
3.4 Upload Evidence Document (JPG/PNG)
    ↓
3.5 Submit Claim (switches to UNDER_REVIEW)
    ↓
3.6 Get Claim Details
    ↓
3.7 Download Claim Form (verify upload worked)
```

### **STEP 4: ADMIN REVIEWS CLAIM (Section 4️⃣)**

```
Use {{ADMIN_TOKEN}}

4.1 Get All Claims (ADMIN view)
    ↓
4.2 Get Claims Under Review (pending claims)
    ↓
4.3 Get Specific Claim
    ↓
4.4 Mark as Under Review
    ↓
4.5 APPROVE CLAIM 📧 → EMAIL SENT TO CUSTOMER
    OR
4.6 REJECT CLAIM 📧 → EMAIL SENT TO CUSTOMER
```

### **STEP 5: ADMIN OTHER OPERATIONS (Section 5️⃣)**

```
Use {{ADMIN_TOKEN}}

5.1 Get All Users
    ↓
5.2 Get User by ID
    ↓
5.3 Get All Policies
    ↓
5.4 Get All Audit Logs
    ↓
5.5 Get Recent Audit Logs
```

### **STEP 6: PROCESS PAYMENT (Section 6️⃣)**

```
Use {{CUSTOMER_TOKEN}}

6.1 Initiate Payment → Saves {{RAZORPAY_ORDER_ID}} & {{PAYMENT_ID}}
    ↓
    [Frontend calls Razorpay checkout with razorpayOrderId]
    ↓
6.2 Confirm Payment (after Razorpay success)
    OR
6.3 Fail Payment (if user dismisses)
    ↓
6.4 Get Payment Details
    ↓
6.5 Get My Payment History
```

---

## 🧪 TEST DATA TO USE

### **For Registration**

```json
CUSTOMER:
{
  "email": "customer@gmail.com",
  "password": "SecurePass123!",
  "firstName": "Raj",
  "lastName": "Kumar",
  "role": "CUSTOMER"
}

ADMIN:
{
  "email": "admin@smartsure.com",
  "password": "AdminPass123!",
  "firstName": "Admin",
  "lastName": "User",
  "role": "ADMIN"
}
```

### **For User Info**

```json
{
  "firstName": "Raj",
  "lastName": "Kumar",
  "phoneNumber": "9876543210",
  "dateOfBirth": "1990-05-15"
}
```

### **For Address**

```json
{
  "street": "123 Marine Drive",
  "city": "Mumbai",
  "state": "Maharashtra",
  "zipCode": "400020",
  "country": "India"
}
```

### **For Claim**

```json
{
  "policyId": 1,
  "description": "Accidental damage to car during monsoon. Need windshield replacement."
}
```

### **For Claim Approval** 📧

```json
{
  "remarks": "All documents verified. Claim amount approved: ₹50,000. Payment will be processed within 3-5 business days."
}
```

### **For Claim Rejection** 📧

```json
{
  "remarks": "Claim rejected - Evidence does not match policy coverage. Please contact support for appeal process."
}
```

### **For Payment**

```json
{
  "policyId": 1,
  "amount": 5000,
  "description": "Premium payment for Health Plus policy"
}
```

### **For Payment Confirmation** 💳

```json
{
  "razorpayOrderId": "{{RAZORPAY_ORDER_ID}}",
  "razorpayPaymentId": "pay_Jda834jd2334",
  "razorpaySignature": "9ef4dffbfd84f1318f6739a3ce19f9d85851857ae648f114332d8401e0949a3d"
}
```

---

## ✅ EXPECTED RESPONSES

### **Login Response** (Get Token)
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "customer@gmail.com",
  "userId": 1,
  "role": "CUSTOMER"
}
```

### **Purchase Policy Response**
```json
{
  "id": 1,
  "customerId": 1,
  "policyTypeId": 1,
  "status": "ACTIVE",
  "startDate": "2026-04-01",
  "endDate": "2027-03-31",
  "totalPremium": 60000
}
```

### **Create Claim Response**
```json
{
  "id": 1,
  "customerId": 1,
  "policyId": 1,
  "status": "DRAFT",
  "description": "Accidental damage...",
  "createdAt": "2026-03-30T10:00:00"
}
```

### **Approve Claim Response** (Email Sent!)
```json
{
  "id": 1,
  "status": "APPROVED",
  "approvedBy": 2,
  "remarks": "All documents verified...",
  "approvedAt": "2026-03-30T11:00:00"
}
```

### **Initiate Payment Response**
```json
{
  "id": 1,
  "razorpayOrderId": "order_xyz123",
  "razorpayKeyId": "rzp_live_abc123",
  "amount": 5000,
  "status": "INITIATED"
}
```

---

## 🎯 IMPORTANT NOTES

### **📧 EMAIL SERVICE**
- Approval/Rejection notifications are sent to customer's registered email
- **Use REAL email when registering** to test email feature
- Configure SMTP in `application.properties` for each service:
  ```properties
  spring.mail.host=smtp.gmail.com
  spring.mail.port=587
  spring.mail.username=YOUR_EMAIL@gmail.com
  spring.mail.password=YOUR_APP_PASSWORD
  ```

### **🔐 Authorization Header**
All authenticated endpoints automatically use:
```
Authorization: Bearer {{CUSTOMER_TOKEN}}
```
or
```
Authorization: Bearer {{ADMIN_TOKEN}}
```

### **🚀 Service URLs**
All base URLs configured in collection variables:
- Auth Service: `http://localhost:8081`
- Policy Service: `http://localhost:8082`
- Claim Service: `http://localhost:8083`
- Admin Service: `http://localhost:8084`
- Payment Service: `http://localhost:8085`

### **📁 File Uploads**
For document uploads (claim form, Aadhaar, evidence):
1. Click "Select Files" in the request body
2. Choose your PDF/image file
3. Send the request

### **💳 Razorpay Testing**
For testing without real Razorpay:
- Use test keys from Razorpay dashboard
- Test payment IDs: `pay_Jda834jd2334` (example)
- Signature can be any value for testing

### **⏰ JWT Token Duration**
- Tokens typically expire after 24 hours
- If expired, re-login to get new token
- Auto-saved in {{CUSTOMER_TOKEN}} or {{ADMIN_TOKEN}}

---

## 🔍 DEBUGGING TIPS

### **If getting 401 Unauthorized:**
- Login first (1.3 or 1.4)
- Check if token is saved: Look at {{CUSTOMER_TOKEN}} value
- Re-run login request to refresh token

### **If getting 403 Forbidden:**
- Using CUSTOMER_TOKEN for ADMIN-only endpoint?
- Switch to {{ADMIN_TOKEN}}
- Ensure role is set correctly during registration

### **If file upload fails:**
- Check file size (should be < 50MB)
- Ensure file format is correct (PDF, JPG, PNG)
- Try with a small test file first

### **If claim submission fails:**
- Verify all 3 documents uploaded (form, Aadhaar, evidence)
- Claim must be in DRAFT status
- Check claim ID is correct

### **If email not received:**
- Verify SMTP configuration
- Check spam/junk folder
- Ensure customer registered with valid email
- Check service logs for email errors

---

## 📊 QUICK SUMMARY TABLE

| Feature | Token | Role | API |
|---------|-------|------|-----|
| Register | ❌ | - | `POST /api/auth/register` |
| Login | ❌ | - | `POST /api/auth/login` |
| Browse Policies | ❌ | - | `GET /api/policy-types` |
| Buy Policy | ✅ | CUSTOMER | `POST /api/policies/purchase` |
| File Claim | ✅ | CUSTOMER | `POST /api/claims` |
| Upload Docs | ✅ | CUSTOMER | `POST /api/claims/{id}/upload/*` |
| Submit Claim | ✅ | CUSTOMER | `PUT /api/claims/{id}/submit` |
| Review Claims | ✅ | ADMIN | `GET /api/admin/claims` |
| Approve Claim 📧 | ✅ | ADMIN | `PUT /api/admin/claims/{id}/approve` |
| Reject Claim 📧 | ✅ | ADMIN | `PUT /api/admin/claims/{id}/reject` |
| Initiate Payment | ✅ | CUSTOMER | `POST /api/payments/initiate` |
| Confirm Payment | ✅ | CUSTOMER | `POST /api/payments/confirm` |
| View Audit Logs | ✅ | ADMIN | `GET /api/admin/audit-logs` |

---

## 🎓 EXAMPLE END-TO-END TEST FLOW

```
1. Run 1.3 (Login Customer) → Get {{CUSTOMER_TOKEN}}
2. Run 2.1 (Browse Policies)
3. Run 2.3 (Calculate Premium)
4. Run 2.4 (Purchase Policy) → Get {{POLICY_ID}}
5. Run 3.1 (Create Claim) → Get {{CLAIM_ID}}
6. Run 3.2, 3.3, 3.4 (Upload 3 docs)
7. Run 3.5 (Submit Claim)
8. Run 1.4 (Login Admin) → Get {{ADMIN_TOKEN}}
9. Run 4.1 (View all claims)
10. Run 4.5 (APPROVE CLAIM) → 📧 Email sent!
11. Run 6.1 (Initiate Payment) → Get {{PAYMENT_ID}}
12. Run 6.2 (Confirm Payment)
```

---

**Happy Testing! 🎉**
