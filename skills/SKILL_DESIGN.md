---
name: SKILL_DESIGN
description: >
  Read this before touching ANY UI on any platform (Android Compose, Web React,
  or web HTML). Covers Obsidian design system, color tokens, typography, spacing,
  component specs, and anti-patterns. This is law — do not invent new tokens.
---

# Design Skill — Raqeem

## Read First

The full design system is in `docs/DESIGN.md`. This skill summarizes the
critical rules and adds implementation guidance.

---

## The Non-Negotiables

1. **Color only from the token system.** No hex values in component code
   that aren't in the token file. If you need a color that doesn't exist,
   you're solving the wrong problem.

2. **Backgrounds:** `--void` / `--bg-base` / `--bg-surface` / `--bg-elevated` / `--bg-subtle`.
   These are the 5 levels. Never go darker than `--void` or lighter than `--bg-subtle`
   for surfaces.

3. **One accent: purple.** `--purple-500` (#7C3AED) is the primary. `--purple-400`
   for hover. `--purple-300` for secondary interactive text. Nothing else competes.

4. **Semantic colors only for semantic meaning:**
   - `--positive` (#10B981): income, gains, on-track status
   - `--negative` (#F87171): expenses, overspend, errors
   - `--warning` (#FBBF24): approaching limit, caution
   
5. **Monospace for every number.** JetBrains Mono on web.
   `GoogleFonts.jetBrainsMono()` on Android.

6. **No shadows.** Depth = background color difference + borders. Never `box-shadow`
   except for focus rings.

7. **No white backgrounds. Ever.** The lightest surface in the app is `--bg-subtle`
   (#242424). If something looks white, you've used the wrong color.

---

## Obsidian Aesthetic — What This Means in Practice

Obsidian (the app) is:
- Extremely dark (near black backgrounds)
- Monochromatic except for one accent
- Dense information without clutter
- No decorative elements — every visual element carries meaning
- Sidebar navigation on desktop
- Functional, not beautiful for beauty's sake

This app should feel like a focused tool, not a consumer app trying to look fun.

**Reference screenshots to keep in mind:**
- Obsidian main interface
- Linear (task management) — same design DNA
- Raycast — minimal, keyboard-driven dark UI
- Revolut dark mode — for the financial number treatment

---

## Quick Token Reference

```
Backgrounds (darkest → lightest):
  void (#080808) → base (#0F0F0F) → surface (#161616) → elevated (#1E1E1E) → subtle (#242424)

Purple:
  500 (#7C3AED) primary  →  400 (#8B5CF6) hover  →  300 (#A78BFA) links

Text:
  primary (#F0F0F0)  →  secondary (#A0A0A0)  →  muted (#5A5A5A)

Semantic:
  positive (#10B981)  |  negative (#F87171)  |  warning (#FBBF24)

Borders:
  subtle (white/5%)  →  default (white/9%)  →  strong (white/16%)  →  accent (purple/45%)
```

---

## Component Checklist

Before shipping any component, verify:

- [ ] Uses only token colors (no raw hex in component code)
- [ ] All amounts in JetBrains Mono with +/− prefix AND semantic color
- [ ] Touch targets ≥ 44×44px on mobile
- [ ] Focus state: 2px `--border-accent` ring with 3px offset
- [ ] Loading state: skeleton, not spinner
- [ ] Error state: defined and handled visually
- [ ] Empty state: defined (not just blank)
- [ ] Dark background — no white surfaces
- [ ] No inline styles for colors — use CSS vars (web) or AppColors (Kotlin)

---

## Skeleton Loading Pattern

Every data-loading component shows a skeleton, not a spinner.

```jsx
// Web skeleton
<div className="animate-pulse bg-subtle rounded h-4 w-3/4" />

// The pulse animation:
// @keyframes pulse { 0%,100% { opacity:1 } 50% { opacity:.4 } }
// animation: pulse 2s cubic-bezier(0.4,0,0.6,1) infinite;
// Use subtle (#242424) as background, elevated (#1E1E1E) as shimmer
```

```kotlin
// Flutter skeleton
Container(
  decoration: BoxDecoration(
    color: AppColors.bgSubtle,
    borderRadius: BorderRadius.circular(4),
  ),
  child: ShimmerWidget(), // or use shimmer package
)
```

---

## Empty States

Every list has an empty state. It should be:
- A simple icon (from Lucide, monochrome `--text-muted`)
- One short headline (text-secondary)
- One action button if applicable

```jsx
// Example: no transactions
<div className="flex flex-col items-center gap-3 py-16">
  <Receipt className="text-muted w-10 h-10" />
  <p className="text-secondary text-sm">No transactions yet</p>
  <button className="btn-primary text-sm">Add your first one</button>
</div>
```

---

## Typography Implementation

### Web CSS classes to define once and reuse:
```css
.text-hero-amount { font-family: 'JetBrains Mono'; font-size: 48px; font-weight: 500; }
.text-section-label { font-family: 'Inter'; font-size: 11px; font-weight: 600; letter-spacing: 0.08em; text-transform: uppercase; color: var(--text-muted); }
.text-amount { font-family: 'JetBrains Mono'; font-size: 15px; font-weight: 500; }
.text-body { font-family: 'Inter'; font-size: 14px; font-weight: 400; color: var(--text-primary); }
.text-caption { font-family: 'Inter'; font-size: 12px; color: var(--text-secondary); }
```

### Kotlin TextStyles to define once in `AppTypography.kt`:
```kotlin
val heroAmount = GoogleFonts.jetBrainsMono(fontSize = 48.sp, fontWeight = FontWeight.Medium)
val sectionLabel = GoogleFonts.inter(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.08.em)
val amountText = GoogleFonts.jetBrainsMono(fontSize = 15.sp, fontWeight = FontWeight.Medium)
val bodyText = GoogleFonts.inter(fontSize = 14.sp)
val captionText = GoogleFonts.inter(fontSize = 12.sp, color = AppColors.textSecondary)
```
