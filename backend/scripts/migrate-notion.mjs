/**
 * ============================================================
 * Raqeem — Notion → Supabase Migration Script
 * ============================================================
 * One-time migration that reads all finance data from Notion
 * and inserts it into the Raqeem Supabase database.
 *
 * Usage:
 *   1. Copy .env.example → .env and fill in values
 *   2. npm install
 *   3. npm run migrate
 * ============================================================
 */

import 'dotenv/config';
import { Client } from '@notionhq/client';
import { createClient } from '@supabase/supabase-js';

// ─── Config ──────────────────────────────────────────────────

const NOTION_TOKEN = process.env.NOTION_TOKEN;
const SUPABASE_URL = process.env.SUPABASE_URL;
const SUPABASE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY;
const USER_ID      = process.env.USER_ID || 'eb7eaeef-e731-4aff-8c95-7e66c7538e27';

if (!NOTION_TOKEN || !SUPABASE_URL || !SUPABASE_KEY) {
  console.error('❌ Missing environment variables. Copy .env.example → .env');
  process.exit(1);
}

const notion   = new Client({ auth: NOTION_TOKEN });
const supabase = createClient(SUPABASE_URL, SUPABASE_KEY);

// ─── Notion Database IDs ─────────────────────────────────────

const DB = {
  accounts:       'b4ee9d24-e231-4e04-9388-f6d80700747c',
  incomeRecords:  '55a601c7-e65d-4363-95b6-2147dc36d6f5',
  expenseRecords: 'fe20e3b5-2f90-4963-8d12-0de3d299cb04',
  transfers:      'caf31f52-1aa7-4199-a204-879834d606b6',
  goals:          '55d2a3aa-16cf-4877-98a8-1f25f5ddc5c6',
  subscriptions:  '6ed6703f-450f-4ff2-a72b-f1b1f65b07c9',
  expenseTypes:   '934a5dd6-e858-4787-b5fd-67b93b06eb1e',
  incomeTypes:    '494e3a57-ec27-4ab6-9dbf-6c4af1afd718',
};

// ─── Helpers ─────────────────────────────────────────────────

/** Convert dollar amount (possibly with decimals) to integer cents.
 *  If minOne=true, ensures at least 1 cent (for DB CHECK constraints). */
function toCents(amount, minOne = false) {
  if (amount == null || isNaN(amount)) return minOne ? 1 : 0;
  const cents = Math.round(Number(amount) * 100);
  return minOne ? Math.max(cents, 1) : cents;
}

/** Get a fallback date: use the date property, or fall back to the Notion page's created_time */
function getDateOrFallback(prop, page) {
  return prop?.date?.start ?? page.created_time?.split('T')[0] ?? new Date().toISOString().split('T')[0];
}

/** Get the default (first) account ID */
function getDefaultAccountId() {
  return notionIdToSupabaseId.accounts.values().next().value;
}

/** Extract plain text from a Notion title/rich_text property */
function getText(prop) {
  if (!prop) return '';
  const items = prop.title || prop.rich_text || [];
  return items.map(t => t.plain_text).join('');
}

/** Extract number from a Notion number property */
function getNumber(prop) {
  return prop?.number ?? 0;
}

/** Extract date string from a Notion date property */
function getDate(prop) {
  return prop?.date?.start ?? null;
}

/** Extract select value */
function getSelect(prop) {
  return prop?.select?.name ?? null;
}

/** Extract checkbox */
function getCheckbox(prop) {
  return prop?.checkbox ?? false;
}

/** Extract relation IDs */
function getRelationIds(prop) {
  return (prop?.relation || []).map(r => r.id);
}

/** Fetch ALL pages from a Notion database (handles pagination) */
async function fetchAllPages(databaseId) {
  const pages = [];
  let cursor = undefined;
  let pageNum = 0;

  do {
    const response = await notion.databases.query({
      database_id: databaseId,
      start_cursor: cursor,
      page_size: 100,
    });

    pages.push(...response.results);
    cursor = response.has_more ? response.next_cursor : undefined;
    pageNum++;
    process.stdout.write(`  … fetched page ${pageNum} (${pages.length} records so far)\r`);
  } while (cursor);

  console.log(`  ✓ ${pages.length} records total                    `);
  return pages;
}

