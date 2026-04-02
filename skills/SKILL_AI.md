---
name: SKILL_AI
description: >
  Read this for ALL AI-related work: Groq API integration, prompt engineering,
  context injection, the Supabase Edge Function, and the AI chat UI on both
  platforms. Full spec in docs/AI_INTEGRATION.md — this skill adds implementation.
---

# AI Skill — Raqeem

## Read First

Full architecture and prompt specs are in `docs/AI_INTEGRATION.md`.
This skill adds the implementation details.

---

## The Golden Rule

**The AI must never make up numbers.**

Before calling Groq, always:
1. Fetch the actual data from Supabase (server) or Room (Android)
2. Format it into a structured context string
3. Inject it into the system prompt
4. Only THEN call Groq

If the data fetch fails, show an error — do NOT call Groq with empty context.

---

## Edge Function Call — Web

```typescript
// lib/groq.ts
export async function getMonthlyInsight(month: string): Promise<string> {
  const { data, error } = await supabase.functions.invoke('ai-insights', {
    body: { type: 'monthly_insight', month },
  });
  
  if (error) throw new Error('AI unavailable');
  return data.content as string;
}

export async function sendChatMessage(
  message: string,
  conversation: Array<{role: string, content: string}>,
  month: string,
): Promise<string> {
  const { data, error } = await supabase.functions.invoke('ai-insights', {
    body: { type: 'chat', message, conversation, month },
  });
  
  if (error) throw new Error('AI unavailable');
  return data.content as string;
}
```

---

## Edge Function Call — Android (Kotlin)

```kotlin
// data/remote/AiRepository.kt
class AiRepository @Inject constructor(
  private val supabaseClient: SupabaseClient,
) {
  suspend fun getMonthlyInsight(month: String): Result<String> {
    return try {
      val response = supabaseClient.functions.invoke(
        function = "ai-insights",
        body = buildJsonObject {
          put("type", "monthly_insight")
          put("month", month)
        }
      )
      val json = Json.parseToJsonElement(response.body!!).jsonObject
      Result.Success(json["content"]!!.jsonPrimitive.content)
    } catch (e: Exception) {
      Result.Error("AI unavailable", e)
    }
  }
}
```

---

## Context Builder

```typescript
// lib/buildAiContext.ts

interface FinancialContext {
  accounts: Account[];
  transactions: Transaction[];
  budgets: Array<{ category: string; spent: number; budget: number; pct: number }>;
  month: string;
}

export function buildContext(ctx: FinancialContext): string {
  const totalIncome = ctx.transactions
    .filter(t => t.type === 'income')
    .reduce((sum, t) => sum + (t.currency === 'USD' ? t.amountCents : Math.round(t.amountCents / 52)), 0);
  
  const totalExpense = ctx.transactions
    .filter(t => t.type === 'expense')
    .reduce((sum, t) => sum + (t.currency === 'USD' ? t.amountCents : Math.round(t.amountCents / 52)), 0);

  const savingsRate = totalIncome > 0
    ? Math.round(((totalIncome - totalExpense) / totalIncome) * 100)
    : 0;

  return `
FINANCIAL DATA FOR ${ctx.month}:

ACCOUNTS:
${ctx.accounts.map(a =>
  `- ${a.name}: ${formatAmount(a.balanceCents, a.currency)} (${a.type})`
).join('\n')}

NET WORTH (approx): ${formatAmount(
  ctx.accounts.reduce((sum, a) => sum + (a.currency === 'USD' ? a.balanceCents : Math.round(a.balanceCents / 52)), 0),
  'USD'
)}

TRANSACTIONS THIS MONTH (${ctx.transactions.length} entries):
${ctx.transactions.map(t =>
  `${t.date} | ${t.type.toUpperCase()} | ${(t as any).category?.name ?? 'Uncategorized'} | ${formatAmount(t.amountCents, t.currency)} | ${t.note ?? ''}`
).join('\n')}

MONTHLY TOTALS:
- Income: ${formatAmount(totalIncome, 'USD')}
- Expenses: ${formatAmount(totalExpense, 'USD')}
- Savings: ${formatAmount(totalIncome - totalExpense, 'USD')}
- Savings rate: ${savingsRate}%

BUDGET STATUS:
${ctx.budgets.map(b =>
  `- ${b.category}: ${formatAmount(b.spent * 100, 'USD')} / ${formatAmount(b.budget * 100, 'USD')} (${b.pct}%)${b.pct >= 100 ? ' ⚠ OVER BUDGET' : b.pct >= 80 ? ' ⚠ NEAR LIMIT' : ''}`
).join('\n')}
`.trim();
}
```

---

## AI Chat UI (Android)

```kotlin
// A simple chat composable
// Messages are stored in ViewModel as a List<ChatMessage>
// Not persisted — ephemeral per session

data class ChatMessage(
  val role: String,  // "user" | "assistant"
  val content: String,
  val timestamp: Instant = Instant.now(),
)

@Composable
fun AiBotScreen(viewModel: AiBotViewModel = hiltViewModel()) {
  val state by viewModel.uiState.collectAsState()
  
  Scaffold(
    topBar = { /* "Financial Assistant" header */ },
    bottomBar = {
      ChatInput(
        value = state.inputText,
        onValueChange = viewModel::onInputChange,
        onSend = viewModel::sendMessage,
        isLoading = state.isTyping,
      )
    }
  ) { padding ->
    if (state.messages.isEmpty()) {
      SuggestedPrompts(
        prompts = listOf(
          "What did I spend most on this month?",
          "How's my savings rate?",
          "Am I on track for my goals?",
          "Summarize this month for me",
        ),
        onPromptClick = viewModel::sendMessage,
      )
    } else {
      LazyColumn(reverseLayout = true) {
        items(state.messages.reversed()) { msg ->
          ChatBubble(message = msg)
        }
        if (state.isTyping) item { TypingIndicator() }
      }
    }
  }
}
```

---

## AI Chat UI (Web)

Same structure as Android but rendered as a right sidebar panel on the Analytics page,
or can be opened full-page via a "Full screen" button.

The panel has a fixed max-height with scrolling. Input is pinned to the bottom.

---

## Token Usage Management

Each chat message sends:
- System prompt: ~300 tokens
- Context (monthly data): ~500–800 tokens
- Conversation history: up to last 10 messages
- User message: varies

Total per call: ~1,000–2,000 tokens.
With llama3-70b on Groq free tier (14,400 req/day), this is more than enough
for a single user.

**Prune conversation history to last 10 messages** before sending:
```typescript
const pruned = conversation.slice(-10);
```

---

## AI Failure States

The AI panel always shows one of:
1. **Loaded** — insight bullet points or chat messages
2. **Loading** — animated typing indicator (3 pulsing dots)
3. **Error** — "AI unavailable right now. Your data is fine." + retry button
4. **Empty** (chat, no messages) — suggested prompts

Never show a crash, never show a raw error from Groq, never leave the panel blank.
