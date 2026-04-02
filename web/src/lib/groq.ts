import { answerFinanceQuestion, buildMonthlyInsights, getCurrentMonthKey } from './analytics';
import { isSupabaseConfigured, supabase } from './supabase';
import type { AIRequestPayload, AIResponsePayload, FinanceSnapshot } from '../types';

export async function requestAiResponse(
  payload: AIRequestPayload,
  snapshot: FinanceSnapshot,
): Promise<AIResponsePayload> {
  if (isSupabaseConfigured && supabase) {
    const { data, error } = await supabase.functions.invoke<{ content: string }>('ai-insights', {
      body: payload,
    });

    if (!error && data?.content) {
      return {
        content: data.content,
        source: 'supabase',
      };
    }
  }

  const month = payload.month ?? getCurrentMonthKey();

  if (payload.type === 'monthly_insight') {
    return {
      content: buildMonthlyInsights(snapshot, month)
        .map((line) => `• ${line}`)
        .join('\n'),
      source: 'mock',
    };
  }

  return {
    content: answerFinanceQuestion(snapshot, month, payload.message ?? ''),
    source: 'mock',
  };
}
