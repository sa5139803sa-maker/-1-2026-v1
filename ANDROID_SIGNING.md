# إعداد توقيع Android الدائم

يجب استخدام **ملف keystore نفسه** في كل إصدار لاحق من التطبيق. لا تنشئ مفتاحاً جديداً في كل تشغيل لـ GitHub Actions؛ لأن Android لن يسمح بتثبيت APK موقّع بشهادة مختلفة كتحديث للتطبيق المثبت.

## إعداد مفتاح جديد للإصدارات المستقبلية

إذا كنت تستخدم المفتاح الذي تم تسليمه لك في المحادثة، فلا تنشئ مفتاحاً آخر. الملف الحالي هو المرجع الدائم. التعليمات التالية تخص إنشاء مفتاح بديل فقط إذا قررت بدء سلسلة توقيع جديدة:

```bash
keytool -genkeypair -v \
  -keystore sales-ledger-upload.jks \
  -storetype JKS \
  -alias salesledger \
  -keyalg RSA -keysize 2048 -validity 10000
```

حوّل الملف إلى Base64:

- Linux / macOS:
  ```bash
  base64 -w 0 sales-ledger-upload.jks
  ```
- PowerShell:
  ```powershell
  [Convert]::ToBase64String([IO.File]::ReadAllBytes("sales-ledger-upload.jks"))
  ```

أضف القيم التالية في **GitHub → Settings → Secrets and variables → Actions → New repository secret**:

| Secret | القيمة |
|---|---|
| `ANDROID_SIGNING_KEYSTORE_BASE64` | محتوى Base64 للملف |
| `ANDROID_SIGNING_STORE_PASSWORD` | كلمة مرور keystore |
| `ANDROID_SIGNING_KEY_ALIAS` | `salesledger` |
| `ANDROID_SIGNING_KEY_PASSWORD` | كلمة مرور المفتاح |

بعد الإعداد شغّل workflow باسم **Build Android APK** من تبويب Actions. احتفظ بملف keystore وكلمات المرور خارج المستودع ولا ترفعها إلى Git.

## ملاحظة مهمة حول النسخة الأولى

أول APK في هذا المستودع بُني بمفتاح Debug أُنشئ مؤقتاً داخل GitHub Actions، ولم يُحفظ كملف أو Secret. لذلك لا يمكن استخدام المفتاح الجديد كتحديث لذلك APK. يلزم ملف keystore الأصلي المطابق لشهادة النسخة الأولى كي يسمح Android بالتحديث فوقها. لا تحذف النسخة المثبتة أو تمسح بياناتها قبل تأمين نسخة احتياطية من بياناتها.

## ترحيل قاعدة البيانات

قاعدة البيانات المحلية اسمها `sales_ledger_db`. ترحيل Room من الإصدار 1 إلى 2 يضيف جدول المصروفات وفهارسه دون حذف جداول المبيعات والدفعات. يجب أن يبقى `applicationId` ثابتاً وأن تزداد `versionCode` في كل إصدار قابل للتحديث. يضبط المشروع `versionCode` تلقائياً اعتماداً على رقم تشغيل GitHub Actions.