/** Insert rows in batches to avoid timeouts */
async function batchInsert(table, rows, batchSize = 50) {
  let inserted = 0;
  let failed = 0;

  for (let i = 0; i < rows.length; i += batchSize) {
    const batch = rows.slice(i, i + batchSize);
    const { error } = await supabase.from(table).insert(batch);

    if (error) {
      console.error(`  ⚠ Batch error on ${table}[${i}..${i + batch.length}]:`, error.message);
      // Try one by one
      for (const row of batch) {
        const { error: singleErr } = await supabase.from(table).insert(row);
        if (singleErr) {
          console.error(`    ✗ Failed row:`, singleErr.message, JSON.stringify(row).slice(0, 200));
          failed++;
        } else {
          inserted++;
        }
      }
    } else {
      inserted += batch.length;
    }
  }

  console.log(`  ✓ Inserted ${inserted}/${rows.length} rows into "${table}" (${failed} failed)`);
  return { inserted, failed };
}

// ─── Lookup Maps (filled during migration) ───────────────────

const notionIdToSupabaseId = {
  accounts:    new Map(), // notion page ID → supabase UUID
  categories:  new Map(),
  goals:       new Map(),
};

// Also keep account names for relation resolution
const notionAccountNames = new Map(); // notion page ID → account name

// ─── Default icons/colors for categories ─────────────────────

const EXPENSE_ICONS = {
  'Food':            'utensils',
  'Transport':       'car',
  'Entertainment':   'gamepad',
  'Shopping':        'shopping-bag',
  'Bills':           'file-text',
  'Health':          'heart',
  'Education':       'book',
  'Charity':         'hand-heart',
  'Subscriptions':   'repeat',
  'Other':           'circle',
};

const EXPENSE_COLORS = {
  'Food':            '#F97316',
  'Transport':       '#3B82F6',
  'Entertainment':   '#A855F7',
  'Shopping':        '#EC4899',
  'Bills':           '#EAB308',
  'Health':          '#EF4444',
  'Education':       '#14B8A6',
  'Charity':         '#22C55E',
  'Subscriptions':   '#6366F1',
  'Other':           '#8B5CF6',
};

const INCOME_COLORS = {
  'Salary':  '#22C55E',
  'Bonus':   '#3B82F6',
  'Manual':  '#A855F7',
  'Other':   '#14B8A6',
};

// ─── Migration Steps ─────────────────────────────────────────

async function migrateCategories() {
  console.log('\n📂 Step 1: Migrating Categories (Expense Types + Income Types)');

  // --- Expense Types ---
  console.log('  Fetching Expense Types from Notion...');
  const expenseTypes = await fetchAllPages(DB.expenseTypes);

  const expenseCategoryRows = expenseTypes.map(p => {
    const name = getText(p.properties['Expense Type']);
    const budget = getNumber(p.properties['Monthly Budget']);
    return {
      user_id:     USER_ID,
      name:        name || 'Unknown',
      type:        'expense',
      icon:        EXPENSE_ICONS[name] || 'circle',
      color:       EXPENSE_COLORS[name] || '#8B5CF6',
      budget_cents: toCents(budget) || null,
    };
  });

  if (expenseCategoryRows.length > 0) {
    const { data } = await supabase.from('categories').insert(expenseCategoryRows).select('id, name');
    if (data) {
      // Map Notion page IDs to Supabase IDs
      for (let i = 0; i < expenseTypes.length; i++) {
        const match = data.find(d => d.name === (getText(expenseTypes[i].properties['Expense Type']) || 'Unknown'));
        if (match) {
          notionIdToSupabaseId.categories.set(expenseTypes[i].id, match.id);
        }
      }
      console.log(`  ✓ Inserted ${data.length} expense categories`);
    }
  }

  // --- Income Types ---
  console.log('  Fetching Income Types from Notion...');
  const incomeTypes = await fetchAllPages(DB.incomeTypes);

  const incomeCategoryRows = incomeTypes.map(p => {
    const name = getText(p.properties['Income Type']);
    return {
      user_id: USER_ID,
      name:    name || 'Unknown',
      type:    'income',
      icon:    'trending-up',
      color:   INCOME_COLORS[name] || '#22C55E',
    };
  });

  if (incomeCategoryRows.length > 0) {
    const { data } = await supabase.from('categories').insert(incomeCategoryRows).select('id, name');
    if (data) {
      for (let i = 0; i < incomeTypes.length; i++) {
        const match = data.find(d => d.name === (getText(incomeTypes[i].properties['Income Type']) || 'Unknown'));
        if (match) {
          notionIdToSupabaseId.categories.set(incomeTypes[i].id, match.id);
        }
      }
      console.log(`  ✓ Inserted ${data.length} income categories`);
    }
  }
}

