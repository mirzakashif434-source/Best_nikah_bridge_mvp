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

            toast("Profile saved • " + profileCompletion() + "% complete",
                    "پروفائل محفوظ • " + profileCompletion() + "% مکمل");
            showHome();
        });

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

        if (!myCountry.isEmpty()
                && DEMO_COUNTRIES[i].equalsIgnoreCase(myCountry)) {
            score += 8;
        }

        if (!pref.isEmpty()
                && (pref.contains(DEMO_COUNTRIES[i].toLowerCase(Locale.US))
                || pref.contains(DEMO_CITIES[i].toLowerCase(Locale.US)))) {
            score += 7;
        }

        if (myIntention.contains("serious")
                || myIntention.contains("ready")) {
            score += 4;
        }

        String timeline = prefs.getString(K_TIMELINE, "").toLowerCase(Locale.US);
        if (timeline.contains("6 months")
                || timeline.contains("as soon")) {
            score += 3;
        }

        String familyPref = prefs.getString(K_FAMILY, "").toLowerCase(Locale.US);
        if (familyPref.contains("family")
                || familyPref.contains("wali")) {
            score += 2;
        }

        String age = prefs.getString(K_AGE, "");
        try {
            int mine = Integer.parseInt(age);
            int theirs = Integer.parseInt(DEMO_AGES[i]);
            int diff = Math.abs(mine - theirs);
            if (diff <= 3) score += 7;
            else if (diff <= 7) score += 4;
        } catch (Exception ignored) {
        }

        score += (i % 3);
        if (score > 97) score = 97;
        if (score < 60) score = 60;
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
        setupRoot(true);

        root.addView(title(
                tr("Recommended Matches", "تجویز کردہ میچز"), 28));

        if (!prefs.getBoolean(K_PROFILE, false)) {
            root.addView(subtitle(tr(
                    "Complete your profile before viewing recommended matches.",
                    "تجویز کردہ میچز دیکھنے سے پہلے پروفائل مکمل کریں۔"
            )));
            Button create = appButton(tr("Create Profile", "پروفائل بنائیں"));
            Button back = outlineButton(tr("Back", "واپس"));
            addFull(create);
            addFull(back);
            create.setOnClickListener(v -> showProfile());
            back.setOnClickListener(v -> showHome());
            return;
        }

        root.addView(subtitle(tr(
                "Compatibility is a client-side demo score in this build. A production server must calculate matches from real user data.",
                "اس بلڈ میں compatibility صرف ڈیمو اسکور ہے۔ پروڈکشن سرور کو حقیقی صارف ڈیٹا سے میچ بنانا ہوگا۔"
        )));

        boolean any = false;

        for (int i = 0; i < DEMO_NAMES.length; i++) {
            String key = profileKey(i);
            if (blockedProfiles.contains(key)) continue;

            any = true;
            addMatchCard(i);
        }

        if (!any) {
            root.addView(subtitle(tr(
                    "No profiles are currently visible.",
                    "اس وقت کوئی پروفائل نظر نہیں آ رہا۔"
            )));
        }

        Button back = outlineButton(tr("Back", "واپس"));
        addFull(back);
        back.setOnClickListener(v -> showHome());
    }

    private void addMatchCard(int i) {
        String key = profileKey(i);
        int score = calculateCompatibility(i);

        LinearLayout c = card();

        c.addView(cardText(
                DEMO_NAMES[i] + " • " + DEMO_AGES[i], 21, dark));

        c.addView(cardText(
                "● " + tr("Demo profile • Not verified",
                        "ڈیمو پروفائل • تصدیق شدہ نہیں"),
                13, warning));

        c.addView(cardText(
                DEMO_CITIES[i] + ", " + DEMO_COUNTRIES[i], 15, gray));

        c.addView(cardText(
                tr("Compatibility: ", "مطابقت: ") + score + "%",
                18, primary));

        c.addView(cardText(
                "✓ " + DEMO_REASONS[i], 15, dark));

        String myTimeline = prefs.getString(K_TIMELINE, "");
        if (!myTimeline.isEmpty()) {
            c.addView(cardText(
                    tr("Marriage timeline: ", "نکاح کی مدت: ") + myTimeline,
                    14, gray));
        }

        c.addView(cardText(
                DEMO_ABOUT[i], 15, gray));

        Button view = outlineButton(tr("View Full Profile", "مکمل پروفائل دیکھیں"));
        Button interest = appButton(sentInterests.contains(key)
                ? tr("Interest Sent ✓", "دلچسپی بھیجی ✓")
                : tr("Express Interest", "دلچسپی ظاہر کریں"));
        Button report = dangerButton(tr("Report / Block", "رپورٹ / بلاک"));

        if (sentInterests.contains(key)) {
            interest.setEnabled(false);
            interest.setAlpha(0.65f);
        }

        c.addView(view);
        c.addView(interest);
        c.addView(report);

        view.setOnClickListener(v -> showProfileDetails(i));
        interest.setOnClickListener(v -> {
            sentInterests.add(key);
            saveSets();

            toast(
                    "Interest saved. Real mutual acceptance must be server-enforced before chat.",
                    "دلچسپی محفوظ ہوگئی۔ حقیقی چیٹ سے پہلے باہمی رضامندی سرور پر نافذ ہونی چاہیے۔"
            );
            showMatches();
        });

        report.setOnClickListener(v -> showReportBlock(i));
    }

    private void showProfileDetails(int i) {
        setupRoot(true);

        String key = profileKey(i);
        int score = calculateCompatibility(i);

        root.addView(title(
                DEMO_NAMES[i] + " • " + DEMO_AGES[i], 28));

        root.addView(body(
                tr(
                        "Demo profile — not a real verified user.\n\n"
                                + "Location: " + DEMO_CITIES[i] + ", " + DEMO_COUNTRIES[i]
                                + "\n\nCompatibility: " + score + "%"
                                + "\n\nWhy this may fit:\n✓ " + DEMO_REASONS[i]
                                + "\n\nAbout:\n" + DEMO_ABOUT[i]
                                + "\n\nSafety:\nKeep phone numbers, passwords, OTPs and private documents private until a genuine mutual connection.",
                        "ڈیمو پروفائل — حقیقی تصدیق شدہ صارف نہیں۔\n\n"
                                + "مقام: " + DEMO_CITIES[i] + ", " + DEMO_COUNTRIES[i]
                                + "\n\nمطابقت: " + score + "%"
                                + "\n\nممکنہ مطابقت:\n✓ " + DEMO_REASONS[i]
                                + "\n\nتعارف:\n" + DEMO_ABOUT[i]
                                + "\n\nحفاظت:\nحقیقی باہمی رابطے تک فون نمبر، پاس ورڈ، OTP اور نجی دستاویزات محفوظ رکھیں۔"
                )));

        Button interest = appButton(sentInterests.contains(key)
                ? tr("Interest Sent ✓", "دلچسپی بھیجی ✓")
                : tr("Express Interest", "دلچسپی ظاہر کریں"));

        Button chat = outlineButton(
                tr("Safe Chat (Mutual Only)", "محفوظ چیٹ (صرف باہمی رضامندی)"));

        Button report = dangerButton(tr("Report / Block", "رپورٹ / بلاک"));
        Button back = outlineButton(tr("Back to Matches", "میچز پر واپس"));

        addFull(interest);
        addFull(chat);
        addFull(report);
        addFull(back);

        if (sentInterests.contains(key)) {
            interest.setEnabled(false);
            interest.setAlpha(0.65f);
        }

        interest.setOnClickListener(v -> {
            sentInterests.add(key);
            saveSets();
            toast("Interest sent. Wait for mutual acceptance.",
                    "دلچسپی بھیج دی گئی۔ باہمی رضامندی کا انتظار کریں۔");
            showProfileDetails(i);
        });

        chat.setOnClickListener(v -> {
            if (acceptedConnections.contains(key)) {
                showChat(i);
            } else {
                toast(
                        "Chat is locked. Both sides must accept the connection.",
                        "چیٹ بند ہے۔ دونوں طرف سے رابطہ قبول ہونا ضروری ہے۔"
                );
            }
        });

        report.setOnClickListener(v -> showReportBlock(i));
        back.setOnClickListener(v -> showMatches());
    }

    private void showInterests() {
        setupRoot(true);

        root.addView(title(
                tr("Interests & Connections", "دلچسپیاں اور روابط"), 28));
        root.addView(subtitle(tr(
                "A genuine connection should be mutual, respectful and safe.",
                "حقیقی رابطہ باہمی، باوقار اور محفوظ ہونا چاہیے۔"
        )));

        section(tr("Sent Interests", "بھیجی گئی دلچسپیاں"));

        if (sentInterests.isEmpty()) {
            root.addView(body(tr(
                    "No interests sent yet.",
                    "ابھی کوئی دلچسپی نہیں بھیجی گئی۔"
            )));
        } else {
            for (String key : sentInterests) {
                root.addView(body("✓ " + key.replace("|", " • ")));
            }
        }

        section(tr("Demo Incoming Interests", "ڈیمو موصول ہونے والی دلچسپیاں"));

        if (incomingInterests.isEmpty()) {
            root.addView(body(tr(
                    "No incoming demo interests.",
                    "کوئی موصول شدہ ڈیمو دلچسپی نہیں۔"
            )));
        } else {
            for (String key : incomingInterests) {
                LinearLayout c = card();
                c.addView(cardText(key.replace("|", " • "), 16, dark));

                Button accept = appButton(
                        tr("Accept Connection", "رابطہ قبول کریں"));
                Button decline = outlineButton(
                        tr("Decline", "انکار"));

                c.addView(accept);
                c.addView(decline);

                accept.setOnClickListener(v -> {
                    acceptedConnections.add(key);
                    saveSets();
                    toast(
                            "Connection accepted in local demo. Production authorization must be server-side.",
                            "مقامی ڈیمو میں رابطہ قبول ہوگیا۔ پروڈکشن اجازت سرور پر ہونی چاہیے۔"
                    );
                    showInterests();
                });

                decline.setOnClickListener(v -> {
                    incomingInterests.remove(key);
                    saveSets();
                    showInterests();
                });
            }
        }

        section(tr("Accepted Connections", "قبول شدہ روابط"));

        if (acceptedConnections.isEmpty()) {
            root.addView(body(tr(
                    "No accepted connections yet.",
                    "ابھی کوئی قبول شدہ رابطہ نہیں۔"
            )));
        } else {
            for (String key : acceptedConnections) {
                LinearLayout c = card();
                c.addView(cardText(
                        "✓ " + key.replace("|", " • "), 16, dark));

                Button open = outlineButton(
                        tr("Open Safe Chat", "محفوظ چیٹ کھولیں"));
                c.addView(open);

                open.setOnClickListener(v -> {
                    String[] parts = key.split("\\|");
                    if (parts.length >= 3) {
                        int idx = findDemo(parts[0], parts[1], parts[2]);
                        if (idx >= 0) showChat(idx);
                    }
                });
            }
        }

        Button clear = dangerButton(
                tr("Clear Local Interest History", "مقامی دلچسپی کی تاریخ صاف کریں"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(clear);
        addFull(back);

        clear.setOnClickListener(v -> {
            sentInterests.clear();
            incomingInterests.clear();
            acceptedConnections.clear();
            saveSets();
            toast("Local connection history cleared.",
                    "مقامی رابطوں کی تاریخ صاف ہوگئی۔");
            showInterests();
        });

        back.setOnClickListener(v -> showHome());
    }

    private int findDemo(String name, String age, String country) {
        for (int i = 0; i < DEMO_NAMES.length; i++) {
            if (DEMO_NAMES[i].equals(name)
                    && DEMO_AGES[i].equals(age)
                    && DEMO_COUNTRIES[i].equals(country)) {
                return i;
            }
        }
        return -1;
    }

    private void showChat(int i) {
        setupRoot(true);

        String key = profileKey(i);

        if (!acceptedConnections.contains(key)) {
            root.addView(title(tr("Safe Chat Locked", "محفوظ چیٹ بند ہے"), 28));
            root.addView(body(tr(
                    "Chat is available only after mutual acceptance.",
                    "چیٹ صرف باہمی رضامندی کے بعد دستیاب ہے۔"
            )));
            Button back = outlineButton(tr("Back", "واپس"));
            addFull(back);
            back.setOnClickListener(v -> showInterests());
            return;
        }

        root.addView(title(
                tr("Safe Chat • ", "محفوظ چیٹ • ") + DEMO_NAMES[i], 27));

        root.addView(subtitle(tr(
                "Local demo chat. Production chat must use authenticated server authorization.",
                "مقامی ڈیمو چیٹ۔ پروڈکشن چیٹ کو محفوظ سرور اجازت درکار ہے۔"
        )));

        if (prefs.getBoolean(K_CHAPERONE, false)) {
            root.addView(body(tr(
                    "Family / Wali Mode is enabled for this account.",
                    "اس اکاؤنٹ کے لیے Family / Wali Mode فعال ہے۔")));
        }

        String chatKey = "chat_" + key;
        String history = prefs.getString(chatKey, "");

        if (history.isEmpty()) {
            root.addView(body(tr(
                    "No messages yet. Keep communication respectful and avoid sharing passwords, OTPs, money or private documents.",
                    "ابھی کوئی پیغام نہیں۔ گفتگو باوقار رکھیں اور پاس ورڈ، OTP، رقم یا نجی دستاویزات شیئر نہ کریں۔"
            )));
        } else {
            root.addView(body(history));
        }

        EditText message = new EditText(this);
        message.setHint(tr("Write a respectful message...", "باوقار پیغام لکھیں..."));
        message.setGravity(Gravity.TOP);
        message.setMinLines(3);
        message.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        addInput(message, 120);

        Button send = appButton(tr("Send Message", "پیغام بھیجیں"));
        Button report = dangerButton(tr("Report / Block", "رپورٹ / بلاک"));
        Button back = outlineButton(tr("Back", "واپس"));

        addFull(send);
        addFull(report);
        addFull(back);

        send.setOnClickListener(v -> {
            String text = message.getText().toString().trim();

            if (text.isEmpty()) {
                toast("Write a message first.", "پہلے پیغام لکھیں۔");
                return;
            }

            if (!consumeMessageAllowance()) {
                toast(
                        "Demo daily message allowance is finished. Production ads/entitlements must be server-controlled.",
                        "ڈیمو روزانہ پیغام کی حد ختم ہوگئی۔ پروڈکشن ads/entitlement سرور سے کنٹرول ہونے چاہیے۔"
                );
                return;
            }

            String stamp = new SimpleDateFormat(
                    "HH:mm", Locale.US).format(new Date());

            String old = prefs.getString(chatKey, "");
            String updated = old
                    + (old.isEmpty() ? "" : "\n\n")
                    + tr("You", "آپ") + " • " + stamp + "\n" + text;

            prefs.edit().putString(chatKey, updated).apply();
            showChat(i);
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
