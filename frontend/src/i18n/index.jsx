import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import api from "../services/api";

const STORAGE_KEY = "verdixa_language";
const EXCLUDED_SELECTOR = ["code", "pre", "textarea", "script", "style", "[data-i18n-ignore]", ".monaco-editor", ".view-lines", ".view-line"].join(",");

// English UI text is the canonical key. Common Hindi and Telugu strings are bundled
// so core navigation remains translated when the external provider is disabled.
const bundled = {
  en: {},
  hi: {
    Language: "भाषा", "Search languages…": "भाषाएँ खोजें…", Analytics: "विश्लेषण",
    Assessments: "मूल्यांकन", "Assessment Studio": "मूल्यांकन स्टूडियो", Loading: "लोड हो रहा है",
    "Loading…": "लोड हो रहा है…", Refresh: "रीफ़्रेश", Live: "लाइव", Practice: "अभ्यास",
    Compete: "प्रतिस्पर्धा", Paths: "पथ", Daily: "दैनिक", Activity: "गतिविधि", Ranks: "रैंक",
    Profile: "प्रोफ़ाइल", "Sign out": "साइन आउट", Workspace: "कार्यस्थान", Administration: "प्रशासन",
    "Sign in": "साइन इन", "Start coding": "कोडिंग शुरू करें", "Create account": "खाता बनाएँ",
    Username: "उपयोगकर्ता नाम", Password: "पासवर्ड", Email: "ईमेल", "Forgot password?": "पासवर्ड भूल गए?",
    Submit: "जमा करें", Cancel: "रद्द करें", Save: "सहेजें", Search: "खोजें", Filter: "फ़िल्टर",
    Previous: "पिछला", Next: "अगला", Problems: "समस्याएँ", Contests: "प्रतियोगिताएँ",
    Submissions: "सबमिशन", Certificates: "प्रमाणपत्र", Users: "उपयोगकर्ता", Dashboard: "डैशबोर्ड",
    "No results found.": "कोई परिणाम नहीं मिला।", "Something went wrong.": "कुछ गलत हो गया।",
    "Back to Problems": "समस्याओं पर वापस जाएँ", "User navigation": "उपयोगकर्ता नेविगेशन",
  },
  te: {
    Language: "భాష", "Search languages…": "భాషలను వెతకండి…", Analytics: "విశ్లేషణలు",
    Assessments: "మూల్యాంకనాలు", "Assessment Studio": "అసెస్‌మెంట్ స్టూడియో", Loading: "లోడ్ అవుతోంది",
    "Loading…": "లోడ్ అవుతోంది…", Refresh: "రిఫ్రెష్", Live: "లైవ్", Practice: "అభ్యాసం",
    Compete: "పోటీ", Paths: "మార్గాలు", Daily: "రోజువారీ", Activity: "కార్యకలాపం", Ranks: "ర్యాంకులు",
    Profile: "ప్రొఫైల్", "Sign out": "సైన్ అవుట్", Workspace: "వర్క్‌స్పేస్", Administration: "నిర్వహణ",
    "Sign in": "సైన్ ఇన్", "Start coding": "కోడింగ్ ప్రారంభించండి", "Create account": "ఖాతా సృష్టించండి",
    Username: "వినియోగదారు పేరు", Password: "పాస్‌వర్డ్", Email: "ఈమెయిల్", "Forgot password?": "పాస్‌వర్డ్ మర్చిపోయారా?",
    Submit: "సమర్పించండి", Cancel: "రద్దు చేయండి", Save: "సేవ్ చేయండి", Search: "వెతకండి", Filter: "ఫిల్టర్",
    Previous: "మునుపటి", Next: "తదుపరి", Problems: "సమస్యలు", Contests: "పోటీలు",
    Submissions: "సమర్పణలు", Certificates: "ధృవపత్రాలు", Users: "వినియోగదారులు", Dashboard: "డ్యాష్‌బోర్డ్",
    "No results found.": "ఫలితాలు కనబడలేదు.", "Something went wrong.": "ఏదో తప్పు జరిగింది.",
    "Back to Problems": "సమస్యలకు తిరిగి వెళ్లండి", "User navigation": "వినియోగదారు నావిగేషన్",
  },
  // A compact offline pack keeps the core admin workspace useful when the
  // optional server-side translation provider is not configured.
  ja: {
    Language: "言語", "Choose your display language": "表示言語を選択", "Search languages…": "言語を検索…",
    Current: "現在", Languages: "言語", "No languages found": "言語が見つかりません",
    Administration: "管理", "Control Room / Overview": "管理室 / 概要", "Control room / overview": "管理室 / 概要", "Control Room": "管理室",
    Overview: "概要", Problems: "問題", Contests: "コンテスト", Users: "ユーザー", Collections: "コレクション",
    "Learning paths": "学習パス", "Daily challenges": "デイリーチャレンジ", "Creator verification": "作成者確認",
    "Sign out": "サインアウト", Refresh: "更新", "New problem": "新しい問題", "Platform operations": "プラットフォーム運用",
    "Content integrity, user activity, and judge outcomes from persisted platform data.": "保存済みのプラットフォームデータに基づくコンテンツ整合性、ユーザー活動、判定結果。",
    "Published problems": "公開済みの問題", "Published Problems": "公開済みの問題", "Total users": "総ユーザー数", "Total Users": "総ユーザー数", "Verified creators": "認証済み作成者", "Verified Users": "認証済みユーザー",
    "Active / 30d": "直近30日のアクティブ", Submissions: "提出", Accepted: "正解", "Contest registrations": "コンテスト登録", "Pending creators": "保留中の作成者", Acceptance: "正答率", "Public assessments": "公開評価", "Private assessments": "非公開評価",
    Live: "ライブ", "Last updated": "最終更新", "Certificates Issued": "発行済み証明書",
    "50 Problem Certificates": "50問証明書", "100 Problem Certificates": "100問証明書", "150 Problem Certificates": "150問証明書",
    "Total Certificates Issued": "証明書発行総数", "Recently managed problems": "最近管理した問題", "Full inventory": "すべての一覧", "User Management": "ユーザー管理",
    "Review identities, account roles, and user-level performance records.": "ユーザー情報、アカウント権限、パフォーマンス記録を確認します。",
    "Regular Users": "一般ユーザー", Administrators: "管理者", "Search by username or email...": "ユーザー名またはメールを検索…",
    User: "ユーザー", Email: "メール", Role: "権限", Actions: "操作", "Access role": "アクセス権限",
    "User role": "ユーザー権限", Admin: "管理者", "Analytics & Certificates": "分析と証明書",
    "Loading user registry": "ユーザー一覧を読み込み中", "No users found": "ユーザーが見つかりません",
    "No users match your search.": "検索条件に一致するユーザーはいません。", "There are no users to display.": "表示するユーザーはいません。",
    "Change this user's role to": "このユーザーの権限を変更します:", "Current role": "現在の権限", "Access controls": "アクセス管理",
    "Light": "ライト", "Dark": "ダーク", "Theme": "テーマ", Profile: "プロフィール", Dashboard: "ダッシュボード",
  },
};