async function migrateAccounts() {
  console.log('\n🏦 Step 2: Migrating Accounts');
  console.log('  Fetching Accounts from Notion...');
  const accounts = await fetchAllPages(DB.accounts);

  const typeMap = {
    'Cash':       'cash',
    'Checking':   'checking',
    'Saving':     'saving',
    'Investment': 'investment',
    'Business':   'checking',
    'Other':      'cash',
  };

  const accountRows = accounts.map(p => {
    const name = getText(p.properties['Account Name']);
    const type = getSelect(p.properties['Type']) || 'Cash';
    const initialAmount = getNumber(p.properties['Initial Amount']);

    // Store name for later relation resolution
    notionAccountNames.set(p.id, name);

    // Detect currency: if the name contains common EGP keywords
    const isEgp = /wallet|محفظة|egp|جنيه/i.test(name);

    return {
      user_id:            USER_ID,
      name:               name || 'Unknown Account',
      type:               typeMap[type] || 'cash',
      currency:           isEgp ? 'EGP' : 'USD',
      initial_amount_cents: toCents(initialAmount),
      balance_cents:       toCents(initialAmount), // Will be recalculated by triggers
      sort_order:          0,
    };
  });

  if (accountRows.length > 0) {
    const { data, error } = await supabase.from('accounts').insert(accountRows).select('id, name');
    if (error) {
      console.error('  ✗ Error inserting accounts:', error.message);
    }
    if (data) {
      for (let i = 0; i < accounts.length; i++) {
        const name = getText(accounts[i].properties['Account Name']) || 'Unknown Account';
        const match = data.find(d => d.name === name);
        if (match) {
          notionIdToSupabaseId.accounts.set(accounts[i].id, match.id);
        }
      }
      console.log(`  ✓ Inserted ${data.length} accounts`);
    }
  }
}

async function migrateGoals() {
  console.log('\n🎯 Step 3: Migrating Goals');
  console.log('  Fetching Goals from Notion...');
  const goals = await fetchAllPages(DB.goals);

  const goalRows = goals.map(p => {
    const name = getText(p.properties['Goal Name']);
    const targetAmount = getNumber(p.properties['Target Amount']);
    const initialAmount = getNumber(p.properties['Initial Amount']);
    const deadline = getDate(p.properties['Target Date']);
    const completed = getCheckbox(p.properties['Completed']);

    return {
      user_id:       USER_ID,
      name:          name || 'Unknown Goal',
      target_cents:  toCents(targetAmount) || 100, // Must be > 0
      current_cents: toCents(initialAmount),       // Will be updated by transfer triggers
      currency:      'USD',
      deadline:      deadline,
      is_completed:  completed,
      icon:          'flag',
    };
  });

  if (goalRows.length > 0) {
    const { data, error } = await supabase.from('goals').insert(goalRows).select('id, name');
    if (error) {
      console.error('  ✗ Error inserting goals:', error.message);
    }
    if (data) {
      for (let i = 0; i < goals.length; i++) {
        const name = getText(goals[i].properties['Goal Name']) || 'Unknown Goal';
        const match = data.find(d => d.name === name);
        if (match) {
          notionIdToSupabaseId.goals.set(goals[i].id, match.id);
        }
      }
      console.log(`  ✓ Inserted ${data.length} goals`);
    }
  }
}

/** Resolve a Notion relation to a Supabase UUID */
function resolveRelation(prop, lookupMap) {
  const ids = getRelationIds(prop);
  if (ids.length === 0) return null;
  return lookupMap.get(ids[0]) || null;
}

