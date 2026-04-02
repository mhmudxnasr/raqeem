import { createClient } from '@supabase/supabase-js';

const SUPABASE_URL = 'https://awzutywadsgvjjrksovy.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_kgEmVUOo60N4py6pEEdgYA_NQHTq6Xu';

const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

async function test() {
  const email = `test-${Date.now()}@example.com`;
  const password = 'TestPassword123!';

  console.log('Signing up...');
  const { data: authData, error: authError } = await supabase.auth.signUp({
    email,
    password,
  });

  if (authError) {
    console.error('Sign up error:', authError);
    return;
  }

  console.log('Sign up successful:', authData.user.id);
  const token = authData.session.access_token;

  console.log('Testing monthly_insight...');
  const res1 = await fetch(`${SUPABASE_URL}/functions/v1/ai-insights`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ type: 'monthly_insight', month: '2026-04' }),
  });

  const text1 = await res1.text();
  console.log('monthly_insight status:', res1.status);
  console.log('monthly_insight response:', text1);

  console.log('Testing chat...');
  const res2 = await fetch(`${SUPABASE_URL}/functions/v1/ai-insights`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      type: 'chat',
      month: '2026-04',
      message: 'What is my biggest spending risk this month?',
      conversation: [],
    }),
  });

  const text2 = await res2.text();
  console.log('chat status:', res2.status);
  console.log('chat response:', text2);
}

test();
