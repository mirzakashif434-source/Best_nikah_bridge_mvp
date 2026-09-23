package com.nikahbridge;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Production 14-language UI localization: offline critical pack + authenticated Azure translation + local cache. */
public final class LanguageManager {
    public static final String[] NAMES={
        "English","اردو","العربية","বাংলা","हिन्दी","Türkçe",
        "Bahasa Indonesia","Melayu","ਪੰਜਾਬੀ","فارسی","Français","Deutsch","Español","Italiano"
    };
    public static final String[] CODES={"en","ur","ar","bn","hi","tr","id","ms","pa","fa","fr","de","es","it"};

    private static final String PREFS="best_nikah_bridge_language", KEY="language_code";
    private static final String CACHE_PREFS="best_nikah_bridge_translation_cache_v1";
    private static final Map<String,String[]> T=new HashMap<>();

    public static final int NO_TRANSLATE_TAG=0x7f0b7a01;
    private static final int SOURCE_TAG=0x7f0b7a02;
    private static final int RENDERED_TAG=0x7f0b7a03;
    private static final int HINT_SOURCE_TAG=0x7f0b7a04;
    private static final int HINT_RENDERED_TAG=0x7f0b7a05;

    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final Object LOCK=new Object();
    private static final Map<String,LinkedHashMap<String,List<TranslationCallback>>> PENDING=new HashMap<>();
    private static final Set<String> SCHEDULED=new LinkedHashSet<>();
    private static Context appContext;

    private interface TranslationCallback{void done(String translated);}

