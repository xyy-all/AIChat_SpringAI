import { createI18n } from 'vue-i18n'

// 导入语言包
import zhLocale from '../locales/zh.json'
import enLocale from '../locales/en.json'

interface LanguageMessages {
  [key: string]: any
}

const messages: Record<string, LanguageMessages> = {
  zh: zhLocale,
  en: enLocale
}

// 获取浏览器语言环境
const getBrowserLanguage = (): string => {
  const browserLang: string = navigator.language.toLowerCase()
  const lang: string = browserLang.indexOf('zh') > -1 ? 'zh' : 'en'
  return lang
}

// 从localStorage获取保存的语言设置，如果没有则使用浏览器默认语言
const savedLang: string | null = localStorage.getItem('lang')
const locale: string = savedLang || getBrowserLanguage()

const i18n = createI18n({
  legacy: false, // 使用 composition API
  locale: locale, // 设置默认语言
  fallbackLocale: 'en', // 回退语言
  messages, // 语言包
})

export default i18n