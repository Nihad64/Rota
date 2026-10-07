private void renderToday() {
    Calendar now = Calendar.getInstance();
    String todayKey = key(now);
    selectedDate = todayKey;
    List<Plan> today = plansFor(todayKey);
    int total = today.size();
    int done = countDone(today);
    int remaining = total - done;

    LinearLayout page = scrollColumn();

    // 1. ÜST BAR & SELAMLAMA
    LinearLayout header = row();
    LinearLayout headText = column();

    int hour = now.get(Calendar.HOUR_OF_DAY);
    String greeting = (hour >= 5 && hour < 12) ? "Günaydın" : (hour >= 12 && hour < 18) ? "Tünaydın" : "İyi akşamlar";

    headText.addView(text(greeting + ", Nihad", 22, TEXT, true));
    headText.addView(text("Bugün güzel bir gün!", 13, MUTED, false), marginTop(3));
    headText.addView(text(capitalize(new SimpleDateFormat("d MMMM yyyy · EEEE", tr).format(now.getTime())), 12, MUTED, false), marginTop(2));

    header.addView(headText, new LinearLayout.LayoutParams(0, -2, 1));

    LinearLayout rightHead = column();
    rightHead.setGravity(Gravity.END);

    ImageView bell = new ImageView(this);
    bell.setImageResource(R.drawable.ic_notification_bell);
    bell.setColorFilter(TEXT);
    bell.setPadding(dp(10), dp(10), dp(10), dp(10));
    bell.setBackground(round(SURFACE, 18));
    bell.setOnClickListener(v -> { tap(v); showNotificationInfo(); });
    rightHead.addView(bell, new LinearLayout.LayoutParams(dp(44), dp(44)));

    TextView quote = text("“Her adım, seni\nhedefine yaklaştırır.”", 10, MUTED, false);
    quote.setGravity(Gravity.END);
    rightHead.addView(quote, marginTop(6));

    header.addView(rightHead);
    page.addView(header, margin(18, 12, 18, 14));

    // 2. GÜNÜN GİDİŞATI KARTI (% İlerleme & İstatistikler)
    LinearLayout progress = card();
    progress.setPadding(dp(16), dp(16), dp(16), dp(16));

    LinearLayout pr = row();

    ProgressRing ring = new ProgressRing(this);
    float pct = total == 0 ? 0f : done / (float) total;
    ring.setProgress(pct);
    ring.setLabel((int)(pct * 100f) + "%");
    pr.addView(ring, new LinearLayout.LayoutParams(dp(82), dp(82)));

    LinearLayout ptxt = column();
    ptxt.setPadding(dp(14), 0, 0, 0);

    LinearLayout prTitleRow = row();
    prTitleRow.addView(text("Günün gidişatı", 16, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
    prTitleRow.addView(text(done + "/" + total + " tamamlandı", 12, MUTED, false));
    ptxt.addView(prTitleRow);

    ptxt.addView(progressBar(done, total), marginTop(8));

    LinearLayout stats = row();
    stats.addView(text("📅 " + total + " plan\ntoplam", 11, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
    stats.addView(text("✓ " + done + "\ntamamlandı", 11, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
    stats.addView(text("⏱ " + remaining + "\nkalan", 11, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
    ptxt.addView(stats, marginTop(10));

    pr.addView(ptxt, new LinearLayout.LayoutParams(0, -2, 1));
    progress.addView(pr);
    page.addView(progress, margin(18, 0, 18, 14));

    // 3. 4'LÜ HIZLI AKSİYON KARTLARI
    LinearLayout quick = row();
    quick.addView(quickActionCard("✦", "AI ile planla", true, v -> { tap(v); showInlineAiPanel(); }), new LinearLayout.LayoutParams(0, dp(76), 1));
    addGap(quick, 8);
    quick.addView(quickActionCard("＋", "Hızlı ekle", false, v -> { tap(v); openPlanWizard(todayKey); }), new LinearLayout.LayoutParams(0, dp(76), 1));
    addGap(quick, 8);
    quick.addView(quickActionCard("📅", "Takvim", false, v -> { tap(v); tab = 1; render(); }), new LinearLayout.LayoutParams(0, dp(76), 1));
    addGap(quick, 8);
    quick.addView(quickActionCard("🎯", "Hedefler", false, v -> { tap(v); toast("Hedefler çok yakında!"); }), new LinearLayout.LayoutParams(0, dp(76), 1));
    page.addView(quick, margin(18, 0, 18, 18));

    // 4. ZAMAN ÇİZELGESİ & FİLTRELER
    LinearLayout section = row();
    section.addView(text("Bugünün zaman çizelgesi", 18, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));

    LinearLayout filterBox = row();
    filterBox.setBackground(round(SURFACE_2, 12));
    filterBox.setPadding(dp(2), dp(2), dp(2), dp(2));
    filterBox.addView(filterChip("Gün", true));
    filterBox.addView(filterChip("Hafta", false));
    filterBox.addView(filterChip("Ay", false));
    section.addView(filterBox);

    page.addView(section, margin(18, 0, 18, 12));

    if (today.isEmpty()) {
        page.addView(buildCalendarEmptyCard(), margin(18, 0, 18, 18));
    } else {
        for (int i = 0; i < today.size(); i++) {
            page.addView(addPlanCard(today.get(i), i), margin(18, 0, 18, 9));
        }
    }

    // 5. ROTAM AI ÖNERİ KARTI
    LinearLayout aiCard = card();
    aiCard.setBackground(round(SURFACE_2, 20));
    aiCard.setPadding(dp(16), dp(14), dp(16), dp(14));

    LinearLayout aiHeader = row();
    TextView aiStar = text("✦", 20, ORANGE, true);
    aiHeader.addView(aiStar);
    aiHeader.addView(text(" Rotam AI", 14, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
    aiCard.addView(aiHeader);

    LinearLayout aiBody = row();
    LinearLayout aiTextCol = column();
    aiTextCol.addView(text(hasAiKey() 
        ? "Bugün yoğun bir gün. Planlarına göre saat 14:00'teki randevuna 1 saat önce çıkmanı öneririm." 
        : "Gemini'yi bağlayarak güne özel akıllı öneriler alabilirsin.", 12, MUTED, false), marginTop(4));
    aiBody.addView(aiTextCol, new LinearLayout.LayoutParams(0, -2, 1));

    TextView aiArrow = text("→", 18, Color.WHITE, true);
    aiArrow.setGravity(Gravity.CENTER);
    aiArrow.setBackground(round(ORANGE, 20));
    aiBody.addView(aiArrow, new LinearLayout.LayoutParams(dp(38), dp(38)));

    aiCard.addView(aiBody, marginTop(8));
    aiCard.setOnClickListener(v -> { tap(v); showInlineAiPanel(); });

    page.addView(aiCard, margin(18, 8, 18, 24));
}

private LinearLayout quickActionCard(String icon, String label, boolean isPrimary, View.OnClickListener listener) {
    LinearLayout q = column();
    q.setGravity(Gravity.CENTER);
    q.setPadding(dp(6), dp(8), dp(6), dp(8));
    q.setBackground(round(isPrimary ? ORANGE : SURFACE, 18));

    TextView i = text(icon, 18, isPrimary ? Color.WHITE : ORANGE, true);
    i.setGravity(Gravity.CENTER);
    q.addView(i, new LinearLayout.LayoutParams(-1, dp(26)));

    TextView l = text(label, 11, isPrimary ? Color.WHITE : TEXT, true);
    l.setGravity(Gravity.CENTER);
    q.addView(l, marginTop(3));

    q.setOnClickListener(listener);
    return q;
}

private TextView filterChip(String label, boolean active) {
    TextView t = text(label, 11, active ? Color.WHITE : MUTED, true);
    t.setGravity(Gravity.CENTER);
    t.setPadding(dp(10), dp(4), dp(10), dp(4));
    if (active) t.setBackground(round(ORANGE, 10));
    return t;
}
