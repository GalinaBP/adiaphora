// Russian display labels for backend enum codes. The API keeps stable English codes; only the
// presentation is localised. Unknown codes fall back to the raw code so new backend values are
// never hidden.

const STATUS_RU: Record<string, string> = {
  DRAFT: 'Черновик',
  QUESTIONNAIRE_IN_PROGRESS: 'Анкета заполняется',
  READY_FOR_EVALUATION: 'Готово к оценке',
  SUBMITTED: 'Подано',
  NEEDS_INFORMATION: 'Нужна дополнительная информация',
  MANUAL_REVIEW_REQUIRED: 'Требуется проверка юристом',
  UNDER_REVIEW: 'На проверке',
  APPROVED_FOR_DOCUMENT_GENERATION: 'Одобрено: готовим документы',
  DOCUMENTS_READY: 'Документы готовы',
  COMPLETED: 'Завершено',
  CANCELLED: 'Отменено',
};

const ROUTE_RU: Record<string, string> = {
  NOT_EVALUATED: 'Не оценено',
  MFC_PRELIMINARY: 'Внесудебное (МФЦ), предварительно',
  COURT_PRELIMINARY: 'Судебное, предварительно',
  MANUAL_REVIEW: 'Нужна проверка юристом',
  INSUFFICIENT_INFORMATION: 'Недостаточно данных',
  NOT_CURRENTLY_RECOMMENDED: 'Пока не рекомендуется',
};

const REVIEW_STATUS_RU: Record<string, string> = {
  OPEN: 'Открыта',
  ASSIGNED: 'Назначена',
  WAITING_FOR_INFORMATION: 'Ожидает информации',
  IN_PROGRESS: 'В работе',
  APPROVED: 'Одобрена',
  REJECTED: 'Отклонена',
  CLOSED: 'Закрыта',
};

const CREDITOR_TYPE_RU: Record<string, string> = {
  BANK: 'Банк',
  MICROFINANCE: 'МФО',
  INDIVIDUAL: 'Физическое лицо',
  TAX_AUTHORITY: 'Налоговая',
  UTILITY: 'ЖКХ',
  OTHER: 'Другое',
};

const ASSET_TYPE_RU: Record<string, string> = {
  REAL_ESTATE: 'Недвижимость',
  VEHICLE: 'Транспорт',
  BANK_ACCOUNT: 'Банковский счёт',
  SECURITIES: 'Ценные бумаги',
  CASH: 'Наличные',
  RECEIVABLE: 'Дебиторская задолженность',
  MOVABLE_PROPERTY: 'Движимое имущество',
  OTHER: 'Другое',
};

const label = (map: Record<string, string>, code: string) => map[code] ?? code;

export const statusRu = (code: string) => label(STATUS_RU, code);
export const routeRu = (code: string) => label(ROUTE_RU, code);
export const reviewStatusRu = (code: string) => label(REVIEW_STATUS_RU, code);
export const creditorTypeRu = (code: string) => label(CREDITOR_TYPE_RU, code);
export const assetTypeRu = (code: string) => label(ASSET_TYPE_RU, code);
