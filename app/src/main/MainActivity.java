package com.nikahbridge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
/*
 * BEST NIKAH BRIDGE
 * MainActivity.java - stronger V4 client foundation
 *
 * IMPORTANT:
 * This Activity is deliberately dependency-free and can compile in the existing
 * simple Android project without adding Firebase/Ads/Billing libraries.
 *
 * It is NOT a substitute for a secure production backend. Real production
 * authentication, cloud profiles, server matching, document verification,
 * mutual connections, chat authorization, moderation, rewarded ads,
 * Google Play Billing and server-side account deletion must be connected
 * outside this Activity before public production launch.
 *
 * This file never presents local/demo data as genuine verified users.
 */
public class MainActivity extends Activity {

    private LinearLayout root;
    private SharedPreferences prefs;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private static final String PREFS = "best_nikah_bridge_v2";

    private static final String K_ONBOARDING = "onboarding_done";
    private static final String K_CONSENT = "safety_consent";
    private static final String K_PROFILE = "profile_saved";
    private static final String K_NAME = "profile_name";
    private static final String K_AGE = "profile_age";
    private static final String K_COUNTRY = "profile_country";
    private static final String K_CITY = "profile_city";
    private static final String K_GENDER = "profile_gender";
    private static final String K_MARITAL = "profile_marital";
    private static final String K_INTENTION = "profile_intention";
    private static final String K_PRACTICE = "profile_practice";
    private static final String K_ABOUT = "profile_about";
    private static final String K_PREFERENCE = "profile_preference";
    private static final String K_TIMELINE = "profile_timeline";
    private static final String K_FAMILY = "profile_family";
    private static final String K_DEALBREAKERS = "profile_dealbreakers";
    private static final String K_CHAPERONE = "chaperone_enabled";
    private static final String K_CHAPERONE_EMAIL = "chaperone_email";
    private static final String K_PHOTO_PRIVACY = "photo_privacy";
    private static final String K_LANGUAGE = "language";

    private static final String K_VERIFY = "verification_status";
    private static final String K_INTERESTS = "interests";
    private static final String K_INCOMING = "incoming_interests";
    private static final String K_ACCEPTED = "accepted_connections";
    private static final String K_BLOCKED = "blocked_profiles";
    private static final String K_REPORTED = "reported_profiles";
    private static final String K_CHAT = "chat_messages";

    private static final String K_AD_DAY = "ad_day";
    private static final String K_AD_USED = "ad_used";
    private static final String K_PREMIUM = "premium_demo";

    private final int primary = Color.rgb(25, 105, 88);
    private final int primaryDark = Color.rgb(18, 67, 58);
    private final int dark = Color.rgb(28, 45, 41);
    private final int gray = Color.rgb(96, 112, 106);
    private final int light = Color.rgb(247, 250, 249);
    private final int white = Color.WHITE;
    private final int border = Color.rgb(218, 229, 224);
    private final int warning = Color.rgb(156, 92, 18);
    private final int danger = Color.rgb(160, 45, 45);

    private final Set<String> sentInterests = new HashSet<>();
    private final Set<String> incomingInterests = new HashSet<>();
    private final Set<String> acceptedConnections = new HashSet<>();
    private final Set<String> blockedProfiles = new HashSet<>();
    private final Set<String> reportedProfiles = new HashSet<>();

    private static final String[] DEMO_NAMES = {
            "Ayesha", "Fatima", "Maryam", "Zainab", "Hafsa",
            "Sana", "Hira", "Amna"
    };

    private static final String[] DEMO_AGES = {
            "27", "29", "26", "30", "28", "25", "31", "27"
    };

    private static final String[] DEMO_COUNTRIES = {
            "Pakistan", "Saudi Arabia", "Germany", "United Kingdom",
            "United Arab Emirates", "Pakistan", "Saudi Arabia", "Germany"
    };

    private static final String[] DEMO_CITIES = {
            "Lahore", "Jeddah", "Berlin", "London",
            "Dubai", "Islamabad", "Riyadh", "Munich"
    };

    private static final String[] DEMO_ABOUT = {
            "Serious about Nikah, family values, honesty and respectful communication.",
            "Looking for a sincere Muslim marriage with mutual respect and family involvement.",
            "Interested in a values-based family life and a responsible marriage.",
            "Values honesty, family, responsibility and a clear intention for Nikah.",
            "Looking for a compatible spouse with good character and serious marriage intention.",
            "Family-oriented and interested in a respectful, halal path toward marriage.",
            "Serious about marriage and building a stable family with mutual respect.",
            "Looking for a sincere spouse and a clear, respectful path toward Nikah."
    };

    private static final String[] DEMO_REASONS = {
            "Family-oriented • Serious Nikah",
            "Respectful • Serious intention",
            "Educated • Values-based",
            "Family values • Responsible",
            "Responsible • Marriage-focused",
            "Family-oriented • Honest",
            "Serious Nikah • Respectful",
            "Values • Compatibility"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        loadSets();
        
        if (!prefs.getBoolean(K_ONBOARDING, false)) {
            showWelcome();
        } else {
            showHome();
        }
    }

    @Override
    public void onBackPressed() {
        showHome();
    }

    private boolean isUrdu() {
        return "Urdu".equals(prefs.getString(K_LANGUAGE, "English"));
    }

    private String tr(String en, String ur) {
        return isUrdu() ? ur : en;
    }

    private void toast(String en, String ur) {
        Toast.makeText(this, tr(en, ur), Toast.LENGTH_LONG).show();
    }

    private String todayKey() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private void loadSets() {
        sentInterests.clear();
        incomingInterests.clear();
        acceptedConnections.clear();
        blockedProfiles.clear();
        reportedProfiles.clear();

        sentInterests.addAll(prefs.getStringSet(K_INTERESTS, new HashSet<String>()));
        incomingInterests.addAll(prefs.getStringSet(K_INCOMING, new HashSet<String>()));
        acceptedConnections.addAll(prefs.getStringSet(K_ACCEPTED, new HashSet<String>()));
        blockedProfiles.addAll(prefs.getStringSet(K_BLOCKED, new HashSet<String>()));
        reportedProfiles.addAll(prefs.getStringSet(K_REPORTED, new HashSet<String>()));
    }

    private void saveSets() {
        prefs.edit()
                .putStringSet(K_INTERESTS, new HashSet<>(sentInterests))
                .putStringSet(K_INCOMING, new HashSet<>(incomingInterests))
                .putStringSet(K_ACCEPTED, new HashSet<>(acceptedConnections))
                .putStringSet(K_BLOCKED, new HashSet<>(blockedProfiles))
                .putStringSet(K_REPORTED, new HashSet<>(reportedProfiles))
                .apply();
    }

    private void setupRoot(boolean top) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(light);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(top ? Gravity.TOP : Gravity.CENTER_HORIZONTAL);
        root.setPadding(22, 24, 22, 30);

