# AI Integration — Raqeem
## Groq API via Supabase Edge Function

---

## Architecture

**NEVER expose the Groq API key to the client (Android or Web).**

All Groq calls go through a **Supabase Edge Function** (`/functions/ai-insights`).
The client sends the user's financial data to the edge function. The edge function
builds the prompt, calls Groq, and returns the result.

```
Android/Web → Supabase Edge Function → Groq API → response → client
```

The edge function authenticates the request using the user's Supabase JWT.
Only authenticated requests are processed.

---

## Model Selection

Use **`llama3-70b-8192`** (Groq's fastest, highest-quality open model).
Context window: 8192 tokens — enough for a full month of transactions.

Fallback: `mixtral-8x7b-32768` if more context is needed (e.g., full year analysis).

---

## Edge Function: `ai-insights`

**Location:** `backend/supabase/functions/ai-insights/index.ts`

**Accepts:**
```typescript
interface AIRequest {
  type: 'monthly_insight' | 'chat';
  month?: string;         // YYYY-MM, for monthly_insight
  message?: string;       // user message, for chat
  conversation?: Array<{role: 'user'|'assistant', content: string}>;
}
```

**Process:**
1. Verify JWT from Authorization header
2. Fetch relevant data from Supabase (scoped to auth.uid())
3. Build system prompt with data context
4. Call Groq API
5. Return response

---

## Data Context Injection

The AI NEVER accesses the database directly.
The edge function fetches the data, formats it, and injects it into the system prompt.

```typescript
// Example context for monthly insight
const context = `
FINANCIAL DATA FOR ${month}:

ACCOUNTS:
- Binance Savings: $3,240.00 (saving)
- Binance Free: $180.50 (checking)
- Wallet: EGP 240.00 (~$4.62)
- Charity: $50.00 (checking)

TOTAL NET WORTH: ~$3,475.12

TRANSACTIONS THIS MONTH (${transactions.length} total):
${transactions.map(t =>
  `${t.date} | ${t.type.toUpperCase()} | ${t.category} | ${formatAmount(t.amountCents, t.currency)} | ${t.note || ''}`
).join('\n')}

MONTHLY SUMMARY:
- Total income: $2,400.00
- Total expenses: $312.50
- Savings this month: $2,087.50
- Savings rate: 87%

BUDGET STATUS:
${budgets.map(b =>
  `${b.category}: $${b.spent} / $${b.budget} (${b.pct}%)`
).join('\n')}
`;
```

---

## System Prompts

### Monthly Insight Prompt

```
You are Raqeem, a personal finance assistant. You have access to the user's
complete financial data for the requested month.

RULES:
1. Only reference numbers that appear in the provided financial data. 
   Never make up figures.
2. Be concise — 3 to 5 bullet points maximum for monthly insights.
3. Be direct and specific. "You spent $93.50 on food (78% of budget)" 
   not "Your food spending was somewhat high."
4. If something looks positive, acknowledge it. If there's a concern, 
   name it clearly without being alarmist.
5. Do not add motivational fluff or generic advice unless asked.
6. Respond in the same language as the user's message. Default: English.
7. Format: plain text, no markdown headers. Bullet points only.

${context}
```

### Chat Bot Prompt

```
You are Raqeem, a personal finance assistant. You answer questions about 
the user's finances based only on the data provided.

RULES:
1. Only reference figures from the provided data. Never hallucinate numbers.
2. If asked something you don't have data for (e.g., last year's data when 
   you only have this month), say so clearly.
3. Be direct and helpful. Short answers unless the user asks for detail.
4. If the user asks for a recommendation, give one — don't hedge endlessly.
5. You can do math. Show your work briefly if it helps clarity.
6. Respond in the same language as the user's message.

${context}

Conversation history:
${conversationHistory}
```

---

## Monthly Insight Generation

Triggered automatically when:
1. User opens the Analytics screen AND
2. The current month's insight hasn't been generated yet (check local cache).

**Insight format returned:**
```typescript
interface MonthlyInsight {
  month: string;              // YYYY-MM
  highlights: string[];       // 3-5 bullet points
  savingsRate: number;        // percentage
  topCategory: string;        // biggest spend category
  anomalies: string[];        // unusual patterns, may be empty
  generatedAt: string;        // ISO timestamp
}
```

**Cache:** Store the monthly insight in Supabase (`insights` table or just in
AsyncStorage on Android / localStorage on web). Regenerate only once per day
per month (or on user request).

---

## Rate Limiting

Groq's free tier: 30 requests/minute, 14,400 requests/day.
This is a single-user app — no need for complex rate limiting.
Add a simple debounce on the chat: minimum 1 second between sends.

---

## Error Handling

```typescript
// Edge function response on error
{
  error: true,
  message: "AI unavailable — please try again later.",
  code: "GROQ_ERROR" | "TIMEOUT" | "INVALID_REQUEST"
}
```

On the client: show a quiet inline error state inside the AI panel.
Never crash the screen. The AI is an enhancement, not a core feature.

---

## What the AI Can and Cannot Answer

| Can answer | Cannot answer |
|---|---|
| "How much did I spend on X?" | "What will I spend next month?" |
| "What's my savings rate?" | "Is this a good investment?" |
| "Which day did I spend the most?" | "What's the current USD/EGP rate?" |
| "Am I on track for my goals?" | "What happened before the data period?" |
| "Summarize this month" | Generic financial advice (deflect) |