async function migrateTransactions() {
  console.log('\n💰 Step 4: Migrating Transactions (Income + Expenses)');

  // --- Income Records ---
  console.log('  Fetching Income Records from Notion...');
  const incomeRecords = await fetchAllPages(DB.incomeRecords);

  const incomeTxRows = [];
  let fallbackCount = 0;

  for (const p of incomeRecords) {
    const name = getText(p.properties['Income Name']);
    const amount = getNumber(p.properties['Amount']);
    const date = getDateOrFallback(p.properties['Date'], p);
    const note = getText(p.properties['Notes']);
    let accountId = resolveRelation(p.properties['Account'], notionIdToSupabaseId.accounts);
    const categoryId = resolveRelation(p.properties['Income Soruce'], notionIdToSupabaseId.categories);

    if (!accountId) {
      accountId = getDefaultAccountId();
      fallbackCount++;
      console.log(`    ℹ️ Income "${name}" — no account relation, using default`);
    }

    incomeTxRows.push({
      user_id:      USER_ID,
      account_id:   accountId,
      category_id:  categoryId,
      type:         'income',
      amount_cents: toCents(amount, true),
      currency:     'USD',
      note:         [name, note].filter(Boolean).join(' — ') || null,
      date:         date,
    });
  }

  if (incomeTxRows.length > 0) {
    await batchInsert('transactions', incomeTxRows);
  }
  if (fallbackCount > 0) console.log(`  ℹ️ ${fallbackCount} income records used default account`);

  // --- Expense Records ---
  console.log('  Fetching Expense Records from Notion...');
  const expenseRecords = await fetchAllPages(DB.expenseRecords);

  const expenseTxRows = [];
  let fallbackExpense = 0;

  for (const p of expenseRecords) {
    const name = getText(p.properties['Expense Record']);
    const amount = getNumber(p.properties['Amount']);
    const date = getDateOrFallback(p.properties['Date'], p);
    const note = getText(p.properties['Note']);
    let accountId = resolveRelation(p.properties['Account'], notionIdToSupabaseId.accounts);
    const categoryId = resolveRelation(p.properties['Expense Type'], notionIdToSupabaseId.categories);

    if (!accountId) {
      accountId = getDefaultAccountId();
      fallbackExpense++;
      console.log(`    ℹ️ Expense "${name}" — no account relation, using default`);
    }

    expenseTxRows.push({
      user_id:      USER_ID,
      account_id:   accountId,
      category_id:  categoryId,
      type:         'expense',
      amount_cents: toCents(amount, true),
      currency:     'USD',
      note:         [name, note].filter(Boolean).join(' — ') || null,
      date:         date,
    });
  }

  if (expenseTxRows.length > 0) {
    await batchInsert('transactions', expenseTxRows);
  }
  if (fallbackExpense > 0) console.log(`  ℹ️ ${fallbackExpense} expense records used default account`);
}

async function migrateTransfers() {
  console.log('\n🔄 Step 5: Migrating Transfers');
  console.log('  Fetching Transfer Records from Notion...');
  const transfers = await fetchAllPages(DB.transfers);

  const transferRows = [];
  let fallbackTransfer = 0;

  for (const p of transfers) {
    const name = getText(p.properties['Transfer']);
    const amount = getNumber(p.properties['Amount']);
    const date = getDateOrFallback(p.properties['Date'], p);
    let fromAccountId = resolveRelation(p.properties['From'], notionIdToSupabaseId.accounts);
    let toAccountId = resolveRelation(p.properties['To'], notionIdToSupabaseId.accounts);
    const goalId = resolveRelation(p.properties['Fund to Goals'], notionIdToSupabaseId.goals);

    const defaultAcctId = getDefaultAccountId();
    if (!fromAccountId) {
      fromAccountId = defaultAcctId;
      fallbackTransfer++;
      console.log(`    ℹ️ Transfer "${name}" — no 'from' account, using default`);
    }
    if (!toAccountId) {
      toAccountId = defaultAcctId;
      fallbackTransfer++;
      console.log(`    ℹ️ Transfer "${name}" — no 'to' account, using default`);
    }

    const cents = toCents(amount, true);

    transferRows.push({
      user_id:           USER_ID,
      from_account_id:   fromAccountId,
      to_account_id:     toAccountId,
      from_amount_cents: cents,
      to_amount_cents:   cents,
      from_currency:     'USD',
      to_currency:       'USD',
      exchange_rate:     1.0,
      is_currency_conversion: false,
      goal_id:           goalId,
      note:              name || null,
      date:              date,
    });
  }

  if (transferRows.length > 0) {
    await batchInsert('transfers', transferRows);
  }
  if (fallbackTransfer > 0) console.log(`  ℹ️ ${fallbackTransfer} transfers used default account`);
}