        scroll.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT
        ));

        setContentView(scroll);
    }

    private TextView title(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(dark);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 12, 12, 12);
        return t;
    }

    private TextView subtitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(gray);
        t.setGravity(Gravity.CENTER);
        t.setPadding(12, 8, 12, 20);
        return t;
    }

    private TextView body(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(16);
        t.setTextColor(dark);
        t.setPadding(8, 10, 8, 18);
        return t;
    }

    private Button appButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setTextColor(white);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(58);
        b.setPadding(14, 8, 14, 8);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(primary);
        bg.setCornerRadius(18);
        b.setBackground(bg);
        return b;
    }

    private Button outlineButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(primary);
        b.setMinHeight(54);
        b.setPadding(14, 8, 14, 8);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(white);
        bg.setStroke(2, primary);
        bg.setCornerRadius(18);
        b.setBackground(bg);
        return b;
    }

    private Button dangerButton(String text) {
        Button b = outlineButton(text);
        b.setTextColor(danger);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(white);
        bg.setStroke(2, danger);
        bg.setCornerRadius(18);
        b.setBackground(bg);
        return b;
    }

    private void addFull(Button b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 62);
        p.setMargins(0, 6, 0, 6);
        root.addView(b, p);
    }

    private void addSpace(int h) {
        View v = new View(this);
        root.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h));
    }

    private void section(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(18);
        t.setTextColor(dark);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(4, 18, 4, 8);
        root.addView(t);
    }

    private void addInput(EditText e, int h) {
        e.setTextSize(16);
        e.setTextColor(dark);
        e.setHintTextColor(gray);
        e.setPadding(16, 8, 16, 8);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h);
        p.setMargins(0, 0, 0, 8);
        root.addView(e, p);
    }

    private Spinner addSpinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        root.addView(s, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 58));
        return s;
    }

    private int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) return i;
        }
        return 0;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(20, 18, 20, 18);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(white);
        bg.setCornerRadius(22);
        bg.setStroke(1, border);
        c.setBackground(bg);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, 14);
        root.addView(c, p);
        return c;
    }

    private TextView cardText(String text, float size, int color) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setPadding(0, 3, 0, 5);
        return t;
    }

    private String profileKey(int i) {
        return DEMO_NAMES[i] + "|" + DEMO_AGES[i] + "|" + DEMO_COUNTRIES[i];
    }

    private String profileKey(String name, String age, String country) {
        return name + "|" + age + "|" + country;
    }

    private int profileCompletion() {
        String[] keys = {
                K_NAME, K_AGE, K_COUNTRY, K_CITY, K_GENDER,
                K_MARITAL, K_INTENTION, K_PRACTICE, K_ABOUT, K_PREFERENCE,
                K_TIMELINE, K_FAMILY, K_DEALBREAKERS
        };

        int done = 0;
        for (String k : keys) {
            String v = prefs.getString(k, "").trim();
            if (!v.isEmpty() && !v.toLowerCase(Locale.US).contains("select")) {
                done++;
            }
        }
        return done * 10;
    }

    private void showWelcome() {
        setupRoot(false);
        addSpace(30);

        root.addView(title("Best Nikah Bridge", 34));
        root.addView(subtitle(tr(
                "Trusted • Simple • Safe • Serious Nikah",
                "اعتماد • آسان • محفوظ • سنجیدہ نکاح"
        )));

        LinearLayout c = card();
        c.addView(cardText(tr(
                "A serious Muslim matrimonial platform — not a dating app.",
                "سنجیدہ مسلم نکاح کا پلیٹ فارم — ڈیٹنگ ایپ نہیں۔"
        ), 18, dark));
        c.addView(cardText(tr(
                "Privacy first • Respectful connections • Mutual consent before chat",
                "رازداری پہلے • باوقار رابطے • چیٹ سے پہلے باہمی رضامندی"
        ), 15, gray));

        CheckBox agree = new CheckBox(this);
        agree.setText(tr(
                "I understand and agree to use Best Nikah Bridge for serious matrimonial purposes.",
                "میں سمجھتا/سمجھتی ہوں اور Best Nikah Bridge کو سنجیدہ نکاح کے مقصد کے لیے استعمال کرنے سے اتفاق کرتا/کرتی ہوں۔"
        ));
        agree.setTextSize(15);
        agree.setTextColor(dark);
        c.addView(agree);

        Button start = appButton(tr("Start Best Nikah Bridge", "Best Nikah Bridge شروع کریں"));
        addFull(start);

        Button safety = outlineButton(tr("Safety & Privacy", "حفاظت اور رازداری"));
        addFull(safety);

        start.setOnClickListener(v -> {
            if (!agree.isChecked()) {
                toast("Please accept the safety/usage agreement first.",
                        "پہلے حفاظتی/استعمال کی شرائط قبول کریں۔");
                return;
            }

            prefs.edit()
                    .putBoolean(K_ONBOARDING, true)
                    .putBoolean(K_CONSENT, true)
                    .apply();

            showHome();
        });

        safety.setOnClickListener(v -> showSafety());
    }

    private void showHome() {
        setupRoot(false);
        addSpace(8);

        root.addView(title("Best Nikah Bridge", 32));
        root.addView(subtitle(tr(
                "Trusted • Simple • Safe • Serious Nikah",
                "اعتماد • آسان • محفوظ • سنجیدہ نکاح"
        )));

        if (prefs.getBoolean(K_PROFILE, false)) {
            TextView s = cardText(
                    "✓ " + tr("Profile ready • ", "پروفائل تیار • ")
                            + prefs.getString(K_NAME, "")
                            + " • " + profileCompletion() + "% "
                            + tr("complete", "مکمل"),
                    15, primary);
            s.setGravity(Gravity.CENTER);
            root.addView(s);
        } else {
            root.addView(subtitle(tr(
                    "Build an honest profile first. No dating — only serious matrimonial intent.",
                    "پہلے ایماندار پروفائل بنائیں۔ ڈیٹنگ نہیں — صرف سنجیدہ نکاح کا مقصد۔"
            )));
        }

        Button profile = appButton(tr("Create / Edit Profile", "پروفائل بنائیں / تبدیل کریں"));
        Button dashboard = appButton(tr("My Dashboard", "میرا ڈیش بورڈ"));
        Button matches = appButton(tr("Recommended Matches", "تجویز کردہ میچز"));
        Button readiness = appButton(tr("Nikah Readiness & Preferences", "نکاح تیاری اور ترجیحات"));
        Button interests = appButton(tr("Interests & Connections", "دلچسپیاں اور روابط"));
        Button safety = outlineButton(tr("Safety Center", "سیفٹی سینٹر"));
        Button trust = outlineButton(tr("Trust & Family Mode", "اعتماد اور فیملی موڈ"));
        Button help = outlineButton(tr("Help & Nikah Guidance", "مدد اور نکاح رہنمائی"));
        Button settings = outlineButton(tr("Account & Settings", "اکاؤنٹ اور سیٹنگز"));

        addFull(profile);
        addFull(dashboard);
        addFull(matches);
        addFull(readiness);
        addFull(interests);
        addFull(safety);
        addFull(trust);
        addFull(help);
        addFull(settings);

        profile.setOnClickListener(v -> showProfile());
        dashboard.setOnClickListener(v -> showDashboard());
        matches.setOnClickListener(v -> showMatches());
        readiness.setOnClickListener(v -> showReadiness());
        interests.setOnClickListener(v -> showInterests());
        safety.setOnClickListener(v -> showSafety());
        trust.setOnClickListener(v -> showTrustFamily());
        help.setOnClickListener(v -> showHelp());
        settings.setOnClickListener(v -> showSettings());

        addSpace(12);

        root.addView(subtitle(tr(
                "Privacy first • Mutual connection before safe communication • Respectful Nikah only",
                "رازداری پہلے • محفوظ گفتگو سے پہلے باہمی رابطہ • صرف باوقار نکاح"
        )));
    }

    private void showProfile() {
        setupRoot(true);

        root.addView(title(
                tr("Create Your Nikah Profile", "اپنا نکاح پروفائل بنائیں"), 28));
        root.addView(subtitle(tr(
                "Complete information improves trust and future matching.",
                "مکمل معلومات اعتماد اور بہتر میچنگ میں مدد دیتی ہیں۔"
        )));

        EditText name = new EditText(this);
        name.setHint(tr("Full name", "پورا نام"));
        addInput(name, 60);

        EditText age = new EditText(this);
        age.setHint(tr("Age (18+)", "عمر (18+)"));
        age.setInputType(InputType.TYPE_CLASS_NUMBER);
        addInput(age, 60);

        section(tr("Basic Information", "بنیادی معلومات"));

        String[] genders = {
                tr("Select gender", "جنس منتخب کریں"),
                tr("Male", "مرد"),
                tr("Female", "خاتون")
        };
        Spinner gender = addSpinner(genders);

        String[] marital = {
                tr("Select marital status", "ازدواجی حیثیت منتخب کریں"),
                tr("Never married", "کبھی شادی نہیں ہوئی"),
                tr("Divorced", "طلاق یافتہ"),
                tr("Widowed", "بیوہ / بیوہ مرد")
        };
        Spinner marital = addSpinner(marital);

        EditText country = new EditText(this);
        country.setHint(tr("Country", "ملک"));
        addInput(country, 60);

        EditText city = new EditText(this);
        city.setHint(tr("City", "شہر"));
        addInput(city, 60);

        section(tr("Nikah & Values", "نکاح اور اقدار"));

        String[] intention = {
                tr("Select marriage intention", "نکاح کا مقصد منتخب کریں"),
                tr("Serious Nikah", "سنجیدہ نکاح"),
                tr("Ready for marriage", "شادی کے لیے تیار"),
                tr("Exploring marriage", "شادی کے امکانات دیکھ رہا/رہی ہوں")
        };
        Spinner intentionSpinner = addSpinner(intention);

        String[] practice = {
                tr("Select religious practice", "دینی عمل منتخب کریں"),
                tr("Practicing Muslim", "دین پر عمل کرنے والا/والی"),
                tr("Moderately practicing", "درمیانہ دینی عمل"),
                tr("Prefer not to say", "بتانا پسند نہیں")
        };
        Spinner practiceSpinner = addSpinner(practice);

        EditText about = new EditText(this);
        about.setHint(tr(
                "About yourself, family values and marriage goals",
                "اپنے بارے میں، خاندانی اقدار اور نکاح کے مقاصد"
        ));
        about.setGravity(Gravity.TOP);
        about.setMinLines(4);
        about.setPadding(16, 16, 16, 16);
        root.addView(about, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 145));

        section(tr("Partner Preferences", "شریکِ حیات کی ترجیحات"));

        EditText preference = new EditText(this);
        preference.setHint(tr(
                "Preferred age, country/city, values, education and important preferences",
                "پسندیدہ عمر، ملک/شہر، اقدار، تعلیم اور اہم ترجیحات"
        ));
        preference.setGravity(Gravity.TOP);
        preference.setMinLines(4);
        preference.setPadding(16, 16, 16, 16);
        root.addView(preference, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 135));

        section(tr("Serious Marriage Preferences", "سنجیدہ نکاح کی ترجیحات"));

        String[] timelines = {
                tr("Select marriage timeline", "نکاح کی مدت منتخب کریں"),
                tr("As soon as compatible", "موزوں رشتہ ملتے ہی"),
                tr("Within 6 months", "6 ماہ کے اندر"),
                tr("Within 1 year", "1 سال کے اندر"),
                tr("1–2 years", "1–2 سال"),
                tr("Open to discussion", "باہمی مشورے کے لیے تیار")
        };
        Spinner timelineSpinner = addSpinner(timelines);

        String[] familyOptions = {
                tr("Select family involvement", "خاندانی شمولیت منتخب کریں"),
                tr("Family involved early", "ابتدا ہی سے خاندان شامل ہو"),
                tr("Family after mutual interest", "باہمی دلچسپی کے بعد خاندان شامل ہو"),
                tr("Prefer private first, then family", "پہلے نجی طور پر، پھر خاندان"),
                tr("Open to wali/guardian involvement", "ولی/سرپرست کی شمولیت کے لیے تیار")
        };
        Spinner familySpinner = addSpinner(familyOptions);

        EditText dealbreakers = new EditText(this);
        dealbreakers.setHint(tr(
                "Important non-negotiables / deal-breakers",
                "اہم ناقابلِ سمجھوتہ ترجیحات / شرائط"));
        dealbreakers.setGravity(Gravity.TOP);
        dealbreakers.setMinLines(3);
        dealbreakers.setPadding(16, 16, 16, 16);
        root.addView(dealbreakers, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 120));

        section(tr("Profile Safety", "پروفائل سیفٹی"));

        CheckBox family = new CheckBox(this);
        family.setText(tr(
                "I understand that contact details should stay private until a genuine mutual connection.",
                "میں سمجھتا/سمجھتی ہوں کہ حقیقی باہمی رابطے تک رابطے کی معلومات نجی رہنی چاہئیں۔"
        ));
        family.setTextSize(15);
        family.setTextColor(dark);
        root.addView(family);

        if (prefs.getBoolean(K_PROFILE, false)) {
            name.setText(prefs.getString(K_NAME, ""));
            age.setText(prefs.getString(K_AGE, ""));
            country.setText(prefs.getString(K_COUNTRY, ""));
            city.setText(prefs.getString(K_CITY, ""));
            about.setText(prefs.getString(K_ABOUT, ""));
            preference.setText(prefs.getString(K_PREFERENCE, ""));
            dealbreakers.setText(prefs.getString(K_DEALBREAKERS, ""));

            gender.setSelection(indexOf(genders, prefs.getString(K_GENDER, "")));
            marital.setSelection(indexOf(marital, prefs.getString(K_MARITAL, "")));
            intentionSpinner.setSelection(indexOf(
                    intention, prefs.getString(K_INTENTION, "")));
            practiceSpinner.setSelection(indexOf(
                    practice, prefs.getString(K_PRACTICE, "")));
            timelineSpinner.setSelection(indexOf(
                    timelines, prefs.getString(K_TIMELINE, "")));
            familySpinner.setSelection(indexOf(
                    familyOptions, prefs.getString(K_FAMILY, "")));

            family.setChecked(true);
        }

        section(tr("Verification Status", "تصدیق کی حیثیت"));

        String verifyStatus = prefs.getString(K_VERIFY, "Not requested");
        root.addView(body(tr(
                "Current status: " + verifyStatus
                        + "\n\nProduction verification must be performed by a secure server/admin review. "
                        + "Never upload identity documents through ordinary chat.",
                "موجودہ حیثیت: " + verifyStatus
                        + "\n\nپروڈکشن تصدیق محفوظ سرور/ایڈمن ریویو کے ذریعے ہونی چاہیے۔ "
                        + "شناختی دستاویزات عام چیٹ میں کبھی اپ لوڈ نہ کریں۔"
        )));

        Button save = appButton(tr("Save Profile", "پروفائل محفوظ کریں"));
        Button verify = outlineButton(tr("Request Verification", "تصدیق کی درخواست"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(save);
        addFull(verify);
        addFull(back);

        save.setOnClickListener(v -> {
            String n = name.getText().toString().trim();
            String a = age.getText().toString().trim();
            String c = country.getText().toString().trim();
            String ci = city.getText().toString().trim();
            String g = gender.getSelectedItem().toString();
            String m = marital.getSelectedItem().toString();
            String in = intentionSpinner.getSelectedItem().toString();
            String pr = practiceSpinner.getSelectedItem().toString();
            String ab = about.getText().toString().trim();
            String pref = preference.getText().toString().trim();
            String timeline = timelineSpinner.getSelectedItem().toString();
            String familyOption = familySpinner.getSelectedItem().toString();
            String deal = dealbreakers.getText().toString().trim();

            if (n.length() < 2) {
                name.setError(tr("Enter your name", "اپنا نام لکھیں"));
                return;
            }

            int numericAge;
            try {
                numericAge = Integer.parseInt(a);
            } catch (Exception e) {
                age.setError(tr("Enter a valid age", "درست عمر لکھیں"));
                return;
            }

            if (numericAge < 18 || numericAge > 100) {
                age.setError(tr(
                        "Age must be between 18 and 100",
                        "عمر 18 سے 100 سال کے درمیان ہونی چاہیے"));
                return;
            }

            if (c.isEmpty()) {
                country.setError(tr("Enter your country", "اپنا ملک لکھیں"));
                return;
            }

            if (ci.isEmpty()) {
                city.setError(tr("Enter your city", "اپنا شہر لکھیں"));
                return;
            }

            if (gender.getSelectedItemPosition() == 0
                    || marital.getSelectedItemPosition() == 0
                    || intentionSpinner.getSelectedItemPosition() == 0
                    || practiceSpinner.getSelectedItemPosition() == 0) {
                toast("Please complete all basic selections.",
                        "براہ کرم بنیادی تمام انتخاب مکمل کریں۔");
                return;
            }

            if (ab.length() < 30) {
                about.setError(tr(
                        "Please write at least 30 characters",
                        "کم از کم 30 حروف لکھیں"));
                return;
            }

            if (pref.length() < 15) {
                preference.setError(tr(
                        "Please add your partner preferences",
                        "شریکِ حیات کی ترجیحات لکھیں"));
                return;
            }

            if (timelineSpinner.getSelectedItemPosition() == 0
                    || familySpinner.getSelectedItemPosition() == 0) {
                toast(
                        "Please choose your marriage timeline and family involvement preference.",
                        "نکاح کی مدت اور خاندانی شمولیت کی ترجیح منتخب کریں۔");
                return;
            }

            if (deal.length() < 5) {
                dealbreakers.setError(tr(
                        "Add at least one important boundary",
                        "کم از کم ایک اہم شرط یا حد لکھیں"));
                return;
            }

            if (!family.isChecked()) {
                toast("Please confirm the privacy/safety statement.",
                        "براہ کرم رازداری/سیفٹی بیان کی تصدیق کریں۔");
                return;
            }

            if (mAuth.getCurrentUser() == null) {
    mAuth.signInAnonymously()
            .addOnSuccessListener(authResult -> saveRealProfile(
                    n, a, c, ci, g, m, in, pr, ab, pref,
                    timeline, familyOption, deal
            ))
            .addOnFailureListener(e ->
                    toast(
                            "Account setup failed. Please try again.",
                            "اکاؤنٹ بنانے میں مسئلہ آیا، دوبارہ کوشش کریں۔"
                    )
            );
} else {
    saveRealProfile(
            n, a, c, ci, g, m, in, pr, ab, pref,
            timeline, familyOption, deal
    );
            }

        verify.setOnClickListener(v -> {
            if (!prefs.getBoolean(K_PROFILE, false) || profileCompletion() < 100) {
                toast("Complete and save your profile first.",
                        "پہلے پروفائل مکمل اور محفوظ کریں۔");
                return;
            }

            prefs.edit().putString(K_VERIFY, "Requested").apply();
            toast("Verification request recorded locally. Production review must happen securely on the backend.",
                    "تصدیق کی درخواست مقامی طور پر ریکارڈ ہوئی۔ اصل ریویو محفوظ بیک اینڈ پر ہونا چاہیے۔");
            showProfile();
        });

        back.setOnClickListener(v -> showHome());
    }
    }   // line 819 — showProfile() ka closing 

    private void saveRealProfile(
        String n,
        String a,
        String c,
        String ci,
        String g,
        String m,
        String in,
        String pr,
        String ab,
        String pref,
        String timeline,
        String familyOption,
        String deal
) {
    if (mAuth.getCurrentUser() == null) {
        toast(
                "Account is not ready. Please try again.",
                "اکاؤنٹ ابھی تیار نہیں، دوبارہ کوشش کریں۔"
        );
        return;
    }

    String uid = mAuth.getCurrentUser().getUid();

    Map<String, Object> profile = new HashMap<>();

    profile.put("uid", uid);
    profile.put("name", n);
    profile.put("age", Integer.parseInt(a));
    profile.put("country", c);
    profile.put("city", ci);
    profile.put("gender", g);
    profile.put("maritalStatus", m);
    profile.put("marriageIntent", in);
    profile.put("religiousPractice", pr);
    profile.put("about", ab);
    profile.put("partnerPreference", pref);
    profile.put("marriageTimeline", timeline);
    profile.put("familyInvolvement", familyOption);
    profile.put("dealbreakers", deal);
    profile.put("verificationStatus", "Not requested");
    profile.put("profileActive", true);
    profile.put("updatedAt", FieldValue.serverTimestamp());

    firestore.collection("users")
            .document(uid)
            .set(profile)
            .addOnSuccessListener(unused -> {

                prefs.edit()
                        .putBoolean(K_PROFILE, true)
                        .putString(K_NAME, n)
                        .putString(K_AGE, a)
                        .putString(K_COUNTRY, c)
                        .putString(K_CITY, ci)
                        .putString(K_GENDER, g)
                        .putString(K_MARITAL, m)
                        .putString(K_INTENTION, in)
                        .putString(K_PRACTICE, pr)
                        .putString(K_ABOUT, ab)
                        .putString(K_PREFERENCE, pref)
                        .putString(K_TIMELINE, timeline)
                        .putString(K_FAMILY, familyOption)
                        .putString(K_DEALBREAKERS, deal)
                        .apply();

                toast(
                        "Profile saved securely.",
                        "پروفائل محفوظ ہو گیا۔"
                );

                showHome();
            })
            .addOnFailureListener(e ->
                    toast(
                            "Profile could not be saved. Please try again.",
                            "پروفائل محفوظ نہیں ہو سکا، دوبارہ کوشش کریں۔"
                    )
            );
    }


  private void showDashboard() {
        setupRoot(true);

        root.addView(title(tr("My Dashboard", "میرا ڈیش بورڈ"), 28));
        root.addView(subtitle(tr(
                "Your Best Nikah Bridge progress at a glance.",
                "Best Nikah Bridge پر آپ کی پیش رفت کا خلاصہ۔"
        )));

        infoCard(
                tr("Profile completion", "پروفائل مکمل"),
                profileCompletion() + "%");

        infoCard(
                tr("Verification", "تصدیق"),
                prefs.getString(K_VERIFY, "Not requested"));

        infoCard(
                tr("Interests sent", "بھیجی گئی دلچسپیاں"),
                String.valueOf(sentInterests.size()));

        infoCard(
                tr("Connections", "روابط"),
                String.valueOf(acceptedConnections.size()));

        infoCard(
                tr("Marriage timeline", "نکاح کی مدت"),
                prefs.getString(K_TIMELINE, "Not set"));

        infoCard(
                tr("Family preference", "خاندانی ترجیح"),
                prefs.getString(K_FAMILY, "Not set"));

        infoCard(
                tr("Profile quality", "پروفائل معیار"),
                profileQualityLabel());

        infoCard(
                tr("Blocked profiles", "بلاک پروفائلز"),
                String.valueOf(blockedProfiles.size()));

        infoCard(
                tr("Free message allowance", "مفت پیغام کی حد"),
                remainingAdMessages() + " / 2 demo");

        Button edit = appButton(tr("Complete / Edit Profile", "پروفائل مکمل / تبدیل کریں"));
        Button matches = appButton(tr("Find Better Matches", "بہتر میچز تلاش کریں"));
        Button connections = outlineButton(tr("My Connections", "میرے روابط"));
        Button safety = outlineButton(tr("Safety Center", "سیفٹی سینٹر"));
        Button trust = outlineButton(tr("Trust & Family Mode", "اعتماد اور فیملی موڈ"));
        Button back = outlineButton(tr("Back Home", "ہوم پر واپس"));

        addFull(edit);
        addFull(matches);
        addFull(connections);
        addFull(safety);
        addFull(back);

        edit.setOnClickListener(v -> showProfile());
        matches.setOnClickListener(v -> showMatches());
        connections.setOnClickListener(v -> showInterests());
        safety.setOnClickListener(v -> showSafety());
        back.setOnClickListener(v -> showHome());
    }

    private void infoCard(String label, String value) {
        LinearLayout c = card();
        c.addView(cardText(label, 14, gray));
        c.addView(cardText(value, 20, dark));
    }

    private int calculateCompatibility(int i) {
    int score = 72;

    String myCountry = prefs.getString(K_COUNTRY, "");
    String pref = prefs.getString(K_PREFERENCE, "").toLowerCase(Locale.US);
    String myIntention = prefs.getString(K_INTENTION, "").toLowerCase(Locale.US);

    // Country compatibility
    if (!myCountry.isEmpty()
            && DEMO_COUNTRIES[i].equalsIgnoreCase(myCountry)) {
        score += 8;
    }

    // Partner preference compatibility
    if (!pref.isEmpty()
            && (pref.contains(DEMO_COUNTRIES[i].toLowerCase(Locale.US))
            || pref.contains(DEMO_CITIES[i].toLowerCase(Locale.US)))) {
        score += 7;
    }

    // Marriage intention compatibility
    if (myIntention.contains("serious")
            || myIntention.contains("ready")) {
        score += 4;
    }

    // Marriage timeline compatibility
    String timeline = prefs.getString(K_TIMELINE, "").toLowerCase(Locale.US);
    if (timeline.contains("6 months")
            || timeline.contains("as soon")) {
        score += 3;
    }

    // Family involvement compatibility
    String familyPref = prefs.getString(K_FAMILY, "").toLowerCase(Locale.US);
    if (familyPref.contains("yes")
            || familyPref.contains("wali")) {
        score += 2;
    }

    // Age compatibility
    String age = prefs.getString(K_AGE, "");

    try {
        int mine = Integer.parseInt(age);
        int theirs = Integer.parseInt(DEMO_AGES[i]);
        int diff = Math.abs(mine - theirs);

        if (diff <= 3) {
            score += 7;
        } else if (diff <= 7) {
            score += 4;
        }
    } catch (Exception ignored) {
        // Keep base score if age is unavailable.
    }

    // Small deterministic variation so demo matches are not identical.
    score += (i % 3);

    // Keep compatibility within a sensible range.
    if (score > 97) {
        score = 97;
    }

    if (score < 60) {
        score = 60;
    }

    return score;
    }

    private String profileQualityLabel() {
        int completion = profileCompletion();
        String verification = prefs.getString(K_VERIFY, "Not requested");
        if ("Verified".equalsIgnoreCase(verification) && completion >= 90) {
            return tr("Strong + verified", "مضبوط + تصدیق شدہ");
        }
        if (completion >= 90) {
            return tr("Strong profile", "مضبوط پروفائل");
        }
        if (completion >= 70) {
            return tr("Good — finish a few items", "اچھی — چند چیزیں مکمل کریں");
        }
        return tr("Needs more detail", "مزید معلومات درکار ہیں");
    }

    private void showReadiness() {
        setupRoot(true);

        root.addView(title(
                tr("Nikah Readiness", "نکاح کی تیاری"), 28));

        root.addView(subtitle(tr(
                "Clear preferences reduce wasted conversations and improve serious matching.",
                "واضح ترجیحات غیر ضروری گفتگو کم کرتی ہیں اور سنجیدہ میچنگ بہتر بناتی ہیں."
        )));

        infoCard(tr("Profile quality", "پروفائل معیار"), profileQualityLabel());
        infoCard(tr("Marriage intention", "نکاح کا مقصد"),
                prefs.getString(K_INTENTION, "Not set"));
        infoCard(tr("Marriage timeline", "نکاح کی مدت"),
                prefs.getString(K_TIMELINE, "Not set"));
        infoCard(tr("Family involvement", "خاندانی شمولیت"),
                prefs.getString(K_FAMILY, "Not set"));

        section(tr("Before you connect", "رابطے سے پہلے"));
        root.addView(body(
                "✓ " + tr("Know your non-negotiables and boundaries.",
                        "اپنی اہم شرائط اور حدود واضح رکھیں۔") + "\n\n" +
                "✓ " + tr("Discuss marriage timeline respectfully.",
                        "نکاح کی مدت کے بارے میں احترام سے بات کریں۔") + "\n\n" +
                "✓ " + tr("Use mutual interest before private communication.",
                        "نجی گفتگو سے پہلے باہمی دلچسپی کا انتظار کریں۔") + "\n\n" +
                "✓ " + tr("Involve family/wali when appropriate and mutually agreed.",
                        "مناسب وقت پر باہمی رضامندی سے خاندان/ولی کو شامل کریں۔") + "\n\n" +
                "✓ " + tr("Never send money, OTPs, passwords or identity documents to another user.",
                        "کسی صارف کو رقم، OTP، پاس ورڈ یا شناختی دستاویزات نہ بھیجیں۔")
        ));

        Button edit = appButton(tr("Improve My Profile", "میرا پروفائل بہتر کریں"));
        Button matches = appButton(tr("See Recommended Matches", "تجویز کردہ میچز دیکھیں"));
        Button safety = outlineButton(tr("Open Safety Center", "سیفٹی سینٹر کھولیں"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(edit);
        addFull(matches);
        addFull(safety);
        addFull(back);

        edit.setOnClickListener(v -> showProfile());
        matches.setOnClickListener(v -> showMatches());
        safety.setOnClickListener(v -> showSafety());
        back.setOnClickListener(v -> showHome());
    }

    private void showMatches() {
    setuRoot(true);

    root.addView(title(
            tr("Recommended Matches", "تجویز کردہ رشتے"),
            28
    ));

    root.addView(subtitle(tr(
            "Real profiles from Best Nikah Bridge",
            "Best Nikah Bridge کے حقیقی پروفائلز"
    )));

    if (mAuth.getCurrentUser() == null) {
        root.addView(subtitle(tr(
                "Please sign in first.",
                "براہ کرم پہلے سائن اِن کریں۔"
        )));

        Button back = outlineButton(tr("Back", "واپس"));
        addFull(back);
        back.setOnClickListener(v -> showHome());
        return;
    }

    String currentUid = mAuth.getCurrentUser().getUid();

    root.addView(subtitle(tr(
            "Loading real profiles...",
            "حقیقی پروفائلز لوڈ ہو رہے ہیں..."
    )));

    firestore.collection("users")
            .get()
            .addOnSuccessListener(snapshot -> {

                root.removeAllViews();

                root.addView(title(
                        tr("Recommended Matches", "تجویز کردہ رشتے"),
                        28
                ));

                root.addView(subtitle(tr(
                        "Real profiles from Best Nikah Bridge",
                        "Best Nikah Bridge کے حقیقی پروفائلز"
                )));

                boolean found = false;

                for (com.google.firebase.firestore.DocumentSnapshot doc
                        : snapshot.getDocuments()) {

                    String uid = doc.getString("uid");

                    if (uid == null || uid.equals(currentUid)) {
                        continue;
                    }

                    Boolean active = doc.getBoolean("profileActive");

                    if (active != null && !active) {
                        continue;
                    }

                    if (blockedProfiles.contains(uid)) {
                        continue;
                    }

                    String name = doc.getString("name");
                    String country = doc.getString("country");
                    String city = doc.getString("city");
                    String gender = doc.getString("gender");
                    String maritalStatus = doc.getString("maritalStatus");
                    String intention = doc.getString("marriageIntent");
                    String practice = doc.getString("religiousPractice");
                    String about = doc.getString("about");
                    String verification =
                            doc.getString("verificationStatus");

                    Long ageValue = doc.getLong("age");

                    if (name == null || name.trim().isEmpty()) {
                        name = tr("Member", "رکن");
                    }

                    if (country == null) country = "";
                    if (city == null) city = "";
                    if (maritalStatus == null) maritalStatus = "";
                    if (intention == null) intention = "";
                    if (practice == null) practice = "";
                    if (about == null) about = "";
                    if (verification == null) {
                        verification = "Not requested";
                    }

                    String location = city.trim();

                    if (!location.isEmpty()
                            && !country.trim().isEmpty()) {
                        location += ", " + country.trim();
                    } else if (location.isEmpty()) {
                        location = country.trim();
                    }

                    String ageText = "";

                    if (ageValue != null) {
                        ageText = String.valueOf(ageValue);
                    }

                    LinearLayout card = card();

                    String heading = name;

                    if (!ageText.isEmpty()) {
                        heading += " • " + ageText;
                    }

                    card.addView(cardText(
                            heading,
                            21,
                            dark
                    ));

                    if (!location.isEmpty()) {
                        card.addView(cardText(
                                tr("Location: ", "مقام: ") + location,
                                15,
                                gray
                        ));
                    }

                    if (!maritalStatus.isEmpty()) {
                        card.addView(cardText(
                                tr("Marital status: ",
                                        "ازدواجی حیثیت: ")
                                        + maritalStatus,
                                15,
                                gray
                        ));
                    }

                    if (!intention.isEmpty()) {
                        card.addView(cardText(
                                tr("Marriage intention: ",
                                        "نکاح کا ارادہ: ")
                                        + intention,
                                15,
                                gray
                        ));
                    }

                    if (!practice.isEmpty()) {
                        card.addView(cardText(
                                tr("Religious practice: ",
                                        "دینی عمل: ")
                                        + practice,
                                15,
                                gray
                        ));
                    }

                    card.addView(cardText(
                            tr("Verification: ",
                                    "تصدیق: ")
                                    + verification,
                            15,
                            gray
                    ));

                    if (!about.isEmpty()) {
                        String shortAbout = about;

                        if (shortAbout.length() > 180) {
                            shortAbout =
                                    shortAbout.substring(0, 180)
                                            + "...";
                        }

                        card.addView(cardText(
                                tr("About: ", "تعارف: ")
                                        + shortAbout,
                                15,
                                gray
                        ));
                    }

                    Button viewProfile = appButton(
                            tr("View Profile", "پروفائل دیکھیں")
                    );

                    Button interest = outlineButton(
                            tr("Express Interest",
                                    "رغبت کا اظہار کریں")
                    );

                    card.addView(viewProfile);
                    card.addView(interest);

                    final String profileUid = uid;
                    final String profileName = name;

                    viewProfile.setOnClickListener(v -> {

                        new android.app.AlertDialog.Builder(this)
                                .setTitle(profileName)
                                .setMessage(
                                        tr(
                                                "Profile details are shown from the real Best Nikah Bridge account.",
                                                "یہ پروفائل Best Nikah Bridge کے حقیقی اکاؤنٹ سے لیا گیا ہے۔"
                                        )
                                )
                                .setPositiveButton(
                                        tr("Close", "بند کریں"),
                                        null
                                )
                                .show();
                    });

                    interest.setOnClickListener(v -> {

                        if (sentInterests.contains(profileUid)) {
                            toast(
                                    tr(
                                            "Interest already sent.",
                                            "رغبت پہلے ہی بھیجی جا چکی ہے۔"
                                    )
                            );
                            return;
                        }

                        if (profileUid.equals(currentUid)) {
                            return;
                        }

                        Map<String, Object> interestData =
                                new HashMap<>();

                        interestData.put("fromUid", currentUid);
                        interestData.put("toUid", profileUid);
                        interestData.put("status", "pending");
                        interestData.put(
                                "createdAt",
                                com.google.firebase.firestore.FieldValue
                                        .serverTimestamp()
                        );

                        firestore.collection("interests")
                                .add(interestData)
                                .addOnSuccessListener(ref -> {

                                    sentInterests.add(profileUid);

                                    toast(
                                            tr(
                                                    "Interest sent securely.",
                                                    "رغبت محفوظ طریقے سے بھیج دی گئی۔"
                                            )
                                    );

                                    interest.setEnabled(false);
                                    interest.setText(
                                            tr(
                                                    "Interest Sent",
                                                    "رغبت بھیج دی گئی"
                                            )
                                    );
                                })
                                .addOnFailureListener(e -> {

                                    toast(
                                            tr(
                                                    "Could not send interest. Please try again.",
                                                    "رغبت نہیں بھیجی جا سکی۔ دوبارہ کوشش کریں۔"
                                            )
                                    );
                                });
                    });

                    addFull(card);

                    found = true;
                }

                if (!found) {
                    root.addView(subtitle(
                            tr(
                                    "No suitable real profiles are available yet.",
                                    "ابھی کوئی مناسب حقیقی پروفائل دستیاب نہیں۔"
                            )
                    ));
                }

                Button back = outlineButton(
                        tr("Back", "واپس")
                );

                addFull(back);

                back.setOnClickListener(
                        v -> showHome()
                );
            })
            .addOnFailureListener(e -> {

                root.removeAllViews();

                root.addView(title(
                        tr("Recommended Matches",
                                "تجویز کردہ رشتے"),
                        28
                ));

                root.addView(subtitle(
                        tr(
                                "Could not load real profiles. Please try again.",
                                "حقیقی پروفائلز لوڈ نہیں ہو سکے۔ دوبارہ کوشش کریں۔"
                        )
                ));

                Button retry = appButton(
                        tr("Try Again", "دوبارہ کوشش کریں")
                );

                Button back = outlineButton(
                        tr("Back", "واپس")
                );

                addFull(retry);
                addFull(back);

                retry.setOnClickListener(
                        v -> showMatches()
                );

                back.setOnClickListener(
                        v -> showHome()
                );
            });
    }

    private void addMatchCard(int i) {

    firestore.collection("users")
            .get()
            .addOnSuccessListener(snapshot -> {

                int visibleIndex = 0;

                for (com.google.firebase.firestore.DocumentSnapshot doc
                        : snapshot.getDocuments()) {

                    String uid = doc.getString("uid");

                    if (uid == null || uid.trim().isEmpty()) {
                        continue;
                    }

                    if (mAuth.getCurrentUser() != null
                            && uid.equals(mAuth.getCurrentUser().getUid())) {
                        continue;
                    }

                    Boolean active = doc.getBoolean("profileActive");
                    if (active != null && !active) {
                        continue;
                    }

                    if (blockedProfiles.contains(uid)) {
                        continue;
                    }

                    if (visibleIndex != i) {
                        visibleIndex++;
                        continue;
                    }

                    String name = doc.getString("name");
                    String country = doc.getString("country");
                    String city = doc.getString("city");
                    String gender = doc.getString("gender");
                    String maritalStatus = doc.getString("maritalStatus");
                    String intention = doc.getString("marriageIntent");
                    String practice = doc.getString("religiousPractice");
                    String about = doc.getString("about");
                    String verification = doc.getString("verificationStatus");

                    Long ageValue = doc.getLong("age");

                    if (name == null || name.trim().isEmpty()) {
                        name = tr("Member", "صارف");
                    }

                    if (country == null) country = "";
                    if (city == null) city = "";
                    if (maritalStatus == null) maritalStatus = "";
                    if (intention == null) intention = "";
                    if (practice == null) practice = "";
                    if (about == null) about = "";
                    if (verification == null) {
                        verification = "Not requested";
                    }

                    String location = city.trim();

                    if (!location.isEmpty() && !country.trim().isEmpty()) {
                        location = city.trim() + ", " + country.trim();
                    } else if (location.isEmpty()) {
                        location = country.trim();
                    }

                    String ageText = "";

                    if (ageValue != null) {
                        ageText = String.valueOf(ageValue);
                    }

                    int score = 60;

                    if (!intention.isEmpty()
                            && intention.equalsIgnoreCase(
                            prefs.getString(K_INTENTION, ""))) {
                        score += 15;
                    }

                    if (!country.isEmpty()
                            && country.equalsIgnoreCase(
                            prefs.getString(K_COUNTRY, ""))) {
                        score += 10;
                    }

                    if ("Verified".equalsIgnoreCase(verification)) {
                        score += 10;
                    }

                    if (score > 97) {
                        score = 97;
                    }

                    LinearLayout card = card();

                    String heading = name;

                    if (!ageText.isEmpty()) {
                        heading += " • " + ageText;
                    }

                    card.addView(cardText(
                            heading,
                            21,
                            dark
                    ));

                    if ("Verified".equalsIgnoreCase(verification)) {

                        card.addView(cardText(
                                "✓ " + tr(
                                        "Verified profile",
                                        "✓ تصدیق شدہ پروفائل"
                                ),
                                13,
                                primary
                        ));

                    } else {

                        card.addView(cardText(
                                tr(
                                        "Verification pending / not verified",
                                        "تصدیق زیرِ عمل / ابھی تصدیق شدہ نہیں"
                                ),
                                13,
                                gray
                        ));
                    }

                    if (!location.isEmpty()) {

                        card.addView(cardText(
                                tr("Location: ", "مقام: ") + location,
                                15,
                                gray
                        ));
                    }

                    if (!maritalStatus.isEmpty()) {

                        card.addView(cardText(
                                tr("Marital status: ", "ازدواجی حیثیت: ")
                                        + maritalStatus,
                                15,
                                gray
                        ));
                    }

                    if (!intention.isEmpty()) {

                        card.addView(cardText(
                                tr("Marriage intention: ", "نکاح کا ارادہ: ")
                                        + intention,
                                15,
                                gray
                        ));
                    }

                    if (!practice.isEmpty()) {

                        card.addView(cardText(
                                tr("Religious practice: ", "دینی عمل: ")
                                        + practice,
                                15,
                                gray
                        ));
                    }

                    card.addView(cardText(
                            tr("Compatibility: ", "مطابقت: ")
                                    + score + "%",
                            18,
                            primary
                    ));

                    if (!about.trim().isEmpty()) {

                        String shortAbout = about.trim();

                        if (shortAbout.length() > 180) {
                            shortAbout =
                                    shortAbout.substring(0, 180) + "…";
                        }

                        card.addView(cardText(
                                tr("About: ", "تعارف: ") + shortAbout,
                                15,
                                gray
                        ));
                    }

                    Button viewProfile = appButton(
                            tr(
                                    "View Profile",
                                    "پروفائل دیکھیں"
                            )
                    );

                    Button interest = outlineButton(
                            tr(
                                    "Express Interest",
                                    "رغبت کا اظہار کریں"
                            )
                    );

                    Button report = dangerButton(
                            tr(
                                    "Report / Block",
                                    "رپورٹ / بلاک"
                            )
                    );

                    card.addView(viewProfile);
                    card.addView(interest);
                    card.addView(report);

                    final String profileUid = uid;
                    final String profileName = name;

                    viewProfile.setOnClickListener(v -> {

                        new android.app.AlertDialog.Builder(this)
                                .setTitle(profileName)
                                .setMessage(
                                        tr(
                                                "Real profile from Best Nikah Bridge.\n\n"
                                                        + "Location: " + location
                                                        + "\nMarriage intention: " + intention
                                                        + "\nMarital status: " + maritalStatus
                                                        + "\nReligious practice: " + practice
                                                        + "\nVerification: " + verification
                                                        + "\n\nAbout:\n" + about,
                                                "Best Nikah Bridge کا حقیقی پروفائل۔\n\n"
                                                        + "مقام: " + location
                                                        + "\nنکاح کا ارادہ: " + intention
                                                        + "\nازدواجی حیثیت: " + maritalStatus
                                                        + "\nدینی عمل: " + practice
                                                        + "\nتصدیق: " + verification
                                                        + "\n\nتعارف:\n" + about
                                        )
                                )
                                .setPositiveButton(
                                        tr("Close", "بند کریں"),
                                        null
                                )
                                .show();
                    });

                    interest.setOnClickListener(v -> {

                        if (sentInterests.contains(profileUid)) {

                            toast(
                                    tr(
                                            "Interest already sent.",
                                            "رغبت پہلے ہی بھیجی جا چکی ہے۔"
                                    )
                            );

                            return;
                        }

                        if (mAuth.getCurrentUser() == null) {

                            toast(
                                    tr(
                                            "Please sign in first.",
                                            "براہ کرم پہلے سائن اِن کریں۔"
                                    )
                            );

                            return;
                        }

                        Map<String, Object> interestData =
                                new HashMap<>();

                        interestData.put(
                                "fromUid",
                                mAuth.getCurrentUser().getUid()
                        );

                        interestData.put(
                                "toUid",
                                profileUid
                        );

                        interestData.put(
                                "status",
                                "pending"
                        );

                        interestData.put(
                                "createdAt",
                                com.google.firebase.firestore.FieldValue
                                        .serverTimestamp()
                        );

                        firestore.collection("interests")
                                .add(interestData)
                                .addOnSuccessListener(ref -> {

                                    sentInterests.add(profileUid);
                                    saveSets();

                                    interest.setEnabled(false);

                                    interest.setText(
                                            tr(
                                                    "Interest Sent",
                                                    "رغبت بھیج دی گئی"
                                            )
                                    );

                                    toast(
                                            tr(
                                                    "Interest sent securely.",
                                                    "رغبت محفوظ طریقے سے بھیج دی گئی۔"
                                            )
                                    );
                                })
                                .addOnFailureListener(e -> {

                                    toast(
                                            tr(
                                                    "Could not send interest. Please try again.",
                                                    "رغبت نہیں بھیجی جا سکی۔ دوبارہ کوشش کریں۔"
                                            )
                                    );
                                });
                    });

                    if (sentInterests.contains(profileUid)) {
                        interest.setEnabled(false);
                        interest.setText(
                                tr(
                                        "Interest Sent",
                                        "رغبت بھیج دی گئی"
                                )
                        );
                    }

                    report.setOnClickListener(
                            v -> showReportBlock(i)
                    );

                    addFull(card);

                    return;
                }

                toast(
                        tr(
                                "Profile is no longer available.",
                                "یہ پروفائل اب دستیاب نہیں ہے۔"
                        )
                );
            })
            .addOnFailureListener(e -> {

                toast(
                        tr(
                                "Could not load real profile. Please try again.",
                                "حقیقی پروفائل لوڈ نہیں ہو سکا۔ دوبارہ کوشش کریں۔"
                        )
                );
            });
                                               }

    private void showProfileDetails(int i) {
        setupRoot(true);

        String key = profileKey(i);
        int score = calculateCompatibility(i);

        firestore.collection("users")
        .document(key)
        .get()
        .addOnSuccessListener(doc -> {

            if (!doc.exists()) {
                Toast.makeText(
                        this,
                        tr(
                                "Profile is no longer available.",
                                "یہ پروفائل اب دستیاب نہیں ہے۔"
                        ),
                        Toast.LENGTH_SHORT
                ).show();
                showMatches();
                return;
            }

            String name = doc.getString("name");
            String country = doc.getString("country");
            String city = doc.getString("city");
            String gender = doc.getString("gender");
            String maritalStatus = doc.getString("maritalStatus");
            String intention = doc.getString("marriageIntent");
            String practice = doc.getString("religiousPractice");
            String about = doc.getString("about");
            String verification = doc.getString("verificationStatus");

            Long ageValue = doc.getLong("age");

            if (name == null || name.trim().isEmpty()) {
                name = tr("Member", "صارف");
            }

            if (country == null) country = "";
            if (city == null) city = "";
            if (gender == null) gender = "";
            if (maritalStatus == null) maritalStatus = "";
            if (intention == null) intention = "";
            if (practice == null) practice = "";
            if (about == null) about = "";
            if (verification == null) verification = "Not requested";

            String location = city.trim();

            if (!location.isEmpty() && !country.trim().isEmpty()) {
                location = city.trim() + ", " + country.trim();
            } else if (location.isEmpty()) {
                location = country.trim();
            }

            String ageText = "";

            if (ageValue != null) {
                ageText = String.valueOf(ageValue);
            }

            String heading = name;

            if (!ageText.isEmpty()) {
                heading += " • " + ageText;
            }

            root.addView(title(heading, 28));

            StringBuilder details = new StringBuilder();

            if (!location.isEmpty()) {
                details.append(
                        tr("Location: ", "مقام: ")
                ).append(location).append("\n");
            }

            if (!gender.trim().isEmpty()) {
                details.append(
                        tr("Gender: ", "جنس: ")
                ).append(gender).append("\n");
            }

            if (!maritalStatus.trim().isEmpty()) {
                details.append(
                        tr("Marital status: ", "ازدواجی حیثیت: ")
                ).append(maritalStatus).append("\n");
            }

            if (!intention.trim().isEmpty()) {
                details.append(
                        tr("Marriage intention: ", "نکاح کا ارادہ: ")
                ).append(intention).append("\n");
            }

            if (!practice.trim().isEmpty()) {
                details.append(
                        tr("Religious practice: ", "دینی عمل: ")
                ).append(practice).append("\n");
            }

            details.append(
                    tr("Compatibility: ", "مطابقت: ")
            ).append(score).append("%\n");

            details.append(
                    tr("Verification: ", "تصدیق: ")
            ).append(verification);

            if (!about.trim().isEmpty()) {
                details.append("\n\n")
                        .append(
                                tr("About: ", "تعارف: ")
                        )
                        .append(about.trim());
            }

            details.append("\n\n")
                    .append(
                            tr(
                                    "Safety: Never share your password, OTP or private documents.",
                                    "حفاظت: اپنا پاس ورڈ، OTP یا نجی دستاویزات کبھی شیئر نہ کریں۔"
                            )
                    );

            root.addView(body(details.toString()));

            Button interest = appButton(
                    sentInterests.contains(key)
                            ? tr("Interest Sent ✓", "دلچسپی بھیجی ✓")
                            : tr("Express Interest", "دلچسپی کا اظہار کریں")
            );

            Button chat = outlineButton(
                    tr(
                            "Safe Chat (Mutual Only)",
                            "محفوظ چیٹ (صرف باہمی رضامندی)"
                    )
            );

            Button report = dangerButton(
                    tr("Report / Block", "رپورٹ / بلاک")
            );

            Button back = outlineButton(
                    tr("Back to Matches", "میچز پر واپس")
            );

            addFull(interest);
            addFull(chat);
            addFull(report);
            addFull(back);

            if (sentInterests.contains(key)) {
                interest.setEnabled(false);
                interest.setAlpha(0.65f);
            }

            interest.setOnClickListener(v -> {

                if (mAuth.getCurrentUser() == null) {
                    Toast.makeText(
                            this,
                            tr(
                                    "Please sign in first.",
                                    "براہ کرم پہلے سائن اِن کریں۔"
                            ),
                            Toast.LENGTH_SHORT
                    );
                    return;
                }

                if (sentInterests.contains(key)) {
                    Toast.makeText(
                            this,
                            tr(
                                    "Interest already sent.",
                                    "دلچسپی پہلے ہی بھیجی جا چکی ہے۔"
                            ),
                            Toast.LENGTH_SHORT
                    );
                    return;
                }

                Map<String, Object> interestData = new HashMap<>();

                interestData.put(
                        "fromUid",
                        mAuth.getCurrentUser().getUid()
                );

                interestData.put(
                        "toUid",
                        key
                );

                interestData.put(
                        "status",
                        "pending"
                );

                interestData.put(
                        "createdAt",
                        com.google.firebase.firestore.FieldValue
                                .serverTimestamp()
                );

                firestore.collection("interests")
                        .add(interestData)
                        .addOnSuccessListener(ref -> {

                            sentInterests.add(key);
                            saveSets();

                            interest.setEnabled(false);
                            interest.setText(
                                    tr(
                                            "Interest Sent ✓",
                                            "دلچسپی بھیجی ✓"
                                    )
                            );
                            interest.setAlpha(0.65f);

                            Toast.makeText(
                                    this,
                                    tr(
                                            "Interest sent securely.",
                                            "دلچسپی محفوظ طریقے سے بھیج دی گئی۔"
                                    ),
                                    Toast.LENGTH_SHORT
                            );
                        })
                        .addOnFailureListener(e -> {

                            Toast.makeText(
                                    this,
                                    tr(
                                            "Could not send interest. Please try again.",
                                            "دلچسپی نہیں بھیجی جا سکی۔ دوبارہ کوشش کریں۔"
                                    ),
                                    Toast.LENGTH_SHORT
                            );
                        });
            });

            chat.setOnClickListener(v -> {

                if (acceptedConnections.contains(key)) {
                    showChat(i);
                } else {
                    Toast.makeText(
                            this,
                            tr(
                                    "Chat is locked. Both sides must accept the connection.",
                                    "چیٹ بند ہے۔ دونوں طرف سے رابطہ قبول ہونا ضروری ہے۔"
                            ),
                            Toast.LENGTH_LONG
                    ).show();
                }
            });

            report.setOnClickListener(
                    v -> showReportBlock(i)
            );

            back.setOnClickListener(
                    v -> showMatches()
            );
        })
        .addOnFailureListener(e -> {

            Toast.makeText(
                    this,
                    tr(
                            "Could not load real profile. Please try again.",
                            "حقیقی پروفائل لوڈ نہیں ہو سکا۔ دوبارہ کوشش کریں۔"
                    ),
                    Toast.LENGTH_SHORT
            );
        });

        private void showInterests() {
    setupRoot(true);

    root.addView(title(
            tr("Interests & Connections", "دلچسپیاں اور روابط"), 28));

    root.addView(subtitle(tr(
            "Real connections are mutual, respectful and safe.",
            "حقیقی رابطہ باہمی، باعزت اور محفوظ ہونا چاہیے۔"
    )));

    if (mAuth.getCurrentUser() == null) {
        root.addView(body(tr(
                "Please sign in first.",
                "براہ کرم پہلے سائن اِن کریں۔"
        )));
        return;
    }

    String currentUid = mAuth.getCurrentUser().getUid();

    section(tr("Sent Interests", "بھیجی گئی دلچسپیاں"));

    firestore.collection("interests")
            .whereEqualTo("fromUid", currentUid)
            .get()
            .addOnSuccessListener(snapshot -> {

                if (snapshot.isEmpty()) {
                    root.addView(body(tr(
                            "No interests sent yet.",
                            "ابھی کوئی دلچسپی نہیں بھیجی گئی۔"
                    )));
                    return;
                }

                for (com.google.firebase.firestore.DocumentSnapshot doc
                        : snapshot.getDocuments()) {

                    String toUid = doc.getString("toUid");
                    String status = doc.getString("status");

                    if (toUid == null) continue;

                    root.addView(cardText(
                            "→ " + toUid + "   [" +
                                    (status == null ? "pending" : status) + "]",
                            16,
                            dark
                    ));
                }
            })
            .addOnFailureListener(e ->
                    Toast.makeText(
                            this,
                            tr(
                                    "Could not load sent interests.",
                                    "بھیجی گئی دلچسپیاں لوڈ نہیں ہو سکیں۔"
                            ),
                            Toast.LENGTH_SHORT
                    ).show()
            );

    section(tr("Incoming Interests", "موصول ہونے والی دلچسپیاں"));

    firestore.collection("interests")
            .whereEqualTo("toUid", currentUid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener(snapshot -> {

                if (snapshot.isEmpty()) {
                    root.addView(body(tr(
                            "No pending interests.",
                            "کوئی زیرِ التوا دلچسپی نہیں۔"
                    )));
                    return;
                }

                for (com.google.firebase.firestore.DocumentSnapshot doc
                        : snapshot.getDocuments()) {

                    String interestId = doc.getId();
                    String fromUid = doc.getString("fromUid");

                    if (fromUid == null) continue;

                    LinearLayout card = card();
                    card.addView(cardText(
                            "Interest from: " + fromUid,
                            16,
                            dark
                    ));

                    Button accept = appButton(
                            tr("Accept Connection", "رابطہ قبول کریں")
                    );

                    Button decline = outlineButton(
                            tr("Decline", "انکار")
                    );

                    card.addView(accept);
                    card.addView(decline);
                    root.addView(card);

                    accept.setOnClickListener(v -> {

                        firestore.collection("interests")
                                .document(interestId)
                                .update("status", "accepted")
                                .addOnSuccessListener(unused -> {

                                    firestore.collection("interests")
                                            .whereEqualTo("fromUid", currentUid)
                                            .whereEqualTo("toUid", fromUid)
                                            .whereEqualTo("status", "pending")
                                            .get()
                                            .addOnSuccessListener(reverse -> {

                                                if (!reverse.isEmpty()) {

                                                    String reverseId =
                                                            reverse.getDocuments()
                                                                    .get(0)
                                                                    .getId();

                                                    firestore.collection("interests")
                                                            .document(reverseId)
                                                            .update(
                                                                    "status",
                                                                    "accepted"
                                                            );
                                                }

                                                Toast.makeText(
                                                        this,
                                                        tr(
                                                                "Connection accepted.",
                                                                "رابطہ قبول ہو گیا۔"
                                                        ),
                                                        Toast.LENGTH_SHORT
                                                ).show();

                                                showInterests();
                                            });
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                this,
                                                tr(
                                                        "Could not accept connection.",
                                                        "رابطہ قبول نہیں ہو سکا۔"
                                                ),
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );
                    });

                    decline.setOnClickListener(v -> {

                        firestore.collection("interests")
                                .document(interestId)
                                .update("status", "declined")
                                .addOnSuccessListener(unused ->
                                        showInterests()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(
                                                this,
                                                tr(
                                                        "Could not decline interest.",
                                                        "دلچسپی مسترد نہیں ہو سکی۔"
                                                ),
                                                Toast.LENGTH_SHORT
                                        ).show()
                                );
                    });
                }
            })
            .addOnFailureListener(e ->
                    Toast.makeText(
                            this,
                            tr(
                                    "Could not load incoming interests.",
                                    "موصول ہونے والی دلچسپیاں لوڈ نہیں ہو سکیں۔"
                            // ===== REAL FIRESTORE CONNECTIONS =====

if (mAuth.getCurrentUser() == null) {
    root.addView(body(tr(
            "Please sign in first.",
            "براہ کرم پہلے سائن اِن کریں۔"
    )));
    Button back = outlineButton(tr("Back", "واپس"));
    addFull(back);
    back.setOnClickListener(v -> showHome());
    return;
}

String currentUid = mAuth.getCurrentUser().getUid();

section(tr("Incoming Interests", "موصول شدہ دلچسپیاں"));

firestore.collection("interests")
        .whereEqualTo("toUid", currentUid)
        .whereEqualTo("status", "pending")
        .get()
        .addOnSuccessListener(snapshot -> {

            if (snapshot.isEmpty()) {
                root.addView(body(tr(
                        "No pending interests.",
                        "کوئی زیرِ التوا دلچسپی نہیں۔"
                )));
                return;
            }

            for (com.google.firebase.firestore.DocumentSnapshot doc
                    : snapshot.getDocuments()) {

                String interestId = doc.getId();
                String fromUid = doc.getString("fromUid");

                if (fromUid == null || fromUid.trim().isEmpty()) {
                    continue;
                }

                firestore.collection("users")
                        .document(fromUid)
                        .get()
                        .addOnSuccessListener(userDoc -> {

                            String name = userDoc.getString("name");
                            String country = userDoc.getString("country");
                            String city = userDoc.getString("city");

                            if (name == null || name.trim().isEmpty()) {
                                name = tr("Member", "رکن");
                            }

                            String location = "";

                            if (city != null && !city.trim().isEmpty()) {
                                location = city.trim();
                            }

                            if (country != null && !country.trim().isEmpty()) {
                                if (!location.isEmpty()) {
                                    location += ", ";
                                }
                                location += country.trim();
                            }

                            LinearLayout card = card();

                            String displayName = name;

                            if (!location.isEmpty()) {
                                displayName += " • " + location;
                            }

                            card.addView(cardText(
                                    displayName,
                                    16,
                                    dark
                            ));

                            Button accept = appButton(
                                    tr("Accept Connection",
                                            "رابطہ قبول کریں")
                            );

                            Button decline = outlineButton(
                                    tr("Decline", "انکار")
                            );

                            card.addView(accept);
                            card.addView(decline);

                            root.addView(card);

        accept.setOnClickListener(v -> {

    accept.setEnabled(false);
    decline.setEnabled(false);

    firestore.collection("interests")
            .document(interestId)
            .get()
            .addOnSuccessListener(interestDoc -> {

                if (!interestDoc.exists()) {
                    accept.setEnabled(true);
                    decline.setEnabled(true);

                    Toast.makeText(
                            this,
                            tr(
                                    "This interest no longer exists.",
                                    "یہ دلچسپی اب موجود نہیں ہے۔"
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    showInterests();
                    return;
                }

                String incomingFromUid = interestDoc.getString("fromUid");
                String incomingToUid = interestDoc.getString("toUid");
                String incomingStatus = interestDoc.getString("status");

                if (incomingFromUid == null ||
                        incomingToUid == null ||
                        !currentUid.equals(incomingToUid)) {

                    accept.setEnabled(true);
                    decline.setEnabled(true);

                    Toast.makeText(
                            this,
                            tr(
                                    "This interest is not valid for your account.",
                                    "یہ دلچسپی آپ کے اکاؤنٹ کے لیے درست نہیں ہے۔"
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    showInterests();
                    return;
                }

                if (!"pending".equals(incomingStatus)) {

                    accept.setEnabled(true);
                    decline.setEnabled(true);

                    Toast.makeText(
                            this,
                            tr(
                                    "This interest has already been processed.",
                                    "یہ دلچسپی پہلے ہی پراسیس ہو چکی ہے۔"
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    showInterests();
                    return;
                }

                String fromUid = incomingFromUid;
                String toUid = incomingToUid;

                /*
                 * REAL FIRESTORE MUTUAL CHECK
                 *
                 * We only create a connection when the other user
                 * has also sent an active pending interest back.
                 */
                firestore.collection("interests")
                        .whereEqualTo("fromUid", toUid)
                        .whereEqualTo("toUid", fromUid)
                        .whereEqualTo("status", "pending")
                        .limit(1)
                        .get()
                        .addOnSuccessListener(reverseSnapshot -> {

                            if (reverseSnapshot.isEmpty()) {

                                firestore.collection("interests")
                                        .document(interestId)
                                        .update("status", "accepted")
                                        .addOnSuccessListener(unused -> {

                                            Toast.makeText(
                                                    this,
                                                    tr(
                                                            "Interest accepted. Waiting for mutual acceptance.",
                                                            "دلچسپی قبول ہوگئی۔ باہمی رضامندی کا انتظار ہے۔"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            showInterests();

                                        })
                                        .addOnFailureListener(e -> {

                                            accept.setEnabled(true);
                                            decline.setEnabled(true);

                                            Toast.makeText(
                                                    this,
                                                    tr(
                                                            "Could not accept this interest.",
                                                            "یہ دلچسپی قبول نہیں ہو سکی۔"
                                                    ),
                                                    Toast.LENGTH_SHORT
                                            ).show();
                                        });

                                return;
                            }

                            String reverseId = reverseSnapshot
                                    .getDocuments()
                                    .get(0)
                                    .getId();

                            /*
                             * Stable connection ID.
                             * Same two users always produce the same ID,
                             * preventing duplicate connections.
                             */
                            String uidA;
                            String uidB;

                            if (fromUid.compareTo(toUid) < 0) {
                                uidA = fromUid;
                                uidB = toUid;
                            } else {
                                uidA = toUid;
                                uidB = fromUid;
                            }

                            String connectionId = uidA + "_" + uidB;

                            DocumentReference interestRef =
                                    firestore.collection("interests")
                                            .document(interestId);

                            DocumentReference reverseRef =
                                    firestore.collection("interests")
                                            .document(reverseId);

                            DocumentReference connectionRef =
                                    firestore.collection("connections")
                                            .document(connectionId);

                            Map<String, Object> connection =
                                    new HashMap<>();

                            connection.put("uid1", uidA);
                            connection.put("uid2", uidB);
                            connection.put("status", "active");
                            connection.put(
                                    "createdAt",
                                    com.google.firebase.firestore.FieldValue
                                            .serverTimestamp()
                            );

                            /*
                             * REAL ATOMIC FIRESTORE TRANSACTION
                             *
                             * Both interests are accepted and the
                             * connection is created together.
                             */
                            firestore.runTransaction(transaction -> {

                                DocumentSnapshot currentInterest =
                                        transaction.get(interestRef);

                                DocumentSnapshot reverseInterest =
                                        transaction.get(reverseRef);

                                DocumentSnapshot existingConnection =
                                        transaction.get(connectionRef);

                                if (!currentInterest.exists()) {
                                    throw new IllegalStateException(
                                            "Interest does not exist"
                                    );
                                }

                                if (!reverseInterest.exists()) {
                                    throw new IllegalStateException(
                                            "Mutual interest does not exist"
                                    );
                                }

                                String currentStatus =
                                        currentInterest.getString("status");

                                String reverseStatus =
                                        reverseInterest.getString("status");

                                if (!"pending".equals(currentStatus)) {
                                    throw new IllegalStateException(
                                            "Current interest is no longer pending"
                                    );
                                }

                                if (!"pending".equals(reverseStatus)) {
                                    throw new IllegalStateException(
                                            "Reverse interest is no longer pending"
                                    );
                                }

                                /*
                                 * Mark BOTH interests as accepted.
                                 */
                                transaction.update(
                                        interestRef,
                                        "status",
                                        "accepted"
                                );

                                transaction.update(
                                        reverseRef,
                                        "status",
                                        "accepted"
                                );

                                /*
                                 * Create the connection only if it does
                                 * not already exist.
                                 */
                                if (!existingConnection.exists()) {
                                    transaction.set(
                                            connectionRef,
                                            connection
                                    );
                                }

                                return null;

                            }).addOnSuccessListener(transactionResult -> {

                                Toast.makeText(
                                        this,
                                        tr(
                                                "Mutual connection created. Safe Chat is now available.",
                                                "باہمی رضامندی مکمل ہوگئی۔ محفوظ چیٹ اب دستیاب ہے۔"
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();

                                showInterests();

                            }).addOnFailureListener(e -> {

                                accept.setEnabled(true);
                                decline.setEnabled(true);

                                Toast.makeText(
                                        this,
                                        tr(
                                                "The mutual connection could not be created. Please try again.",
                                                "باہمی رابطہ نہیں بن سکا۔ براہ کرم دوبارہ کوشش کریں۔"
                                        ),
                                        Toast.LENGTH_LONG
                                ).show();
                            });

                        })
                        .addOnFailureListener(e -> {

                            accept.setEnabled(true);
                            decline.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    tr(
                                            "Could not check mutual interest.",
                                            "باہمی دلچسپی چیک نہیں ہو سکی۔"
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();
                        });

            })
            .addOnFailureListener(e -> {

                accept.setEnabled(true);
                decline.setEnabled(true);

                Toast.makeText(
                        this,
                        tr(
                                "Could not load this interest.",
                                "یہ دلچسپی لوڈ نہیں ہو سکی۔"
                        ),
                        Toast.LENGTH_SHORT
                ).show();
            });
});


decline.setOnClickListener(v -> {

    accept.setEnabled(false);
    decline.setEnabled(false);

    firestore.collection("interests")
            .document(interestId)
            .get()
            .addOnSuccessListener(interestDoc -> {

                if (!interestDoc.exists()) {

                    accept.setEnabled(true);
                    decline.setEnabled(true);

                    Toast.makeText(
                            this,
                            tr(
                                    "This interest no longer exists.",
                                    "یہ دلچسپی اب موجود نہیں ہے۔"
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    showInterests();
                    return;
                }

                String toUid = interestDoc.getString("toUid");

                if (toUid == null || !currentUid.equals(toUid)) {

                    accept.setEnabled(true);
                    decline.setEnabled(true);

                    Toast.makeText(
                            this,
                            tr(
                                    "You cannot decline this interest.",
                                    "آپ اس دلچسپی کو مسترد نہیں کر سکتے۔"
                            ),
                            Toast.LENGTH_SHORT
                    ).show();

                    showInterests();
                    return;
                }

                firestore.collection("interests")
                        .document(interestId)
                        .update("status", "declined")
                        .addOnSuccessListener(unused -> {

                            Toast.makeText(
                                    this,
                                    tr(
                                            "Interest declined.",
                                            "دلچسپی مسترد کر دی گئی۔"
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();

                            showInterests();

                        })
                        .addOnFailureListener(e -> {

                            accept.setEnabled(true);
                            decline.setEnabled(true);

                            Toast.makeText(
                                    this,
                                    tr(
                                            "Could not decline this interest.",
                                            "دلچسپی مسترد نہیں ہو سکی۔"
                                    ),
                                    Toast.LENGTH_SHORT
                            ).show();
                        });

            })
            .addOnFailureListener(e -> {

                accept.setEnabled(true);
                decline.setEnabled(true);

                Toast.makeText(
                        this,
                        tr(
                                "Could not load this interest.",
                                "یہ دلچسپی لوڈ نہیں ہو سکی۔"
                        ),
                        Toast.LENGTH_SHORT
                ).show();
            });

section(tr(
        "Accepted Connections",
        "قبول شدہ روابط"
));

firestore.collection("connections")
        .whereEqualTo("uid1", currentUid)
        .whereEqualTo("status", "active")
        .get()
        .addOnSuccessListener(firstSnapshot -> {

            firestore.collection("connections")
                    .whereEqualTo("uid2", currentUid)
                    .whereEqualTo("status", "active")
                    .get()
                    .addOnSuccessListener(secondSnapshot -> {

                        int total =
                                firstSnapshot.size()
                                        + secondSnapshot.size();

                        if (total == 0) {
                            root.addView(body(tr(
                                    "No accepted connections yet.",
                                    "ابھی کوئی قبول شدہ رابطہ نہیں۔"
                            )));
                            return;
                        }

                        for (com.google.firebase.firestore.DocumentSnapshot
                                connection
                                : firstSnapshot.getDocuments()) {

                            String otherUid =
                                    connection.getString("uid2");

                            if (otherUid != null) {
                                addRealConnectionCard(
                                        otherUid
                                );
                            }
                        }

                        for (com.google.firebase.firestore.DocumentSnapshot
                                connection
                                : secondSnapshot.getDocuments()) {

                            String otherUid =
                                    connection.getString("uid1");

                            if (otherUid != null) {
                                addRealConnectionCard(
                                        otherUid
                                );
                            }
                        }
                    });
        })
        .addOnFailureListener(e -> {

            toast(
                    "Could not load accepted connections.",
                    "قبول شدہ روابط لوڈ نہیں ہو سکے۔"
            );
        });

Button back = outlineButton(
        tr("Back", "واپس")
);

addFull(back);

back.setOnClickListener(v -> showHome());
    private void showChat(int i) {
    setRoot(true);

    String key = profileKey(i);

    if (!acceptedConnections.contains(key)) {
        root.addView(title(
                tr("Safe Chat • محفوظ چیٹ", "محفوظ چیٹ • Safe Chat"), 28
        ));

        root.addView(body(tr(
                "Chat is available only after mutual acceptance.",
                "چیٹ صرف باہمی رضامندی کے بعد دستیاب ہے۔"
        )));

        Button back = outlineButton(tr("Back", "واپس"));
        addFull(back);
        back.setOnClickListener(v -> showInterests());
        return;
    }

    com.google.firebase.auth.FirebaseUser firebaseUser =
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();

    if (firebaseUser == null) {
        root.addView(title(
                tr("Sign in required", "لاگ اِن ضروری ہے"), 27
        ));

        root.addView(body(tr(
                "Please sign in again to use Safe Chat.",
                "محفوظ چیٹ استعمال کرنے کے لیے دوبارہ لاگ اِن کریں۔"
        )));

        Button back = outlineButton(tr("Back", "واپس"));
        addFull(back);
        back.setOnClickListener(v -> showInterests());
        return;
    }

    final String myUid = firebaseUser.getUid();

    root.addView(title(
            tr("Safe Chat • محفوظ چیٹ", "محفوظ چیٹ • Safe Chat"), 27
    ));

    root.addView(subtitle(tr(
            "Private communication is available only after mutual acceptance.",
            "نجی گفتگو صرف باہمی رضامندی کے بعد دستیاب ہے۔"
    )));

    if (prefs.getBoolean(K_CHAPERONE, false)) {
        root.addView(body(tr(
                "Family / Wali Mode is enabled for this account.",
                "اس اکاؤنٹ کے لیے Family / Wali Mode فعال ہے۔"
        )));
    }

    TextView statusView = body(tr(
            "Checking secure connection...",
            "محفوظ رابطہ چیک کیا جا رہا ہے..."
    ));
    root.addView(statusView);

    EditText message = new EditText(this);
    message.setHint(tr(
            "Write a respectful message...",
            "باوقار پیغام لکھیں..."
    ));
    message.setGravity(Gravity.TOP);
    message.setMinLines(3);
    message.setInputType(
            android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
    );

    Button send = appButton(tr("Send Message", "پیغام بھیجیں"));
    Button report = dangerButton(tr("Report / Block", "رپورٹ / بلاک"));
    Button back = outlineButton(tr("Back", "واپس"));

    /*
     * REAL FIRESTORE CHAT
     *
     * Connection documents are stored as:
     * connections/{uidA_uidB}
     *
     * Messages are stored as:
     * connections/{uidA_uidB}/messages/{messageId}
     */

    com.google.firebase.firestore.FirebaseFirestore db = firestore;

    final String[] otherUid = new String[]{null};
    final String[] connectionId = new String[]{null};

    /*
     * Find an active connection where current user is uid1.
     */
    db.collection("connections")
            .whereEqualTo("uid1", myUid)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener(first -> {

                if (!first.isEmpty()) {
                    com.google.firebase.firestore.DocumentSnapshot doc =
                            first.getDocuments().get(0);

                    String foundUid = doc.getString("uid2");

                    if (foundUid != null && !foundUid.equals(myUid)) {
                        otherUid[0] = foundUid;
                        connectionId[0] = doc.getId();
                    }
                }

                /*
                 * If not found, check uid2.
                 */
                if (otherUid[0] == null) {
                    db.collection("connections")
                            .whereEqualTo("uid2", myUid)
                            .whereEqualTo("status", "active")
                            .get()
                            .addOnSuccessListener(second -> {

                                if (!second.isEmpty()) {
                                    com.google.firebase.firestore.DocumentSnapshot doc =
                                            second.getDocuments().get(0);

                                    String foundUid = doc.getString("uid1");

                                    if (foundUid != null && !foundUid.equals(myUid)) {
                                        otherUid[0] = foundUid;
                                        connectionId[0] = doc.getId();
                                    }
                                }

                                if (otherUid[0] == null) {
                                    statusView.setText(tr(
                                            "No active mutual connection was found.",
                                            "کوئی فعال باہمی رابطہ نہیں ملا۔"
                                    ));
                                    return;
                                }

                                statusView.setText(tr(
                                        "Secure chat connected.",
                                        "محفوظ چیٹ منسلک ہے۔"
                                ));

                                loadRealChatMessages(
                                        db,
                                        connectionId[0],
                                        myUid,
                                        root
                                );

                            })
                            .addOnFailureListener(e -> statusView.setText(
                                    tr(
                                            "Could not verify your connection.",
                                            "آپ کا رابطہ تصدیق نہیں ہو سکا۔"
                                    )
                            ));

                } else {

                    statusView.setText(tr(
                            "Secure chat connected.",
                            "محفوظ چیٹ منسلک ہے۔"
                    ));

                    loadRealChatMessages(
                            db,
                            connectionId[0],
                            myUid,
                            root
                    );
                }

            })
            .addOnFailureListener(e -> {

                /*
                 * Try the second direction if the first query fails.
                 */
                db.collection("connections")
                        .whereEqualTo("uid2", myUid)
                        .whereEqualTo("status", "active")
                        .get()
                        .addOnSuccessListener(second -> {

                            if (!second.isEmpty()) {

                                com.google.firebase.firestore.DocumentSnapshot doc =
                                        second.getDocuments().get(0);

                                String foundUid = doc.getString("uid1");

                                if (foundUid != null && !foundUid.equals(myUid)) {
                                    otherUid[0] = foundUid;
                                    connectionId[0] = doc.getId();
                                }
                            }

                            if (otherUid[0] == null) {
                                statusView.setText(tr(
                                        "No active mutual connection was found.",
                                        "کوئی فعال باہمی رابطہ نہیں ملا۔"
                                ));
                                return;
                            }

                            statusView.setText(tr(
                                    "Secure chat connected.",
                                    "محفوظ چیٹ منسلک ہے۔"
                            ));

                            loadRealChatMessages(
                                    db,
                                    connectionId[0],
                                    myUid,
                                    root
                            );

                        })
                        .addOnFailureListener(error -> statusView.setText(
                                tr(
                                        "Could not verify secure connection.",
                                        "محفوظ رابطہ تصدیق نہیں ہو سکا۔"
                                )
                        ));
            });

    addInput(message, 120);
    addFull(send);
    addFull(report);
    addFull(back);

    send.setOnClickListener(v -> {

        String text = message.getText().toString().trim();

        if (text.isEmpty()) {
            toast(
                    "Write a message first.",
                    "پہلے پیغام لکھیں۔"
            );
            return;
        }

        if (otherUid[0] == null || connectionId[0] == null) {
            toast(
                    "Secure connection is not ready.",
                    "محفوظ رابطہ ابھی تیار نہیں۔"
            );
            return;
        }

        send.setEnabled(false);

        java.util.Map<String, Object> msg =
                new java.util.HashMap<>();

        msg.put("fromUid", myUid);
        msg.put("toUid", otherUid[0]);
        msg.put("text", text);
        msg.put(
                "sentAt",
                com.google.firebase.firestore.FieldValue.serverTimestamp()
        );

        db.collection("connections")
                .document(connectionId[0])
                .collection("messages")
                .add(msg)
                .addOnSuccessListener(messageRef -> {

                    message.setText("");
                    send.setEnabled(true);

                    toast(
                            "Message sent.",
                            "پیغام بھیج دیا گیا۔"
                    );

                })
                .addOnFailureListener(e -> {

                    send.setEnabled(true);

                    toast(
                            "Message could not be sent. Please try again.",
                            "پیغام نہیں بھیجا جا سکا۔ دوبارہ کوشش کریں۔"
                    );
                });
    });

    report.setOnClickListener(v -> showReportBlock(i));

    back.setOnClickListener(v -> showInterests());
            }

    private int remainingAdMessages() {
        String day = prefs.getString(K_AD_DAY, "");
        int used = prefs.getInt(K_AD_USED, 0);

        if (!todayKey().equals(day)) {
            return 2;
        }

        return Math.max(0, 2 - used);
    }

    private boolean consumeMessageAllowance() {
        if (prefs.getBoolean(K_PREMIUM, false)) {
            return true;
        }

        String day = prefs.getString(K_AD_DAY, "");
        int used = prefs.getInt(K_AD_USED, 0);

        if (!todayKey().equals(day)) {
            day = todayKey();
            used = 0;
        }

        if (used >= 2) return false;

        prefs.edit()
                .putString(K_AD_DAY, day)
                .putInt(K_AD_USED, used + 1)
                .apply();

        return true;
    }

    private void showReportBlock(int i) {
        setupRoot(true);

        String key = profileKey(i);

        root.addView(title(
                tr("Report / Block", "رپورٹ / بلاک"), 27));

        root.addView(body(
                tr("Choose the safest action. Production reports must be stored and reviewed by the moderation backend.",
                        "محفوظ کارروائی منتخب کریں۔ پروڈکشن رپورٹس moderation بیک اینڈ میں محفوظ اور ریویو ہونی چاہئیں۔")));

        String[] reasons = {
                tr("Select reason", "وجہ منتخب کریں"),
                tr("Suspicious profile", "مشکوک پروفائل"),
                tr("Inappropriate content", "نامناسب مواد"),
                tr("Harassment", "ہراسانی"),
                tr("Spam / scam", "اسپیم / فراڈ"),
                tr("Safety concern", "حفاظتی مسئلہ")
        };

        Spinner reason = addSpinner(reasons);

        Button block = dangerButton(tr("Block Profile", "پروفائل بلاک کریں"));
        Button report = appButton(tr("Report & Block", "رپورٹ اور بلاک"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(block);
        addFull(report);
        addFull(back);

        block.setOnClickListener(v -> {
            blockedProfiles.add(key);
            saveSets();
            toast(
                    DEMO_NAMES[i] + " blocked on this device.",
                    DEMO_NAMES[i] + " کو اس ڈیوائس پر بلاک کردیا گیا۔"
            );
            showMatches();
        });

        report.setOnClickListener(v -> {
            if (reason.getSelectedItemPosition() == 0) {
                toast("Select a report reason.", "رپورٹ کی وجہ منتخب کریں۔");
                return;
            }

            reportedProfiles.add(key);
            blockedProfiles.add(key);
            saveSets();

            toast(
                    "Report recorded locally. Production moderation must use a secure backend.",
                    "رپورٹ مقامی طور پر ریکارڈ ہوگئی۔ پروڈکشن moderation محفوظ بیک اینڈ پر ہونی چاہیے۔"
            );
            showMatches();
        });

        back.setOnClickListener(v -> showMatches());
    }

    private void showSafety() {
        setupRoot(true);

        root.addView(title(
                tr("Safety Center", "سیفٹی سینٹر"), 28));

        root.addView(subtitle(tr(
                "Your safety comes before matches, messages or money.",
                "آپ کی حفاظت میچ، پیغام یا رقم سے زیادہ اہم ہے۔"
        )));

        root.addView(body(
                "✓ " + tr("Never share your password or OTP.",
                "اپنا پاس ورڈ یا OTP کبھی شیئر نہ کریں۔") + "\n\n" +
                "✓ " + tr("Never send money to another user.",
                "کسی دوسرے صارف کو رقم کبھی نہ بھیجیں۔") + "\n\n" +
                "✓ " + tr("Keep phone number and personal contact details private.",
                "فون نمبر اور ذاتی رابطے کی معلومات نجی رکھیں۔") + "\n\n" +
                "✓ " + tr("Do not upload identity documents into ordinary chat.",
                "شناختی دستاویزات عام چیٹ میں اپ لوڈ نہ کریں۔") + "\n\n" +
                "✓ " + tr("Use Express Interest before communication.",
                "گفتگو سے پہلے دلچسپی ظاہر کریں۔") + "\n\n" +
                "✓ " + tr("Chat should unlock only after mutual acceptance.",
                "چیٹ صرف باہمی رضامندی کے بعد کھلنی چاہیے۔") + "\n\n" +
                "✓ " + tr("Use Family / Wali Mode when you want a trusted person involved.",
                "جب مناسب ہو تو کسی قابلِ اعتماد فرد کو Family / Wali Mode میں شامل کریں۔") + "\n\n" +
                "✓ " + tr("Report or block suspicious or disrespectful profiles.",
                "مشکوک یا نامناسب پروفائل کو رپورٹ یا بلاک کریں۔") + "\n\n"
                "✓ " + tr("For religious questions, consult a qualified trusted scholar.",
                "دینی سوالات کے لیے معتبر اہلِ علم سے رہنمائی لیں۔") + "\n\n" +
                "✓ " + tr("For threats or emergencies, contact appropriate local authorities.",
                "دھمکی یا ایمرجنسی کی صورت میں متعلقہ مقامی حکام سے رابطہ کریں۔")
        ));

        Button privacy = outlineButton(
                tr("Privacy & Data Controls", "رازداری اور ڈیٹا کنٹرول"));
        Button delete = dangerButton(
                tr("Delete Local Account Data", "مقامی اکاؤنٹ ڈیٹا حذف کریں"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(privacy);
        addFull(trust);
        addFull(delete);
        addFull(back);

        privacy.setOnClickListener(v -> showPrivacy());
        trust.setOnClickListener(v -> showTrustFamily());
        delete.setOnClickListener(v -> confirmDelete());
        back.setOnClickListener(v -> showHome());
    }

    private void showPrivacy() {
        setupRoot(true);

        root.addView(title(
                tr("Privacy & Data", "رازداری اور ڈیٹا"), 28));

        root.addView(body(
                tr(
                        "Best Nikah Bridge should collect only information needed for matrimonial functionality. "
                                + "Production privacy controls must explain exactly what data is collected, why it is used, "
                                + "who receives it, retention periods and deletion procedures.\n\n"
                                + "This current client stores profile/demo information locally on the device. "
                                + "It does not claim that local storage is secure cloud account storage.\n\n"
                                + "Before Play publication, add a real public privacy-policy URL and keep the in-app policy consistent with the Play Console Data Safety declaration.",
                        "Best Nikah Bridge کو صرف نکاح کی بنیادی فعالیت کے لیے ضروری معلومات لینی چاہئیں۔ "
                                + "پروڈکشن privacy policy میں واضح ہونا چاہیے کہ کون سا ڈیٹا لیا جاتا ہے، کیوں استعمال ہوتا ہے، "
                                + "کس کے ساتھ شیئر ہوتا ہے، کتنی مدت رکھا جاتا ہے اور کیسے حذف ہوتا ہے۔\n\n"
                                + "یہ موجودہ کلائنٹ پروفائل/ڈیمو معلومات ڈیوائس پر مقامی طور پر رکھتا ہے۔ "
                                + "یہ محفوظ cloud account ہونے کا دعویٰ نہیں کرتا۔\n\n"
                                + "Play publication سے پہلے حقیقی public privacy-policy URL شامل کریں اور اسے Play Console Data Safety declaration کے مطابق رکھیں۔"
                )));

        Button terms = outlineButton(
                tr("Terms & Community Rules", "شرائط اور کمیونٹی اصول"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(terms);
        addFull(back);

        terms.setOnClickListener(v -> showTerms());
        back.setOnClickListener(v -> showSafety());
    }

    private void showTerms() {
        setupRoot(true);

        root.addView(title(
                tr("Terms & Community Rules", "شرائط اور کمیونٹی اصول"), 27));

        root.addView(body(
                "1. " + tr("Best Nikah Bridge is for serious matrimonial intent, not casual dating.",
                "Best Nikah Bridge سنجیدہ نکاح کے لیے ہے، casual dating کے لیے نہیں۔") + "\n\n" +
                "2. " + tr("Users must provide truthful information.",
                "صارفین کو درست معلومات فراہم کرنی چاہئیں۔") + "\n\n" +
                "3. " + tr("Harassment, scams, threats, impersonation and abusive content are prohibited.",
                "ہراسانی، فراڈ، دھمکیاں، جعلی شناخت اور بدسلوکی ممنوع ہیں۔") + "\n\n" +
                "4. " + tr("Do not request or send money through the platform.",
                "پلیٹ فارم کے ذریعے رقم نہ مانگیں اور نہ بھیجیں۔") + "\n\n" +
                "5. " + tr("Respect mutual consent and personal boundaries.",
                "باہمی رضامندی اور ذاتی حدود کا احترام کریں۔") + "\n\n" +
                "6. " + tr("Report unsafe behavior promptly.",
                "غیر محفوظ رویے کو فوراً رپورٹ کریں۔") + "\n\n" +
                "7. " + tr("Religious guidance should be obtained from qualified scholars.",
                "دینی رہنمائی اہلِ علم سے حاصل کی جائے۔")
        ));

        Button back = outlineButton(tr("Back", "واپس"));
        addFull(back);
        back.setOnClickListener(v -> showPrivacy());
    }

    private void showHelp() {
        setupRoot(true);

        root.addView(title(
                tr("Help & Nikah Guidance", "مدد اور نکاح رہنمائی"), 28));

        root.addView(body(
                "1. " + tr("Complete your profile honestly.",
                "اپنا پروفائل ایمانداری سے مکمل کریں۔") + "\n\n" +
                "2. " + tr("Set clear partner preferences.",
                "شریکِ حیات کی واضح ترجیحات طے کریں۔") + "\n\n" +
                "3. " + tr("Review profiles carefully.",
                "پروفائل احتیاط سے دیکھیں۔") + "\n\n" +
                "4. " + tr("Express Interest respectfully.",
                "احترام سے دلچسپی ظاہر کریں۔") + "\n\n" +
                "5. " + tr("Wait for mutual acceptance before chat.",
                "چیٹ سے پہلے باہمی رضامندی کا انتظار کریں۔") + "\n\n" +
                "6. " + tr("Use report/block when something feels unsafe.",
                "غیر محفوظ محسوس ہو تو رپورٹ/بلاک کریں۔") + "\n\n" +
                "7. " + tr("For religious questions, consult a trusted qualified scholar.",
                "دینی سوالات کے لیے معتبر اہلِ علم سے مشورہ کریں۔") + "\n\n" +
                "8. " + tr("For emergencies or threats, contact appropriate authorities.",
                "ایمرجنسی یا دھمکی کی صورت میں متعلقہ حکام سے رابطہ کریں۔")
        ));

        Button support = outlineButton(
                tr("Support / Help Line", "سپورٹ / ہیلپ لائن"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(support);
        addFull(back);

        support.setOnClickListener(v -> toast(
                "Production support should be connected to a real secure support channel.",
                "پروڈکشن سپورٹ کو حقیقی محفوظ سپورٹ چینل سے جوڑنا ہوگا۔"));

        back.setOnClickListener(v -> showHome());
    }

    private void showTrustFamily() {
        setupRoot(true);

        root.addView(title(
                tr("Trust & Family Mode", "اعتماد اور فیملی موڈ"), 28));
        root.addView(subtitle(tr(
                "Give serious matches a safer path toward family involvement without exposing private contact details.",
                "سنجیدہ رشتے کے لیے خاندان کی شمولیت کا محفوظ راستہ بنائیں، بغیر ذاتی رابطہ معلومات ظاہر کیے۔"
        )));

        CheckBox chaperone = new CheckBox(this);
        chaperone.setText(tr(
                "Enable Family / Wali Mode",
                "فیملی / ولی موڈ فعال کریں"));
        chaperone.setTextSize(16);
        chaperone.setTextColor(dark);
        chaperone.setChecked(prefs.getBoolean(K_CHAPERONE, false));
        root.addView(chaperone);

        EditText email = new EditText(this);
        email.setHint(tr(
                "Trusted family/wali email (optional)",
                "قابلِ اعتماد خاندان/ولی کا ای میل (اختیاری)"));
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setText(prefs.getString(K_CHAPERONE_EMAIL, ""));
        addInput(email, 80);

        String[] privacyOptions = {
                tr("Select photo visibility", "تصویر کی رازداری منتخب کریں"),
                tr("Visible to matches", "میچز کو نظر آئے"),
                tr("Private until mutual interest", "باہمی دلچسپی تک نجی"),
                tr("Private until I choose to reveal", "میری مرضی تک نجی")
        };
        Spinner photoPrivacy = addSpinner(privacyOptions);
        photoPrivacy.setSelection(indexOf(privacyOptions,
                prefs.getString(K_PHOTO_PRIVACY, "")));

        root.addView(body(tr(
                "Best practice: family participation should be voluntary and consent-based. In production, invitations and transcript access must be authenticated and server-controlled.",
                "بہتر طریقہ: خاندانی شمولیت رضاکارانہ اور رضامندی پر مبنی ہونی چاہیے۔ پروڈکشن میں دعوت اور چیٹ رسائی authenticated اور server-controlled ہونی چاہیے۔"
        )));

        Button save = appButton(tr("Save Trust Settings", "اعتماد کی سیٹنگز محفوظ کریں"));
        Button safety = outlineButton(tr("Open Safety Center", "سیفٹی سینٹر کھولیں"));
        Button back = outlineButton(tr("Back", "واپس"));
        addFull(save); addFull(safety); addFull(back);

        save.setOnClickListener(v -> {
            String emailText = email.getText().toString().trim();
            if (chaperone.isChecked() && !emailText.isEmpty() && !emailText.contains("@")) {
                email.setError(tr("Enter a valid email", "درست ای میل درج کریں"));
                return;
            }
            prefs.edit()
                    .putBoolean(K_CHAPERONE, chaperone.isChecked())
                    .putString(K_CHAPERONE_EMAIL, emailText)
                    .putString(K_PHOTO_PRIVACY, photoPrivacy.getSelectedItem().toString())
                    .apply();
            toast(
                    "Trust settings saved. Production family access must be server-authorized.",
                    "اعتماد کی سیٹنگز محفوظ ہوگئیں۔ پروڈکشن فیملی رسائی سرور سے مجاز ہونی چاہیے۔");
            showHome();
        });
        safety.setOnClickListener(v -> showSafety());
        back.setOnClickListener(v -> showHome());
    }

    private void showSettings() {
        setupRoot(true);

        root.addView(title(
                tr("Account & Settings", "اکاؤنٹ اور سیٹنگز"), 28));

        String current = prefs.getString(K_LANGUAGE, "English");

        Button language = outlineButton(
                "English".equals(current)
                        ? "Language: English"
                        : "Language: Urdu");

        Button messages = appButton(tr(
                "Watch Ad — 1 Demo Message",
                "اشتہار دیکھیں — 1 ڈیمو پیغام"));

        Button premium = outlineButton(
                prefs.getBoolean(K_PREMIUM, false)
                        ? tr("Premium Demo: ON", "پریمیم ڈیمو: آن")
                        : tr("Premium Demo: OFF", "پریمیم ڈیمو: آف"));

        Button privacy = outlineButton(
                tr("Privacy & Data", "رازداری اور ڈیٹا"));
        Button trust = outlineButton(
                tr("Trust & Family Mode", "اعتماد اور فیملی موڈ"));

        Button delete = dangerButton(
                tr("Delete Local Account Data", "مقامی اکاؤنٹ ڈیٹا حذف کریں"));

        Button back = outlineButton(tr("Back", "واپس"));

        root.addView(subtitle(tr(
                "These are local demo controls. Real authentication, billing, ads and entitlements require secure services.",
                "یہ مقامی ڈیمو کنٹرولز ہیں۔ حقیقی authentication، billing، ads اور entitlements کے لیے محفوظ سروسز ضروری ہیں۔"
        )));

        addFull(language);
        addFull(messages);
        addFull(premium);
        addFull(privacy);
        addFull(delete);
        addFull(back);

        language.setOnClickListener(v -> {
            prefs.edit().putString(K_LANGUAGE,
                    "English".equals(current) ? "Urdu" : "English").apply();
            showHome();
        });

        messages.setOnClickListener(v -> {
            if (prefs.getBoolean(K_PREMIUM, false)) {
                toast("Premium demo is ON.", "پریمیم ڈیمو آن ہے۔");
                return;
            }

            String day = prefs.getString(K_AD_DAY, "");
            int used = prefs.getInt(K_AD_USED, 0);

            if (!todayKey().equals(day)) {
                day = todayKey();
                used = 0;
            }

            if (used >= 2) {
                toast(
                        "Today's 2-message demo allowance is used.",
                        "آج کے 2 ڈیمو پیغامات کی حد پوری ہوچکی ہے۔"
                );
                return;
            }

            prefs.edit()
                    .putString(K_AD_DAY, day)
                    .putInt(K_AD_USED, used + 1)
                    .apply();

            toast(
                    "Demo message entitlement granted. Real rewarded ads must be integrated before production.",
                    "ڈیمو پیغام entitlement دیا گیا۔ پروڈکشن سے پہلے حقیقی rewarded ads شامل کرنا ضروری ہے۔"
            );
        });

        premium.setOnClickListener(v -> {
            boolean on = prefs.getBoolean(K_PREMIUM, false);
            prefs.edit().putBoolean(K_PREMIUM, !on).apply();

            toast(
                    "Local premium demo changed. Real digital purchases must use Google Play Billing.",
                    "مقامی پریمیم ڈیمو تبدیل ہوگیا۔ حقیقی ڈیجیٹل خریداری Google Play Billing سے ہونی چاہیے۔"
            );
            showSettings();
        });

        privacy.setOnClickListener(v -> showPrivacy());
        delete.setOnClickListener(v -> confirmDelete());
        back.setOnClickListener(v -> showHome());
    }

    private void confirmDelete() {
        setupRoot(true);

        root.addView(title(
                tr("Delete Account Data", "اکاؤنٹ ڈیٹا حذف کریں"), 27));

        root.addView(body(tr(
                "This current build can delete its local device data. A production account must also delete server-side account data and associated user data through the real backend.",
                "یہ موجودہ بلڈ ڈیوائس کا مقامی ڈیٹا حذف کرسکتا ہے۔ پروڈکشن اکاؤنٹ میں حقیقی بیک اینڈ کے ذریعے server-side account اور متعلقہ user data بھی حذف ہونا چاہیے۔"
        )));

        Button delete = dangerButton(
                tr("Delete Local Data", "مقامی ڈیٹا حذف کریں"));
        Button cancel = outlineButton(tr("Cancel", "منسوخ"));

        addFull(delete);
        addFull(cancel);

        delete.setOnClickListener(v -> {
            prefs.edit().clear().apply();

            sentInterests.clear();
            incomingInterests.clear();
            acceptedConnections.clear();
            blockedProfiles.clear();
            reportedProfiles.clear();

            toast("Local account data deleted.",
                    "مقامی اکاؤنٹ ڈیٹا حذف ہوگیا۔");

            showWelcome();
        });

        cancel.setOnClickListener(v -> showHome());
    }
}