    static{
        put("Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge","Best Nikah Bridge");
        put("Back","واپس","رجوع","পেছনে","वापस","Geri","Kembali","Kembali","ਵਾਪਸ","بازگشت","Retour","Zurück","Atrás","Indietro");
        put("Refresh","تازہ کریں","تحديث","রিফ্রেশ","रीफ्रेश","Yenile","Segarkan","Muat semula","ਤਾਜ਼ਾ ਕਰੋ","تازه‌سازی","Actualiser","Aktualisieren","Actualizar","Aggiorna");
        put("Get Started","شروع کریں","ابدأ","শুরু করুন","शुरू करें","Başla","Mulai","Mula","ਸ਼ੁਰੂ ਕਰੋ","شروع کنید","Commencer","Loslegen","Comenzar","Inizia");
        put("Sign In","لاگ اِن","تسجيل الدخول","সাইন ইন","साइन इन","Giriş Yap","Masuk","Log Masuk","ਸਾਈਨ ਇਨ","ورود","Se connecter","Anmelden","Iniciar sesión","Accedi");
        put("Cancel","منسوخ","إلغاء","বাতিল","रद्द करें","İptal","Batal","Batal","ਰੱਦ ਕਰੋ","لغو","Annuler","Abbrechen","Cancelar","Annulla");
        put("Choose Your Language","اپنی زبان منتخب کریں","اختر لغتك","আপনার ভাষা বেছে নিন","अपनी भाषा चुनें","Dilinizi Seçin","Pilih Bahasa Anda","Pilih Bahasa Anda","ਆਪਣੀ ਭਾਸ਼ਾ ਚੁਣੋ","زبان خود را انتخاب کنید","Choisissez votre langue","Sprache wählen","Elige tu idioma","Scegli la tua lingua");
        put("Welcome","خوش آمدید","مرحبًا","স্বাগতম","स्वागत है","Hoş Geldiniz","Selamat Datang","Selamat Datang","ਜੀ ਆਇਆਂ ਨੂੰ","خوش آمدید","Bienvenue","Willkommen","Bienvenido","Benvenuto");
        put("A trusted Muslim matrimonial platform","ایک قابلِ اعتماد مسلم ازدواجی پلیٹ فارم","منصة زواج إسلامية موثوقة","বিশ্বস্ত মুসলিম বিবাহ প্ল্যাটফর্ম","विश्वसनीय मुस्लिम वैवाहिक मंच","Güvenilir Müslüman evlilik platformu","Platform pernikahan Muslim tepercaya","Platform perkahwinan Muslim yang dipercayai","ਭਰੋਸੇਯੋਗ ਮੁਸਲਿਮ ਵਿਆਹ ਪਲੇਟਫਾਰਮ","پلتفرم معتبر ازدواج مسلمانان","Plateforme matrimoniale musulmane de confiance","Vertrauenswürdige muslimische Eheplattform","Plataforma matrimonial musulmana de confianza","Piattaforma matrimoniale musulmana affidabile");
        put("Meaningful Matches","بامعنی رشتے","توافقات هادفة","অর্থবহ মিল","सार्थक रिश्ते","Anlamlı Eşleşmeler","Kecocokan Bermakna","Padanan Bermakna","ਅਰਥਪੂਰਨ ਰਿਸ਼ਤੇ","تطابق‌های معنادار","Correspondances sérieuses","Bedeutsame Matches","Coincidencias significativas","Incontri significativi");
        put("Verified Profiles","تصدیق شدہ پروفائلز","ملفات موثقة","যাচাইকৃত প্রোফাইল","सत्यापित प्रोफ़ाइल","Doğrulanmış Profiller","Profil Terverifikasi","Profil Disahkan","ਤਸਦੀਕਸ਼ੁਦਾ ਪ੍ਰੋਫਾਈਲ","پروفایل‌های تأییدشده","Profils vérifiés","Verifizierte Profile","Perfiles verificados","Profili verificati");
        put("Private & Safe","نجی اور محفوظ","خاص وآمن","ব্যক্তিগত ও নিরাপদ","निजी और सुरक्षित","Gizli ve Güvenli","Privat & Aman","Peribadi & Selamat","ਨਿੱਜੀ ਅਤੇ ਸੁਰੱਖਿਅਤ","خصوصی و امن","Privé et sûr","Privat & sicher","Privado y seguro","Privato e sicuro");
        put("Halal Nikah • Trust • Family • Privacy","حلال نکاح • اعتماد • خاندان • رازداری","نكاح حلال • ثقة • أسرة • خصوصية","হালাল নিকাহ • আস্থা • পরিবার • গোপনীয়তা","हलाल निकाह • भरोसा • परिवार • गोपनीयता","Helal Nikah • Güven • Aile • Gizlilik","Nikah Halal • Kepercayaan • Keluarga • Privasi","Nikah Halal • Kepercayaan • Keluarga • Privasi","ਹਲਾਲ ਨਿਕਾਹ • ਭਰੋਸਾ • ਪਰਿਵਾਰ • ਪਰਾਈਵੇਸੀ","نکاح حلال • اعتماد • خانواده • حریم خصوصی","Nikah halal • Confiance • Famille • Confidentialité","Halal Nikah • Vertrauen • Familie • Privatsphäre","Nikah halal • Confianza • Familia • Privacidad","Nikah halal • Fiducia • Famiglia • Privacy");
        put("Discover","تلاش","اكتشف","খুঁজুন","खोजें","Keşfet","Temukan","Terokai","ਖੋਜੋ","کشف","Découvrir","Entdecken","Descubrir","Scopri");
        put("Send Interest","دلچسپی بھیجیں","إرسال اهتمام","আগ্রহ পাঠান","रुचि भेजें","İlgi Gönder","Kirim Minat","Hantar Minat","ਦਿਲਚਸਪੀ ਭੇਜੋ","ارسال علاقه","Envoyer un intérêt","Interesse senden","Enviar interés","Invia interesse");
        put("Interest Sent","دلچسپی بھیج دی گئی","تم إرسال الاهتمام","আগ্রহ পাঠানো হয়েছে","रुचि भेजी गई","İlgi Gönderildi","Minat Terkirim","Minat Dihantar","ਦਿਲਚਸਪੀ ਭੇਜੀ ਗਈ","علاقه ارسال شد","Intérêt envoyé","Interesse gesendet","Interés enviado","Interesse inviato");
        put("Privacy Control Center","پرائیویسی کنٹرول سینٹر","مركز التحكم بالخصوصية","প্রাইভেসি কন্ট্রোল সেন্টার","प्राइवेसी कंट्रोल सेंटर","Gizlilik Kontrol Merkezi","Pusat Kontrol Privasi","Pusat Kawalan Privasi","ਪਰਾਈਵੇਸੀ ਕੰਟਰੋਲ ਸੈਂਟਰ","مرکز کنترل حریم خصوصی","Centre de confidentialité","Datenschutzkontrolle","Centro de privacidad","Centro privacy");
        put("Identity Verification","شناخت کی تصدیق","التحقق من الهوية","পরিচয় যাচাই","पहचान सत्यापन","Kimlik Doğrulama","Verifikasi Identitas","Pengesahan Identiti","ਪਛਾਣ ਤਸਦੀਕ","تأیید هویت","Vérification d'identité","Identitätsprüfung","Verificación de identidad","Verifica identità");
        put("Family / Wali Connect","فیملی / ولی کنیکٹ","ربط الأسرة / الولي","পরিবার / ওয়ালি সংযোগ","परिवार / वली कनेक्ट","Aile / Veli Bağlantısı","Keluarga / Wali","Keluarga / Wali","ਪਰਿਵਾਰ / ਵਲੀ ਕਨੈਕਟ","اتصال خانواده / ولی","Famille / Wali","Familie / Wali","Familia / Wali","Famiglia / Wali");
        put("Safe Communication","محفوظ رابطہ","تواصل آمن","নিরাপদ যোগাযোগ","सुरक्षित संचार","Güvenli İletişim","Komunikasi Aman","Komunikasi Selamat","ਸੁਰੱਖਿਅਤ ਸੰਚਾਰ","ارتباط امن","Communication sûre","Sichere Kommunikation","Comunicación segura","Comunicazione sicura");
        put("Go Premium","پریمیم لیں","الترقية إلى بريميوم","প্রিমিয়াম নিন","प्रीमियम लें","Premium'a Geç","Jadi Premium","Naik Taraf Premium","ਪ੍ਰੀਮੀਅਮ ਲਵੋ","ارتقا به پریمیوم","Passer Premium","Premium werden","Pasar a Premium","Passa a Premium");
        put("Wallet","والیٹ","المحفظة","ওয়ালেট","वॉलेट","Cüzdan","Dompet","Dompet","ਵਾਲਿਟ","کیف پول","Portefeuille","Wallet","Cartera","Portafoglio");
        put("Account & Privacy","اکاؤنٹ اور پرائیویسی","الحساب والخصوصية","অ্যাকাউন্ট ও গোপনীয়তা","खाता और गोपनीयता","Hesap ve Gizlilik","Akun & Privasi","Akaun & Privasi","ਖਾਤਾ ਅਤੇ ਪਰਾਈਵੇਸੀ","حساب و حریم خصوصی","Compte et confidentialité","Konto & Datenschutz","Cuenta y privacidad","Account e privacy");
        put("Blocked Members","بلاک کیے گئے ممبرز","الأعضاء المحظورون","ব্লক করা সদস্য","ब्लॉक किए सदस्य","Engellenen Üyeler","Anggota Diblokir","Ahli Disekat","ਬਲਾਕ ਮੈਂਬਰ","اعضای مسدود","Membres bloqués","Blockierte Mitglieder","Miembros bloqueados","Membri bloccati");
        put("Community Chat","کمیونٹی چیٹ","دردشة المجتمع","কমিউনিটি চ্যাট","कम्युनिटी चैट","Topluluk Sohbeti","Chat Komunitas","Sembang Komuniti","ਕਮਿਊਨਿਟੀ ਚੈਟ","گفتگوی جامعه","Chat communautaire","Community-Chat","Chat comunitario","Chat comunitaria");
        put("Premium Plans — 20 / 40 / 60 SAR","پریمیم پلان — 20 / 40 / 60 ریال","خطط بريميوم — 20 / 40 / 60 ر.س","প্রিমিয়াম প্ল্যান — 20 / 40 / 60 SAR","प्रीमियम प्लान — 20 / 40 / 60 SAR","Premium Planlar — 20 / 40 / 60 SAR","Paket Premium — 20 / 40 / 60 SAR","Pelan Premium — 20 / 40 / 60 SAR","ਪ੍ਰੀਮੀਅਮ ਪਲਾਨ — 20 / 40 / 60 SAR","پلن پریمیوم — 20 / 40 / 60 SAR","Plans Premium — 20 / 40 / 60 SAR","Premium-Pläne — 20 / 40 / 60 SAR","Planes Premium — 20 / 40 / 60 SAR","Piani Premium — 20 / 40 / 60 SAR");
        put("Microsoft / Azure Sign in","Microsoft / Azure لاگ اِن","تسجيل الدخول عبر Microsoft / Azure","Microsoft / Azure সাইন ইন","Microsoft / Azure साइन इन","Microsoft / Azure Giriş","Masuk Microsoft / Azure","Log Masuk Microsoft / Azure","Microsoft / Azure ਸਾਈਨ ਇਨ","ورود Microsoft / Azure","Connexion Microsoft / Azure","Microsoft / Azure Anmeldung","Inicio de sesión Microsoft / Azure","Accesso Microsoft / Azure");
        put("Connecting to Microsoft Entra External ID…","Microsoft Entra External ID سے رابطہ ہو رہا ہے…","جارٍ الاتصال بـ Microsoft Entra External ID…","Microsoft Entra External ID-এর সাথে সংযোগ হচ্ছে…","Microsoft Entra External ID से कनेक्ट हो रहा है…","Microsoft Entra External ID bağlanıyor…","Menghubungkan ke Microsoft Entra External ID…","Menyambung ke Microsoft Entra External ID…","Microsoft Entra External ID ਨਾਲ ਕਨੈਕਟ ਹੋ ਰਿਹਾ ਹੈ…","در حال اتصال به Microsoft Entra External ID…","Connexion à Microsoft Entra External ID…","Verbindung mit Microsoft Entra External ID…","Conectando con Microsoft Entra External ID…","Connessione a Microsoft Entra External ID…");
        put("Continue with Microsoft / Azure","Microsoft / Azure کے ساتھ جاری رکھیں","المتابعة باستخدام Microsoft / Azure","Microsoft / Azure দিয়ে চালিয়ে যান","Microsoft / Azure के साथ जारी रखें","Microsoft / Azure ile devam et","Lanjutkan dengan Microsoft / Azure","Teruskan dengan Microsoft / Azure","Microsoft / Azure ਨਾਲ ਜਾਰੀ ਰੱਖੋ","ادامه با Microsoft / Azure","Continuer avec Microsoft / Azure","Mit Microsoft / Azure fortfahren","Continuar con Microsoft / Azure","Continua con Microsoft / Azure");
        put("Azure External ID is ready. Sign in or create your real account.","Azure External ID تیار ہے۔ لاگ اِن کریں یا اپنا حقیقی اکاؤنٹ بنائیں۔","Azure External ID جاهز. سجّل الدخول أو أنشئ حسابك الحقيقي.","Azure External ID প্রস্তুত। সাইন ইন করুন বা আপনার আসল অ্যাকাউন্ট তৈরি করুন।","Azure External ID तैयार है। साइन इन करें या अपना वास्तविक खाता बनाएं।","Azure External ID hazır. Giriş yapın veya gerçek hesabınızı oluşturun.","Azure External ID siap. Masuk atau buat akun asli Anda.","Azure External ID sedia. Log masuk atau cipta akaun sebenar anda.","Azure External ID ਤਿਆਰ ਹੈ। ਸਾਈਨ ਇਨ ਕਰੋ ਜਾਂ ਆਪਣਾ ਅਸਲੀ ਖਾਤਾ ਬਣਾਓ।","Azure External ID آماده است. وارد شوید یا حساب واقعی خود را بسازید.","Azure External ID est prêt. Connectez-vous ou créez votre vrai compte.","Azure External ID ist bereit. Melden Sie sich an oder erstellen Sie Ihr echtes Konto.","Azure External ID está listo. Inicia sesión o crea tu cuenta real.","Azure External ID è pronto. Accedi o crea il tuo account reale.");
        put("Azure sign-in cancelled.","Azure لاگ اِن منسوخ ہو گیا۔","تم إلغاء تسجيل الدخول إلى Azure.","Azure সাইন ইন বাতিল হয়েছে।","Azure साइन इन रद्द हुआ।","Azure girişi iptal edildi.","Masuk Azure dibatalkan.","Log masuk Azure dibatalkan.","Azure ਸਾਈਨ ਇਨ ਰੱਦ ਹੋ ਗਿਆ।","ورود Azure لغو شد.","Connexion Azure annulée.","Azure-Anmeldung abgebrochen.","Inicio de sesión de Azure cancelado.","Accesso Azure annullato.");
        put("Real Azure login verified.","حقیقی Azure لاگ اِن کی تصدیق ہو گئی۔","تم التحقق من تسجيل دخول Azure الحقيقي.","আসল Azure লগইন যাচাই হয়েছে।","वास्तविक Azure लॉगिन सत्यापित हुआ।","Gerçek Azure girişi doğrulandı.","Login Azure asli terverifikasi.","Log masuk Azure sebenar disahkan.","ਅਸਲੀ Azure ਲਾਗਇਨ ਤਸਦੀਕ ਹੋ ਗਿਆ।","ورود واقعی Azure تأیید شد.","Connexion Azure réelle vérifiée.","Echte Azure-Anmeldung verifiziert.","Inicio de sesión real de Azure verificado.","Accesso Azure reale verificato.");

    }

