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
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadInsight() {
      setIsLoading(true);
      setError(null);

      try {
        const response = await requestAiResponse({ type: 'monthly_insight', month });
        if (isMounted) {
          setInsight(response.content);
        }
      } catch (reason) {
        if (isMounted) {
          setInsight('');
          setError(reason instanceof Error ? reason.message : 'AI unavailable — please try again later.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
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
    setError(null);

    try {
      const response = await requestAiResponse({
        type: 'chat',
        month,
        message: question,
        conversation: nextMessages,
      });
      setMessages([...nextMessages, { role: 'assistant', content: response.content }]);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'AI unavailable — please try again later.');
    } finally {
      setIsLoading(false);
    }
  }

  const insightLines = insight
    .split('\n')
    .map((line) => line.replace(/^•\s*/, '').trim())
    .filter(Boolean);

  return (
    <Card className="flex flex-col h-fit sticky top-24">
      <div className="flex items-start justify-between gap-4 mb-6">
        <div>
          <p className="section-label">Intelligence</p>
          <h3 className="mt-1 flex items-center gap-2 text-base font-semibold tracking-tight text-white">
            <Sparkles className="h-4 w-4 text-purple-400" />
            AI Finance Insights
          </h3>
        </div>
      </div>

      <div className="space-y-4 mb-8">
        <div className="rounded-xl border border-white/5 bg-elevated p-5">
          {isLoading && insightLines.length === 0 ? (
            <div className="flex items-center gap-2 py-2 text-sm text-[#A0A0A0]">
              <div className="h-1.5 w-1.5 animate-pulse rounded-full bg-purple-500"></div>
              <span className="text-xs text-[#5A5A5A]">Analyzing your data...</span>
            </div>
          ) : error ? (
            <p className="text-sm text-negative">{error}</p>
          ) : (
            <div className="space-y-3">
              {insightLines.map((line, i) => (
                <p key={i} className="text-sm leading-relaxed text-[#A0A0A0] last:mb-0">
                  {line}
                </p>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="mb-8 overflow-hidden">
        <p className="section-label mb-4">Prompt Library</p>
        <div className="flex flex-wrap gap-2">
          {suggestions.map((suggestion) => (
            <button
              key={suggestion}
              className="rounded-lg border border-white/5 bg-elevated px-3 py-2 text-[11px] font-semibold text-[#A0A0A0] transition-colors hover:bg-subtle hover:text-white"
              onClick={() => {
                void submitQuestion(suggestion);
              }}
              type="button"
            >
              {suggestion}
            </button>
          ))}
        </div>
      </div>

      <div className="space-y-4 mb-6">
        {messages.slice(-2).map((message, index) => (
          <div 
            key={`${message.role}-${index}`} 
            className={`rounded-xl p-4 border border-white/5 ${
              message.role === 'user' 
                ? "bg-elevated ml-4" 
                : "bg-subtle mr-4"
            }`}
          >
            <p className="section-label mb-2">{message.role === 'user' ? 'You' : 'Raqeem AI'}</p>
            <p className="whitespace-pre-line text-sm leading-relaxed text-[#A0A0A0]">{message.content}</p>
          </div>
        ))}
      </div>

      <div className="mt-auto flex gap-3">
        <input
          className="input flex-1"
          onChange={(event) => setInput(event.target.value)}
          placeholder="Ask about your trajectory..."
          value={input}
          onKeyDown={(e) => e.key === 'Enter' && !isLoading && void submitQuestion(input)}
        />
        <Button 
          disabled={isLoading || !input.trim()} 
          onClick={() => void submitQuestion(input)} 
          type="button"
          className="px-6 h-11"
        >
          Send
        </Button>
      </div>
    </Card>
  );
}
