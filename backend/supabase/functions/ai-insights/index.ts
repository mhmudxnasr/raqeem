// ============================================================
// Raqeem — ai-insights Edge Function
// Proxies requests to Groq API. Never exposes the API key.
// Stubbed for P1 — full implementation when AI features ship.
// ============================================================

import { serve } from 'https://deno.land/std@0.168.0/http/server.ts';
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2';

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
};

interface AIRequest {
  type: 'monthly_insight' | 'chat';
  month?: string;
  message?: string;
  conversation?: Array<{ role: 'user' | 'assistant'; content: string }>;
}

serve(async (req: Request) => {
  // CORS preflight
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders });
  }

  try {
    // Auth check
    const authHeader = req.headers.get('Authorization');
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: true, message: 'Unauthorized', code: 'INVALID_REQUEST' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      );
    }

    const supabase = createClient(
      Deno.env.get('SUPABASE_URL')!,
      Deno.env.get('SUPABASE_ANON_KEY')!,
      { global: { headers: { Authorization: authHeader } } },
    );

    const { data: { user } } = await supabase.auth.getUser();
    if (!user) {
      return new Response(
        JSON.stringify({ error: true, message: 'Unauthorized', code: 'INVALID_REQUEST' }),
        { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      );
    }

    const body: AIRequest = await req.json();
    const { type, month, message, conversation } = body;

    // Fetch user's financial data for context
    const currentMonth = month || new Date().toISOString().slice(0, 7);
    const startDate = `${currentMonth}-01`;
    const endDate = `${currentMonth}-31`;

    const [
      { data: accounts },
      { data: transactions },
      { data: categories },
      { data: goals },
    ] = await Promise.all([
      supabase
        .from('accounts')
        .select('id, name, type, currency, balance_cents')
        .is('deleted_at', null),
      supabase
        .from('transactions')
        .select('id, type, amount_cents, currency, date, note, category_id')
        .gte('date', startDate)
        .lte('date', endDate)
        .is('deleted_at', null)
        .order('date', { ascending: true }),
      supabase
        .from('categories')
        .select('id, name, type, budget_cents')
        .is('deleted_at', null),
      supabase
        .from('goals')
        .select('id, name, target_cents, current_cents, is_completed')
        .is('deleted_at', null),
    ]);

    // Build context string
    const categoryMap = new Map((categories ?? []).map((c: any) => [c.id, c]));

    const totalIncome = (transactions ?? [])
      .filter((t: any) => t.type === 'income')
      .reduce((sum: number, t: any) => sum + t.amount_cents, 0);

    const totalExpense = (transactions ?? [])
      .filter((t: any) => t.type === 'expense')
      .reduce((sum: number, t: any) => sum + t.amount_cents, 0);

    const savingsRate = totalIncome > 0
      ? Math.round(((totalIncome - totalExpense) / totalIncome) * 100)
      : 0;

    const formatCents = (cents: number, currency = 'USD') => {
      const abs = Math.abs(cents) / 100;
      const symbol = currency === 'USD' ? '$' : 'EGP ';
      return `${symbol}${abs.toFixed(2)}`;
    };

    const context = `
FINANCIAL DATA FOR ${currentMonth}:

ACCOUNTS:
${(accounts ?? []).map((a: any) => `- ${a.name}: ${formatCents(a.balance_cents, a.currency)} (${a.type})`).join('\n')}

TRANSACTIONS THIS MONTH (${(transactions ?? []).length} entries):
${(transactions ?? []).map((t: any) => {
  const cat = categoryMap.get(t.category_id);
  return `${t.date} | ${t.type.toUpperCase()} | ${cat?.name ?? 'Uncategorized'} | ${formatCents(t.amount_cents, t.currency)} | ${t.note ?? ''}`;
}).join('\n')}

MONTHLY TOTALS:
- Income: ${formatCents(totalIncome)}
- Expenses: ${formatCents(totalExpense)}
- Net: ${formatCents(totalIncome - totalExpense)}
- Savings rate: ${savingsRate}%

BUDGET STATUS:
${(categories ?? [])
  .filter((c: any) => c.type === 'expense' && c.budget_cents)
  .map((c: any) => {
    const spent = (transactions ?? [])
      .filter((t: any) => t.type === 'expense' && t.category_id === c.id)
      .reduce((sum: number, t: any) => sum + t.amount_cents, 0);
    const pct = Math.round((spent / c.budget_cents) * 100);
    return `- ${c.name}: ${formatCents(spent)} / ${formatCents(c.budget_cents)} (${pct}%)${pct >= 100 ? ' OVER BUDGET' : pct >= 80 ? ' NEAR LIMIT' : ''}`;
  }).join('\n')}

GOALS:
${(goals ?? []).map((g: any) => `- ${g.name}: ${formatCents(g.current_cents)} / ${formatCents(g.target_cents)} (${Math.round((g.current_cents / g.target_cents) * 100)}%)${g.is_completed ? ' COMPLETED' : ''}`).join('\n')}
`.trim();

    // Build messages for Groq
    let messages: Array<{ role: string; content: string }>;

    if (type === 'monthly_insight') {
      messages = [
        {
          role: 'system',
          content: `You are Raqeem, a personal finance assistant. You have access to the user's complete financial data for the requested month.

RULES:
1. Only reference numbers that appear in the provided financial data. Never make up figures.
2. Be concise — 3 to 5 bullet points maximum for monthly insights.
3. Be direct and specific. "You spent $93.50 on food (78% of budget)" not "Your food spending was somewhat high."
4. If something looks positive, acknowledge it. If there's a concern, name it clearly without being alarmist.
5. Do not add motivational fluff or generic advice unless asked.
6. Respond in the same language as the user's message. Default: English.
7. Format: plain text, no markdown headers. Bullet points only.

${context}`,
        },
        { role: 'user', content: `Give me a brief financial summary for ${currentMonth}.` },
      ];
    } else {
      const prunedConversation = (conversation ?? []).slice(-10);
      messages = [
        {
          role: 'system',
          content: `You are Raqeem, a personal finance assistant. You answer questions about the user's finances based only on the data provided.

RULES:
1. Only reference figures from the provided data. Never hallucinate numbers.
2. If asked something you don't have data for, say so clearly.
3. Be direct and helpful. Short answers unless the user asks for detail.
4. If the user asks for a recommendation, give one — don't hedge endlessly.
5. You can do math. Show your work briefly if it helps clarity.
6. Respond in the same language as the user's message.

${context}`,
        },
        ...prunedConversation.map((m: any) => ({ role: m.role, content: m.content })),
        { role: 'user', content: message ?? '' },
      ];
    }

    // Call Groq
    const groqKey = Deno.env.get('GROQ_API_KEY');
    if (!groqKey) {
      return new Response(
        JSON.stringify({ error: true, message: 'AI not configured', code: 'GROQ_ERROR' }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      );
    }

    const groqResponse = await fetch('https://api.groq.com/openai/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${groqKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: 'llama-3.3-70b-versatile',
        messages,
        max_tokens: 800,
        temperature: 0.3,
      }),
    });

    if (!groqResponse.ok) {
      const errorText = await groqResponse.text();
      console.error('Groq API error:', errorText);
      return new Response(
        JSON.stringify({ error: true, message: 'AI unavailable — please try again later.', code: 'GROQ_ERROR' }),
        { status: 502, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
      );
    }

    const result = await groqResponse.json();
    const content = result.choices?.[0]?.message?.content ?? 'No response generated.';

    return new Response(
      JSON.stringify({ content }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
    );

  } catch (err) {
    console.error('Edge function error:', err);
    return new Response(
      JSON.stringify({ error: true, message: 'AI unavailable — please try again later.', code: 'TIMEOUT' }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } },
    );
  }
});