const legacyKeys = {
  language: "Language", searchLanguages: "Search languages…", analytics: "Analytics",
  assessments: "Assessments", studio: "Assessment Studio", loading: "Loading…",
  refresh: "Refresh", live: "Live", empty: "Nothing to show yet.",
};

const I18nContext = createContext({ language: "en", setLanguage: () => {}, t: (key) => key });
const sourceForKey = (key) => legacyKeys[key] || key;

function isTranslatableText(value) {
  const text = value.trim();
  if (!text || text.length > 4000) return false;
  if (/^(https?:\/\/|\/api\/|[\w.+-]+@[\w.-]+\.[A-Za-z]{2,})/i.test(text)) return false;
  if (/^#?\d+(?:[.:/-]\d+)*$/.test(text)) return false;
  // Lowercase identifier-shaped values are normally usernames, slugs, IDs, or technical tokens.
  if (/^[a-z][a-z0-9_.-]{2,}$/.test(text)) return false;
  if (/^[{}[\]();<>:=+*/&|!]+$/.test(text)) return false;
  return true;
}

function preserveWhitespace(original, translated) {
  return `${original.match(/^\s*/)?.[0] || ""}${translated}${original.match(/\s*$/)?.[0] || ""}`;
}

export function I18nProvider({ children }) {
  const [language, setState] = useState(() => localStorage.getItem(STORAGE_KEY) || "en");
  const languageRef = useRef(language);
  const cacheRef = useRef(new Map());
  const inFlightRef = useRef(new Map());
  const sourceNodes = useRef(new WeakMap());
  const sourceAttributes = useRef(new WeakMap());

  const setLanguage = useCallback((code) => {
    setState(code);
    localStorage.setItem(STORAGE_KEY, code);
    if (localStorage.getItem("algosphere_token")) api.put("/users/me/language", { language: code }).catch(() => {});
  }, []);

  const t = useCallback((key) => {
    const source = sourceForKey(key);
    return bundled[language]?.[source] || cacheRef.current.get(`${language}\u0000${source}`) || source;
  }, [language]);

  useEffect(() => {
    languageRef.current = language;
    document.documentElement.lang = language;
    const root = document.getElementById("root") || document.body;
    if (!root) return undefined;
    let stopped = false;
    let timer;

    const translateBatch = async (texts, requestedLanguage) => {
      const missing = [...new Set(texts)].filter((text) => !bundled[requestedLanguage]?.[text] && !cacheRef.current.has(`${requestedLanguage}\u0000${text}`));
      const owned = missing.filter((text) => !inFlightRef.current.has(`${requestedLanguage}\u0000${text}`));
      for (let index = 0; index < owned.length; index += 50) {
        const batch = owned.slice(index, index + 50);
        const request = api.post("/i18n/translate-batch", { sourceLanguage: "en", targetLanguage: requestedLanguage, texts: batch })
          .then((response) => (response.data?.translations || []).forEach((translated, offset) => {
            if (typeof translated === "string" && translated) cacheRef.current.set(`${requestedLanguage}\u0000${batch[offset]}`, translated);
          })).catch(() => {}).finally(() => batch.forEach((text) => inFlightRef.current.delete(`${requestedLanguage}\u0000${text}`)));
        batch.forEach((text) => inFlightRef.current.set(`${requestedLanguage}\u0000${text}`, request));
      }
      await Promise.all(missing.map((text) => inFlightRef.current.get(`${requestedLanguage}\u0000${text}`)).filter(Boolean));
    };

    const collect = () => {
      const textEntries = [];
      const attributeEntries = [];
      const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
      while (walker.nextNode()) {
        const node = walker.currentNode;
        const parent = node.parentElement;
        if (!parent || parent.closest(EXCLUDED_SELECTOR)) continue;
        let record = sourceNodes.current.get(node);
        if (!record && isTranslatableText(node.nodeValue)) {
          record = { source: node.nodeValue, applied: null };
          sourceNodes.current.set(node, record);
        } else if (record && node.nodeValue !== record.applied && node.nodeValue !== record.source) {
          if (isTranslatableText(node.nodeValue)) {
            record.source = node.nodeValue;
            record.applied = null;
          } else {
            sourceNodes.current.delete(node);
            record = null;
          }
        }
        if (record) textEntries.push([node, record]);
      }
      root.querySelectorAll("[placeholder],[title],[aria-label]").forEach((element) => {
        if (element.closest(EXCLUDED_SELECTOR)) return;
        let records = sourceAttributes.current.get(element);
        if (!records) {
          records = {};
          sourceAttributes.current.set(element, records);
        }
        for (const name of ["placeholder", "title", "aria-label"]) {
          const value = element.getAttribute(name);
          if (!value) continue;
          const record = records[name];
          if (!record && isTranslatableText(value)) records[name] = { source: value, applied: null };
          else if (record && value !== record.applied && value !== record.source && isTranslatableText(value)) records[name] = { source: value, applied: null };
        }
        Object.entries(records).forEach(([name, record]) => attributeEntries.push([element, name, record]));
      });
      return { textEntries, attributeEntries };
    };

    const apply = async () => {
      const requestedLanguage = languageRef.current;
      const { textEntries, attributeEntries } = collect();
      const sources = [...textEntries.map(([, record]) => record.source.trim()), ...attributeEntries.map(([, , record]) => record.source.trim())];
      if (requestedLanguage !== "en") await translateBatch(sources, requestedLanguage);
      if (stopped || requestedLanguage !== languageRef.current) return;
      const translated = (source) => requestedLanguage === "en" ? source.trim() : bundled[requestedLanguage]?.[source.trim()] || cacheRef.current.get(`${requestedLanguage}\u0000${source.trim()}`) || source.trim();
      textEntries.forEach(([node, record]) => {
        const value = preserveWhitespace(record.source, translated(record.source));
        record.applied = value;
        if (node.nodeValue !== value) node.nodeValue = value;
      });
      attributeEntries.forEach(([element, name, record]) => {
        const value = translated(record.source);
        record.applied = value;
        if (element.getAttribute(name) !== value) element.setAttribute(name, value);
      });
    };

    const schedule = () => {
      clearTimeout(timer);
      timer = setTimeout(apply, 25);
    };
    const observer = new MutationObserver(schedule);
    observer.observe(root, { childList: true, subtree: true, characterData: true, attributes: true, attributeFilter: ["placeholder", "title", "aria-label"] });
    apply();
    return () => {
      stopped = true;
      clearTimeout(timer);
      observer.disconnect();
    };
  }, [language]);

  const value = useMemo(() => ({ language, setLanguage, t }), [language, setLanguage, t]);
  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export const useI18n = () => useContext(I18nContext);
