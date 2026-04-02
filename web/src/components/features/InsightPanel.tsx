import { Sparkles } from 'lucide-react';
import { useEffect, useState } from 'react';

import { requestAiResponse } from '../../lib/groq';
import { Button } from '../ui/Button';
import { Card } from '../ui/Card';
import type { AIChatMessage, FinanceSnapshot } from '../../types';

interface InsightPanelProps {
  snapshot: FinanceSnapshot;
  month: string;
}

const suggestions = [
  'What did I spend most on?',
  "How's my savings rate?",
  'Am I on track for my goals?',
];

export function InsightPanel({ snapshot, month }: InsightPanelProps) {
  const [insight, setInsight] = useState<string>('');
  const [input, setInput] = useState<string>('');
  const [messages, setMessages] = useState<AIChatMessage[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);

  useEffect(() => {
    let isMounted = true;

    async function loadInsight() {
      setIsLoading(true);
      const response = await requestAiResponse({ type: 'monthly_insight', month }, snapshot);
      if (isMounted) {
        setInsight(response.content);
        setIsLoading(false);
      }
    }

    void loadInsight();

    return () => {
      isMounted = false;
    };
  }, [
    month,
    snapshot.accounts,
    snapshot.categories,
    snapshot.transactions,
    snapshot.transfers,
    snapshot.goals,
    snapshot.subscriptions,
    snapshot.settings,
  ]);

  async function submitQuestion(question: string) {
    if (!question.trim()) {
      return;
    }

    const nextMessages = [...messages, { role: 'user' as const, content: question }];
    setMessages(nextMessages);
    setInput('');
    setIsLoading(true);
    const response = await requestAiResponse(
      {
        type: 'chat',
        month,
        message: question,
        conversation: nextMessages,
      },
      snapshot,
    );
    setMessages([...nextMessages, { role: 'assistant', content: response.content }]);
    setIsLoading(false);
  }

  const insightLines = insight
    .split('\n')
    .map((line) => line.replace(/^•\s*/, '').trim())
    .filter(Boolean);

  return (
    <Card className="space-y-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="section-label">AI insights</p>
          <h3 className="mt-2 flex items-center gap-2 text-lg font-semibold text-[#F0F0F0]">
            <Sparkles className="h-4 w-4 text-purple-300" />
            Monthly read on your money
          </h3>
        </div>
      </div>

      <div className="space-y-3 rounded-xl border border-white/5 bg-subtle/50 p-4">
        {isLoading && insightLines.length === 0 ? (
          <p className="text-sm text-[#A0A0A0]">Loading insight...</p>
        ) : (
          insightLines.map((line) => (
            <p key={line} className="text-sm text-[#A0A0A0]">
              {line}
            </p>
          ))
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {suggestions.map((suggestion) => (
          <button
            key={suggestion}
            className="rounded-full border border-white/10 px-3 py-1.5 text-xs text-[#A0A0A0] transition-colors hover:bg-subtle hover:text-white"
            onClick={() => {
              void submitQuestion(suggestion);
            }}
            type="button"
          >
            {suggestion}
          </button>
        ))}
      </div>

      <div className="space-y-3">
        {messages.slice(-2).map((message, index) => (
          <div key={`${message.role}-${index}`} className="rounded-xl border border-white/5 bg-surface/60 p-4">
            <p className="section-label">{message.role === 'user' ? 'You' : 'Raqeem AI'}</p>
            <p className="mt-2 whitespace-pre-line text-sm text-[#A0A0A0]">{message.content}</p>
          </div>
        ))}
      </div>

      <div className="flex gap-3">
        <input
          className="input"
          onChange={(event) => setInput(event.target.value)}
          placeholder="Ask anything about your finances..."
          value={input}
        />
        <Button disabled={isLoading} onClick={() => void submitQuestion(input)} type="button">
          Ask
        </Button>
      </div>
    </Card>
  );
}
