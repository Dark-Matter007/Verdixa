// Verdixa codes are mapped server-side to DeepL's exact target-language values.
// Hindi and Telugu remain available when provider metadata cannot be reached.
export const languageCodes = [
  "en", "te", "hi", "ar", "bn", "bg", "ca", "cs", "da", "de", "el", "es", "et", "fi", "fr", "he",
  "hu", "id", "it", "ja", "ko", "lt", "lv", "ms", "nl", "no", "pl", "pt-BR", "pt-PT", "ro", "ru",
  "sk", "sl", "sr", "sv", "ta", "th", "tr", "uk", "ur", "vi", "zh-HANS", "zh-HANT"
];

const nativeNames = {
  en: "English", te: "తెలుగు", hi: "हिन्दी", ar: "العربية", bn: "বাংলা", de: "Deutsch", es: "Español",
  fr: "Français", he: "עברית", ja: "日本語", ko: "한국어", "pt-BR": "Português (Brasil)", "pt-PT": "Português (Portugal)",
  ru: "Русский", ta: "தமிழ்", th: "ไทย", uk: "Українська", ur: "اردو", vi: "Tiếng Việt", "zh-HANS": "简体中文", "zh-HANT": "繁體中文"
};
const displayCode = (code) => code.replace("-HANS", "-Hans").replace("-HANT", "-Hant");

export function languageOptions() {
  const display = new Intl.DisplayNames(["en"], { type: "language" });
  return languageCodes.map((code) => ({ code, native: nativeNames[code] || display.of(displayCode(code)) || code, english: display.of(displayCode(code)) || code }))
    .sort((left, right) => left.english.localeCompare(right.english));
}
