import { isSupabaseConfigured, supabase } from './supabase';
import type { AIRequestPayload, AIResponsePayload } from '../types';

export async function requestAiResponse(payload: AIRequestPayload): Promise<AIResponsePayload> {
  if (!isSupabaseConfigured || !supabase) {
    throw new Error('AI unavailable — Supabase is not configured.');
  }

  const { data, error } = await supabase.functions.invoke<{ content: string }>('ai-insights', {
    body: payload,
  });

  if (error || !data?.content) {
    throw new Error(error?.message ?? 'AI unavailable — the edge function did not return content.');
  }

  return {
    content: data.content,
    source: 'supabase',
  };
}