async function migrateSubscriptions() {
  console.log('\n📅 Step 6: Migrating Subscriptions');
  console.log('  Fetching Bills & Subscriptions from Notion...');
  const subs = await fetchAllPages(DB.subscriptions);

  const billingMap = {
    'Monthly':   'monthly',
    'Yearly':    'yearly',
    'Quarterly': 'monthly', // No quarterly in Raqeem, use monthly
  };

  // We need a default account to assign subscriptions to.
  // Pick the first account we inserted.
  const firstAccountId = notionIdToSupabaseId.accounts.values().next().value;

  const subRows = subs.map(p => {
    const name = getText(p.properties['Name']);
    const amount = getNumber(p.properties['Amount']);
    const billing = getSelect(p.properties['Billing']) || 'Monthly';
    const status = getSelect(p.properties['Status']);
    const isActive = status === 'Active';

    return {
      user_id:           USER_ID,
      account_id:        firstAccountId, // Assign to first account
      name:              name || 'Unknown Subscription',
      amount_cents:      toCents(amount),
      currency:          'USD',
      billing_cycle:     billingMap[billing] || 'monthly',
      next_billing_date: new Date().toISOString().split('T')[0], // Today as default
      is_active:         isActive,
      auto_log:          false,
    };
  });

  if (subRows.length > 0) {
    await batchInsert('subscriptions', subRows);
  }
}

async function migrateSettings() {
  console.log('\n⚙️  Step 7: Creating Settings');

  const firstAccountId = notionIdToSupabaseId.accounts.values().next().value;

  const { error } = await supabase.from('settings').upsert({
    user_id:            USER_ID,
    usd_to_egp_rate:    52.0,
    default_account_id: firstAccountId,
    analytics_currency: 'USD',
  });

  if (error) {
    console.error('  ✗ Error creating settings:', error.message);
  } else {
    console.log('  ✓ Settings created');
  }
}

// ─── Main ────────────────────────────────────────────────────

async function main() {
  console.log('╔════════════════════════════════════════════════╗');
  console.log('║   Raqeem — Notion → Supabase Migration        ║');
  console.log('╚════════════════════════════════════════════════╝');
  console.log(`\nUser ID: ${USER_ID}`);
  console.log(`Supabase: ${SUPABASE_URL}`);

  // ── Pre-flight: Check connection ──
  console.log('\n🔍 Checking Supabase connection...');
  const { data: healthCheck, error: healthErr } = await supabase.from('accounts').select('id').limit(1);
  if (healthErr) {
    console.error('❌ Could not connect to Supabase:', healthErr.message);
    process.exit(1);
  }
  console.log('  ✓ Connected to Supabase');

  // ── Pre-flight: Clear existing data ──
  console.log('\n🧹 Clearing existing data for this user...');
  const tables = ['transfers', 'transactions', 'subscriptions', 'goals', 'categories', 'settings', 'accounts'];
  for (const table of tables) {
    const k = table === 'settings' ? 'user_id' : 'user_id';
    const { error } = await supabase.from(table).delete().eq(k, USER_ID);
    if (error) {
      console.log(`  ⚠ Could not clear ${table}: ${error.message}`);
    } else {
      console.log(`  ✓ Cleared ${table}`);
    }
  }

  // ── Run migration steps in order ──
  const startTime = Date.now();

  await migrateCategories();
  await migrateAccounts();
  await migrateGoals();
  await migrateTransactions();
  await migrateTransfers();
  await migrateSubscriptions();
  await migrateSettings();

  // ── Summary ──
  const elapsed = ((Date.now() - startTime) / 1000).toFixed(1);
  console.log('\n╔════════════════════════════════════════════════╗');
  console.log('║   ✅ Migration Complete!                       ║');
  console.log('╚════════════════════════════════════════════════╝');
  console.log(`  Time: ${elapsed}s`);
  console.log(`  Categories mapped: ${notionIdToSupabaseId.categories.size}`);
  console.log(`  Accounts mapped:   ${notionIdToSupabaseId.accounts.size}`);
  console.log(`  Goals mapped:      ${notionIdToSupabaseId.goals.size}`);
  console.log('\n  Account balances are auto-recalculated by DB triggers.');
  console.log('  Open the Raqeem app to verify your data! 🎉');
}

main().catch((err) => {
  console.error('\n💥 Migration failed:', err);
  process.exit(1);
});
