export interface Database {
  public: {
    Tables: {
      accounts: {
        Row: {
          id: string;
          user_id: string;
          name: string;
          type: 'cash' | 'checking' | 'saving' | 'investment' | 'crypto';
          currency: 'USD' | 'EGP';
          initial_amount_cents: number;
          balance_cents: number;
          is_hidden: boolean;
          sort_order: number;
          created_at: string;
          updated_at: string;
          deleted_at: string | null;
        };
        Insert: {
          user_id: string;
          name: string;
          type: 'cash' | 'checking' | 'saving' | 'investment' | 'crypto';
          currency?: 'USD' | 'EGP';
          initial_amount_cents?: number;
          balance_cents?: number;
          is_hidden?: boolean;
          sort_order?: number;
          created_at?: string;
          updated_at?: string;
          deleted_at?: string | null;
        };
        Update: Partial<Database['public']['Tables']['accounts']['Row']>;
        Relationships: [];
      };
      categories: {
        Row: {
          id: string;
          user_id: string;
          name: string;
          type: 'income' | 'expense';
          icon: string;
          color: string;
          budget_cents: number | null;
          created_at: string;
          updated_at: string;
          deleted_at: string | null;
        };
        Insert: {
          user_id: string;
          name: string;
          type: 'income' | 'expense';
          icon?: string;
          color?: string;
          budget_cents?: number | null;
          created_at?: string;
          updated_at?: string;
          deleted_at?: string | null;
        };
        Update: Partial<Database['public']['Tables']['categories']['Row']>;
        Relationships: [];
      };
      transactions: {
        Row: {
          id: string;
          user_id: string;
          account_id: string;
          category_id: string | null;
          type: 'income' | 'expense';
          amount_cents: number;
          currency: 'USD' | 'EGP';
          note: string | null;
          date: string;
          receipt_url: string | null;
          created_at: string;
          updated_at: string;
          deleted_at: string | null;
        };
        Insert: {
          user_id: string;
          account_id: string;
          category_id?: string | null;
          type: 'income' | 'expense';
          amount_cents: number;
          currency?: 'USD' | 'EGP';
          note?: string | null;
          date: string;
          receipt_url?: string | null;
          created_at?: string;
          updated_at?: string;
          deleted_at?: string | null;
        };
        Update: Partial<Database['public']['Tables']['transactions']['Row']>;
        Relationships: [];
      };
      transfers: {
        Row: {
          id: string;
          user_id: string;
          from_account_id: string;
          to_account_id: string;
          from_amount_cents: number;
          to_amount_cents: number;
          from_currency: 'USD' | 'EGP';
          to_currency: 'USD' | 'EGP';
          exchange_rate: number;
          is_currency_conversion: boolean;
          goal_id: string | null;
          note: string | null;
          date: string;
          created_at: string;
          updated_at: string;
          deleted_at: string | null;
        };
        Insert: {
          user_id: string;
          from_account_id: string;
          to_account_id: string;
          from_amount_cents: number;
          to_amount_cents: number;
          from_currency: 'USD' | 'EGP';
          to_currency: 'USD' | 'EGP';
          exchange_rate?: number;
          is_currency_conversion?: boolean;
          goal_id?: string | null;
          note?: string | null;
          date: string;
          created_at?: string;
          updated_at?: string;
          deleted_at?: string | null;
        };
        Update: Partial<Database['public']['Tables']['transfers']['Row']>;
        Relationships: [];
      };
      goals: {
        Row: {
          id: string;
          user_id: string;
          name: string;
          target_cents: number;
          current_cents: number;
          currency: 'USD' | 'EGP';
          deadline: string | null;
          is_completed: boolean;
          icon: string;
          note: string | null;
          created_at: string;
          updated_at: string;
          deleted_at: string | null;
        };
        Insert: {
          user_id: string;
          name: string;
          target_cents: number;
          current_cents?: number;
          currency?: 'USD' | 'EGP';
          deadline?: string | null;
          is_completed?: boolean;
          icon?: string;
          note?: string | null;
          created_at?: string;
          updated_at?: string;
          deleted_at?: string | null;
        };
        Update: Partial<Database['public']['Tables']['goals']['Row']>;
        Relationships: [];
      };
      subscriptions: {
        Row: {
          id: string;
          user_id: string;
          account_id: string;
          category_id: string | null;
          name: string;
          amount_cents: number;
          currency: 'USD' | 'EGP';
          billing_cycle: 'weekly' | 'monthly' | 'yearly';
          next_billing_date: string;
          is_active: boolean;
          auto_log: boolean;
          created_at: string;
          updated_at: string;
          deleted_at: string | null;
        };
        Insert: {
          user_id: string;
          account_id: string;
          category_id?: string | null;
          name: string;
          amount_cents: number;
          currency?: 'USD' | 'EGP';
          billing_cycle: 'weekly' | 'monthly' | 'yearly';
          next_billing_date: string;
          is_active?: boolean;
          auto_log?: boolean;
          created_at?: string;
          updated_at?: string;
          deleted_at?: string | null;
        };
        Update: Partial<Database['public']['Tables']['subscriptions']['Row']>;
        Relationships: [];
      };
      settings: {
        Row: {
          user_id: string;
          usd_to_egp_rate: number;
          default_account_id: string | null;
          analytics_currency: 'USD' | 'EGP';
          created_at: string;
          updated_at: string;
        };
        Insert: {
          user_id: string;
          usd_to_egp_rate?: number;
          default_account_id?: string | null;
          analytics_currency?: 'USD' | 'EGP';
          created_at?: string;
          updated_at?: string;
        };
        Update: Partial<Database['public']['Tables']['settings']['Row']>;
        Relationships: [];
      };
    };
    Views: Record<string, never>;
    Functions: Record<string, never>;
    Enums: Record<string, never>;
    CompositeTypes: Record<string, never>;
  };
}
