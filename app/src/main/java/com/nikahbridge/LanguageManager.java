package com.nikahbridge;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Production global language preference and shared UI localization layer. */
public final class LanguageManager {
    public static final String[] NAMES={"English","اردو","العربية","বাংলা","हिन्दी","Türkçe","Bahasa Indonesia","Melayu","ਪੰਜਾਬੀ","فارسی","Français","Deutsch","Español","Italiano"};
    public static final String[] CODES={"en","ur","ar","bn","hi","tr","id","ms","pa","fa","fr","de","es","it"};
    private static final String PREFS="best_nikah_bridge_language", KEY="language_code";
    private static final Map<String,String[]> T=new HashMap<>();
    static{
        put("Back","واپس","رجوع","পেছনে","वापस","Geri","Kembali","Kembali","ਵਾਪਸ","بازگشت","Retour","Zurück","Atrás","Indietro");
        put("Refresh","تازہ کریں","تحديث","রিফ্রেশ","रीफ्रेश","Yenile","Segarkan","Muat semula","ਤਾਜ਼ਾ ਕਰੋ","تازه‌سازی","Actualiser","Aktualisieren","Actualizar","Aggiorna");
        put("Get Started","شروع کریں","ابدأ","শুরু করুন","शुरू करें","Başla","Mulai","Mula","ਸ਼ੁਰੂ ਕਰੋ","شروع کنید","Commencer","Loslegen","Comenzar","Inizia");
        put("Sign In","لاگ اِن","تسجيل الدخول","সাইন ইন","साइन इन","Giriş Yap","Masuk","Log Masuk","ਸਾਈਨ ਇਨ","ورود","Se connecter","Anmelden","Iniciar sesión","Accedi");
        put("Choose Your Language","اپنی زبان منتخب کریں","اختر لغتك","আপনার ভাষা বেছে নিন","अपनी भाषा चुनें","Dilinizi Seçin","Pilih Bahasa Anda","Pilih Bahasa Anda","ਆਪਣੀ ਭਾਸ਼ਾ ਚੁਣੋ","زبان خود را انتخاب کنید","Choisissez votre langue","Sprache wählen","Elige tu idioma","Scegli la tua lingua");
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
    }
    private LanguageManager(){}
    private static void put(String en,String... values){T.put(en,values);}
    public static String currentCode(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"en");}
    public static int currentIndex(Context c){String code=currentCode(c);for(int i=0;i<CODES.length;i++)if(CODES[i].equals(code))return i;return 0;}
    public static void select(Context c,int index){if(index<0||index>=CODES.length)return;c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,CODES[index]).apply();}
    public static void apply(Activity a){Locale locale=new Locale(currentCode(a));Locale.setDefault(locale);Configuration config=new Configuration(a.getResources().getConfiguration());config.setLocale(locale);config.setLayoutDirection(locale);a.getResources().updateConfiguration(config,a.getResources().getDisplayMetrics());}
    public static String currentName(Context c){return NAMES[currentIndex(c)];}
    public static String tr(Context c,String english){if(english==null)return "";int i=currentIndex(c);if(i==0)return english;String[] a=T.get(english);return a!=null&&i-1<a.length?a[i-1]:english;}
    public static void localizeTree(Context c,View v){
        if(v instanceof TextView){TextView t=(TextView)v;CharSequence cs=t.getText();if(cs!=null){String s=cs.toString();String translated=tr(c,s);if(!translated.equals(s))t.setText(translated);}}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)localizeTree(c,g.getChildAt(i));}
    }
}
