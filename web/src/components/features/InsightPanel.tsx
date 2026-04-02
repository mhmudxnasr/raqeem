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
    <Card className="glass-card flex flex-col border-white/5 p-6 h-fit sticky top-24">
      <div className="flex items-start justify-between gap-4 mb-6">
        <div>
          <p className="eyebrow">Intelligence</p>
          <h3 className="mt-1 flex items-center gap-2 text-lg font-bold tracking-tight text-white">
            <Sparkles className="h-4 w-4 text-purple-400 animate-glow" />
            AI Finance Insights
          </h3>
        </div>
      </div>

      <div className="space-y-4 mb-8">
        <div className="rounded-2xl border border-white/5 bg-white/5 p-5 backdrop-blur-sm relative overflow-hidden">
          <div className="absolute -left-4 -top-4 h-16 w-16 rounded-full bg-purple-500/5 blur-xl"></div>
          {isLoading && insightLines.length === 0 ? (
            <div className="flex items-center gap-3 py-2 text-sm text-[#A0A0A0]">
              <div className="h-2 w-2 animate-bounce rounded-full bg-purple-500"></div>
              <div className="h-2 w-2 animate-bounce rounded-full bg-purple-500 delay-75"></div>
              <div className="h-2 w-2 animate-bounce rounded-full bg-purple-500 delay-150"></div>
            </div>
          ) : error ? (
            <p className="text-sm text-rose-400 font-medium">{error}</p>
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
        <p className="eyebrow mb-4">Prompt Library</p>
        <div className="flex flex-wrap gap-2">
          {suggestions.map((suggestion) => (
            <button
              key={suggestion}
              className="rounded-full border border-white/5 bg-white/5 px-4 py-2 text-[11px] font-semibold text-[#A0A0A0] transition-all hover:bg-purple-500/10 hover:text-purple-300 hover:border-purple-500/20 active:scale-95"
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
            className={`rounded-2xl p-4 border border-white/5 ${
              message.role === 'user' 
                ? "bg-white/5 ml-4" 
                : "bg-purple-500/5 mr-4"
            }`}
          >
            <p className="eyebrow mb-2">{message.role === 'user' ? 'You' : 'Raqeem AI'}</p>
            <p className="whitespace-pre-line text-sm leading-relaxed text-[#A0A0A0]">{message.content}</p>
          </div>
        ))}
      </div>

      <div className="mt-auto flex gap-3">
        <input
          className="flex-1 rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-white placeholder-[#5A5A5A] transition-all focus:border-purple-500/50 focus:outline-none focus:ring-1 focus:ring-purple-500/20"
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
