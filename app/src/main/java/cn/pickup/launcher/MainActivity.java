package cn.pickup.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    static final String ACTION_OPEN = "cn.pickup.launcher.OPEN";
    static final String EXTRA_DESTINATION = "destination";

    // ===== 水蓝清凉主题 =====
    private static final int PAGE_BACKGROUND = Color.rgb(240, 246, 249);
    private static final int CARD_WHITE = Color.WHITE;
    private static final int TEXT_PRIMARY = Color.rgb(27, 43, 51);
    private static final int TEXT_SECONDARY = Color.rgb(116, 133, 142);
    private static final int LINE_COLOR = Color.rgb(223, 233, 238);
    private static final int ACCENT_WATER = Color.rgb(42, 122, 158);
    private static final int NAV_IDLE = Color.rgb(154, 166, 157);

    // ===== 平台品牌色（渐变起止） =====
    private static final int[] CAINIAO_COLORS = {Color.rgb(63, 179, 212), Color.rgb(31, 122, 158)};
    private static final int[] TAOBAO_COLORS = {Color.rgb(255, 144, 70), Color.rgb(232, 115, 44)};
    private static final int[] PDD_COLORS = {Color.rgb(232, 80, 106), Color.rgb(214, 58, 82)};
    private static final int[] JD_COLORS = {Color.rgb(224, 74, 50), Color.rgb(196, 52, 31)};
    private static final int[] XHS_COLORS = {Color.rgb(232, 98, 156), Color.rgb(216, 67, 124)};
    private static final int[] DOUYIN_COLORS = {Color.rgb(43, 43, 51), Color.rgb(15, 15, 20)};

    // 当前选中的身份码平台，默认淘宝
    private Destination selectedIdentity = Destination.TAOBAO;

    private LinearLayout identityCard;
    private TextView identityTitle;
    private TextView identitySubtitle;
    private LinearLayout segmentRow;
    private LinearLayout pendingTabButton;
    private LinearLayout codeTabButton;
    private ScrollView pendingPage;
    private ScrollView codePage;
    private LinearLayout packageListContainer;
    private TextView smsStatusView;
    private TextView notifyStatusView;
    private TextView historyCountView;
    private LinearLayout historyDialogContainer;
    private final List<String> justPickedCodes = new ArrayList<>();

    private static final int REQUEST_SMS_PERMISSION = 1001;
    private static final int SWIPE_ACTION_WIDTH = 88;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Destination destination = destinationFromIntent();
        if (destination != null) {
            DeepLinkLauncher.open(this, destination);
            finish();
            return;
        }

        getWindow().setStatusBarColor(PAGE_BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
    }

    private Destination destinationFromIntent() {
        String action = getIntent().getAction();
        if (ACTION_OPEN.equals(action)) {
            return Destination.fromKey(getIntent().getStringExtra(EXTRA_DESTINATION));
        }
        if ("cn.pickup.launcher.OPEN_CAINIAO".equals(action)) {
            return Destination.CAINIAO;
        }
        if ("cn.pickup.launcher.OPEN_TAOBAO".equals(action)) {
            return Destination.TAOBAO;
        }
        if ("cn.pickup.launcher.OPEN_PINDUODUO".equals(action)) {
            return Destination.PINDUODUO;
        }
        if ("cn.pickup.launcher.OPEN_TAOBAO_PENDING".equals(action)) {
            return Destination.TAOBAO_PENDING;
        }
        if ("cn.pickup.launcher.OPEN_PINDUODUO_PENDING".equals(action)) {
            return Destination.PINDUODUO_PENDING;
        }
        if ("cn.pickup.launcher.OPEN_JD".equals(action)) {
            return Destination.JD;
        }
        if ("cn.pickup.launcher.OPEN_XHS".equals(action)) {
            return Destination.XHS;
        }
        return null;
    }

    // =====================================================================
    // 页面骨架：内容区（两个可切换页面） + 底部导航
    // =====================================================================

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE_BACKGROUND);

        // targetSdk 35 强制 edge-to-edge：内容会画到状态栏底下，必须避让
        //（普通主题下 top 为 0，此处理不会生效，两相兼容）
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                top = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
            } else {
                top = insets.getSystemWindowInsetTop();
            }
            if (top > 0) {
                v.setPadding(0, top, 0, 0);
            }
            return insets;
        });

        FrameLayout content = new FrameLayout(this);
        pendingPage = buildPendingPage();
        codePage = buildCodePage();
        codePage.setVisibility(View.GONE);
        content.addView(pendingPage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        content.addView(codePage, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        root.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        root.addView(buildNavBar(), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return root;
    }

    private View buildNavBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(CARD_WHITE);
        bar.setPadding(dp(24), dp(9), dp(24), dp(9) + getNavBarPadding());

        // 高屏 + 手势导航时，用系统窗口 inset 兜底，避免手势条盖住 Tab
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            bar.setOnApplyWindowInsetsListener((v, insets) -> {
                int bottom = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom;
                if (bottom > 0) {
                    bar.setPadding(dp(24), dp(9), dp(24), dp(9) + bottom);
                }
                return insets;
            });
        }

        pendingTabButton = navItem("📦", "待取快递", true);
        pendingTabButton.setOnClickListener(v -> switchTab(true));
        codeTabButton = navItem("🔢", "取件码", false);
        codeTabButton.setOnClickListener(v -> switchTab(false));

        bar.addView(pendingTabButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        bar.addView(codeTabButton, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return bar;
    }

    private LinearLayout navItem(String icon, String label, boolean active) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);

        TextView iconView = text(icon, 20, active ? ACCENT_WATER : NAV_IDLE, Typeface.NORMAL);
        iconView.setGravity(Gravity.CENTER);
        item.addView(iconView);

        TextView labelView = text(label, 11, active ? ACCENT_WATER : NAV_IDLE, Typeface.BOLD);
        labelView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(3);
        labelView.setLayoutParams(labelParams);
        item.addView(labelView);
        return item;
    }

    private void switchTab(boolean pending) {
        pendingPage.setVisibility(pending ? View.VISIBLE : View.GONE);
        codePage.setVisibility(pending ? View.GONE : View.VISIBLE);
        restyleNavItem(pendingTabButton, pending);
        restyleNavItem(codeTabButton, !pending);
    }

    private void restyleNavItem(LinearLayout item, boolean active) {
        int color = active ? ACCENT_WATER : NAV_IDLE;
        for (int i = 0; i < item.getChildCount(); i++) {
            View child = item.getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(color);
            }
        }
    }

    private int getNavBarPadding() {
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return getResources().getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    // =====================================================================
    // 页面一：待取快递
    // =====================================================================

    private ScrollView buildPendingPage() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PAGE_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(20));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        // ---- 顶部问候 ----
        root.addView(buildHero("你好 👋", "先看有没有包裹，再出发去驿站"));

        // ---- 五个待取入口 ----
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.setWeightSum(5f);
        grid.setPadding(0, dp(4), 0, dp(4));
        grid.addView(quickEntry(Destination.TAOBAO_PENDING, "淘", "淘宝待取", TAOBAO_COLORS), horizontalWeightParams(0));
        grid.addView(quickEntry(Destination.PINDUODUO_PENDING, "拼", "拼多多待取", PDD_COLORS), horizontalWeightParams(dp(5)));
        grid.addView(quickEntry(Destination.JD, "京", "京东待取", JD_COLORS), horizontalWeightParams(dp(5)));
        grid.addView(quickEntry(Destination.XHS, "书", "小红书待取", XHS_COLORS), horizontalWeightParams(dp(5)));
        grid.addView(quickEntry(Destination.DOUYIN, "抖", "抖音待取", DOUYIN_COLORS), horizontalWeightParams(dp(5)));
        LinearLayout.LayoutParams gridParams = verticalParams(dp(12));
        grid.setLayoutParams(gridParams);
        root.addView(grid);

        // ---- 我的包裹（手动录入，本地保存） ----
        root.addView(packageHeader());

        packageListContainer = new LinearLayout(this);
        packageListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(packageListContainer);
        refreshPackageList();

        return scrollView;
    }

    private View buildHero(String title, String subtitle) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setLayoutParams(verticalParams(0));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = text(title, 24, TEXT_PRIMARY, Typeface.BOLD);
        titles.addView(titleView);

        TextView subtitleView = text(subtitle, 13, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(4);
        subtitleView.setLayoutParams(subtitleParams);
        titles.addView(subtitleView);

        hero.addView(titles, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        return hero;
    }

    private View quickEntry(Destination destination, String mark, String label, int[] colors) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription("打开" + destination.title);
        item.setOnClickListener(v -> DeepLinkLauncher.open(this, destination));

        TextView icon = text(mark, 18, Color.WHITE, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(gradient(colors, 17));
        item.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

        TextView labelView = text(label, 11, TEXT_SECONDARY, Typeface.NORMAL);
        labelView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(7);
        labelView.setLayoutParams(labelParams);
        item.addView(labelView);
        return item;
    }

    private View pendingCard(
            Destination destination,
            String title,
            String subtitle,
            int[] colors
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(15), dp(14), dp(15));
        card.setMinimumHeight(dp(78));
        card.setBackground(rounded(CARD_WHITE, 18));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("打开" + destination.title);
        card.setOnClickListener(v -> DeepLinkLauncher.open(this, destination));

        LinearLayout.LayoutParams cardParams = verticalParams(dp(12));
        card.setLayoutParams(cardParams);

        TextView mark = text(destination.mark, 16, Color.WHITE, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(gradient(colors, 12));
        card.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(14), 0, dp(10), 0);

        TextView titleView = text(title, 16, TEXT_PRIMARY, Typeface.BOLD);
        labels.addView(titleView);

        TextView subtitleView = text(subtitle, 12, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(3);
        subtitleView.setLayoutParams(subtitleParams);
        labels.addView(subtitleView);

        card.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 26, colors[1], Typeface.NORMAL);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(42)));

        return card;
    }

    // =====================================================================
    // 页面二：取件码
    // =====================================================================

    private ScrollView buildCodePage() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PAGE_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(20));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        root.addView(buildHero("取件码", "到驿站亮出身份码，扫码即取"));

        // ---- 平台切换器：菜鸟 / 淘宝 / 拼多多 ----
        segmentRow = new LinearLayout(this);
        segmentRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams segmentParams = verticalParams(dp(14));
        segmentRow.setLayoutParams(segmentParams);

        segmentRow.addView(segButton(Destination.CAINIAO, "菜", "菜鸟", CAINIAO_COLORS), horizontalWeightParams(0));
        segmentRow.addView(segButton(Destination.TAOBAO, "淘", "淘宝", TAOBAO_COLORS), horizontalWeightParams(dp(8)));
        segmentRow.addView(segButton(Destination.PINDUODUO, "拼", "拼多多", PDD_COLORS), horizontalWeightParams(dp(8)));
        root.addView(segmentRow);

        // ---- 身份码大卡 ----
        identityCard = new LinearLayout(this);
        identityCard.setOrientation(LinearLayout.VERTICAL);
        identityCard.setGravity(Gravity.CENTER_HORIZONTAL);
        identityCard.setPadding(dp(22), dp(20), dp(22), dp(20));
        identityCard.setClickable(true);
        identityCard.setFocusable(true);
        identityCard.setContentDescription("打开当前身份码页面");
        identityCard.setOnClickListener(v -> DeepLinkLauncher.open(this, selectedIdentity));

        LinearLayout.LayoutParams cardParams = verticalParams(dp(16));
        cardParams.bottomMargin = dp(18);
        identityCard.setLayoutParams(cardParams);

        LinearLayout caption = new LinearLayout(this);
        caption.setOrientation(LinearLayout.HORIZONTAL);

        identityTitle = text("", 14, Color.WHITE, Typeface.BOLD);
        caption.addView(identityTitle, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView refresh = text("点击打开 ›", 12, Color.WHITE, Typeface.NORMAL);
        caption.addView(refresh);
        identityCard.addView(caption);

        TextView markBig = text("🎫", 52, Color.WHITE, Typeface.NORMAL);
        markBig.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        markParams.topMargin = dp(16);
        markBig.setLayoutParams(markParams);
        identityCard.addView(markBig);

        TextView identityNum = text("身份码", 22, Color.WHITE, Typeface.BOLD);
        identityNum.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams numParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        numParams.topMargin = dp(10);
        identityNum.setLayoutParams(numParams);
        identityCard.addView(identityNum);

        identitySubtitle = text("", 12, Color.WHITE, Typeface.NORMAL);
        identitySubtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dp(6);
        identitySubtitle.setLayoutParams(subParams);
        identityCard.addView(identitySubtitle);

        TextView tip = text("🛡️ 本码仅用于取件核验，请勿截图外传", 11, Color.WHITE, Typeface.NORMAL);
        tip.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tipParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        tipParams.topMargin = dp(14);
        tip.setLayoutParams(tipParams);
        identityCard.addView(tip);

        root.addView(identityCard);
        applyIdentitySelection();

        // ---- 历史记录入口（点进去才能看） ----
        root.addView(historyEntry());

        refreshHistory();

        // ---- 自动获取取件码（短信 / 通知 / 剪贴板） ----
        root.addView(sectionTitle("自动获取取件码", 16));
        root.addView(hintCard("✨", "开启后，取件短信和菜鸟/淘宝/拼多多通知里的取件码会自动保存，不用手动录入。"));

        smsStatusView = text("", 11, TEXT_SECONDARY, Typeface.NORMAL);
        root.addView(settingsRow("📩", "短信自动识别", smsStatusView, v -> requestSmsPermission()));

        notifyStatusView = text("", 11, TEXT_SECONDARY, Typeface.NORMAL);
        root.addView(settingsRow("🔔", "通知自动识别", notifyStatusView, v -> openNotificationListenerSettings()));

        root.addView(hintCard("💡", "在菜鸟 / 淘宝复制取件码后切回本 App，会自动识别并询问是否保存。"));

        // ---- 添加到桌面 ----
        root.addView(sectionTitle("添加到桌面", 16));
        root.addView(hintCard("📌", "添加后不必进入本应用，桌面点击一次即可跳转。"));

        LinearLayout pinRow1 = new LinearLayout(this);
        pinRow1.setOrientation(LinearLayout.HORIZONTAL);
        pinRow1.setWeightSum(4f);
        pinRow1.setLayoutParams(verticalParams(dp(12)));
        addPinButton(pinRow1, Destination.TAOBAO, "淘宝码");
        addPinButton(pinRow1, Destination.PINDUODUO, "拼多多码");
        addPinButton(pinRow1, Destination.JD, "京东待取");
        addPinButton(pinRow1, Destination.XHS, "小红书待取");
        root.addView(pinRow1);

        LinearLayout pinRow2 = new LinearLayout(this);
        pinRow2.setOrientation(LinearLayout.HORIZONTAL);
        pinRow2.setWeightSum(4f);
        pinRow2.setLayoutParams(verticalParams(dp(8)));
        addPinButton(pinRow2, Destination.CAINIAO, "菜鸟码");
        addPinButton(pinRow2, Destination.TAOBAO_PENDING, "淘宝待取");
        addPinButton(pinRow2, Destination.PINDUODUO_PENDING, "拼多多待取");
        addPinButton(pinRow2, Destination.DOUYIN, "抖音待取");
        root.addView(pinRow2);

        return scrollView;
    }

    private View segButton(Destination destination, String mark, String label, int[] colors) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription("切换到" + destination.title);
        item.setTag(destination);
        item.setOnClickListener(v -> {
            selectedIdentity = destination;
            applyIdentitySelection();
        });

        TextView markView = text(mark, 12, Color.WHITE, Typeface.BOLD);
        markView.setGravity(Gravity.CENTER);
        markView.setBackground(gradient(colors, 8));
        item.addView(markView, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView labelView = text(label, 13, TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.leftMargin = dp(8);
        labelView.setLayoutParams(labelParams);
        item.addView(labelView);
        return item;
    }

    private void applyIdentitySelection() {
        String title;
        String subtitle;
        int[] colors;
        switch (selectedIdentity) {
            case CAINIAO:
                title = "菜鸟驿站身份码";
                subtitle = "向驿站工作人员出示";
                colors = CAINIAO_COLORS;
                break;
            case PINDUODUO:
                title = "拼多多身份码";
                subtitle = "向多多买菜代收点工作人员出示";
                colors = PDD_COLORS;
                break;
            case TAOBAO:
            default:
                title = "淘宝驿站身份码";
                subtitle = "向驿站工作人员出示";
                colors = TAOBAO_COLORS;
                break;
        }

        GradientDrawable cardBackground = gradient(colors, 22);
        identityCard.setBackground(cardBackground);
        identityTitle.setText(title);
        identitySubtitle.setText(subtitle);
        restyleSegmentRow(segmentRow);
    }

    private void restyleSegmentRow(LinearLayout segmentRow) {
        if (segmentRow == null) {
            return;
        }
        for (int i = 0; i < segmentRow.getChildCount(); i++) {
            View child = segmentRow.getChildAt(i);
            if (child instanceof LinearLayout && child.getTag() instanceof Destination) {
                Destination destination = (Destination) child.getTag();
                boolean selected = destination == selectedIdentity;
                GradientDrawable background = rounded(selected ? Color.rgb(247, 250, 252) : CARD_WHITE, 999);
                background.setStroke(dp(2), selected ? colorsFor(destination)[1] : LINE_COLOR);
                child.setBackground(background);
                child.setPadding(dp(6), dp(10), dp(6), dp(10));
            }
        }
    }

    private int[] colorsFor(Destination destination) {
        switch (destination) {
            case CAINIAO:
                return CAINIAO_COLORS;
            case PINDUODUO:
            case PINDUODUO_PENDING:
                return PDD_COLORS;
            case JD:
                return JD_COLORS;
            case XHS:
                return XHS_COLORS;
            case DOUYIN:
                return DOUYIN_COLORS;
            case TAOBAO:
            case TAOBAO_PENDING:
            default:
                return TAOBAO_COLORS;
        }
    }

    // =====================================================================
    // 我的包裹：手动录入 + 本地保存 + 大号取件码卡片
    // =====================================================================

    @Override
    protected void onResume() {
        super.onResume();
        // 上次标记为已取件的卡片，本次进入时归档到历史记录
        justPickedCodes.clear();
        refreshPackageList();
        refreshHistory();
        updateAutoStatus();
        checkClipboard();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            updateAutoStatus();
            if (hasSmsPermission()) {
                Toast.makeText(this, "已开启短信识别", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未授权，短信里的取件码不会自动保存", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private View packageHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.topMargin = dp(14);
        headerParams.bottomMargin = dp(10);
        header.setLayoutParams(headerParams);

        header.addView(text("我的包裹", 17, TEXT_PRIMARY, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView add = text("＋ 添加包裹", 13, Color.WHITE, Typeface.BOLD);
        add.setGravity(Gravity.CENTER);
        add.setBackground(gradient(new int[]{Color.rgb(63, 179, 212), ACCENT_WATER}, 999));
        add.setPadding(dp(14), dp(8), dp(14), dp(8));
        add.setClickable(true);
        add.setFocusable(true);
        add.setContentDescription("添加包裹");
        add.setOnClickListener(v -> showAddDialog());
        header.addView(add);
        return header;
    }

    private void togglePicked(PackageStore.Item item) {
        if (item.picked) {
            PackageStore.markPicked(this, item, false);
            justPickedCodes.remove(item.code);
            Toast.makeText(this, "已恢复为待取件", Toast.LENGTH_SHORT).show();
        } else {
            PackageStore.markPicked(this, item, true);
            justPickedCodes.add(item.code);
            Toast.makeText(this, "已取件，可在取件码页的「历史记录」里查看", Toast.LENGTH_SHORT).show();
        }
        refreshPackageList();
        refreshHistory();
    }

    private View historyEntry() {
        LinearLayout entry = new LinearLayout(this);
        entry.setOrientation(LinearLayout.HORIZONTAL);
        entry.setGravity(Gravity.CENTER_VERTICAL);
        entry.setPadding(dp(16), dp(13), dp(14), dp(13));
        entry.setBackground(rounded(CARD_WHITE, 18));
        entry.setClickable(true);
        entry.setFocusable(true);
        entry.setContentDescription("查看历史记录");
        entry.setOnClickListener(v -> showHistoryDialog());
        entry.setLayoutParams(verticalParams(dp(14)));

        TextView icon = text("🕘", 16, TEXT_PRIMARY, Typeface.NORMAL);
        icon.setGravity(Gravity.CENTER);
        entry.addView(icon, new LinearLayout.LayoutParams(dp(36), dp(36)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(13), 0, dp(10), 0);

        labels.addView(text("历史记录", 14, TEXT_PRIMARY, Typeface.BOLD));

        historyCountView = text("", 11, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        countParams.topMargin = dp(2);
        historyCountView.setLayoutParams(countParams);
        labels.addView(historyCountView);

        entry.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 24, Color.rgb(63, 179, 212), Typeface.NORMAL);
        arrow.setGravity(Gravity.CENTER);
        entry.addView(arrow, new LinearLayout.LayoutParams(dp(26), dp(36)));

        return entry;
    }

    private List<PackageStore.Item> loadHistory() {
        List<PackageStore.Item> history = new ArrayList<>();
        for (PackageStore.Item item : PackageStore.load(this)) {
            if (item.picked) {
                history.add(item);
            }
        }
        return history;
    }

    private View buildClearHistoryButton(int size) {
        TextView clear = text("清空历史记录", 12, TEXT_SECONDARY, Typeface.BOLD);
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(rounded(CARD_WHITE, 999));
        clear.setPadding(dp(16), dp(9), dp(16), dp(9));
        clear.setClickable(true);
        clear.setFocusable(true);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("清空历史记录")
                .setMessage("将删除 " + size + " 条已取件记录，无法恢复。")
                .setPositiveButton("清空", (dialog, which) -> {
                    PackageStore.clearHistory(this);
                    justPickedCodes.clear();
                    refreshPackageList();
                    refreshHistory();
                })
                .setNegativeButton("取消", null)
                .show());
        LinearLayout.LayoutParams clearParams = verticalParams(dp(12));
        clear.setLayoutParams(clearParams);
        return clear;
    }

    private void showHistoryDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(14), dp(22), dp(6));
        scroll.addView(box);

        historyDialogContainer = box;
        refreshHistory();

        new AlertDialog.Builder(this)
                .setTitle("历史记录（已取件）")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void deleteHistoryItem(PackageStore.Item item) {
        List<PackageStore.Item> items = PackageStore.load(this);
        List<PackageStore.Item> remaining = new ArrayList<>();
        for (PackageStore.Item current : items) {
            if (current.createdAt != item.createdAt || !current.code.equals(item.code)) {
                remaining.add(current);
            }
        }
        PackageStore.save(this, remaining);
        justPickedCodes.remove(item.code);
        refreshPackageList();
        refreshHistory();
    }

    private void refreshHistory() {
        if (historyCountView != null) {
            int count = loadHistory().size();
            historyCountView.setText(count == 0
                    ? "暂无已取件记录"
                    : count + " 条已取件记录");
        }
        if (historyDialogContainer == null) {
            return;
        }
        historyDialogContainer.removeAllViews();
        List<PackageStore.Item> history = loadHistory();
        if (history.isEmpty()) {
            historyDialogContainer.addView(hintCard("🗂️",
                    "还没有取件记录。在「待取快递」里点包裹左侧的圆圈打勾，就会归档到这里。"));
            return;
        }
        for (PackageStore.Item item : history) {
            historyDialogContainer.addView(historyRow(item));
        }
        historyDialogContainer.addView(buildClearHistoryButton(history.size()));
    }

    private View historyRow(PackageStore.Item item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(12), dp(12));
        row.setBackground(rounded(Color.rgb(246, 249, 250), 16));
        row.setLayoutParams(verticalParams(dp(10)));

        TextView check = text("✓", 12, Color.rgb(63, 179, 212), Typeface.BOLD);
        check.setGravity(Gravity.CENTER);
        row.addView(check, new LinearLayout.LayoutParams(dp(26), dp(26)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(10), 0, dp(8), 0);

        String title = (item.carrier == null || item.carrier.isEmpty())
                ? item.code
                : item.carrier + " · " + item.code;
        TextView titleView = text(title, 14, TEXT_SECONDARY, Typeface.BOLD);
        titleView.setPaintFlags(titleView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        labels.addView(titleView);

        String detail = formatTime(item.pickedAt);
        if (item.station != null && !item.station.isEmpty()) {
            detail = item.station + " · " + detail;
        }
        TextView detailView = text(detail, 11, Color.rgb(160, 172, 178), Typeface.NORMAL);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = dp(2);
        detailView.setLayoutParams(detailParams);
        labels.addView(detailView);

        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView delete = text("✕", 13, Color.rgb(160, 172, 178), Typeface.NORMAL);
        delete.setGravity(Gravity.CENTER);
        delete.setClickable(true);
        delete.setFocusable(true);
        delete.setContentDescription("删除这条记录");
        delete.setOnClickListener(v -> deleteHistoryItem(item));
        row.addView(delete, new LinearLayout.LayoutParams(dp(28), dp(32)));
        return row;
    }

    private String formatTime(long timeMillis) {
        if (timeMillis <= 0) {
            return "已取件";
        }
        return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                .format(new java.util.Date(timeMillis));
    }

    private void refreshPackageList() {
        if (packageListContainer == null) {
            return;
        }
        packageListContainer.removeAllViews();
        List<PackageStore.Item> items = PackageStore.load(this);

        List<PackageStore.Item> active = new ArrayList<>();
        List<PackageStore.Item> done = new ArrayList<>();
        for (PackageStore.Item item : items) {
            if (item.picked) {
                if (justPickedCodes.contains(item.code)) {
                    done.add(item);
                }
            } else {
                active.add(item);
            }
        }

        if (active.isEmpty() && done.isEmpty()) {
            packageListContainer.addView(hintCard("📭",
                    "还没有待取的包裹。点右上「＋ 添加包裹」录入取件码，它就会一直显示在这里。"));
            return;
        }

        for (PackageStore.Item item : active) {
            packageListContainer.addView(packageCard(item, false));
        }
        for (PackageStore.Item item : done) {
            packageListContainer.addView(packageCard(item, true));
        }
    }

    private View packageCard(PackageStore.Item item) {
        return packageCard(item, item.picked);
    }

    private View packageCard(PackageStore.Item item, boolean done) {
        int[] colors = colorsForCarrierName(item.carrier);
        boolean hasCarrier = item.carrier != null && !item.carrier.isEmpty();
        String mark = hasCarrier ? item.carrier.substring(0, 1) : "件";
        int nameColor = done ? TEXT_SECONDARY : TEXT_PRIMARY;
        int codeColor = done ? Color.rgb(160, 172, 178) : TEXT_PRIMARY;

        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int cardWidth = screenWidth - dp(36);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(done ? Color.rgb(246, 249, 250) : CARD_WHITE, 18));
        card.setLayoutParams(new LinearLayout.LayoutParams(cardWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // ---- 第一行：对勾 + 图标 + 名称/驿站 ----
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        // 未取：水蓝空心圈；已取：实心灰圈 + 白色对勾
        TextView check = text("✓", 14, Color.WHITE, Typeface.BOLD);
        check.setGravity(Gravity.CENTER);
        GradientDrawable checkBackground = rounded(done ? Color.rgb(176, 190, 197) : Color.TRANSPARENT, 999);
        checkBackground.setStroke(dp(2), done ? Color.rgb(176, 190, 197) : Color.rgb(63, 179, 212));
        if (!done) {
            check.setTextColor(Color.TRANSPARENT);
        }
        check.setBackground(checkBackground);
        check.setClickable(true);
        check.setFocusable(true);
        check.setContentDescription(done ? "取消已取件标记" : "标记为已取件");
        check.setOnClickListener(v -> togglePicked(item));
        top.addView(check, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView markView = text(mark, 14, Color.WHITE, Typeface.BOLD);
        markView.setGravity(Gravity.CENTER);
        markView.setBackground(gradient(done
                ? new int[]{Color.rgb(186, 200, 206), Color.rgb(160, 175, 182)}
                : colors, 11));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(36), dp(36));
        markParams.leftMargin = dp(12);
        markView.setLayoutParams(markParams);
        top.addView(markView);

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(12), 0, dp(8), 0);

        TextView nameView = text(hasCarrier ? item.carrier : "我的包裹", 15, nameColor, Typeface.BOLD);
        if (done) {
            nameView.setPaintFlags(nameView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        labels.addView(nameView);

        if (item.station != null && !item.station.isEmpty()) {
            TextView stationView = text(item.station, 11, TEXT_SECONDARY, Typeface.NORMAL);
            LinearLayout.LayoutParams stationParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            stationParams.topMargin = dp(2);
            stationView.setLayoutParams(stationParams);
            labels.addView(stationView);
        }
        top.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        card.addView(top);

        // ---- 第二行：大号取件码 + 复制按钮 ----
        LinearLayout codeRow = new LinearLayout(this);
        codeRow.setOrientation(LinearLayout.HORIZONTAL);
        codeRow.setGravity(Gravity.CENTER_VERTICAL);
        codeRow.setPadding(dp(14), dp(12), dp(12), dp(12));
        GradientDrawable codeBackground = rounded(Color.rgb(242, 249, 252), 14);
        codeBackground.setStroke(dp(1), Color.rgb(211, 230, 239));
        codeRow.setBackground(codeBackground);
        LinearLayout.LayoutParams codeRowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        codeRowParams.topMargin = dp(12);
        codeRow.setLayoutParams(codeRowParams);

        LinearLayout codeLabels = new LinearLayout(this);
        codeLabels.setOrientation(LinearLayout.VERTICAL);

        TextView codeView = text(item.code, 24, codeColor, Typeface.BOLD);
        if (done) {
            codeView.setPaintFlags(codeView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        codeLabels.addView(codeView);

        TextView codeCaption = text("取件码", 10, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        captionParams.topMargin = dp(2);
        codeCaption.setLayoutParams(captionParams);
        codeLabels.addView(codeCaption);

        codeRow.addView(codeLabels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView copy = text(done ? "已取件" : "复制", 13, Color.WHITE, Typeface.BOLD);
        copy.setGravity(Gravity.CENTER);
        copy.setBackground(rounded(done ? Color.rgb(186, 200, 206) : ACCENT_WATER, 999));
        copy.setPadding(dp(16), dp(8), dp(16), dp(8));
        copy.setClickable(true);
        copy.setFocusable(true);
        copy.setContentDescription("复制取件码");
        copy.setOnClickListener(v -> {
            if (done) {
                togglePicked(item);
            } else {
                copyCode(item.code);
            }
        });
        codeRow.addView(copy);

        card.addView(codeRow);

        // ---- 删除只走右滑：卡片左滑露出垃圾桶 ----
        SwipeRow swipe = new SwipeRow();
        swipe.setLayoutParams(verticalParams(dp(12)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        row.addView(card);
        row.addView(buildSwipeDeleteAction(item, swipe));
        swipe.addView(row);

        card.setClickable(true);
        card.setOnClickListener(v -> swipe.smoothScrollTo(0, 0));
        return swipe;
    }

    private final class SwipeRow extends HorizontalScrollView {
        SwipeRow() {
            super(MainActivity.this);
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(View.OVER_SCROLL_NEVER);
            setOnTouchListener((v, event) -> {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    int actionWidth = dp(SWIPE_ACTION_WIDTH);
                    final int target = getScrollX() > actionWidth / 2 ? actionWidth : 0;
                    post(() -> smoothScrollTo(target, 0));
                }
                return false;
            });
        }

        @Override
        public void fling(int velocityX) {
            // 关掉惯性甩动，避免一下滑过头
        }
    }

    private View buildSwipeDeleteAction(PackageStore.Item item, final SwipeRow swipe) {
        LinearLayout action = new LinearLayout(this);
        action.setOrientation(LinearLayout.VERTICAL);
        action.setGravity(Gravity.CENTER);
        action.setBackground(gradient(new int[]{Color.rgb(236, 100, 94), Color.rgb(204, 60, 56)}, 18));
        action.setMinimumHeight(dp(96));
        action.setPadding(dp(8), dp(16), dp(8), dp(16));
        action.setClickable(true);
        action.setFocusable(true);
        action.setContentDescription("删除包裹");
        action.setLayoutParams(new LinearLayout.LayoutParams(
                dp(SWIPE_ACTION_WIDTH), LinearLayout.LayoutParams.WRAP_CONTENT));
        action.setOnClickListener(v -> {
            swipe.smoothScrollTo(0, 0);
            confirmDelete(item);
        });

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_delete);
        icon.setColorFilter(Color.WHITE);
        action.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        TextView label = text("删除", 12, Color.WHITE, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(4);
        label.setLayoutParams(labelParams);
        action.addView(label);
        return action;
    }

    private void showAddDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(18), dp(22), 0);

        EditText carrierInput = inputField("快递公司 / 平台（如：中通、菜鸟驿站）");
        box.addView(carrierInput);

        EditText codeInput = inputField("取件码（如：3-1-2040）");
        LinearLayout.LayoutParams codeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        codeParams.topMargin = dp(10);
        codeInput.setLayoutParams(codeParams);
        box.addView(codeInput);

        EditText stationInput = inputField("驿站位置（可选，如：南区菜鸟驿站）");
        LinearLayout.LayoutParams stationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        stationParams.topMargin = dp(10);
        stationInput.setLayoutParams(stationParams);
        box.addView(stationInput);

        new AlertDialog.Builder(this)
                .setTitle("添加包裹")
                .setView(box)
                .setPositiveButton("保存", (dialog, which) -> {
                    String code = codeInput.getText().toString().trim();
                    if (code.isEmpty()) {
                        Toast.makeText(this, "没有填写取件码，未保存", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PackageStore.Item item = new PackageStore.Item();
                    item.carrier = carrierInput.getText().toString().trim();
                    item.code = code;
                    item.station = stationInput.getText().toString().trim();
                    item.createdAt = System.currentTimeMillis();
                    List<PackageStore.Item> items = PackageStore.load(this);
                    items.add(0, item);
                    PackageStore.save(this, items);
                    refreshPackageList();
                    Toast.makeText(this, "已添加包裹", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void confirmDelete(PackageStore.Item target) {
        new AlertDialog.Builder(this)
                .setTitle("删除包裹")
                .setMessage("确定删除取件码 " + target.code + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    List<PackageStore.Item> items = PackageStore.load(this);
                    List<PackageStore.Item> remaining = new ArrayList<>();
                    for (PackageStore.Item item : items) {
                        boolean same = item.createdAt == target.createdAt
                                && item.code.equals(target.code);
                        if (!same) {
                            remaining.add(item);
                        }
                    }
                    PackageStore.save(this, remaining);
                    refreshPackageList();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void copyCode(String code) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("取件码", code));
        }
        Toast.makeText(this, "已复制 " + code, Toast.LENGTH_SHORT).show();
    }

    // =====================================================================
    // 自动获取：短信权限、通知使用权、剪贴板捕获
    // =====================================================================

    private boolean hasSmsPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(android.Manifest.permission.RECEIVE_SMS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        if (hasSmsPermission()) {
            Toast.makeText(this, "短信权限已开启，收到取件短信会自动保存", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions(new String[]{android.Manifest.permission.RECEIVE_SMS},
                REQUEST_SMS_PERMISSION);
    }

    private void openNotificationListenerSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception ignored) {
            Toast.makeText(this, "无法打开设置，请手动在系统设置里开启通知使用权",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAutoStatus() {
        if (smsStatusView != null) {
            smsStatusView.setText(hasSmsPermission()
                    ? "已开启 · 收到取件短信自动保存"
                    : "未授权 · 点击授权后自动识别取件短信");
        }
        if (notifyStatusView != null) {
            notifyStatusView.setText(PickupNotificationListener.isEnabled(this)
                    ? "已开启 · 菜鸟/淘宝/拼多多通知里的取件码自动保存"
                    : "未开启 · 点击前往系统设置开启通知使用权");
        }
    }

    private void checkClipboard() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) {
            return;
        }
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return;
        }
        CharSequence raw = clip.getItemAt(0).getText();
        if (raw == null) {
            return;
        }
        String value = raw.toString().trim();
        if (value.isEmpty() || PackageStore.isClipHandled(this, value)) {
            return;
        }

        CodeParser.Result parsed = CodeParser.parse(value);
        String code = parsed != null ? parsed.code : CodeParser.parseBareCode(value);
        if (code == null || code.isEmpty()) {
            return;
        }
        PackageStore.markClipHandled(this, value);

        List<PackageStore.Item> existing = PackageStore.load(this);
        for (PackageStore.Item item : existing) {
            if (code.equals(item.code)) {
                return;
            }
        }

        String carrier = parsed != null ? parsed.carrier : "";
        String station = parsed != null ? parsed.station : "";
        showClipDialog(code, carrier, station);
    }

    private void showClipDialog(String code, String carrier, String station) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(18), dp(22), 0);

        TextView tip = text("检测到取件码：" + code, 13, ACCENT_WATER, Typeface.BOLD);
        box.addView(tip);

        EditText carrierInput = inputField("快递公司 / 平台（可留空）");
        carrierInput.setText(carrier);
        LinearLayout.LayoutParams carrierParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        carrierParams.topMargin = dp(12);
        carrierInput.setLayoutParams(carrierParams);
        box.addView(carrierInput);

        EditText stationInput = inputField("驿站位置（可留空）");
        stationInput.setText(station);
        LinearLayout.LayoutParams stationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        stationParams.topMargin = dp(10);
        stationInput.setLayoutParams(stationParams);
        box.addView(stationInput);

        new AlertDialog.Builder(this)
                .setTitle("保存这个取件码？")
                .setView(box)
                .setPositiveButton("保存", (dialog, which) -> {
                    PackageStore.addIfAbsent(this,
                            carrierInput.getText().toString().trim(),
                            code,
                            stationInput.getText().toString().trim());
                    refreshPackageList();
                })
                .setNegativeButton("忽略", null)
                .show();
    }

    private View settingsRow(
            String emoji,
            String title,
            TextView subtitleView,
            View.OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(14), dp(14));
        row.setBackground(rounded(CARD_WHITE, 18));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);
        row.setLayoutParams(verticalParams(dp(12)));

        TextView emojiView = text(emoji, 18, TEXT_PRIMARY, Typeface.NORMAL);
        row.addView(emojiView, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(10), 0, dp(8), 0);

        labels.addView(text(title, 15, TEXT_PRIMARY, Typeface.BOLD));
        subtitleView.setLineSpacing(0, 1.4f);
        labels.addView(subtitleView);

        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 24, ACCENT_WATER, Typeface.NORMAL);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(26), dp(34)));
        return row;
    }

    private EditText inputField(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(14);
        input.setTextColor(TEXT_PRIMARY);
        input.setHintTextColor(TEXT_SECONDARY);
        GradientDrawable background = rounded(Color.rgb(242, 246, 249), 12);
        background.setStroke(dp(1), LINE_COLOR);
        input.setBackground(background);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMaxLines(2);
        return input;
    }

    private int[] colorsForCarrierName(String name) {
        String value = name == null ? "" : name;
        if (value.contains("菜鸟")) return CAINIAO_COLORS;
        if (value.contains("淘")) return TAOBAO_COLORS;
        if (value.contains("拼") || value.contains("多多")) return PDD_COLORS;
        if (value.contains("京东")) return JD_COLORS;
        if (value.contains("小红书")) return XHS_COLORS;
        if (value.contains("抖音")) return DOUYIN_COLORS;
        if (value.contains("中通") || value.contains("圆通") || value.contains("申通")
                || value.contains("韵达") || value.contains("极兔")) {
            return CAINIAO_COLORS;
        }
        return new int[]{Color.rgb(63, 145, 190), Color.rgb(31, 106, 140)};
    }

    // =====================================================================
    // 通用小组件
    // =====================================================================

    private TextView sectionTitle(String value, int topMarginDp) {
        TextView view = text(value, 17, TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams params = verticalParams(topMarginDp);
        params.bottomMargin = dp(10);
        view.setLayoutParams(params);
        return view;
    }

    private View hintCard(String emoji, String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setLayoutParams(verticalParams(dp(12)));

        GradientDrawable background = rounded(Color.rgb(242, 249, 252), 18);
        background.setStroke(dp(1), Color.rgb(211, 230, 239));
        card.setBackground(background);

        TextView emojiView = text(emoji, 20, TEXT_PRIMARY, Typeface.NORMAL);
        LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emojiParams.rightMargin = dp(10);
        emojiView.setLayoutParams(emojiParams);
        card.addView(emojiView);

        TextView messageView = text(message, 12, Color.rgb(61, 92, 110), Typeface.NORMAL);
        messageView.setLineSpacing(0, 1.5f);
        card.addView(messageView, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private void addPinButton(
            LinearLayout parent,
            Destination destination,
            String label
    ) {
        TextView button = text(label, 13, TEXT_PRIMARY, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        GradientDrawable background = rounded(CARD_WHITE, 10);
        background.setStroke(dp(1), LINE_COLOR);
        button.setBackground(background);
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription("将" + destination.title + "添加到桌面");
        button.setOnClickListener(view -> ShortcutPinning.request(this, destination));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
        if (parent.getChildCount() > 0) {
            params.leftMargin = dp(8);
        }
        parent.addView(button, params);
    }

    private TextView text(String value, float sizeSp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", style));
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams verticalParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private LinearLayout.LayoutParams horizontalWeightParams(int leftMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.leftMargin = dp(leftMarginDp);
        return params;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable gradient(int[] colors, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                colors
        );
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