    private LanguageManager(){}
    public static void init(Context c){if(c!=null)appContext=c.getApplicationContext();}
    public static String currentCode(){return appContext==null?"en":currentCode(appContext);}
    private static void put(String en,String... values){T.put(en,values);}

    public static String currentCode(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"en");}
    public static int currentIndex(Context c){String code=currentCode(c);for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code))return i;return 0;}
    public static void select(Context c,int index){if(index<0||index>=CODES.length)return;c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,CODES[index]).apply();}
    public static void apply(Activity a){Locale locale=new Locale(currentCode(a));Locale.setDefault(locale);Configuration config=new Configuration(a.getResources().getConfiguration());config.setLocale(locale);config.setLayoutDirection(locale);a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());}
    public static String currentName(Context c){return NAMES[currentIndex(c)];}

    /** Synchronous offline translation for critical/pre-auth UI. Unknown strings fall back to English until cached Azure translation arrives. */
    public static String tr(Context c,String english){
        if(english==null)return "";
        int i=currentIndex(c);if(i==0)return english;
        String[] a=T.get(english);
        return a!=null&&i-1<a.length?a[i-1]:english;
    }

    /** Localized AlertDialog builder. Chained .show() automatically translates the dialog window, including list items and buttons. */
    public static AlertDialog.Builder dialog(Context context){
        return new LocalizedDialogBuilder(context);
    }

    private static final class LocalizedDialogBuilder extends AlertDialog.Builder{
        private final Context context;
        LocalizedDialogBuilder(Context c){super(c);context=c;}
        @Override public AlertDialog show(){
            AlertDialog d=super.show();
            if(d.getWindow()!=null){
                View root=d.getWindow().getDecorView();
                localizeTree(context,root);
                root.getViewTreeObserver().addOnGlobalLayoutListener(()->{
                    if(d.isShowing())localizeTree(context,root);
                });
            }
            return d;
        }
    }

    /** Drop-in Toast.makeText replacement. Translation is resolved before the toast is shown. */
    public static Toast toast(Context context,CharSequence text,int duration){
        return new DeferredToast(context,text==null?"":text.toString(),duration);
    }

    private static final class DeferredToast extends Toast{
        private final Context context; private final String source; private final int duration;
        DeferredToast(Context c,String s,int d){super(c.getApplicationContext());context=c.getApplicationContext();source=s;duration=d;}
        @Override public void show(){
            requestTranslation(context,source,translated->Toast.makeText(context,translated,duration).show());
        }
    }

    /** Localized form validation error. */
    public static void setError(TextView view,CharSequence error){
        if(view==null)return;
        String source=error==null?"":error.toString();
        requestTranslation(view.getContext(),source,translated->view.setError(translated));
    }

    /** Mark member/user-generated text so automatic UI localization never sends it for translation. */
    public static void protectUserContent(TextView view){
        if(view!=null)view.setTag(NO_TRANSLATE_TAG,Boolean.TRUE);
    }

    public static void localizeTree(Context c,View v){
        if(c==null||v==null||currentIndex(c)==0)return;
        if(appContext==null)appContext=c.getApplicationContext();
        if(v instanceof EditText){
            localizeHint(c,(EditText)v);
        }else if(v instanceof TextView){
            localizeText(c,(TextView)v);
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;
            for(int i=0;i<g.getChildCount();i++)localizeTree(c,g.getChildAt(i));
        }
    }

    private static void localizeText(Context c,TextView t){
        if(Boolean.TRUE.equals(t.getTag(NO_TRANSLATE_TAG)))return;
        String current=t.getText()==null?"":t.getText().toString();
        if(current.trim().isEmpty())return;
        String rendered=(String)t.getTag(RENDERED_TAG);
        String source=(String)t.getTag(SOURCE_TAG);
        if(rendered==null||!current.equals(rendered)){
            source=current;
            t.setTag(SOURCE_TAG,source);
            t.setTag(RENDERED_TAG,current);
        }
        if(source==null||source.trim().isEmpty())return;
        String offline=tr(c,source);
        if(!offline.equals(source)){
            if(!offline.equals(current)){t.setText(offline);t.setTag(RENDERED_TAG,offline);}
            return;
        }
        final String sourceFinal=source;
        requestTranslation(c,sourceFinal,translated->{
            Object stored=t.getTag(SOURCE_TAG);
            if(stored!=null&&sourceFinal.equals(stored.toString())&&!translated.equals(sourceFinal)){
                t.setText(translated);t.setTag(RENDERED_TAG,translated);
            }
        });
    }

    private static void localizeHint(Context c,EditText e){
        CharSequence h=e.getHint();if(h==null||h.toString().trim().isEmpty())return;
        String current=h.toString();
        String rendered=(String)e.getTag(HINT_RENDERED_TAG);
        String source=(String)e.getTag(HINT_SOURCE_TAG);
        if(rendered==null||!current.equals(rendered)){
            source=current;e.setTag(HINT_SOURCE_TAG,source);e.setTag(HINT_RENDERED_TAG,current);
        }
        if(source==null)return;
        String offline=tr(c,source);
        if(!offline.equals(source)){
            if(!offline.equals(current)){e.setHint(offline);e.setTag(HINT_RENDERED_TAG,offline);}
            return;
        }
        final String sourceFinal=source;
        requestTranslation(c,sourceFinal,translated->{
            Object stored=e.getTag(HINT_SOURCE_TAG);
            if(stored!=null&&sourceFinal.equals(stored.toString())&&!translated.equals(sourceFinal)){
                e.setHint(translated);e.setTag(HINT_RENDERED_TAG,translated);
            }
        });
    }

    private static void requestTranslation(Context c,String source,TranslationCallback callback){
        String target=currentCode(c);
        if("en".equals(target)||!shouldAutoTranslate(source)){callback.done(source);return;}
        if(appContext==null)appContext=c.getApplicationContext();
        String cached=cache(appContext).getString(cacheKey(target,source),null);
        if(cached!=null&&!cached.trim().isEmpty()){callback.done(cached);return;}
        if(!AzureAuthManager.hasAccount(c)){callback.done(source);return;}

        synchronized(LOCK){
            LinkedHashMap<String,List<TranslationCallback>> byText=PENDING.get(target);
            if(byText==null){byText=new LinkedHashMap<>();PENDING.put(target,byText);}
            List<TranslationCallback> callbacks=byText.get(source);
            if(callbacks==null){callbacks=new ArrayList<>();byText.put(source,callbacks);}
            callbacks.add(callback);
            if(!SCHEDULED.contains(target)){
                SCHEDULED.add(target);
                final String lang=target;
                MAIN.postDelayed(()->flush(lang),140);
            }
        }
    }

    private static void flush(String target){
        final LinkedHashMap<String,List<TranslationCallback>> batch=new LinkedHashMap<>();
        synchronized(LOCK){
            LinkedHashMap<String,List<TranslationCallback>> all=PENDING.get(target);
            if(all==null||all.isEmpty()){SCHEDULED.remove(target);return;}
            int n=0;
            for(Map.Entry<String,List<TranslationCallback>> e:new ArrayList<>(all.entrySet())){
                batch.put(e.getKey(),e.getValue());all.remove(e.getKey());
                if(++n>=30)break;
            }
            if(all.isEmpty())SCHEDULED.remove(target);
            else MAIN.postDelayed(()->flush(target),180);
        }

        try{
            JSONArray texts=new JSONArray();
            for(String s:batch.keySet())texts.put(s);
            JSONObject body=new JSONObject().put("target",target).put("texts",texts);
            AzureApiClient.post("/localization/translate",body.toString(),new AzureApiClient.Callback(){
                public void ok(int code,String response){
                    try{
                        JSONArray out=new JSONObject(response).optJSONArray("translations");
                        if(out==null||out.length()!=batch.size()){failBatch(batch);return;}
                        int i=0;
                        SharedPreferences.Editor editor=cache(appContext).edit();
                        for(Map.Entry<String,List<TranslationCallback>> e:batch.entrySet()){
                            String source=e.getKey(),translated=out.optString(i++,source);
                            if(translated==null||translated.trim().isEmpty())translated=source;
                            editor.putString(cacheKey(target,source),translated);
                            final String value=translated;
                            MAIN.post(()->{for(TranslationCallback cb:e.getValue())cb.done(value);});
                        }
                        editor.apply();
                    }catch(Exception e){failBatch(batch);}
                }
                public void err(String message){failBatch(batch);}
            });
        }catch(Exception e){failBatch(batch);}
    }

    private static void failBatch(LinkedHashMap<String,List<TranslationCallback>> batch){
        MAIN.post(()->{
            for(Map.Entry<String,List<TranslationCallback>> e:batch.entrySet())
                for(TranslationCallback cb:e.getValue())cb.done(e.getKey());
        });
    }

    private static SharedPreferences cache(Context c){return c.getSharedPreferences(CACHE_PREFS,Context.MODE_PRIVATE);}

    private static String cacheKey(String target,String source){
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest((target+"\u0001"+source).getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder("t_");
            for(byte x:digest)b.append(String.format(Locale.US,"%02x",x));
            return b.toString();
        }catch(Exception e){return "t_"+target+"_"+Integer.toHexString(source.hashCode());}
    }

    /** Conservative filter: translate interface copy, never obvious secrets/IDs/URLs/member contact data. */
    private static boolean shouldAutoTranslate(String s){
        if(s==null)return false;
        String x=s.trim();
        if(x.length()<2||x.length()>500)return false;
        if(!x.matches(".*[A-Za-z].*"))return false;
        String lower=x.toLowerCase(Locale.US);
        if(lower.startsWith("http://")||lower.startsWith("https://")||lower.startsWith("api://")||lower.startsWith("/api/"))return false;
        if(x.contains("@")||x.contains("{")||x.contains("}")||x.contains("[")||x.contains("]"))return false;
        if(x.matches(".*\\b[0-9a-fA-F]{8}-[0-9a-fA-F-]{20,}\\b.*"))return false;
        if(x.matches(".*\\b\\d{8,}\\b.*"))return false;
        if(lower.contains("bearer ")||lower.contains("access_token")||lower.contains("id_token")||lower.contains("password"))return false;
        return true;
    }
}
