# Rotam native design direction

Authority: the user's original screen references, EK A, and subsequent corrections.
The skill's generic website landing-page result was rejected as inapplicable to this native planner. Its verified productivity guidance (flat design with micro-interactions) and Compose animation guidance are used as supporting advice.

- Native Jetpack Compose; one shared visual language across all screens.
- Near-black base, warmer layered surfaces, ivory readable text, orange primary actions. Avoid giant identical gray cards. Develop a coherent light theme separately rather than inverting colors.
- Curved/cutout cards highlight active plans and session summaries. Settings use grouped rows.
- One primary action per screen; 48dp targets, 4/8dp rhythm, body 16sp where practical; allow text growth.
- Motion tokens: feedback 120ms, replacement 180ms, navigation 260ms, departure 160ms. Follow system animator scale and the existing local preference. No information or persistence depends on an animation finishing.
- Short local confirmation sound only on a meaningful successful action when opted in; respect silent/ringer settings. Haptics are selective. Do not start music or sound on every navigation.
- Welcome -> Google sign-in -> optional reminders -> first plan. Existing records remain accessible. Google profile supplies the name; no repeated name form.
- AI planning is a full primary flow: describe -> clarify -> inspect drafts -> confirm. Manual path remains reachable. No save before confirmation.
- Preserve screen state, drafts and keyboard visibility. Error/retry live beside the relevant action.
- Main tabs: Today / Calendar / Routines / Settings. Insights live under Calendar.
- Real data only. Unknown past outcomes are never auto-failures.

Verification: compare real rendered screens to supplied references, inspect small and large text, reduced motion, small/large phones, and actual Google/AI paths. Passing compiler tests does not prove live integration.

Later user correction: the R mark itself is pure white #FFFFFF; retain the orange diamond and original silhouette. Body typography remains warm ivory. Android uses traced native vector assets from the approved mask; iOS brand exports use identical paths.
